package org.wanna.jabbot.binding.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wanna.jabbot.binding.AbstractRoom;
import org.wanna.jabbot.binding.BindingListener;
import org.wanna.jabbot.binding.BindingMessage;
import org.wanna.jabbot.binding.config.RoomConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vmorsiani <vmorsiani>
 * @since 2014-08-14
 */
public class CliRoom extends AbstractRoom<Object> implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(CliRoom.class);
	private RoomConfiguration configuration;
	private final List<BindingListener> listeners;


	public CliRoom(CliBinding connection,List<BindingListener> listeners) {
		super(connection);
		this.listeners = (listeners==null?new ArrayList<BindingListener>() : listeners);

	}

	@Override
	public void run()
	{
		System.out.println("CLI Room started");
		BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				String line=buffer.readLine();
				logger.debug("received {}",line);

				if(line == null) {
					logger.error("Got a null, probably no console, dying now");
					return;
				}

				for (BindingListener listener : listeners) {
					BindingMessage message = new BindingMessage(this.getRoomName(),"cli",line);
					listener.onMessage((CliBinding)connection,message);
				}
			} catch (IOException e) {
				logger.error("IO Error reading sdtin, dying");
				return;
			}
		}
	}

	@Override
	public RoomConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public boolean sendMessage(String message) {
		System.out.println(message);
		return true;
	}

	@Override
	public boolean join(final RoomConfiguration configuration) {
		this.configuration = configuration;
		new Thread(this).start();
		return true;
	}

	@Override
	public String getRoomName() {
		return configuration.getName();
	}
}
