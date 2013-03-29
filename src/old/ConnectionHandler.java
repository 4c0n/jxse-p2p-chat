package old;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
	private Socket socket;
	
	public ConnectionHandler(Socket socket) {
		this.socket = socket;
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		System.out.println("Connection Handler running...");
		while(true) {
			
		}
	}
}
