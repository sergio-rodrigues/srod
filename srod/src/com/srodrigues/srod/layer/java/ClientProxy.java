package com.srodrigues.srod.layer.java;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.layer.transport.TransportProtocol;

public class ClientProxy implements InvocationHandler {
	
	private static final AtomicInteger id = new AtomicInteger();

	private final String name;
	private final TransportProtocol protocol;

	public ClientProxy(final String name, final TransportProtocol protocol) {
		this.protocol = protocol;
		this.name     = name;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object               value = null ;
		int cid = id.incrementAndGet();
		try {
			final MethodRequest  rpcRequest  = new MethodRequest(name, cid, method.getName(), args);
			final MethodResponse rpcResponse = protocol.sendRequest( rpcRequest ) ;
			                     value = Reflection.convert(method.getReturnType(), rpcResponse.getValue());
		} catch (Exception e) {
			throw new SRODException(e);
		}

		if ( value instanceof Exception ) {
			throw new SRODException( (Exception) value );
		}

		return value;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
