package com.srodrigues.srod.layer.transport.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.srodrigues.srod.ServiceProvider;
import com.srodrigues.srod.exception.SRODException;
import com.srodrigues.srod.layer.java.MethodRequest;
import com.srodrigues.srod.layer.java.MethodResponse;
import com.srodrigues.srod.layer.marshalling.MarshallingProtocol;
import com.srodrigues.srod.layer.transport.TransportProtocol;

public class SocketProtocol implements TransportProtocol {
	
	private ServerSocket serverSocket =  null ;
	private Thread       serverThread =  null ;
	private boolean         isRunning = false ;

	private final MarshallingProtocol serializator;
	private       String               host;
	private final int                  port;
	
	
	public SocketProtocol(final MarshallingProtocol serializator, final int port) {
		this.serializator = serializator;
		this.port = port;
		this.host = null;
	}

	public SocketProtocol(final MarshallingProtocol serializator, final String host, final int port ) {
		this(serializator, port);
		this.host = host;
	}

	@Override
	public MethodResponse sendRequest(MethodRequest rpcRequest) {
		Socket client = null;
		try {
			client = new Socket(host, port);
			serializator.encode(rpcRequest, client.getOutputStream());
			
			if ( !client.isOutputShutdown( ) )
				client.shutdownOutput( ) ;
			
			return  serializator.decode(client.getInputStream(), MethodResponse.class);
		} catch (IOException e) {
			throw new SRODException(e);
		} finally {
			if (client != null){
				try {
					if ( !client.isInputShutdown( ) )
						client.shutdownInput();
					if (!client.isClosed())
						client.close();
				} catch (IOException e) {
					/* eat */ 
				}		
			}
			
		}
	}

	public void startServer() {
		if (isRunning) {
			throw new SRODException("Server already started on port " + port);
		}

		isRunning = true;
		serverThread = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					serverSocket = new ServerSocket(SocketProtocol.this.port);
				} catch (Exception e) {
					throw new SRODException(e);
				}

				while (!Thread.interrupted()) {
					try {
						Socket clientSocket = serverSocket.accept();
						new RemoteRequestHandler(clientSocket).start();
					} catch (Exception e) {
						throw new SRODException(e);
					}
				}
				isRunning = false;
			}
		});
		serverThread.start();
	}

	public void stopServer() {
		serverThread.interrupt();
	}
	
	private class RemoteRequestHandler extends Thread {

		private final Socket client;

		public RemoteRequestHandler(final Socket client) {
			super("RemoteRequestHandler");
			this.client = client;
		}

		@Override
		public void run() {
			try {
				MethodRequest  request = serializator.decode(client.getInputStream(), MethodRequest.class);

				if ( !client.isInputShutdown( ) )
					client.shutdownInput();

				Object service = ServiceProvider.getService(request.getService());
				if ( service == null ) {
					throw new SRODException("Cannot find service with name " + request.getService());
				}
				
				MethodResponse remoteResponse = ServiceProvider.invoke( request.getId(), service, request );
				serializator.encode( remoteResponse, client.getOutputStream());
				if ( !client.isOutputShutdown( ) )
					client.shutdownOutput( ) ;
			} catch (Exception e) {
				throw new SRODException(e);
			}
		}
	}


}
