package com.srodrigues.srod.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

public class JSONWriter {
	private static final byte[]  NULL =  "null".getBytes() ;
	private static final byte[] FALSE = "false".getBytes() ;
	private static final byte[]  TRUE =  "true".getBytes() ;

	private int id = 0; 

	private final Stack<Object[]> calls = new Stack<Object[]>();
	
	private final OutputStream output;

	public JSONWriter(final OutputStream output) {
		this.output = output;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void write(final Object object) throws IOException {
		if (object == null ) {
			output.write(NULL);
		} else {
			for(Object[] call : calls) {
				if (object == call[1]){
					output.write(("{\"this\": " + call[ 0 ] + "}").getBytes());
					return ;
				}
			}
			if (object instanceof Class) {
				writeString(((Class<?>) object). getCanonicalName() );
			} else
				if (object instanceof Boolean) {
					output.write((((Boolean) object).booleanValue()) ? TRUE : FALSE);					
				} else
					if (object instanceof Number) {
						output.write( ((Number) object).toString().getBytes() ) ;					
					} else
						if ( ( object instanceof String) || ( object instanceof Character ) ) {
							writeString(object.toString());
						} else 
							if (object.getClass().isArray()) {
								output.write('[');
								int length = Array.getLength(object);
								for (int i = 0; i < length; ++i) {
									write(Array.get(object, i));
									if (i < length - 1)
										output.write(',');
								}
								output.write(']');
							} else {
								calls.push(new Object[]{ ++id, object });
								//System.err.println("call: push -> id:" + id + ", obj:" + object);
								if (object instanceof Map) {
									output.write('{');
									Iterator<Map.Entry<?,?>> it = ((Map) object).entrySet().iterator();
									while (it.hasNext()) {
										Entry<?, ?> e = it.next();
										property(e.getKey().toString(), e.getValue());
										if (it.hasNext())
											output.write(',');
									}
									output.write('}');
								} else
									if (object instanceof Iterator) {
										array((Iterator) object);
									} else
										if (object instanceof Collection) {
											array(((Collection) object).iterator());
										} else {
											output.write('{');
											BeanInfo info;
											boolean addedSomething = false;
											try {
												info = Introspector.getBeanInfo(object.getClass());
												PropertyDescriptor[] props = info.getPropertyDescriptors();
												for (int i = 0; i < props.length; ++i) {
													PropertyDescriptor prop = props[i];
													String name = prop.getName();
													Method accessor = prop.getReadMethod();
													if ((!"class".equals(name)) && accessor != null) {
														if (!accessor.isAccessible())
															accessor.setAccessible(true);
														Object value = accessor.invoke(object, (Object[]) null);
														if (addedSomething)
															output.write(',');
														property(name, value);
														addedSomething = true;
													}
												}
												Field[] ff = object.getClass().getFields();
												for (int i = 0; i < ff.length; ++i) {
													Field field = ff[i];
													if (addedSomething)
														output.write(',');
													property(field.getName(), field.get(object));
													addedSomething = true;
												}
											} catch (IllegalAccessException iae) {
												iae.printStackTrace();
											} catch (InvocationTargetException ite) {
												ite.getCause().printStackTrace();
												ite.printStackTrace();
											} catch (IntrospectionException ie) {
												ie.printStackTrace();
											}
											output.write('}');
										}
								calls.pop();
							}
			
		}
	}

	protected void property(final String name, final Object value) throws IOException {
		output.write('"');
		output.write(name.getBytes());
		output.write("\":".getBytes());
		write(value);
	}

	protected void array(final Iterator<?> iterator) throws IOException {
		output.write('[');
		while (iterator.hasNext()) {
			write(iterator.next());
			if (iterator.hasNext())
				output.write(',');
		}
		output.write(']');
	}

	private final static char[] HEX = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

	// Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
	protected void writeString(final String string) throws IOException {
		output.write('"');
		final int len = string.length();
		for ( int i = 0; i < len; i++ ) {
			final char c = string.charAt(i);
			switch (c) {
				case '"'  : output.write("\\\"".getBytes()); break;
				case '\\' : output.write("\\\\".getBytes()); break;
				case '/'  : output.write( "\\/".getBytes()); break;
				case '\b' : output.write("\\\b".getBytes()); break;
				case '\f' : output.write("\\\f".getBytes()); break;
				case '\n' : output.write("\\\n".getBytes()); break;
				case '\r' : output.write("\\\r".getBytes()); break;
				case '\t' : output.write("\\\t".getBytes()); break;
				default:
	                //Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ( ( c >= '\u0000' && c <= '\u001F' ) || ( c >='\u007F' && c <= '\u009F' ) || ( c >= '\u2000' && c <= '\u20FF' ) ){
						output.write("\\u".getBytes());
						int n = c;
						for (int j = 0; j < 4; ++j) {
							output.write( HEX[ ( n & 0xf000 ) >> 12 ] ) ;
							n <<= 4;
						}
					} else {
						output.write(c);
					}
					break;
			}
		}
		output.write('"');
	}
}