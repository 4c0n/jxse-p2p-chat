import java.util.Vector;


public class MessageRelay {
	private Vector<String[]> incomingMessages;
	private Vector<String> outgoingMessages;
	private Vector<String> users;
	
	private String name;
	
	public MessageRelay() {
		incomingMessages = new Vector<String[]>();
		outgoingMessages = new Vector<String>();
		users = new Vector<String>();
	}
	
	public synchronized void addIncomingMessage(String[] msg) {
		System.out.println("Incoming message added to relay!");
		incomingMessages.add(msg);
	}
	
	public synchronized Vector<String[]> getIncomingMessages() {
		Vector<String[]> incoming = new Vector<String[]>(incomingMessages);
		incomingMessages.clear();
		return incoming;
	}
	
	public synchronized void addOutgoingMessage(String msg) {
		outgoingMessages.add(msg);
		System.out.println("Outgoing message added to relay.");
	}
	
	public synchronized Vector<String> getOutgoingMessages() {
		//System.out.println("There are " + outgoingMessages.size() + "outgoing messages...");
		Vector<String> outgoing = new Vector<String>(outgoingMessages);
		outgoingMessages.clear();
		return outgoing;
	}
	
	public synchronized void setName(String name) {
		this.name = name;
	}
	
	public synchronized String getName() {
		return name;
	}
	
	public synchronized void addUser(String user) {
		if(!users.contains(user)) {
			users.add(user);
		}
	}
	
	public synchronized Vector<String> getUsers() {
		return users;
	}
}
