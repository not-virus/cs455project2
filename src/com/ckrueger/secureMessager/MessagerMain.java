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
    
    private static enum Action {
        NONE,
        ACCEPT_CONNECTION,
        CONNECT_TO_HOST,
        LOAD_KEYS,
        AUTH,
        HELP,
        QUIT
    }
    
    private static String localAddress;
    private static int localPort = 3001;
    // Input Scanner
    //private static Scanner input = new Scanner(System.in);

	public static void main(String[] args) throws Exception {
	    
	    // RSACipher class for encryption and decryption 
	    RSACipher rsa = new RSACipher();

	    // Key pair generation and management
	    RSAKeyPairManager keyMgr = new RSAKeyPairManager(2048);

	    System.out.println("   --- Welcome to CKSM v1.0 ---   ");
	    System.out.println("Type !help for a list of commands.");
	    
	    // Start server on port 3000
	    ServerRunner serverThread = new ServerRunner(localPort);
	    
	    // Start server thread
	    serverThread.setName("Server");
	    serverThread.start();
	    
	    System.out.println("Started local server.");
	    System.out.println("Listening for connections on port " + localPort);
	    System.out.println("Use \"!connect\" to connect to a remote host.");
	    
	    // Attempt to release port if shutdown unexpectedly
        ShutdownHook shutdownHook = new ShutdownHook(serverThread.getServer());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        
        // Specifies next action
        Action next = Action.NONE;
        
        // Program main loop
        while (next != Action.QUIT) {
            // Command Interpreter
            CLInputParser clip = new CLInputParser(System.in);
            // Create thread to wait for c keypress    
            CommandDetector commandThread = new CommandDetector();
            
            // Start command listener thread
            commandThread.setName("command detector");
            commandThread.start();
            
            // Server
            Server server = null;
            
            // Wait for something to happen
            while (next == Action.NONE)
            {
                if (serverThread.connectionAvailable()) {
                    
                    // Stop waiting for user input
                    commandThread.close();
                    
                    // Get the server instance from server thread and close server thread
                    server = serverThread.getServer();
                    serverThread.close();
                    
                    // Prompt user to see if they'd like to accept the connection
                    System.out.println("\nConnection available from server at " + server.getRemoteAddress());
                    System.out.println("Accept connection from server? (y/n)");
                    // Command Interpreter
                    CLInputParser connectConfirm = new CLInputParser(System.in);
                    boolean accept = connectConfirm.yesNo();
                    connectConfirm.close();
                    
                    // Take action if yes
                    if (accept) {
                        System.out.println("You have chosen to connect.\n");
                        next = Action.ACCEPT_CONNECTION;
                    } else {
                        System.out.println("You have chosen to reject the connection.");
                        // Start waiting for user input again
                        commandThread = new CommandDetector();
                        commandThread.start();
                    }
                } else if (commandThread.getCommand().command == CLToken.Commands.CONNECT) {
//                    || !commandThread.isAlive()) {
                    next = Action.CONNECT_TO_HOST;
                    break;
                }
                /*if (commandThread.getCommand().command == CLToken.Commands.AUTH) {
                    next = NextAction.AUTH;
                    break;
                }*/
                else if (commandThread.getCommand().command == CLToken.Commands.LOAD) {
                    next = Action.LOAD_KEYS;
                    break;
                }
                else if (commandThread.getCommand().command == CLToken.Commands.QUIT) {
                    next = Action.QUIT;
                    break;
                }
                else if (commandThread.getCommand().command == CLToken.Commands.HELP) {
                    next = Action.HELP;
                    break;
                }
            }
            
  // ##### SERVER SECTION #####
            if (next == Action.ACCEPT_CONNECTION) {
                // CLInputParser for server connection
                CLInputParser serverClip = new CLInputParser(System.in);
                CommandDetector serverCT = new CommandDetector();
                
                System.out.println("Connected to client.");                
                System.out.println("Awaiting message from client...");
                System.out.println("Type \"!message\" to send a message to " +
                        "the client or type \"!disconnect\" to \nterminate " +
                        "your session with this client.");
                
                boolean disconnect = false;
                
                // Start waiting for commands
                serverCT.start();
                
                while (!disconnect) {
                    //serverCT.getCommand().command == CLToken.Commands.NONE

                    if (server.available() > 0) {
                        serverCT.close();
                        
                        System.out.println("\n----------------------------------------");
                        System.out.println("\nNew message from client:");
                        
                        // Get incoming message
                        String messageFromClient = null;
                        try {
                            messageFromClient = new String(server.readAllBytes(), StandardCharsets.UTF_8);
                        } catch (SocketException e) {
                            System.out.println("NOTICE: Client terminated the connection.");
                            disconnect = true;
                        }
                        
                        String[] messageArr = messageFromClient.split("\n");
                        for (int line = 0; line < messageArr.length; line++) {
                            System.out.println("  " + messageArr[line]);
                        }
                        
                        System.out.println("End of message from client.");
                        System.out.println("----------------------------------------\n");
                        
                        
                        // This is the worst possible way to do this
                        serverCT = new CommandDetector();
                        serverCT.start();                                                
                    } else if (serverCT.getCommand().command == CLToken.Commands.MESSAGE) {
                        serverCT.close();
                        
                        System.out.println("\n----------------------------------------");
                        System.out.println("Enter message to client. Max. 2048 lines. Use \"!done\" to send.");
                        String message = serverClip.message();

                        server.write(message.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Your message has been sent to the client.");
                        System.out.println("----------------------------------------\n");
                        
                        // This is the worst possible way to do this
                        serverCT = new CommandDetector();
                        serverCT.start();
                    } else if (serverCT.getCommand().command == CLToken.Commands.DISCONNECT) {
                        serverCT.close();
                        System.out.println("You have chosen to disconnect from the client.");
                        System.out.println("Are you sure? (y/n)");
                        disconnect = serverClip.yesNo();      
                        
                        if (!disconnect) {
                            // This is the worst possible way to do this
                            serverCT = new CommandDetector();
                            serverCT.start(); 
                        }
                    } else if (serverCT.getCommand().command != CLToken.Commands.NONE) {
                        System.out.println(serverCT.getCommand().value + ": command not available at this time.");
                        serverCT.close();
                        serverCT.getCommand().command = CLToken.Commands.NONE;
                        serverCT = new CommandDetector();
                        serverCT.start();
                    }                    
                }
                
                System.out.println("Disconnected from client");
                
                // Disconnect from client
                server.close();
                server = null;

                // Restart server thread
                serverThread = new ServerRunner(localPort);
                serverThread.setName("Server");
                serverThread.start();
                
                // Rest loop
                next = Action.NONE;                
            }
            
  // ##### CLIENT SECTION #####
            else if (next == Action.CONNECT_TO_HOST) {
                boolean connectSuccessful = false;
                Client client = null;
                String serverAddress = "[no address]";
                int serverPort = -1;
                // Command Interpreter
                CLInputParser clientClip = new CLInputParser(System.in);
                
                while (!connectSuccessful) {
                    // Get remote server info
                    System.out.println("Server address?");
                    serverAddress = clientClip.ipAddress();
                    System.out.println("Server port number?");
                    serverPort = clientClip.port();
                    
                    // Reject existing listening ServerSocket
                    while (serverAddress == localAddress && serverPort == localPort) {
                        System.out.println("Cannot connect to self! Please choose another host.");
                        System.out.println("Server address?");
                        serverAddress = clientClip.ipAddress();
                        System.out.println("Server port number?");
                        serverPort = clientClip.port();
                    }
                    
                    System.out.println("Authenticate server using RSA public key? (y/n)");
                    boolean authenticateServer = clientClip.yesNo();
                    
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
                    System.out.println("\n----------------------------------------");
                    
                    CommandDetector clientCT = new CommandDetector();
                    
                    System.out.println("Awaiting message from server...");
                    System.out.println("Type \"!message\" to send a message to " +
                            "the server or type \"!disconnect\" to \nterminate " +
                            "your session with this server.");
                    
                    boolean disconnect = false;
                    
                    // Start waiting for commands
                    clientCT.start();
                    
                    while (!disconnect) {
                        //serverCT.getCommand().command == CLToken.Commands.NONE

                        if (client.available() > 0) {
                            if (clientCT != null) {
                                clientCT.close();
                            }
                            
                            System.out.println("\n----------------------------------------");
                            System.out.println("\nNew message from server:");
                            
                            // Get incoming message
                            String messageFromServer = null;
                            try {
                                messageFromServer = new String(client.readAllBytes(), StandardCharsets.UTF_8);
                            } catch (SocketException e) {
                                System.out.println("NOTICE: Server terminated the connection.");
                                disconnect = true;
                            }
                            
                            String[] messageArr = messageFromServer.split("\n");
                            for (int line = 0; line < messageArr.length; line++) {
                                System.out.println("  " + messageArr[line]);
                            }
                            
                            System.out.println("End of message from server.");
                            System.out.println("----------------------------------------\n");
                            
                            
                            // This is the worst possible way to do this
                            clientCT = new CommandDetector();
                            clientCT.start();                                                
                        } else if (clientCT.getCommand().command == CLToken.Commands.MESSAGE) {
                            clientCT.close();
                            
                            System.out.println("\n----------------------------------------");
                            System.out.println("Enter message to server. Max. 2048 lines. Use \"!done\" to send.");
                            String message = clientClip.message();

                            client.write(message.getBytes(StandardCharsets.UTF_8));
                            System.out.println("Your message has been sent to the client.");
                            System.out.println("----------------------------------------\n");
                            
                            // This is the worst possible way to do this
                            clientCT = new CommandDetector();
                            clientCT.start();
                        } else if (clientCT.getCommand().command == CLToken.Commands.DISCONNECT) {
                            clientCT.close();
                            System.out.println("You have chosen to disconnect from the client.");
                            System.out.println("Are you sure? (y/n)");
                            disconnect = clientClip.yesNo();   
                            
                            if (!disconnect) {
                                // This is the worst possible way to do this
                                clientCT = new CommandDetector();
                                clientCT.start(); 
                            }
                        } else if (clientCT.getCommand().command != CLToken.Commands.NONE) {
                            System.out.println(clientCT.getCommand().value + ": command not available at this time.");
                            clientCT.close(); 
                            clientCT.getCommand().command = CLToken.Commands.NONE;
                            clientCT = new CommandDetector();
                            clientCT.start(); 
                        }
                    }
                    
                    System.out.println("Disconnected from server.");
                    
                    // Close client
                    if (client != null) {
                        client.close();   
                    }
                    
                    // Rest loop
                    next = Action.NONE;
                } else {
                    System.err.println("ERROR: Server still null.");
                    System.out.println("Terminating...");
                    next = Action.QUIT;
                }
            }   
            
        }
        
        System.out.println("Goodbye.");
        
        //input.close();
        
        System.exit(0);    	    
	    
	    
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
		
		//input.close();        
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