package com.srodrigues.srod.layer.java.mqtt;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.srodrigues.srod.InitialContext;
import com.srodrigues.srod.layer.java.ClientProxy;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;
import com.srodrigues.srod.layer.transport.mqtt.MQTTProtocol;

public class MQTTInitialContext implements InitialContext {

	private final MarshallingProtocol marshaling;
	private final String    topic ;
	private final String   broker ;
	private final String username ;
	private final String password ;

	public MQTTInitialContext(final String url, final String prefix, final MarshallingProtocol marshaling) throws MalformedURLException, IOException {
		this.marshaling = marshaling;
 		if ( prefix != null ){
			final String topic = prefix.trim();
			if ( !"".equals( topic ) ) {
				if ( topic.charAt( 0 ) !='/' ){
					this.topic = "/".concat( topic ) ;
				} else {
					this.topic = topic ;
				}
			} else {
				this.topic = "/" ;			
			}
		} else {
			this.topic = "/" ;
		}
		
		// Broker Possible values :<tcp|ssl|srod|srods>://[<username>:<password>@]<host>[:<port>]
		int i = url.indexOf( "://" ) ;
        if ( i < 0 ) {
            throw new MalformedURLException("Invalid URL [" + url + "]: no protocol specified");
        }

        String protocol = url.substring( 0, i ) ;
        if ( "srod".equals( protocol ) ) {
        	protocol = "tcp";
        } else
        if ( "srods".equals( protocol ) ) {
        	protocol = "ssl";
        }
        
        i += 3 ;
        // username[:password]
        final int j = url.indexOf('@', i );
        if (j >= 0) {
            final String auth = url.substring(i, j);
            final int k = auth.indexOf(':');
            if ( k >= 0) {
                username = auth.substring( 0 , k ) ;
                password = auth.substring( k + 1 ) ;
            } else {
                username = auth;
                password = null;
            }
            i = j + 1;
        } else {
            username = null;
            password = null;
        	
        }
        
        // host[:port]
        broker = protocol + "://" + url.substring(i) + ( (url.indexOf(':', i) < 0 ) ? ":1883" : "" ) ;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T lookup(final String name, final Class<T> clazz) {
        	try {
				return (T) Proxy.newProxyInstance(  clazz.getClassLoader(), 
												    new Class<?>[] { clazz }, 
												    new ClientProxy( name, new MQTTProtocol(marshaling, broker, topic, username, password ) ) 
												 ) ;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return null;
			} catch (MqttException e) {
				e.printStackTrace();
				return null;
			}
	}
	
	public MQTTProtocol getProtocol(){
        try {
			return new MQTTProtocol(marshaling, broker, topic, username, password) ;
		} catch (MqttException e) {
			e.printStackTrace();
			return null;
		}
	}
}