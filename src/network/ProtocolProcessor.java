package network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;

import controllers.ServerController;
import models.Message;
import models.RequestCritical;

public class ProtocolProcessor implements Runnable{
	
	private Object receivedMessage;
	
	public ProtocolProcessor (DatagramPacket packet) {
		this.setReceivedMessage(this.deserialize(packet.getData()));
	}
	
	@Override
	public void run() {
		if (!this.isValidMessage()) { System.err.println("Corrupted message!"); return; }
		
		
		Message message = (Message) this.getReceivedMessage();
		
		
		switch(message.getProtocolHeader()) {
			case "001": // When some process wants know how many processes are active
				if (this.isSelfMessage(message)) return ;
				String [] credentials = new String[] {message.getProcessID(), ServerController.getInstance().getProcessID()};
				ServerController.getInstance().sendMessage("002", credentials);
				break;
			case "002": //When some process notify if is active
				if (this.isSelfMessage(message)) return;
				String [] active = (String[]) message.getContentMessage();
				if (!active[0].equals(ServerController.getInstance().getProcessID())) return;
				ServerController.getInstance().addToGroup(message.getProcessID());
				break;
			case "003":
				System.out.println("O processo "+ message.getProcessID() + " quer entrar na seção crítica no tempo:"+ message.getTimestamp() +" Aguardando confirmação...");
				ServerController.getInstance().registerEvent(message);
				break;
			case "004":
				if (this.isSelfMessage(message)) return;
				RequestCritical request = (RequestCritical)message.getContentMessage();
				ServerController.getInstance().determineCriticalSectionUser(request, message);
				//ServerController.getInstance().sendConfirmation((String)data[0], (Integer)data[1], message);
				break;
			case "005":
				if (this.isSelfMessage(message)) return;
				String[] response = (String[])message.getContentMessage();
				if (response[1].equals(ServerController.getInstance().getProcessID())) {
					ServerController.getInstance().validateEvent(response[0]);
				}
				break;
			case "006":
				if (this.isSelfMessage(message)) return;
				Object[] validMessage = (Object[])message.getContentMessage();
				ServerController.getInstance().sendToApplication((String)validMessage[0], (Integer)validMessage[1], message);
				break;
		}
		
	}
	
	private Object deserialize(byte[] data) {
        ByteArrayInputStream message = new ByteArrayInputStream(data);

        try {
            ObjectInput reader = new ObjectInputStream(message);
            return (Object)reader.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

	private boolean isValidMessage () {
		return this.getReceivedMessage() instanceof Message;
	}
	
	private boolean isSelfMessage (Message message) {
		return message.getProcessID().equals(ServerController.getInstance().getProcessID());
	}
	
	public Object getReceivedMessage() {
		return receivedMessage;
	}

	public void setReceivedMessage(Object receivedMessage) {
		this.receivedMessage = receivedMessage;
	}

}
