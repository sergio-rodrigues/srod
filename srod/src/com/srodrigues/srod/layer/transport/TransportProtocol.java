package com.srodrigues.srod.layer.transport;

import com.srodrigues.srod.layer.java.MethodRequest;
import com.srodrigues.srod.layer.java.MethodResponse;

public interface TransportProtocol {
	public MethodResponse sendRequest(final MethodRequest request);
}
