package com.srodrigues.srod.layer.marshalling.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;

public class JavaSerializator implements MarshallingProtocol {

	@Override
	public void encode(final Object o, final OutputStream out) {
		try {
			new ObjectOutputStream(out).writeObject(o);		
		} catch (IOException e) {
			throw new SRODException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Serializable> T decode(final InputStream in, final Class<T> clazz){
		try {
			return (T)(new ObjectInputStream(in).readObject());
		} catch (Exception e) {
			throw new SRODException(e);
		}
	}

}



