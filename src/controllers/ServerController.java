package controllers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import models.LogicalClock;
import models.Message;
import models.RequestCritical;
import network.Server;

public class ServerController {

	
	private static ServerController serverController = null;
	private LogicalClock clock = new LogicalClock();
	private PriorityQueue<Message> orderedMessages = new PriorityQueue<Message>();
	private Server server;
	private String processID;
	private ConcurrentHashMap<String, Boolean> connectedMembers = new ConcurrentHashMap<String, Boolean>();
	private Thread runner;
	private RequestCritical currentSelfRequest = null;
	private boolean waitingForCriticalSection = false;
	private boolean usingCriticalSection = false;
	private Queue<Object[]> deniedRequests = new LinkedList<Object[]>();
	
	// Singleton Implementation
	private ServerController () {}
	
	public static ServerController getInstance () {
		if (serverController == null) {
			serverController = new ServerController();
			serverController.processID = serverController.generateProcessID();
			
		}
		return serverController;
	}
	
	/**
	 * Generate a random ID to process
	 * 
	 * @param void
	 * @return processID - A random key to process.
	 * */
	private String generateProcessID() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Mount a message to send
	 * 
	 * @param String protocolHeader - An action to be executed in server
	 * @param Object content - The message's content
	 * @return void
	 * */
	public void sendJoinMessage () {
		try {
			clock.tick();
			Message message = new Message("001", "", clock.getTimestamp(), processID);
			this.send(message.serialize());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Mount a message to send
	 * 
	 * @param String protocolHeader - An action to be executed in server
	 * @param Object content - The message's content
	 * @return void
	 * */
	public void sendMessage (String protocolHeader, Object content) {
		try {
			Message message = new Message(protocolHeader, content, clock.getTimestamp(), processID);
			this.send(message.serialize());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Mount a message to send
	 * 
	 * @param String protocolHeader - An action to be executed in server
	 * @param Object content - The message's content
	 * @param ConcurrentHashMap confirmationsNeeded - The expected confirmations
	 * @return void
	 * */
	public void sendMessage (String protocolHeader, Object content, ConcurrentHashMap<String, Boolean> confirmationsNeeded) {
		try {
			Message message = new Message(protocolHeader, content, clock.getTimestamp(), processID);
			message.setConfirmations(confirmationsNeeded);
			this.send(message.serialize());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Receive a message and put in queue
	 * 
	 * @param String protocolHeader - An action to be executed in server
	 * @param Object content - The message's content
	 * @return void
	 * */
	public void registerEvent (Message message) {
		clock.tick(message.getTimestamp());
		orderedMessages.add(message);
	}
	
	/**
	 * Send a message to group
	 * 
	 * @param byte[] message - A serialized message
	 * @return void
	 * */
	public synchronized void send (byte[] message) throws IOException {
		DatagramPacket packet = new DatagramPacket(message, message.length, server.getFormattedGroupAddress(), server.getGroupPort());
		MulticastSocket socket = new MulticastSocket();
		socket.send(packet);
		socket.close();
	}
	
	/**
	 * Define a server to this controller
	 * 
	 * @param Server server - The application's server
	 * @return void
	 * */
	public void setServer (Server server) {
		this.server = server;
	}
	
	/**
	 * Get the processID
	 * 
	 * @param void
	 * @return String processID - A process ID
	 * */
	public String getProcessID () {
		return this.processID;
	}
	
	public synchronized void addToGroup (String processID) {
		this.connectedMembers.put(processID, false);
	}
	
	/**
	 * Return the group size
	 * 
	 * @return int - The group size
	 * */
	public int getGroupSize () {
		return this.connectedMembers.size();
	}
	
	/**
	 * Clear expected confirmations
	 * 
	 * @return void
	 * */
	public void clearGroup () {
		this.connectedMembers = new ConcurrentHashMap<String, Boolean>();
	}
	
	/**
	 * Send a confirmation to another process
	 * 
	 * @param String processID - A processID
	 * @param Integer messageTimestamp - A message timestamp
	 * @param Message message - A message
	 * @return void
	 * */
	public void sendConfirmation(String processID, Integer messageTimestamp, Message message) {
		if (!message.getConfirmations().containsKey(this.getProcessID())) return;
		 System.out.println("Enviando confirmação para o solicitante: " + processID);
		this.sendMessage("005", new String[] {this.getProcessID(), message.getProcessID()});
	}
	
	/**
	 * Register a confirmation
	 * 
	 * @param String processID - A process ID
	 * @return void
	 * */
	public void validateEvent(String processID) {
		if (orderedMessages.size() == 0) return;
		orderedMessages.peek().receiveConfirmation(processID);
	}

	/**
	 * Delivery the event to application
	 * @param String processID - A processID
	 * @param Integer messageTimestamp - A message timestamp
	 * @param Message message - A message
	 * @return void
	 * */
	public void sendToApplication(String processID, Integer messageTimestamp, Message message) {
		if (!message.getConfirmations().containsKey(this.getProcessID())) return;
		removeFromQueue();
	}
	
	
	/**
	 * Process a queue of events
	 * 
	 * @param void
	 * @return void
	 * */
	public void queueProcessor () {
		this.runner = new Thread () {
			public void run () {
				while(true) {
					try {
						sleep(500);
						int currentSize = orderedMessages.size();
						if (currentSize > 0 ) {
							Message currentMessage = orderedMessages.peek();
							if (currentSelfRequest != null) {
								sendMessage("004", currentSelfRequest, connectedMembers); // Request a confirmation								
							}
							sleep(2000);
							if (orderedMessages.size() > 0) {
								if (orderedMessages.peek().isConfirmed()) {
									sendMessage("006", new Object[] {currentMessage.getProcessID(), currentMessage.getTimestamp()}, orderedMessages.peek().getConfirmations());
									sleep(500);
									 System.out.println("Posso usar agora");
									enterSection();
									removeFromQueue();
								}								
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}				
			}
		};
		
		runner.start();
	}
	
	/**
	 * Request a critical section to group
	 * 
	 * @param void
	 * @return void
	 * */
	public void requestCriticalSection() {
		if (this.waitingForCriticalSection) return;

		Thread c = new Thread () {
			public void run () {
				try {
					clearGroup();
					sendMessage("001", ""); // Find active nodes
					Thread.sleep(2000);
					// Send a message with event
					clock.tick();
					currentSelfRequest = new RequestCritical("criticalSection", processID, clock.getTimestamp());
					waitingForCriticalSection = true;
					sendMessage("003", currentSelfRequest, connectedMembers);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};		
		
		c.start();
	}
	
	/**
	 * Remove the head of queue
	 * 
	 * @param void
	 * @return void
	 * */
	private synchronized void removeFromQueue () {
		orderedMessages.poll();
	}
	
	
	private synchronized void criticalSection () throws IOException, InterruptedException {
		Path folder = Paths.get("src/main/resources/").toAbsolutePath();
		Path path = Paths.get("src/main/resources/mutual_exclusion.txt").toAbsolutePath();
		
		if (Files.notExists(folder)) {	Files.createDirectories(folder); }		
		if (Files.notExists(path)) { Files.createFile(path); }
		
		String text = "O processo " + this.getProcessID() + " usou a seção crítica \n";
		Files.write(path, text.getBytes(), StandardOpenOption.APPEND);
		System.err.println("Executei o código da seção crítica. Sou o processo:" + this.getProcessID());
		
		Thread.sleep(2000);
	}
	
	public synchronized void determineCriticalSectionUser(RequestCritical request, Message message) {
		
		if (this.usingCriticalSection) {
			deniedRequests.add(new Object[] {request, message});
		} else if (this.waitingForCriticalSection) {
			if (this.clock.getTimestamp() > request.getTimestamp()) { // This process has priority
				this.sendConfirmation(request.getProcessID(), request.getTimestamp(), message);			
			} else if (this.clock.getTimestamp() < request.getTimestamp()) { // Another process has priority
				deniedRequests.add(new Object[] {request, message});
			} else {
				if (this.getProcessID().compareTo(request.getProcessID()) < 0) { // Another process has priority
					this.sendConfirmation(request.getProcessID(), request.getTimestamp(), message);
				}else { // This process has priority
					deniedRequests.add(new Object[] {request, message});
				}
			}			
		} else { // Another process has priority
			this.sendConfirmation(request.getProcessID(), request.getTimestamp(), message);
		}
	}
	
	public void enterSection () {		
		try {
			System.err.println("Entrei na seção crítica");
			this.usingCriticalSection = true;
			criticalSection();
			exitSection();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void exitSection() {
		this.waitingForCriticalSection = false;
		this.usingCriticalSection = false;
		
		while(this.deniedRequests.size() > 0) {
			Object[] data = this.deniedRequests.poll();
			RequestCritical request = (RequestCritical) data[0];
			this.sendConfirmation(request.getProcessID(), request.getTimestamp(), (Message)data[1]);
		}
		
		System.err.println("Saí da seção crítica");
		this.deniedRequests = new LinkedList<Object[]>();
	}
}
