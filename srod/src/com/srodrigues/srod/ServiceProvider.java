package com.srodrigues.srod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.layer.java.MethodRequest;
import com.srodrigues.srod.layer.java.MethodResponse;
import com.srodrigues.srod.layer.java.Reflection;

public class ServiceProvider {
	
	private static final Map<String, Object> services = Collections.synchronizedMap(new HashMap<String, Object>());

	public static void register(final String name, final Object implementation) {
		services.put(name, implementation);
	}

	public static void unregister(final String name) {
		services.remove(name);
	}

	public static Object getService(final String name){
		return services.get(name);
	}
	
	public static MethodResponse invoke(final int id, final Object service , final MethodRequest request) {
		
		//find correct method.
		final Object[]        requestArgs = request.getArgs();
		final String      requestedMethod = request.getMethod();
		final int          parameterCount = ( requestArgs == null ) ? 0 : requestArgs.length ;
		final Set<Method>         methods = new HashSet<Method>();

		for (final Method method : service.getClass().getMethods() ) {
			if (requestedMethod.equals( method.getName() ) && parameterCount == method.getParameterTypes().length ){
				methods.add(method);
			}
		}

		for (final Method method : methods ) {
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			
			final Object[] args = Reflection.convert( method.getParameterTypes(), requestArgs );
			try {
				return new MethodResponse (id, method.invoke( service, args ) ) ;
			} catch (IllegalArgumentException e) {
				// try next;
			} catch (IllegalAccessException e) {
				// try next;
			} catch (InvocationTargetException e) {
				//invoke exception -> exception is in Cause 
				return new MethodResponse (id, e.getCause() ) ;
			}
		}
		throw new SRODException( requestedMethod + " failed to invoke (unknown method by parameters) found: " + methods.size() + " candidates"  );
	}

}
