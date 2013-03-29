import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleClassAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PipeAdvertisement;


public class P2PChat implements DiscoveryListener, PipeMsgListener {
	private NetworkManager manager;
	private PeerGroup netPeerGroup;
	private Vector<HashSet<PeerID>> peers;
	private MessageRelay messages;
	private String peerName;
	private PeerID peerID;
	private PeerGroup chatGroup;
	private PipeID unicastID;
	private PipeID multicastID;
	private PipeID serviceID;
	private PipeService pipeService;
	private DiscoveryService discovery;
	
	private static final String groupName = "myP2Pchat";
	private static final String groupDesc = "P2P chat";
	private static final PeerGroupID groupID = IDFactory.newPeerGroupID(
			PeerGroupID.defaultNetPeerGroupID, groupName.getBytes());
	private static final String unicastName = "uniP2PChat";
	private static final String multicastName = "multiP2PChat";
	private static final String serviceName = "P2PChat";
	
	public P2PChat(MessageRelay messages) throws IOException, PeerGroupException {
		this.messages = messages;
		
		peerName = "Peer " + new Random().nextInt(1000000);
		peerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, peerName.getBytes());
		
		manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
				peerName, new File(new File(".cache"), "Chat").toURI());
		
		int port = 9000 + new Random().nextInt(100);
		
		NetworkConfigurator config = manager.getConfigurator();
		config.setTcpPort(port);
		config.setTcpEnabled(true);
		config.setTcpIncoming(true);
		config.setTcpOutgoing(true);
		config.setUseMulticast(true);
		config.setPeerID(peerID);
		
		
		peers = new Vector<HashSet<PeerID>>();
	}

	public void start() {
		try {
			netPeerGroup = manager.startNetwork();
		} 
		catch (PeerGroupException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		ModuleImplAdvertisement mAdv = null;
	  
		try {
			mAdv = netPeerGroup.getAllPurposePeerGroupImplAdvertisement();
			chatGroup = netPeerGroup.newGroup(groupID, mAdv, groupName, groupDesc);
		} 
		catch (Exception ex) {
			System.err.println(ex.toString());
		}
		  
        if (Module.START_OK != chatGroup.startApp(new String[0]))
            System.err.println("Cannot start child peergroup");
        
        unicastID = IDFactory.newPipeID(chatGroup.getPeerGroupID(), unicastName.getBytes());
        multicastID = IDFactory.newPipeID(chatGroup.getPeerGroupID(), multicastName.getBytes());
        
        pipeService = chatGroup.getPipeService();
        try {
			pipeService.createInputPipe(getAdvertisement(unicastID, false), this);
			pipeService.createInputPipe(getAdvertisement(multicastID, true), this);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        discovery = chatGroup.getDiscoveryService();
        discovery.addDiscoveryListener(this);
        
        ModuleClassAdvertisement mcadv = (ModuleClassAdvertisement)
        AdvertisementFactory.newAdvertisement(ModuleClassAdvertisement.getAdvertisementType());
        mcadv.setName("P2PChat");
        mcadv.setDescription("P2PChat Module Advertisement");
        
        ModuleClassID mcID = IDFactory.newModuleClassID();
        mcadv.setModuleClassID(mcID);
        
        try {
			discovery.publish(mcadv);
		} 
        catch (IOException e) {
			e.printStackTrace();
		}
        discovery.remotePublish(mcadv);
        
        ModuleSpecAdvertisement mdadv = (ModuleSpecAdvertisement)
                AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());
        mdadv.setName("P2PChat");
        mdadv.setVersion("Version 1.0");
        mdadv.setCreator("4c0n.nl");
        mdadv.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
        mdadv.setSpecURI("http://www.4c0n.nl");
        
        serviceID = (PipeID) IDFactory.newPipeID(chatGroup.getPeerGroupID(), serviceName.getBytes());
        PipeAdvertisement pipeAdv = getAdvertisement(serviceID, false);
        mdadv.setPipeAdvertisement(pipeAdv);
        
        try {
			discovery.publish(mdadv);
	        discovery.remotePublish(mdadv);
	        pipeService.createInputPipe(pipeAdv, this);
		} 
        catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    private static PipeAdvertisement getAdvertisement(PipeID id, boolean isMulticast) {
        PipeAdvertisement adv = (PipeAdvertisement )AdvertisementFactory.
            newAdvertisement(PipeAdvertisement.getAdvertisementType());
        adv.setPipeID(id);
        if (isMulticast)
            adv.setType(PipeService.PropagateType); 
        else 
            adv.setType(PipeService.UnicastType); 
        adv.setName("P2PChatPipe");
        adv.setDescription("Pipe for p2p chat messages");
        return adv;
    }
	
	@Override
	public void discoveryEvent(DiscoveryEvent event) { // Peer/pipe found
		String addr = "urn:jxta:" + event.getSource().toString().substring(7);
		System.out.println("Discovered: " + addr);
		PeerID peer;
		try {
			URI uri = new URI(addr);
			System.out.println("to: " + uri.toString());
			peer = (PeerID) IDFactory.fromURI(uri);
			HashSet<PeerID> pids = new HashSet<>();
			pids.add(peer);
			if(!peers.contains(pids)) {
				peers.add(pids);
				sendUsername(pids);
			}
		} 
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private void sendUsername(HashSet<PeerID> pids) {
		PipeAdvertisement pipeAdv = getAdvertisement(unicastID, false);
		try {
			OutputPipe out = pipeService.createOutputPipe(pipeAdv, pids, 10000);
			Message msg = new Message();
			MessageElement username = new StringMessageElement("username", messages.getName(), null);
			msg.addMessageElement(username);
			out.send(msg);			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessages() {
		new Thread("Message sender thread") {
			@Override
			public void run() {
				while(true) {
					// Create Message
					PipeAdvertisement pipeAdv = getAdvertisement(unicastID, false);
					Vector<String> msgs = messages.getOutgoingMessages();
					if(!msgs.isEmpty()) {
						MessageElement from = new StringMessageElement("from", messages.getName(), null);
						try {
							for(HashSet<PeerID> pids: peers) {
								OutputPipe out = pipeService.createOutputPipe(pipeAdv, pids, 0);
								for(String s: msgs) {
									Message msg = new Message();
									MessageElement body = new StringMessageElement("body", s, null);
									msg.addMessageElement(from);
									msg.addMessageElement(body);
									out.send(msg);
									System.out.println("Message sent!");
								}
							}
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(1000);
					} 
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	
    private void fetchAdvertisements() {
        new Thread("fetch advertisements thread") {
           public void run() {
              while(true) {
                  discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", "P2PChat", 1, null);
                  try {
                      sleep(10000);

                  }
                  catch(InterruptedException e) {} 
              }
           }
        }.start();
     }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MessageRelay messages = new MessageRelay();
		String name = (String) JOptionPane.showInputDialog(null, "Please enter a username", "Username",
				JOptionPane.QUESTION_MESSAGE, null, null, "user");
		if(name.equals("")) {
			name = "user";
		}
		messages.setName(name);
		try {
			P2PChat chat = new P2PChat(messages);
			chat.start();
			chat.fetchAdvertisements();
			chat.sendMessages();
		} 
		catch (PeerGroupException | IOException e) {
			System.err.println("Could not be started!");
			return;
		}
		MainFrame mainFrame = new MainFrame(messages);
	}

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {
		System.out.println("Message Received!!");
		
		Message msg = event.getMessage();
		Object user = msg.getMessageElement("username");
		if(user != null) {
			messages.addUser(user.toString());
		}
		else {
			String content[] = new String[2];
			content[0] = msg.getMessageElement("from").toString();
			content[1] = msg.getMessageElement("body").toString();
			messages.addIncomingMessage(content);
		}
	}
}
