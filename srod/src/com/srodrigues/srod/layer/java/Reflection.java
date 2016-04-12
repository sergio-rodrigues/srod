package com.srodrigues.srod.layer.java;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reflection {

	private static final String IS_PREFIX = "is";
	private static final String GET_PREFIX = "get";
	private static final String SET_PREFIX = "set";

	public static Object[] convert(final Class<?>[] parameterTypes, final Object[] args) {
		if (args == null) {
			return null;
		}

		final List<Object> cache = new ArrayList<Object>();
		final Object[] parameters = new Object[args.length];

		for (int i = 0; i < parameterTypes.length; i++) {
			parameters[i] = convert(parameterTypes[i], args[i], cache);
		}

		return parameters;
	}

	public static Object convert(final Class<?> clazz, final Object object) {
		return convert(clazz, object, new ArrayList<Object>());
	}

	protected static Object convert(final Class<?> clazz, final Object object, final List<Object> cache) {

		// if object is null return null
		if (object == null) {
			return null;
		}

		// if is an object.. return the object (can't do more)
		if (clazz == Object.class) {
			return object;
		}

		// arrays are converted to arrayList by json
		if (clazz.isArray()) {
			if (object instanceof List<?>) {

				final List<?> arr = (List<?>) object;
				final int length = arr.size();
				final Class<?> componentType = clazz.getComponentType();

				final Object ret = Array.newInstance(componentType, length);

				for (int i = 0; i < length; ++i) {
					final Object object2 = arr.get(i);
					Array.set(ret, i, convert(componentType, object2, cache));
				}
				return ret;
			}
			// if not.. noting to do...
			return object;
		}

		// this -> replace this:xsxsx with object
		if (clazz == Class.class) {
			try {
				final String value = object.toString();

				// primitive types
				if (value.equals("byte")) {
					return Byte.TYPE;
				}
				if (value.equals("short")) {
					return Short.TYPE;
				}
				if (value.equals("int")) {
					return Integer.TYPE;
				}
				if (value.equals("long")) {
					return Long.TYPE;
				}
				if (value.equals("float")) {
					return Float.TYPE;
				}
				if (value.equals("double")) {
					return Double.TYPE;
				}
				if (value.equals("char")) {
					return Character.TYPE;
				}
				if (value.equals("boolean")) {
					return Boolean.TYPE;
				}

				return Class.forName(value);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		// Number Hierarchy
		if (clazz == Boolean.class || clazz == boolean.class) {
			return Boolean.parseBoolean(object.toString());
		}
		if (clazz == Byte.class || clazz == byte.class) {
			return Byte.parseByte(object.toString());
		}
		if (clazz == Double.class || clazz == double.class) {
			return Double.parseDouble(object.toString());
		}
		if (clazz == Float.class || clazz == float.class) {
			return Float.parseFloat(object.toString());
		}
		if (clazz == Integer.class || clazz == int.class) {
			return Integer.parseInt(object.toString());
		}
		if (clazz == Long.class || clazz == long.class) {
			return Long.parseLong(object.toString());
		}
		if (clazz == Short.class || clazz == short.class) {
			return Short.parseShort(object.toString());
		}
		if (Number.class.isAssignableFrom(clazz)) {
			if (clazz == BigDecimal.class) {
				return new BigDecimal(object.toString());
			}
			if (clazz == BigInteger.class) {
				return new BigInteger(object.toString());
			}
			if (clazz == AtomicInteger.class) {
				return new AtomicInteger(Integer.parseInt(object.toString()));
			}
			if (clazz == AtomicLong.class) {
				return new AtomicLong(Long.parseLong(object.toString()));
			}

		}

		// String & Char
		if (clazz == String.class) {
			return object.toString();
		} // String->toString = String
		if (clazz == Character.class || clazz == char.class) {
			return object.toString().charAt(0);
		}

		// process bean
		if (object instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> attributes = (Map<Object, Object>) object;
			Object tag = attributes.get("this");
			if (tag != null) {
				// Check already converted object // Cyclic redundance
				int id = Integer.valueOf(tag.toString());
				try {
					return cache.get(id - 1);
				} catch (IndexOutOfBoundsException e) {
					throw new RuntimeException(
							"invalid Attribute in Map<?,?>, No java Object can have an \"this\" Attribute");
				}
			}

			try {
				final Object t = clazz.newInstance();
				cache.add(t);

				final Field[] fields = clazz.getDeclaredFields();
				for (final Field field : fields) {
					final String element = field.getName();
					if (attributes.containsKey(element)) {
						final Class<?> type = field.getType();
						final Method method = clazz.getDeclaredMethod(SET_PREFIX + element.substring(0, 1).toUpperCase() + element.substring(1), type);
						if (method != null) {
							method.invoke(t, convert(type, attributes.get(element), cache));
						}
					}
				}
				return t;
			} catch (InstantiationException e) {
				e.printStackTrace();
				return null;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return null;
			} catch (NegativeArraySizeException e) {
				e.printStackTrace();
				return null;
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return null;
			} catch (SecurityException e) {
				e.printStackTrace();
				return null;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return null;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				return null;
			}
		}
		return object;
	}

	public static Map<String, Method> getGetters(final Class<?> clazz) {
		final Map<String, Method> ret = new HashMap<String, Method>();

		final Method methodList[] = clazz.getMethods();
		for (final Method method : methodList) {
			if ( method != null && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers())) {
				final String name = method.getName();
				final Class<?> argTypes[] = method.getParameterTypes();
				final Class<?> resultType = method.getReturnType();
				final int argCount = argTypes.length;

				if (argCount == 0) {
					if (name.startsWith(GET_PREFIX)) {
						ret.put(name.substring(3,4).toLowerCase() + name.substring(4), method);// Simple getter
					} else if (resultType == boolean.class && name.startsWith(IS_PREFIX)) {
						ret.put(name.substring(2,3).toLowerCase() + name.substring(3), method);// Boolean getter
					}
				}
			}
		}
		return ret;
	}

	public static Map<String, Object> getAttributes(Object object) {
		final Map<String, Object> res = new HashMap<String, Object>();
		final Map<String, Method> getters = getGetters(object.getClass());
		for (Entry<String, Method> entry : getters.entrySet()) {
			final Method method = entry.getValue();
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			try {
				res.put(entry.getKey(), method.invoke(object, (Object[]) null));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		final Field[] ff = object.getClass().getFields();
        for (final Field field : ff) {
        	try {
				res.put(field.getName(), field.get(object));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
        }
        res.remove("class");
		return res;
	}
}