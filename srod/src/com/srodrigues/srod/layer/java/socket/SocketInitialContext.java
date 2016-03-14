package com.srodrigues.srod.layer.java.socket;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import com.srodrigues.srod.InitialContext;
import com.srodrigues.srod.layer.java.ClientProxy;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;
import com.srodrigues.srod.layer.transport.socket.SocketProtocol;

public class SocketInitialContext implements InitialContext {

	private final SocketProtocol protocol;

	public SocketInitialContext(final String host, final int port, final MarshallingProtocol protocol) {
		this.protocol = new SocketProtocol(protocol, host, port);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T lookup(final String name, final Class<T> clazz) {
		return (T) Proxy.newProxyInstance(  clazz.getClassLoader(),
											new Class<?>[] { clazz },
											new ClientProxy(name, protocol)
										 );
	}
}