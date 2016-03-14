package com.srodrigues.srod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientLocator {
	private static final List<InitialContext> contextes = Collections.synchronizedList( new ArrayList<InitialContext>());
	private static final Map<String, Object>   services = Collections.synchronizedMap( new HashMap<String, Object>() );

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T getService(final String name, final Class<T> clazz) {
		Object service = services.get(name);
		if (service == null) {
			for (final InitialContext context : contextes) {
				service = context.lookup(name, clazz);
				if (service != null) {
					if (!services.containsKey(name)) {
						services.put(name, service);
					}					
					break;
				}
			}
		}
		
		return ( T ) service ;
	}

	public static void addContext(final InitialContext... context){
		for (final InitialContext initialClientContext : context) {
			contextes.add(initialClientContext);
		}
	}
}
