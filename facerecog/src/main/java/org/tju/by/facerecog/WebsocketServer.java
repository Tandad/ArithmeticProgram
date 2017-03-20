package org.tju.by.facerecog;

import org.eclipse.jetty.server.Server;

public class WebsocketServer extends Thread{
	@Override
	public void run() {
		super.run();
		try{
			Server server = new Server(2014);
			server.setHandler(new FaceDetectionHandler());
			server.setStopTimeout(0);
			server.start();
			server.join();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		WebsocketServer ws = new WebsocketServer();
		ws.start();
	}
}
