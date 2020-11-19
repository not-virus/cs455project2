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
    // Input Scanner
    private static Scanner input = new Scanner(System.in);

	public static void main(String[] args) throws Exception {
	    // Command Interpreter
	    CLInputParser clip = new CLInputParser(System.in);
	    
	    // RSACipher class for encryption and decryption 
	    RSACipher rsa = new RSACipher();

	    // Key pair generation and management
	    RSAKeyPairManager keyMgr = new RSAKeyPairManager(2048);

	    System.out.println("   --- Welcome to CKSM v1.0 ---   ");
	    System.out.println("Type !help for a list of commands.");
	    //CLCommandInterpreter.Command next = clcli.command();
	    
	    // Start server on port 3000
	    ServerRunner serverThread = new ServerRunner(localPort);
	    
	    // Create thread to wait for c keypress    
	    CommandDetector commandThread = new CommandDetector(clip);
	    
	    // Start server thread
	    serverThread.setName("Server");
	    serverThread.start();
	    
	    System.out.println("Server listening for connections on port " + localPort);
	    System.out.println("Awaiting connection. Use \"!connect\" to connect to a remote host.");
	    
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
	        if (commandThread.getCommand().command == CLToken.Commands.CONNECT
	            || !commandThread.isAlive()) {
	            next = NextAction.CONNECT_TO_HOST;
	            break;
	        }
	    }
	    
	    // Declare this for use later, if client connection is received
	    Server server = null;
	    if (next == NextAction.ACCEPT_CONNECTION) {
	        System.out.println("Connection from client received.");
	        server = serverThread.getServer();
	        serverThread.close();
	        
	        System.out.println("Awaiting message from client...");
	        
	        // Get incoming message
	        String incoming = new String();
	        incoming = new String(server.readAllBytes(), StandardCharsets.UTF_8);
	        
	        System.out.println("Message from client: ");
	        System.out.print("> ");
	        System.out.println(incoming);
	        
	        System.out.println("End of message. Goodbye.");
	        
           if (server != null) {
                server.close();
            }
           
           input.close();
           
           System.exit(0);	        
	    }
	    else if (next == NextAction.CONNECT_TO_HOST) {
	        boolean connectSuccessful = false;
	        Client client = null;
	        String serverAddress = "[no address]";
	        int serverPort = -1;
	        
	        while (!connectSuccessful) {
	            // Get remote server info
	            System.out.println("Server address?");
	            serverAddress = clip.ipAddress();
	            System.out.println("Server port number?");
	            serverPort = clip.port();
	            
	            // Reject existing listening ServerSocket
	            while (serverAddress == localAddress && serverPort == localPort) {
	                System.out.println("Cannot connect to self! Please choose another host.");
	                System.out.println("Server address?");
	                serverAddress = clip.ipAddress();
	                System.out.println("Server port number?");
	                serverPort = clip.port();
	            }
	            
	            System.out.println("Authenticate server using RSA public key? (yes/no)");
	            boolean authenticateServer = clip.yesNo();
	            
	            // FIXME Need to add a conditional block in here which executes if authFlag is true
	            //  Will need to obtain server's public key, encrypt a message with it, then see if server can decrypt, re-encrypt and return
	            if (authenticateServer) {
	                // FIXME
	            }
	            
	            System.out.println("Attempting connection to " + serverAddress + " on port " + serverPort + " ...");
	            
	            // Establish a connection to the server
	            try {
	                client = new Client(serverAddress, serverPort);
	                connectSuccessful = true;
	            } catch (ConnectException e) {
	                System.out.println("ERROR: Failed to connect to " + serverAddress + " on port " + serverPort + ".");
	                connectSuccessful = false;
	            } catch (UnknownHostException e) {
	                System.out.println("ERROR: Host " + serverAddress + " not reachable, is down or does not exist.");
	                connectSuccessful = false;
	            }
	        }
	        
	        if (client != null)
	        {
	            System.out.println("Connected to " + serverAddress + ":" + serverPort + ".");
	            System.out.println("----------");
	            
	            // Get message from user
	            System.out.println("Enter message to server. Max. 2048 lines. Use \"!done\" when finished.");
	            String messageStr = clip.message();
	            
	            // Convert message to byte array
	            byte[] messageBytes = messageStr.getBytes(StandardCharsets.UTF_8);
	            
	            // Send it!
	            client.write(messageBytes);
	            
	            client.close();
	            
	            System.out.println("Message sent. Goodbye.");
	        } else {
	            System.out.println("Could not connect to server. Goodbye.");
	        }
	        
	        input.close();
	        
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
	
	public static String nextLine() {
	    System.out.print("cksm> ");
	    return input.nextLine();
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