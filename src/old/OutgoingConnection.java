package old;
import java.io.IOException;

import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;


public class OutgoingConnection implements Runnable {
	private NetworkManager manager;
	private PipeAdvertisement pipeAdv;
	
	public OutgoingConnection(NetworkManager manager, PipeAdvertisement pipeAdv) {
		this.manager = manager;
		this.pipeAdv = pipeAdv;
		
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		while(true) {
			//manager.waitForRendezvousConnection(0);
			try {
				JxtaSocket socket = new JxtaSocket(manager.getNetPeerGroup(), pipeAdv);
				@SuppressWarnings("unused")
				ConnectionHandler handler = new ConnectionHandler(socket);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
