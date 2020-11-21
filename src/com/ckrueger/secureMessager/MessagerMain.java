package com.ckrueger.secureMessager;

import com.ckrueger.secureMessager.*;
import com.ckrueger.secureMessager.crypto.*;
import com.ckrueger.secureMessager.comms.*;
import java.lang.System;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.*;
import java.lang.Runtime;

public class MessagerMain {
    
    private static enum Action {
        NONE,
        ACCEPT_CONNECTION,
        CONNECT_TO_HOST,
        LOAD_KEYS,
        SAVE_KEYS,
        GENERATE_KEYS,
        AUTH,
        
        HELP,
        QUIT
    }
    
    private static String localAddress;
    private static int localPort = 3001;
    private final static String DEFAULT_KEY_FILE_PATH = "." + File.separator + "key_files";
            //"C:\\Workspaces\\JavaProjects\\secureMessenger\\key_files";
        //System.getProperty("user.home")+ File.separator + ".ssh"
        //+ File.separator;
    private final static String DEFAULT_PRIVATE_KEY_FILE_NAME = "key_rsa";
    private final static String DEFAULT_PUBLIC_KEY_FILE_NAME = "key_rsa.pub";
    
    private final static String DEFAULT_REMOTE_KEY_FILE_PATH = "." + File.separator + "remote_key_files";
            //"C:\\Workspaces\\JavaProjects\\secureMessenger\\remote_key_files";
        //System.getProperty("user.home")+ File.separator + ".ssh"
        //+ File.separator;
    private final static String DEFAULT_REMOTE_PUBLIC_KEY_FILE_NAME = "remote_key_rsa.pub";
    private final static byte[] PK_MESSAGE_PREFIX = {0x01, 'P', 'r', 'i', 'v', 'K', 'e', 'y', 0x02};

	public static void main(String[] args) throws Exception {
	    
	    // RSACipher class for encryption and decryption 
	    RSACipher rsa = new RSACipher();

	    System.out.println("   --- Welcome to CKSM v1.0 ---   ");
	    
	    CLInputParser mainClip = new CLInputParser(System.in);
	    
	    // Load RSA keys from file
	    RSAKeyPairManager rsakpm = loadKeys();
	    
	    // Handle load failure
	    boolean loadRetry = true;
	    while (rsakpm == null && loadRetry == true) {
	        System.out.println("Failed to load one or both keys from files.");
	        System.out.println("You can try to load the keys again or generate new keys.");
	        System.out.println("Would you like to try again? (y/n)");
	        loadRetry = mainClip.yesNo();
	        
	        if (loadRetry) {
	            rsakpm = loadKeys();
	        }
	    }
	    
	    // If keys were not loaded and user chooses not to try again, generate new keys
	    if (rsakpm == null) {
	        System.out.println("You have chosen to generate new keys.");
	        System.out.println("Generating new keys...");
	        rsakpm = new RSAKeyPairManager(2048);
	        
	        // If still unable to generate keys, terminate. Something is wrong!
	        if (!rsakpm.privateKeySet() || !rsakpm.publicKeySet()) {
	            System.out.println("FATAL ERROR: Failed to generate new key " +
	                    "pair of size " + 2048 + "bits.");
	            System.out.println("Terminating...");
	            System.exit(1);
	        }
	        
	        System.out.println("Successfully generated new key pair.");
	        System.out.println("Would you like to save these keys?");
	        boolean save = mainClip.yesNo();
	        
	        if (save) {
	            boolean saveSuccess = saveKeys(rsakpm);
	            
	            // Retry until success or skip
	            boolean saveRetry = true;
	            while (!saveSuccess && saveRetry) {
	                System.out.println("Failed to save keys. Retry? (y/n)");
	                saveRetry = mainClip.yesNo();
	                
	                if (saveRetry) {
	                    saveSuccess = saveKeys(rsakpm);   
	                } else {
	                    System.out.println("Skipping. Keys will not be saved.");
	                }
	            }
	        }
	    }
	    
	    // Key pair generation and management
        RSAKeyPairManager keyMgr = new RSAKeyPairManager(2048);
        
	    System.out.println("Type !help for a list of commands.");
	    
	    // Start server on port 3000
	    ServerRunner serverThread = new ServerRunner(localPort);
	    
	    // Start server thread
	    serverThread.setName("Server");
	    serverThread.start();
	    
	    System.out.println("\n----------------------------------------");
	    System.out.println("Started local server. Listening for connections on port " + localPort);
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
                else if (commandThread.getCommand().command == CLToken.Commands.GENERATE) {
                    next = Action.GENERATE_KEYS;
                    break;
                }
                else if (commandThread.getCommand().command == CLToken.Commands.SAVE) {
                    System.out.println("This functionality has not yet been implemented!");
                    //next = Action.SAVE_KEYS;
                    //break;
                }
                else if (commandThread.getCommand().command == CLToken.Commands.LOAD) {
                    System.out.println("This functionality has not yet been implemented!");
                    //next = Action.LOAD_KEYS;
                    //break;
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
                
                System.out.println("Would you like to authenticate the client? (y/n)");
                boolean authClient = serverClip.yesNo();
                
                RSAPublicKeyManager rpkm = null;
                
                if (authClient) {
                    server.write("AUTH".getBytes());
                    
                    rpkm = loadRemotePublic();
                    
                    // Authenticate using a PublicKey as a randomly generated string of bytes
                    byte[] authData = new byte[64];
                    new Random().nextBytes(authData);
                    
                    // Encrypt with remote host's public key
                    byte[] authDataEnc = null;
                    try {
                        authDataEnc = rsa.encrypt(authData, rpkm.getKey());
                        System.out.println("Encrypted authentication string.");
                    } catch (InvalidKeyException e) {
                        System.out.println("ERROR: Public key not valid for encryption");
                        e.printStackTrace();
                    }
                    
                    // Send encrypted auth server
                    server.write(authDataEnc);
                    
                    // Get respoonse from server, decrypt and verify
                    byte[] authResponseEnc = server.readAllBytes();
                    byte[] authResponse = rsa.decrypt(authResponseEnc, rsakpm.getPrivateKey());
                    
                    if (!(authResponse == authData)) {
                        System.out.println("ERROR: Failed to authenticate server.");
                    } else {
                        System.out.println("Successfully authenticated server.");
                    }                    
                }
                
                
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
                            if (!messageArr[line].strip().contentEquals("")) {
                                System.out.print("  " + messageArr[line]);
                            }
                        }
                        
                        System.out.println("\nEnd of message from client.");
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
                    
                    // Get server's public key
                    RSAPublicKeyManager serverRpkm = null;
                    serverRpkm = loadRemotePublic();
                    
                    if (client.readAllBytes() == "AUTH".getBytes()) {
                        System.out.println("Server has requested authentication.");

                        // Now that we have the server's public key, get the server's
                        //  authentication message, decrypt, re-encrypt and send
                        byte[] authMessageEnc = client.readAllBytes();
                        byte[] authMessage = rsa.decrypt(authMessageEnc, rsakpm.getPrivateKey());
                        byte[] returnAuthEnc = rsa.encrypt(authMessage, serverRpkm.getKey());
                        client.write(returnAuthEnc);
                        
                        System.out.println("Returned authentication message. Awaiting response from server...");
                        
                        if (client.readAllBytes() == "SECURED".getBytes())
                        {
                            System.out.println("Successfully authenticated server.");
                        } else {
                            System.out.println("ERROR: Failed to authenticate server");
                        }
                    }
                    
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
                            System.out.println("Your message has been sent to the server.");
                            System.out.println("----------------------------------------\n");
                            
                            // This is the worst possible way to do this
                            clientCT = new CommandDetector();
                            clientCT.start();
                        } else if (clientCT.getCommand().command == CLToken.Commands.DISCONNECT) {
                            clientCT.close();
                            System.out.println("You have chosen to disconnect from the server.");
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
                    System.err.println("ERROR: Server object still null.");
                    System.out.println("Terminating...");
                    next = Action.QUIT;
                }
            }
            
  // ##### MANUAL KEYPAIR GENERATION #####
            else if (next == Action.GENERATE_KEYS) {
                CLInputParser genClip = new CLInputParser(System.in);
                
                // Confirm
                System.out.println("You have chosen to generate new keys.");
                System.out.println("Are you sure? (y/n)");
                boolean proceed = genClip.yesNo();
                
                // If user confirms, generate new keys
                if (proceed) {
                    System.out.println("Generating new keys...");
                    rsakpm = new RSAKeyPairManager(2048);
                    
                    // If still unable to generate keys, terminate. Something is wrong!
                    if (!rsakpm.privateKeySet() || !rsakpm.publicKeySet()) {
                        System.out.println("FATAL ERROR: Failed to generate new key " +
                                "pair of size " + 2048 + "bits.");
                        System.out.println("Terminating...");
                        System.exit(1);
                    }
                    
                    System.out.println("Successfully generated new key pair.");
                    System.out.println("Would you like to save these keys?");
                    boolean save = mainClip.yesNo();
                    
                    if (save) {
                        boolean saveSuccess = saveKeys(rsakpm);
                        
                        // Retry until success or skip
                        boolean saveRetry = true;
                        while (!saveSuccess && saveRetry) {
                            System.out.println("Failed to save keys. Retry? (y/n)");
                            saveRetry = mainClip.yesNo();
                            
                            if (saveRetry) {
                                saveSuccess = saveKeys(rsakpm);   
                            } else {
                                System.out.println("Skipping. Keys will not be saved.");
                            }
                        }
                    }
                } 
                
                // Rest loop
                next = Action.NONE;  
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
	
	
	public static RSAKeyPairManager loadKeys() throws IOException {
	    // Get directory in which RSA key files are stored
        System.out.println("RSA key files location? (default: " + DEFAULT_KEY_FILE_PATH + ")");
        CLInputParser fileInput = new CLInputParser(System.in);
        String keyFilePath = fileInput.dirPath();
        
        // If user did not provide a path, use default path
        if (keyFilePath == "") {
            keyFilePath = DEFAULT_KEY_FILE_PATH;
            if (!new File(keyFilePath).isDirectory()) {
                boolean created = new File(keyFilePath).mkdirs();
                if (!created) {
                    System.out.println("Directory " + keyFilePath + " not found and could not be created.");
                    System.out.println("Keys cannot be loaded.");
                    return null;
                }
            }
        }
        
        
        
        // Get private key file name
        System.out.println("Private key file name? (default: " + DEFAULT_PRIVATE_KEY_FILE_NAME + ")");
        String privateKeyFilePath = fileInput.filePath(keyFilePath);
        
        // If user did not provide a name, use default
        if (privateKeyFilePath.contentEquals(keyFilePath)) {
            privateKeyFilePath += File.separator + DEFAULT_PRIVATE_KEY_FILE_NAME;
        }
        
        // Get public key file name
        System.out.println("Public key file name? (default: " + DEFAULT_PUBLIC_KEY_FILE_NAME + ")");
        String publicKeyFilePath = fileInput.filePath(keyFilePath);
        
        // If user did not provide a name, use default
        if (publicKeyFilePath.contentEquals(keyFilePath)) {
            publicKeyFilePath += File.separator + DEFAULT_PUBLIC_KEY_FILE_NAME;
        }
        
        // RSA key pair manager
        RSAKeyPairManager rkpm = new RSAKeyPairManager();
        
        // Try to load private key
        //System.out.println("Attempting to load private key from " + privateKeyFilePath + "...");
        try {
            rkpm.loadPrivate(privateKeyFilePath);
        } catch (InvalidKeySpecException e) {
            System.out.println("ERROR: The private key file was not in the correct format.");
        } catch (IOException e) {
            //System.out.println("ERROR: I/O Error accessing private key file.");
        }
        
        // Notify of success or failure
        if (rkpm.privateKeySet()) {
            System.out.println("Successfully loaded private key from " + privateKeyFilePath + ".");
        } else {
            System.out.println("ERROR: Failed to load private key from " + privateKeyFilePath + ".");
        }
        
        // Try to load public key
        //System.out.println("Attempting to load public key from " + publicKeyFilePath + "...");       
        try {
            rkpm.loadPublic(publicKeyFilePath);
        } catch (InvalidKeySpecException e) {
            System.out.println("ERROR: The public key file was not in the correct format.");
        } catch (IOException e) {
            //System.out.println("ERROR: I/O Error accessing public key file.");
        }
        
        // Notify of success or failure
        if (rkpm.publicKeySet()) {
            System.out.println("Successfully loaded public key from " + publicKeyFilePath + ".");
        } else {
            System.out.println("ERROR: Failed to load public key from " + publicKeyFilePath + ".");
        }
        
        if (rkpm.publicKeySet() && rkpm.privateKeySet()) {
            return rkpm;
        } else {
            return null;
        }        
	}
	
	public static RSAPublicKeyManager loadRemotePublic() throws IOException {
        // Get directory in which RSA key file is stored
        System.out.println("RSA key file location? (default: " + DEFAULT_REMOTE_KEY_FILE_PATH + ")");
        CLInputParser fileInput = new CLInputParser(System.in);
        String keyFilePath = fileInput.dirPath();
        
        // If user did not provide a path, use default path
        if (keyFilePath == "") {
            keyFilePath = DEFAULT_REMOTE_KEY_FILE_PATH;
            if (!new File(keyFilePath).isDirectory()) {
                boolean created = new File(keyFilePath).mkdirs();
                if (!created) {
                    System.out.println("Directory " + keyFilePath + " not found and could not be created.");
                    System.out.println("Keys cannot be loaded.");
                    return null;
                }
            }
        }
        
        // Get public key file name
        System.out.println("Public key file name? (default: " + DEFAULT_REMOTE_PUBLIC_KEY_FILE_NAME + ")");
        String publicKeyFilePath = fileInput.filePath(keyFilePath);
        
        // If user did not provide a name, use default
        if (publicKeyFilePath.contentEquals(keyFilePath)) {
            publicKeyFilePath += File.separator + DEFAULT_REMOTE_PUBLIC_KEY_FILE_NAME;
        }
        
        // RSA public key manager
        RSAPublicKeyManager rpkm = new RSAPublicKeyManager();
        
        // Try to load public key
        //System.out.println("Attempting to load remote host's public key from " + publicKeyFilePath + "...");       
        try {
            rpkm.loadKey(publicKeyFilePath);
        } catch (InvalidKeySpecException e) {
            System.out.println("ERROR: The remote host's public key file was not in the correct format.");
        } catch (IOException e) {
            //System.out.println("ERROR: I/O Error accessing remote host's public key file.");
        }
        
        // Notify of success or failure
        if (rpkm.keySet()) {
            System.out.println("Successfully loaded remote host's public key from " + publicKeyFilePath + ".");
        } else {
            System.out.println("ERROR: Failed to load remote host's public key from " + publicKeyFilePath + ".");
        }
        
        if (rpkm.keySet()) {
            return rpkm;
        } else {
            return null;
        }        
    }
	
	/**
	 * Saves both keys stored in a RSAKeyPairManager object
	 * @param rkpm a RSAKeyPairManager object
	 * @return true if keys were successfully saved, else false
	 * @throws IOException in the event of File IO errors
	 */
	public static boolean saveKeys(RSAKeyPairManager rkpm) throws IOException {
	    // Return value
	    boolean success = false;
	    
	    // Get directory in which to save RSA key files
        System.out.println("RSA key files location? (default: " + DEFAULT_KEY_FILE_PATH + ")");
        CLInputParser fileInput = new CLInputParser(System.in);
        String keyFilePath = fileInput.dirPath();
        
        // If user did not provide a path, use default path
        if (keyFilePath == "") {
            keyFilePath = DEFAULT_KEY_FILE_PATH;
        }
        
    // GET PRIVATE KEY FILE NAME
        String privateKeyFilePath = null;
        
        // Retry until desired path is set
        boolean privatePathSet = false;
        while (!privatePathSet) {
            // Get private key file name
            System.out.println("Private key file name? (default: " + DEFAULT_PRIVATE_KEY_FILE_NAME + ")");
            privateKeyFilePath = fileInput.filePath(keyFilePath);
            
            // If user did not provide a name, use default
            if (privateKeyFilePath.contentEquals(keyFilePath)) {
                privateKeyFilePath += File.separator + DEFAULT_PRIVATE_KEY_FILE_NAME;
            }
            
            // If file exists, ask to overwrite
            if (new File(privateKeyFilePath).exists()) {
                System.out.println("File " + privateKeyFilePath + "exists.");
                System.out.println("Overwrite? (y/n)");
                privatePathSet = fileInput.yesNo();
            } else {
                privatePathSet = true;
            }
	    }
        

    // GET PUBLIC KEY FILE NAME
        String publicKeyFilePath = null;
        
        // Retry until desired path is set
        boolean publicPathSet = false;
        while (!publicPathSet) {
            // Get public key file name
            System.out.println("Public key file name? (default: " + DEFAULT_PUBLIC_KEY_FILE_NAME + ")");
            publicKeyFilePath = fileInput.filePath(keyFilePath);
            
            // If user did not provide a name, use default
            if (publicKeyFilePath.contentEquals(keyFilePath)) {
                publicKeyFilePath += File.separator + DEFAULT_PUBLIC_KEY_FILE_NAME;
            }
            
            // If file exists, ask to overwrite
            if (new File(publicKeyFilePath).exists()) {
                System.out.println("File " + publicKeyFilePath + "exists.");
                System.out.println("Overwrite? (y/n)");
                publicPathSet = fileInput.yesNo();
            } else {
                publicPathSet = true;
            }
        }
        
    // SAVE PRIVATE KEY FILE	
	    // Attempt to save private key file
	    System.out.println("Attempting to save public key file at " + privateKeyFilePath + "...");
	    boolean writePrivateSuccess = false;
	    try {
	        rkpm.writePrivate(privateKeyFilePath);
	        writePrivateSuccess = true;
	    } catch (IOException e) {
	        System.out.println("ERROR: Failed to save private key file.");
	    }
	    
	    // Notify success
	    if (writePrivateSuccess) {
	        System.out.println("Successfully saved private key file to " + privateKeyFilePath + ".");
	    } else {
	        //System.out.println("ERROR: Unknown error. Failed to save private key file.");
	    }
    
    // SAVE PUBLIC KEY FILE
        // Attempt to save public key file
        System.out.println("Attempting to save public key file at " + publicKeyFilePath + "...");
        boolean writePublicSuccess = false;
        try {
            rkpm.writePublic(publicKeyFilePath);
            writePublicSuccess = true;
        } catch (IOException e) {
            System.out.println("ERROR: Failed to save public key file.");
        }
        
        // Notify success
        if (writePublicSuccess) {
            System.out.println("Successfully saved public key file to " + publicKeyFilePath + ".");
        } else {
            //System.out.println("ERROR: Unknown error. Failed to save public key file.");
        }
        
        success = writePrivateSuccess && writePublicSuccess;
        return success;
	}
}

/*// Send our public key
byte[] pkMessage = new byte[PK_MESSAGE_PREFIX.length + authDataEnc.length];
System.arraycopy(PK_MESSAGE_PREFIX, 0, pkMessage, 0, PK_MESSAGE_PREFIX.length);
System.arraycopy(authDataEnc, 0, pkMessage, PK_MESSAGE_PREFIX.length, authDataEnc.length);
server.write(pkMessage);*/



/*byte[] serverPKMessage = client.readAllBytes();

// Verify it's the public key message
boolean pkMessageValid = true;
for (int i = 0; i < PK_MESSAGE_PREFIX.length; i++) {
    if (!(serverPKMessage[i] == PK_MESSAGE_PREFIX[i])) {
        pkMessageValid = false;
    }
}

// If message format not recognized, notify and skip authentication, else continue
if (!pkMessageValid) {
    System.out.println("ERROR: Invalid public key transmission from server.");
    System.out.println(new String(serverPKMessage, StandardCharsets.UTF_8));
    System.out.println("NOTICE: Skipping server authentication.");
} else {
    byte[] serverPrivateKeyEncoded = new byte[serverPKMessage.length - PK_MESSAGE_PREFIX.length];
    System.arraycopy(serverPKMessage, PK_MESSAGE_PREFIX.length, serverPrivateKeyEncoded, 0, serverPKMessage.length - PK_MESSAGE_PREFIX.length);
    serverRpkm.loadKey(serverPrivateKeyEncoded);*/