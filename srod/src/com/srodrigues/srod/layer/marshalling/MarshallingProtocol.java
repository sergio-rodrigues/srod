package com.srodrigues.srod.layer.marshalling;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public interface MarshallingProtocol {

	public void encode(final Object o, final OutputStream out);

	public <T extends Serializable> T decode(final InputStream in, final Class<T> clazz);

}
