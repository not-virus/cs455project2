package com.ckrueger.secureMessager;

import com.ckrueger.secureMessager.*;
import com.ckrueger.secureMessager.crypto.*;
import com.ckrueger.secureMessager.comms.*;
import java.lang.System;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import javax.crypto.*;
import java.lang.Runtime;

public class MessagerMain {
    
    private static enum NextAction {
        NONE,
        ACCEPT_CONNECTION,
        CONNECT_TO_HOST
    }
    
    private static String localAddress;
    private static int localPort = 3001;

	public static void main(String[] args) throws Exception {
	    // Input Scanner
	    Scanner input = new Scanner(System.in);
	    
	    // RSACipher class for encryption and decryption 
	    RSACipher rsa = new RSACipher();

	    // Key pair generation and management
	    RSAKeyPairManager keyMgr = new RSAKeyPairManager(2048);

	    System.out.println(" --- Welcome to CKSM --- ");
	    System.out.println("Awaiting connection. Use \"connect\" to connect to a remote host.");
	    
	    // Start server on port 3000
	    ServerRunner serverThread = new ServerRunner(localPort);
	    
	    // Create thread to wait for c keypress    
	    CommandDetector commandThread = new CommandDetector("connect", input);
	    
	    // Start server thread
	    serverThread.setName("Server");
	    serverThread.start();
	    
	    // Attempt to release port if shutdown unexpectedly
        ShutdownHook shutdownHook = new ShutdownHook(serverThread.getServer());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
	    
        // Start command listener thread
        commandThread.setName("command detector");
	    commandThread.start();
	    
	    // Wait for one of two things to happen
	    NextAction next = NextAction.NONE;
	    while (true)
	    {
	        if (serverThread.connectionAvailable()) {
	            next = NextAction.ACCEPT_CONNECTION;
	            break;
	        }
	        if (commandThread.success() || !commandThread.isAlive()) {
	            next = NextAction.CONNECT_TO_HOST;
	            break;
	        }
	        //System.out.println("Server exists? " + serverThread.getServer());
	    }
	    
	    // Declare this for use later, if client connection is received
	    Server messageServer = null;
	    if (next == NextAction.ACCEPT_CONNECTION) {
	        System.out.println("Connection from client received");
	        messageServer = serverThread.getServer();
	        serverThread.close();
	        
	        // Get incoming message
	        String incoming = null;
	        
	        while (incoming == null) {
	            // Get incoming message
	            try {
	                incoming = new String(messageServer.readAllBytes(), StandardCharsets.UTF_8);
	            } catch (NullPointerException e) {
	                ;
	            }
	        }
	        
	        System.out.println("Message from client: ");
	        System.out.println(incoming);
	        
	        System.out.println("End of message. Goodbye.");
	        
	    }
	    else if (next == NextAction.CONNECT_TO_HOST) {
	        // Get remote server info
	        System.out.print("Server address? ");
	        String serverAddress = input.nextLine();
	        System.out.print("Server port number? ");
	        int serverPort = input.nextInt();
	        input.nextLine();
	        
	        // Reject existing listening ServerSocket
	        /*while (serverAddress == localAddress && serverPort == localPort) {
	            System.out.println("Cannot connect to self! Please choose another port.");
	            System.out.print("Server port number? ");
	            serverPort = input.nextInt();
	        }*/
	        System.out.println("Attempting connection to server...");
	        
	        // Establish a connection to the server
	        Client client = null;
	        try {
	            client = new Client(serverAddress, serverPort);
	        } catch (ConnectException e) {
	            System.out.println("Failed to connect to " + serverAddress + ":" + serverPort);
	        }
	        
	        if (client != null)
	        {
	            System.out.println("Connected to " + serverAddress + ":" + serverPort);
	            
	            // Get message from user
	            System.out.println("Message?");
	            String messageStr = input.nextLine();
	            
	            // Convert message to byte array
	            byte[] messageBytes = messageStr.getBytes(StandardCharsets.UTF_8);
	            
	            // Send it!
	            client.write(messageBytes);
	            
	            System.out.println("Message sent. Goodbye.");
	        } else {
	            System.out.println("Could not connect to server. Goodbye.");
	        }
	        
	        System.exit(0);
	    }	    	    
	    
	    
	    /*
	    
	    // Get message from user
		System.out.println("Message?");
		String messageStr = input.nextLine();
        
		// Convert message to byte array
        byte[] messageBytes = messageStr.getBytes(StandardCharsets.UTF_8);

        // Encrypt message
        byte[] messageEnc = null;
		try {
            messageEnc = rsa.encrypt(messageBytes, keyMgr.getPublicKey());
        } catch (InvalidKeyException e) {
            System.out.println("WARNING Public key not valid for encryption");
            e.printStackTrace();
        }
		
		// Display encrypted message
		System.out.println("Encrytped message: ");
		String messageEncStr = new String(messageEnc, StandardCharsets.UTF_8);
		System.out.println(messageEncStr);
		
		
		// Decrypt message
        byte[] messageDec = null;
        try {
            messageDec = rsa.decrypt(messageEnc, keyMgr.getPrivateKey());
        } catch (InvalidKeyException e) {
            System.out.println("WARNING Private key not valid for decryption");
            e.printStackTrace();
        }

        // Display decrypted message
		System.out.println("Decrypted message: ");
		String messageDecStr = new String(messageDec, StandardCharsets.UTF_8);
        System.out.println(messageDecStr);
        */
		
		System.out.println("Done");
		
		input.close();        
	}
	
}

/*
// FOR TESTING ONLY
RSAPublicKeyManager rpkm = new RSAPublicKeyManager(keyMgr.getPublicKey());

// Write keyfile
String fileName = "test";
Path filePath = Paths.get("C:\\Workspaces\\JavaProjects\\secureMessager\\keyfiles\\" + fileName);
System.out.println("Attempting to write public key to file...");
System.out.println("Public key:");
System.out.println(rpkm.getKey());
System.out.println("Output file path:");
System.out.println(filePath + ".pub");
rpkm.writeKey(filePath);

// Read keyFile
System.out.println("Attempting to load public key from file...");
System.out.println("Input file path:");
System.out.println(filePath + ".pub");
rpkm.loadKey(filePath);
System.out.println("Public key:");
System.out.println(rpkm.getKey());
*/