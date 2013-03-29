import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class MainFrame extends JFrame implements Runnable, KeyListener {
	private static final long serialVersionUID = 6895585993723515253L;
	private MessageRelay messages;
	private JTextArea txtChat;
	private JTextField txtMessage;
	private JList<String> lstUsers;
	private DefaultListModel<String> model;
	
	public MainFrame(MessageRelay messages) {
		super("P2PChat");
		this.messages = messages;
		
		txtChat = new JTextArea(100, 10);
		txtChat.setEditable(false);
		txtMessage = new JTextField(300);
		txtMessage.addKeyListener(this);
		
		model = new DefaultListModel<String>();
		lstUsers = new JList<String>(model);
		model.addElement("@" + messages.getName());
		
		JScrollPane scrollPane = new JScrollPane(txtChat, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatPanel.add(scrollPane, BorderLayout.CENTER);
		chatPanel.add(txtMessage, BorderLayout.SOUTH);
		chatPanel.add(lstUsers, BorderLayout.EAST);
		this.add(chatPanel);
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(800, 600);
		this.setVisible(true);
		
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		while(true) {
			Vector<String[]> msgs = messages.getIncomingMessages();
			if(!msgs.isEmpty()) {
				System.out.println("Frame received message!");
				for(String s[]: msgs) {
					txtChat.append(s[0] + ": " + s[1] + "\n\r");
				}
			}
			Vector<String> users = messages.getUsers();
			for(String user: users) {
				if(!model.contains(user)) {
					model.addElement(user);
				}
			}
			try {
				Thread.sleep(100);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		//System.out.println("Key typed! " + e.getKeyCode());
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			String msg = txtMessage.getText();
			if(!msg.equals("")) {
				messages.addOutgoingMessage(msg);
				txtMessage.setText("");
				txtChat.append(messages.getName() + ": " + msg + "\n\r");
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {		
	}

	@Override
	public void keyTyped(KeyEvent e) {				
	}
}
