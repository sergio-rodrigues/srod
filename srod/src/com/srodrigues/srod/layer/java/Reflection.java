package com.srodrigues.srod.layer.java;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reflection {

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

      //if object is null return null
      if (object == null) {
         return null;
      }

      //if is an object.. return the object (can't do more)
      if (clazz == Object.class) {
         return object;
      }

      //arrays are converted to arrayList by json
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
         //if not.. noting to do...
         return object;
      }

      // this -> replace this:xsxsx with object
      if (clazz == Class.class) {
         try {
            final String value = object.toString();

            //primitive types
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

      //Number Hierarchy
      if (Number.class.isAssignableFrom(clazz)) {
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

      //String & Char
      if (clazz == String.class) {
         return object.toString();
      } //String->toString = String
      if (clazz == Character.class || clazz == char.class) {
         return object.toString().charAt(0);
      }

      // process bean
      if (object instanceof Map) {
         @SuppressWarnings("unchecked")
         Map<Object, Object> attributes = (Map<Object, Object>) object;
         Object tag = attributes.get("this");
         if (tag != null) {
            //Check already converted object // Cyclic redundance
            int id = Integer.valueOf(tag.toString());
            try {
               return cache.get(id - 1);
            } catch (IndexOutOfBoundsException e) {
               throw new RuntimeException("invalid Attribute in Map<?,?>, No java Object can have an \"this\" Attribute");
            }
         }

         try {
            Object t = clazz.newInstance();
            cache.add(t);
            //beanInfo
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
               String element = field.getName();
               if (attributes.containsKey(element)) {
                  field.setAccessible(true);
                  field.set(t, convert(field.getType(), attributes.get(element), cache));
               }
            }
            return t;
         } catch (InstantiationException e) {
            return null;
         } catch (IllegalAccessException e) {
            return null;
         } catch (NegativeArraySizeException e) {
            return null;
         }
      }
      return object;
   }
}