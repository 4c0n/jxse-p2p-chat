package old;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import net.jxta.document.AdvertisementFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;


public class P2PChat extends Thread {
	private NetworkManager manager;
	private PipeAdvertisement pipeAdv;
	private final String socketIDString = "urn:jxta:uuid-59616261646162614E5047205032503393B5C2F6CA7A41FBB0F890173088E79404";
	
	public P2PChat() throws IOException, PeerGroupException {
		manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
				"Chat", new File(new File(".cache"), "Chat").toURI());
		manager.startNetwork();
		//netPeerGroup = manager.getNetPeerGroup();
		
		try {
			PipeID socketID = PipeID.create(new URI(socketIDString));
			pipeAdv = (PipeAdvertisement) 
					AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
			pipeAdv.setPipeID(socketID);
			pipeAdv.setType(PipeService.UnicastType);
			pipeAdv.setName("Chat");
		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	public void run() {
		OutgoingConnection outgoing = new OutgoingConnection(manager, pipeAdv);
		JxtaServerSocket serverSocket = null;
		try {
			serverSocket = new JxtaServerSocket(manager.getNetPeerGroup(), pipeAdv, 10);
			serverSocket.setSoTimeout(0);
			
		} 
		catch (IOException e) {
			System.err.println("Could not create server socket!");
			e.printStackTrace();
			System.exit(-1);
		}
		
		while(true) {
			try {
				Socket socket = serverSocket.accept();
				if(socket != null) {
					System.out.println("Connection accepted!");
					ConnectionHandler handler = new ConnectionHandler(socket);
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			P2PChat chat = new P2PChat();
			chat.start();
		} 
		catch (PeerGroupException | IOException e) {
			System.err.println("Could not be started!");
		}
	}

}
