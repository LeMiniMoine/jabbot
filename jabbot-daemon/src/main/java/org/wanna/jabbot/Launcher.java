package org.wanna.jabbot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.wanna.jabbot.binding.Binding;
import org.wanna.jabbot.binding.ConnectionFactory;
import org.wanna.jabbot.binding.config.BindingDefinition;
import org.wanna.jabbot.config.JabbotConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;

/**
 * @author vmorsiani <vmorsiani>
 * @since 2014-05-29
 */
public class Launcher implements Daemon{
	final Logger logger = LoggerFactory.getLogger(Launcher.class);
	private final static String CONFIG_FILE = "jabbot.json";

	private Jabbot jabbot;

	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException{
		logger.info("initializing...");
		//Install slf4j bridge
		SLF4JBridgeHandler.uninstall();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(CONFIG_FILE);
		JabbotConfiguration jabbotConfiguration = newConfiguration(is);

		jabbot = newInstance(jabbotConfiguration,newConnectionFactory(jabbotConfiguration.getBindings()));
		logger.info("initialization completed.");
	}

	@Override
	public void start() throws Exception {
		jabbot.connect();
	}

	@Override
	public void stop() throws Exception {
		logger.info("Stopping Jabbot service");
		jabbot.disconnect();
	}

	@Override
	public void destroy() {

	}

	private JabbotConfiguration newConfiguration(InputStream is){
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return mapper.readValue(is,JabbotConfiguration.class);
		} catch (IOException e) {
			logger.error("unable to read json config file",e);
			return null;
		}

	}

	public Jabbot newInstance(JabbotConfiguration configuration,
							  ConnectionFactory connectionFactory){
		Jabbot jabbot = new Jabbot(configuration);
		jabbot.setConnectionFactory(connectionFactory);
		return jabbot;
	}

	public ConnectionFactory newConnectionFactory(Collection<BindingDefinition> bindings){
		ConnectionFactory factory =  new JabbotConnectionFactory();
		for (BindingDefinition binding : bindings) {
			try {
				Class clazz = Class.forName(String.valueOf(binding.getClassName()));
					Class<? extends Binding> connectionClass = (Class<? extends Binding>)clazz;
					logger.info("registering {} binding with class {}",binding.getName(),binding.getClassName());
					factory.register(binding.getName(),connectionClass);
			} catch (ClassNotFoundException e) {
				logger.error("unable to register {} binding with class {}",binding.getName(),binding.getClassName());
			}
		}
		return factory;
	}

	/**
	 * Main method allowing to easily run the bot in an IDE
	 *
	 * @param args command line args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Launcher launcher = new Launcher();
		launcher.init(null);
		launcher.start();

		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw e;
			}
		}

	}
}
