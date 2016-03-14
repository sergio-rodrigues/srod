package com.srodrigues.srod.layer.marshalling.json;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;

import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.json.JSONReader;
import com.srodrigues.srod.json.JSONWriter;
import com.srodrigues.srod.layer.java.Reflection;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;

public class JSONMarshalling implements MarshallingProtocol {
	private static final boolean DEBUG = true;

	@Override
	public void encode(final Object o, final OutputStream out) {
		try {
			if ( DEBUG ){
				final ByteArrayOutputStream os = new ByteArrayOutputStream() ;
				
				new JSONWriter( os ).write(o);

				final byte[] ba = os.toByteArray();
				
				out.write(ba);
				System.err.println(new String(ba));
				
			} else {
				new JSONWriter( out ).write(o);
			}
		} catch (IOException e) { 
			throw new SRODException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Serializable> T decode(final InputStream in, final Class<T> clazz){
		try {
			if ( DEBUG ){
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				String read ;
				while((read = br.readLine()) != null) {
				    sb.append(read);
				}

				final String json = sb.toString();
				System.err.println(json);
				final Object o = new JSONReader(json).read();
				return (T)Reflection.convert(clazz, o);
			} else {
				final Object o = new JSONReader(in).read();
				return (T)Reflection.convert(clazz, o);
			}
		} catch (IOException e1) {
			return null;
		}
	}
}
