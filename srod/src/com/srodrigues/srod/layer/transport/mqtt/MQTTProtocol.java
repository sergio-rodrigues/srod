package com.srodrigues.srod.layer.transport.mqtt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import com.srodrigues.srod.ServiceProvider;
import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.layer.java.MethodRequest;
import com.srodrigues.srod.layer.java.MethodResponse;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;
import com.srodrigues.srod.layer.transport.TransportProtocol;

public class MQTTProtocol implements TransportProtocol {

	private static final String QUESTION = "/question";
	private static final String RESPONSE = "/response";
	private static final int QOS = 1;
	private static final long TIMEOUT = 300;

	private final MarshallingProtocol serializator;

	// MQTT Client connected to broker
	private final MqttClient client;
	private final MqttConnectOptions connOpts;
	private final String topic;

	public MQTTProtocol(final MarshallingProtocol serializator, final String broker, final String topic, final String username, final String password) throws MqttException {

		this.serializator = serializator;
		this.topic = topic;
		this.client = new MqttClient(broker, MqttClient.generateClientId());

		this.connOpts = new MqttConnectOptions();
		if (username != null && !"".equals(username)) {
			connOpts.setUserName(username);
		}

		if (password != null && !"".equals(password)) {
			connOpts.setPassword(password.toCharArray());
		}

	}

	private static final Map<String,Map<Integer, MethodResponse>> topicResponses = new HashMap<String,Map<Integer, MethodResponse>>();
	
	private Map<Integer, MethodResponse> getResponseMap(final String service) {
		synchronized (topicResponses) {
			Map<Integer, MethodResponse> responses;
			responses  = topicResponses.get(service) ;
			if (responses == null ){
				responses = Collections.synchronizedMap( new HashMap<Integer, MethodResponse>() );
				topicResponses.put(service, responses);
			}
			return responses;
		}
	}
	
	@Override
	public MethodResponse sendRequest(MethodRequest request) {
		try {
			final String service = request.getService();
			final Map<Integer, MethodResponse> responses = getResponseMap(service);
			final String baseTopic   = topic + "/" + service;
			final String rpcRequest  = baseTopic + QUESTION;
			final String rpcResponse = baseTopic + RESPONSE;

			// Encode request.
			final ByteArrayOutputStream ba = new ByteArrayOutputStream();
			serializator.encode(request, ba);

			// System.out.println( "[SROD]: Connecting to broker: " + broker ) ;
			client.connect(connOpts);
			// System.out.println("[SROD]: Connected!");

			// subscribe response
			client.subscribe(rpcResponse);
			// System.out.println( "[SROD]: [" + rpcResponse + "] Messages
			// Subscrived!" ) ;

			// callback for response
			client.setCallback(new MqttCallback() {
				@Override
				public void messageArrived(String topic, MqttMessage msg) throws Exception {
					final MethodResponse message = serializator.decode(new ByteArrayInputStream(msg.getPayload()),
							MethodResponse.class);

					// Transform an async call in a Sync Call
					synchronized (responses) {
						// responses.add( message ) ;
						responses.put(message.getId(), message);
						responses.notifyAll();
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					// Called when a message has completed delivery to the
					// server. The token passed in here is the same one that was
					// returned in the original call to publish.
					// This allows applications to perform asychronous delivery
					// without blocking until delivery completes.
				}

				@Override
				public void connectionLost(Throwable arg0) {
					// Called when the connection to the server has been lost.
					// An application may choose to implement reconnection logic
					// at this point.
				}
			});

			// System.out.println("[SROD]: Publishing message: " +
			// ba.toString());
			// Get an instance of the topic
			final MqttTopic tp = client.getTopic(rpcRequest);
			final MqttMessage message = new MqttMessage(ba.toByteArray());
			message.setQos(QOS);

			// Publish the message
			final MqttDeliveryToken token = tp.publish(message);

			// Wait until the message has been delivered to the server
			token.waitForCompletion();

			// request Service
			// System.out.println( "[SROD]: [" + rpcRequest + "] Request
			// published!" ) ;

			MethodResponse r = null;
			synchronized (responses) {
				final int id = request.getId();
				while (!responses.containsKey(id)) {
					try {
						responses.wait(TIMEOUT);
					} catch (InterruptedException e) {
						return null;
					}
				}
				r = responses.remove(id);
			}
			client.unsubscribe(rpcResponse);
			client.disconnect();
			return r;

		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg    " + me.getMessage());
			System.out.println("loc    " + me.getLocalizedMessage());
			System.out.println("cause  " + me.getCause());
			System.out.println("excep  " + me);
			me.printStackTrace();
		}
		return null;
	}

	public void startServer(String service) {
		// service is service name
		// topic = topic + /service/
		// subscribe questions

		final String rpc = topic + "/" + service + QUESTION;
		final String rpcr = topic + "/" + service + RESPONSE;

		try {
			// System.out.println( "[SROD]: Connecting to broker: " + broker ) ;
			client.connect(connOpts);
			// System.out.println("[SROD]: Connected!");
			client.subscribe(rpc);
			client.setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String tp, MqttMessage msg) throws Exception {

					// SROD
					final MethodRequest request = serializator.decode(new ByteArrayInputStream(msg.getPayload()),
							MethodRequest.class);
					final Object service = ServiceProvider.getService(request.getService());
					if (service == null) {
						throw new SRODException("Cannot find service with name " + request.getService());
					}

					final MethodResponse remoteResponse = ServiceProvider.invoke(request.getId(), service, request);

					final ByteArrayOutputStream ba = new ByteArrayOutputStream();
					serializator.encode(remoteResponse, ba);

					// MQTT
					// Get an instance of the topic
					final MqttTopic topic = client.getTopic(rpcr);
					final MqttMessage message = new MqttMessage(ba.toByteArray());
					message.setQos(QOS);

					// Publish the message
					topic.publish(message);
					System.out.println("[SROD]: [" + rpcr + "] Response published!");
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					// Called when a message has completed delivery to the
					// server. The token passed in here is the same one
					// that was returned in the original call to publish.
					// This allows applications to perform asychronous delivery
					// without blocking until delivery completes.
					System.out.println("[SROD]: [" + rpcr + "] Response delivered!");
				}

				@Override
				public void connectionLost(Throwable arg0) {
					// Called when the connection to the server has been lost.
					// An application may choose to implement reconnection
					// logic at this point.
				}
			});
			System.out.println("[SROD]: Messages Subscrived!");
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg    " + me.getMessage());
			System.out.println("loc    " + me.getLocalizedMessage());
			System.out.println("cause  " + me.getCause());
			System.out.println("excep  " + me);
			me.printStackTrace();
		}

	}
}