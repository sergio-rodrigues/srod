package com.srodrigues.srod.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONReader {

   private static enum Status {
      OBJECT_END, ARRAY_END, COLON, COMMA
   }

   private final InputStream input;

   //push one char back to the stream
   private char push_back = 0;

   protected void putChar(final char c) {
      push_back = c;
   }

   protected char getChar() throws IOException {
      if (push_back != 0) {
         final char c = push_back;
         push_back = 0;
         return c;
      }
      return (char) input.read();
   }

   public JSONReader(final InputStream input) {
      this.input = input;
   }

   public JSONReader(final String string) {
      this.input = new ByteArrayInputStream(string.getBytes());
   }

   //   private int id = 0;
   //
   //   private final List<Object[]> calls = new ArrayList<Object[]>();
   //
   //   protected Object cyclic(Object object) {
   //      if (object instanceof Map) {
   //         @SuppressWarnings("unchecked")
   //         Map<Object, Object> new_name = (Map<Object, Object>) object;
   //
   //         final Object tag = new_name.get("this");
   //         if (tag != null) {
   //            int j = ((Number) tag).intValue();
   //            for (Object[] call : calls) {
   //               if (j == (Integer) call[0]) {
   //                  return call[1];
   //               }
   //            }
   //         } else {
   //            calls.add(new Object[] { ++id, object });
   //            //System.err.println("call: push -> id:" + id + ", obj:" + object);
   //            for (Entry<Object, Object> entry : new_name.entrySet()) {
   //               final Object value = entry.getValue();
   //               if (value instanceof Map || value instanceof List) {
   //                  entry.setValue(cyclic(value));
   //               }
   //            }
   //         }
   //
   //      } else if (object instanceof List) {
   //         @SuppressWarnings("unchecked")
   //         List<Object> new_name = (List<Object>) object;
   //         for (Object object2 : new_name) {
   //            if (object2 instanceof Map || object2 instanceof List) {
   //               object2 = cyclic(object2);
   //            }
   //         }
   //      }
   //      return object;
   //   }

   public Object read() throws IOException {
      //      id = 0;
      //      calls.clear();
      //      return cyclic(readJSON());
      return readJSON();
   }

   protected Object readJSON() throws IOException {
      char ch;
      do {
         ch = getChar();
      } while (Character.isWhitespace(ch));

      switch (ch) {
      case '"':
         return string();
      case '[':
         return array();
      case ']':
         return Status.ARRAY_END;
      case ',':
         return Status.COMMA;
      case '{':
         return object();
      case '}':
         return Status.OBJECT_END;
      case ':':
         return Status.COLON;
      case 't':
         getChar();
         getChar();
         getChar(); // assumed r-u-e
         return Boolean.TRUE;
      case 'f':
         getChar();
         getChar();
         getChar();
         getChar(); // assumed a-l-s-e
         return Boolean.FALSE;
      case 'n':
         getChar();
         getChar();
         getChar(); // assumed u-l-l
         return null;
      default:
      }
      if (Character.isDigit(ch) || ch == '-') {
         return number(ch);
      }
      throw new IOException();
   }

   protected Map<Object, Object> object() throws IOException {
      final Map<Object, Object> ret = new LinkedHashMap<Object, Object>();
      Object key = readJSON();
      Object token = key;
      while (token != Status.OBJECT_END) {
         token = readJSON(); // should be a colon
         if (token != Status.OBJECT_END) {
            ret.put(key, readJSON());
            token = readJSON();
            if (token == Status.COMMA) {
               token = key = readJSON();
            }
         }
      }

      return ret;
   }

   protected List<Object> array() throws IOException {
      final List<Object> ret = new ArrayList<Object>();
      Object value = readJSON();
      while (value != Status.ARRAY_END) {
         ret.add(value);
         value = readJSON();
         if (value == Status.COMMA) {
            value = readJSON();
         }
      }
      return ret;
   }

   protected Number number(char c) throws IOException {
      int length = 0;
      boolean isFloatingPoint = false;

      StringBuilder buf = new StringBuilder();
      if (c == '-') {
         buf.append(c);
         c = getChar();
      }

      while (Character.isDigit(c)) {
         ++length;
         buf.append(c);
         c = getChar();
      }

      if (c == '.') {
         buf.append(c);
         c = getChar();
         while (Character.isDigit(c)) {
            ++length;
            buf.append(c);
            c = getChar();
         }
         isFloatingPoint = true;
      }

      if (c == 'e' || c == 'E') {
         buf.append(c);
         c = getChar();
         if (c == '+' || c == '-') {
            buf.append(c);
            c = getChar();
         }

         while (Character.isDigit(c)) {
            ++length;
            buf.append(c);
            c = getChar();
         }
         isFloatingPoint = true;
      }

      if (!Character.isWhitespace(c)) {
         putChar(c);
      }

      final String s = buf.toString();
      return isFloatingPoint ? (length < 17) ? Double.valueOf(s) : new BigDecimal(s) : (length < 19) ? Long.valueOf(s)
         : new BigInteger(s);
   }

   protected String string() throws IOException {
      final StringBuilder buf = new StringBuilder();
      char c = getChar();
      while (c != '"') {
         if (c == '\\') {
            c = getChar();
            switch (c) {
            case 'u':
               buf.append(unicode());
               break;
            case '"':
            case '/':
               buf.append(c);
               break;
            case '\\':
               buf.append('\\');
               break;
            case 'b':
               buf.append('\b');
               break;
            case 'f':
               buf.append('\f');
               break;
            case 'n':
               buf.append('\n');
               break;
            case 'r':
               buf.append('\r');
               break;
            case 't':
               buf.append('\t');
               break;
            default:
               break;
            }
         } else {
            buf.append(c);
         }
         c = getChar();
      }
      return buf.toString();
   }

   protected char unicode() throws IOException {
      int value = 0;

      for (int i = 0; i < 4; ++i) {
         final char c = getChar();

         if (c >= '0' && c <= '9') {
            value = (value << 4) + c - '0';
         } else if (c >= 'a' && c <= 'f') {
            value = (value << 4) + (c - 'a') + 10;
         } else if (c >= 'A' && c <= 'F') {
            value = (value << 4) + (c - 'A') + 10;
         }

      }
      return (char) value;
   }
}