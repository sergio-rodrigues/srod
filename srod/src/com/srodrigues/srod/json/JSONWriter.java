package com.srodrigues.srod.json;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.srodrigues.srod.layer.java.Reflection;

public class JSONWriter {
	private static final byte[] UNICODE = "\\u".getBytes();

	private static final byte[] SLASH = "\\/".getBytes();

	private static final byte[] DOUBLE_QUOTE = "\\\"".getBytes();

	private static final byte[] BACKSLASH = "\\\\".getBytes();

	private static final byte[] BACKSPACE = "\\\b".getBytes();

	private static final byte[] FORMFEED = "\\\f".getBytes();

	private static final byte[] LF = "\\\n".getBytes();

	private static final byte[] CR = "\\\r".getBytes();

	private static final byte[] TAB = "\\\t".getBytes();

	private static final byte[] NULL = "null".getBytes();

	private static final byte[] FALSE = "false".getBytes();

	private static final byte[] TRUE = "true".getBytes();

	private final List<Object> calls = new ArrayList<Object>();

	private final OutputStream output;

	public JSONWriter(final OutputStream output) {
		this.output = output;
	}

	public void write(final Object object) throws IOException {
		calls.clear();
		writeJSON(object);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void writeJSON(final Object object) throws IOException {
		if (object == null) {
			output.write(NULL);
		} else {
			for (int i = 0; i < calls.size(); i++) {
				Object o = calls.get(i);
				if (object == o) {
					output.write(("{\"this\": " + (i + 1) + "}").getBytes());
					return;
				}
			}
			if (object instanceof Class) {
				writeString(((Class<?>) object).getCanonicalName());
			} else if (object instanceof Boolean) {
				output.write((((Boolean) object).booleanValue()) ? TRUE : FALSE);
			} else if (object instanceof Number) {
				output.write(((Number) object).toString().getBytes());
			} else if ((object instanceof String) || (object instanceof Character)) {
				writeString(object.toString());
			} else if (object.getClass().isArray()) {
				output.write('[');
				final int length = Array.getLength(object);
				for (int i = 0; i < length; ++i) {
					writeJSON(Array.get(object, i));
					if (i < length - 1) {
						output.write(',');
					}
				}
				output.write(']');
			} else {
				calls.add(object);
				if (object instanceof Map) {
					output.write('{');
					final Iterator<Map.Entry<?, ?>> it = ((Map) object).entrySet().iterator();
					while (it.hasNext()) {
						final Entry<?, ?> e = it.next();
						property(e.getKey().toString(), e.getValue());
						if (it.hasNext()) {
							output.write(',');
						}
					}
					output.write('}');
				} else if (object instanceof Iterator) {
					array((Iterator) object);
				} else if (object instanceof Collection) {
					array(((Collection) object).iterator());
				} else {
					output.write('{');
					boolean addedSomething = false;
					final Map<String, Object> attributes = Reflection.getAttributes(object);
					for (final Entry<String, Object> entry : attributes.entrySet()) {
						if (addedSomething) {
							output.write(',');
						}
						property(entry.getKey(), entry.getValue());
						addedSomething = true;
					}
					output.write('}');
				}
			}
		}
	}

	protected void property(final String name, final Object value) throws IOException {
		output.write('"');
		output.write(name.getBytes());
		output.write("\":".getBytes());
		writeJSON(value);
	}

	protected void array(final Iterator<?> iterator) throws IOException {
		output.write('[');
		while (iterator.hasNext()) {
			writeJSON(iterator.next());
			if (iterator.hasNext()) {
				output.write(',');
			}
		}
		output.write(']');
	}

	private final static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
			'F' };

	// Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters
	// (U+0000 through U+001F).
	protected void writeString(final String string) throws IOException {
		output.write('"');
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			final char c = string.charAt(i);
			switch (c) {
			case '"': output.write(DOUBLE_QUOTE);break;
			case '\\':output.write(BACKSLASH);break;
			case '/': output.write(SLASH);break;
			case '\b':output.write(BACKSPACE);break;
			case '\f':output.write(FORMFEED);break;
			case '\n':output.write(LF);break;
			case '\r':output.write(CR);break;
			case '\t':output.write(TAB);break;
			default:
				// Reference: http://www.unicode.org/versions/Unicode5.1.0/
				if ( (c >= '\u0000' && c <= '\u001F') || //
					 (c >= '\u007F' && c <= '\u009F') || //
					 (c >= '\u2000' && c <= '\u20FF')) {
					output.write(UNICODE);
					int n = c;
					for (int j = 0; j < 4; ++j) {
						output.write(HEX[(n & 0xf000) >> 12]);
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