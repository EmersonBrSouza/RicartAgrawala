package models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Message implements Serializable, Comparable<Object>{
	
	private static final long serialVersionUID = 1L;
	private String protocolHeader;
	private Object contentMessage;
	private Integer timestamp;
	private String processID;
	private ConcurrentHashMap<String, Boolean> confirmations = new ConcurrentHashMap<String, Boolean>();
	
	public Message (String protocolHeader, Object contentMessage, Integer timestamp, String processID) {
		this.protocolHeader = protocolHeader;
		this.contentMessage = contentMessage;
		this.timestamp = timestamp;
		this.processID = processID;
	}
	
	public String getProtocolHeader () {
		return this.protocolHeader;
	}
	
	public Object getContentMessage () {
		return this.contentMessage;
	}
	
	public Integer getTimestamp () {
		return this.timestamp;
	}
	
	public String getProcessID() {
		return processID;
	}
	
	public ConcurrentHashMap<String, Boolean> getConfirmations () {
		return this.confirmations;
	}
	
	public void setConfirmations (ConcurrentHashMap<String, Boolean> confirmations) {
		this.confirmations = confirmations;
	}
	
	public void receiveConfirmation (String processID) {
		this.confirmations.put(processID, true);
	}

	public boolean isConfirmed() {
		Iterator<Entry<String, Boolean>> it = this.confirmations.entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<String, Boolean> currentKey = (Entry<String, Boolean>) it.next();
			if (!this.confirmations.get(currentKey.getKey())) return false;
		}
		
		return true;
	}

	/**
	 * Serialize a message
	 * 
	 * @param void
	 * @return byte[] serializedMessage
	 * */
	public byte[] serialize () {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		try {
            ObjectOutput out = new ObjectOutputStream(b);
            out.writeObject(this);
            out.flush();
            return b.toByteArray();
        } catch (IOException e) {
        	e.printStackTrace();
            System.err.println("Falha ao serializar mensagem");
        }
		return null;
	}

	@Override
	public int compareTo(Object o) {
		Message m1 = (Message)o;
		if(m1.getTimestamp() > this.getTimestamp()) { 
			return -1;
		} else if (m1.getTimestamp() == this.getTimestamp()) {
			return this.getProcessID().compareTo(m1.getProcessID());
		} else {
			return 1;
		}
	}
}
