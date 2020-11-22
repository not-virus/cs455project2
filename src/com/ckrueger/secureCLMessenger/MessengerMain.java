package com.ckrueger.secureCLMessenger;

import com.ckrueger.secureCLMessenger.*;
import com.ckrueger.secureCLMessenger.comms.*;
import com.ckrueger.secureCLMessenger.crypto.*;

import java.lang.System;
import java.util.Arrays;
import java.util.Random;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.lang.Runtime;
import java.util.Base64;

/**
 * @author Cameron Krueger
 * 
 * ### NOTE ###
 *   THIS SOFTWARE CURRENTLY SENDS ALL MESSAGES AS PLAINTEXT (I.E. UNENCRYPTED)
 *   BYTETREAMS. It is not actually secure!
 *   
 *   This software is not suitable for the purposes of conducting secure
 *   peer-to-peer messaging!  This software does not yet support message
 *   encryption, and even if it did, messages would be encrypted using the
 *   RSA algoritm.  RSA is relatively weak and is typically reserved for
 *   exchanging keys for a symmetric-key encryption algorithm such as AES, DES
 *   or blowfish.
 * 
 * This is very much a work-in-progress.  It was created as the result of my
 * misinterpretation of a school project's (arguably vague) requirements.  The
 * project's requitements included establishing a client-server socket
 * connection, authenticating that connection with RSA keys, conducting RSA
 * encrypted communication, and finally timing the different steps of the
 * process for analysis.  This software is on its way to being capable of all
 * of that, but is evidently far, far more complicated than it needs to be.
 * This is due to the fact that when I created it, I had absolutely no prior
 * experience with socket programming, nor encryption.  Additionally,
 * I wrote this entire program with somewhere between 50 and 70 hours of
 * panicked work spread out over the course of only 6 days.  This code is an
 * absolute mess and needs to be cleaned up.  Hopefully someday I can fix it
 * up.
 * 
 * Oh, and it's worth noting that, due to the file IO performed herein, this
 * software isn't really cross-platform.  It runs on Windows and Linux, but I
 * don't know if files would be stored in the correct directories in Linux,
 * should any changes be made to the system's functionality. 
 */
public class MessengerMain {
    
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
    private final static int MAX_MESSAGE_LENGTH = 256;
    

	public static void main(String[] args) throws Exception {
	    
	    // RSACipher class for encryption and decryption 
	    RSACipher rsa = new RSACipher();

	    System.out.println("   --- Welcome to CLSM v1.0 ---   ");
	    
	    CLInputParser mainClip = new CLInputParser(System.in);
	    
	    // Load RSA keys from file
	    RSAKeyPairManager localKeyMgr = loadKeys();
	    
	    // Handle load failure
	    boolean loadRetry = true;
	    while (localKeyMgr == null && loadRetry == true) {
	        System.out.println("Failed to load one or both keys from files.");
	        System.out.println("You can try to load the keys again or generate new keys.");
	        System.out.println("Would you like to try again? (y/n)");
	        loadRetry = mainClip.yesNo();
	        
	        if (loadRetry) {
	            localKeyMgr = loadKeys();
	        }
	    }
	    
	    // If keys were not loaded and user chooses not to try again, generate new keys
	    if (localKeyMgr == null) {
	        System.out.println("You have chosen to generate new keys.");
	        System.out.println("Generating new keys...");
	        long keyGenStart = System.currentTimeMillis();
	        localKeyMgr = new RSAKeyPairManager(2048);
	        long keyGenStop = System.currentTimeMillis();
	        
	        System.out.println("Key pair generation took " + (keyGenStart - keyGenStop) + " milliseconds.");
	        
	        // If still unable to generate keys, terminate. Something is wrong!
	        if (!localKeyMgr.privateKeySet() || !localKeyMgr.publicKeySet()) {
	            System.out.println("FATAL ERROR: Failed to generate new key " +
	                    "pair of size " + 2048 + "bits.");
	            System.out.println("Terminating...");
	            System.exit(1);
	        }
	        
	        System.out.println("Successfully generated new key pair.");
	        System.out.println("Would you like to save these keys?");
	        boolean save = mainClip.yesNo();
	        
	        if (save) {
	            boolean saveSuccess = saveKeys(localKeyMgr);
	            
	            // Retry until success or skip
	            boolean saveRetry = true;
	            while (!saveSuccess && saveRetry) {
	                System.out.println("Failed to save keys. Retry? (y/n)");
	                saveRetry = mainClip.yesNo();
	                
	                if (saveRetry) {
	                    saveSuccess = saveKeys(localKeyMgr);   
	                } else {
	                    System.out.println("Skipping. Keys will not be saved.");
	                }
	            }
	        }
	    }
	    
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
                
                System.out.println("Would you like to authenticate the client? (y/n)");
                boolean authClient = serverClip.yesNo();
                
                RSAPublicKeyManager clientRpkm = null;
                
                if (authClient) {
                    server.write("RQ_AUTH".getBytes(StandardCharsets.UTF_8));
                    
                    clientRpkm = loadRemotePublic();
                    
                    // Authenticate using a randomly generated string of bytes
                    byte[] authData = new byte[64]; //"hello".getBytes(StandardCharsets.UTF_8);
                    new Random().nextBytes(authData); 

                    /* This is a test that uses the local public key and private key
                    // Encrypt message
                    byte[] authEnc = null;
                    try {    
                        authEnc = rsa.encrypt(authData, localKeyMgr.getPublicKey());
                    } catch (InvalidKeyException e) {
                        System.out.println("WARNING Public key not valid for encryption");
                        e.printStackTrace();
                    }
                    
                    // Decrypt message
                    byte[] authDec = null;
                    try {
                        authDec = rsa.decrypt(authEnc, localKeyMgr.getPrivateKey());
                    } catch (InvalidKeyException e) {
                        System.out.println("WARNING Private key not valid for decryption");
                        e.printStackTrace();
                    }

                    // Display decrypted message
                    System.out.println("Decrypted message: ");
                    String authDecStr = new String(authDec, StandardCharsets.UTF_8);
                    System.out.println(authDecStr + "\n"); */
                    
                    long authProcessStart = System.currentTimeMillis();                    
                    
                    // Encrypt with client's public key
                    byte[] authDataEnc = null;
                    long authMessageEncryptStart = System.currentTimeMillis();
                    try {
                        authDataEnc = rsa.encrypt(authData, clientRpkm.getKey());
                        //System.out.println("Encrypted authentication string.");
                        //System.out.println("Encrypted, decoded message len: " + authDataEnc.length);
                    } catch (InvalidKeyException e) {
                        System.out.println("ERROR: Public key not valid for encryption");
                        e.printStackTrace();
                    }
                    long authMessageEncryptStop = System.currentTimeMillis();
                    
                    //System.out.print("Encrypted auth string (converted to Base64):");
                    //System.out.println(new String(Base64.getEncoder().encode(authData), StandardCharsets.UTF_8));
                    System.out.println("in " + (authMessageEncryptStop - authMessageEncryptStart) + " milliseconds.");
                    
                    // Encode to base64
                    authDataEnc = Base64.getEncoder().encode(authDataEnc);
                    // Send encrypted auth data to client
                    server.write(authDataEnc);
                    System.out.println("Sent authentication message to client.");
                    //System.out.println("Encrypted, encoded message len: " + authDataEnc.length);
                    //System.out.println(new String(authDataEnc, StandardCharsets.UTF_8));
                    
                    // Get response from client, decrypt and verify
                    byte[] authResponseEnc = server.readAllBytes();
                    //System.out.println("Received response from client.");
                    //System.out.println("Encrypted, encoded response len: " + authResponseEnc.length);
                    //System.out.println(new String(authResponseEnc, StandardCharsets.UTF_8));
                    
                    // Decode from base64
                    byte[] tmp = Base64.getDecoder().decode(authResponseEnc);
                    //System.out.println("Encrypted, decoded response len: " + tmp.length);
                    
                    // Decrypt with private key
                    byte[] authResponse = null;
                    try {
                        authResponse = rsa.decrypt(tmp, localKeyMgr.getPrivateKey());
                    } catch (Exception e) {
                        System.out.println("ERROR: Could not decrypt authentication response from client.");
                        e.printStackTrace();
                    }
                    //System.out.println("Decrypted, decoded response len: " + authResponseEnc.length);
                    //System.out.println(new String(authResponse, StandardCharsets.UTF_8));
                    
                    long authProcessStop = System.currentTimeMillis();
                    
                    // Verify match
                    if (!(Arrays.equals(authResponse, authData))) {
                        System.out.println("WARNING: Failed to authenticate client.");
                        System.out.println("WARNING: Continuing in plaintext mode.");
                    } else {
                        System.out.println("Client's identity has been validated.");
                        System.out.println("Client authentication completed in " + (authProcessStop - authProcessStart) + " milliseconds.");
                    }                    
                } else {
                    server.write("NO_AUTH".getBytes(StandardCharsets.UTF_8));
                }
                
                System.out.println("\n----------------------------------------");
                
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
                        
                        // Print out lines of message
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
                    
                    // Reject local listening ServerSocket
                    while (serverAddress == localAddress) { //&& serverPort == localPort) {
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
                    
                    // For server's public key
                    RSAPublicKeyManager serverRpkm = null;
                    
                    // Authenticate server
                    String servMsg = new String(client.readBytes(8), StandardCharsets.UTF_8);
                    
                    if (servMsg.contains("RQ_AUTH")) {
                        System.out.println("Server has requested authentication.");

                        // Get server's public key
                        serverRpkm = loadRemotePublic();

                        // Get the server's authentication message, decrypt,
                        // re-encrypt and send
                        byte[] authDataEnc = client.readAllBytes();
                        System.out.println("Received authentication message from server");
                        //System.out.println("Encrypted, encoded message len: " + authDataEnc.length);
                        //System.out.println(new String(authDataEnc, StandardCharsets.UTF_8));
                        
                        authDataEnc = Base64.getDecoder().decode(authDataEnc);
                        //System.out.println("Encrypted, decoded message len: " + authDataEnc.length);
                        
                        byte[] authDataDec = null;
                        try {
                            authDataDec = rsa.decrypt(authDataEnc, localKeyMgr.getPrivateKey());
                        } catch (Exception e) {
                            System.out.println("ERROR: Could not decrypt authentication message from server.");
                        }
                        //System.out.println("Decrypted message: ");
                        //System.out.println(new String(authDataDec, StandardCharsets.UTF_8));
                        
                        byte[] tmp = null;
                        try {
                            tmp = rsa.encrypt(authDataDec, serverRpkm.getKey());
                        } catch (Exception e) {
                            System.out.println("ERROR: Could not decrypt authentication message from server.");
                        }
                        //System.out.println("Encrypted, decoded len: " + tmp.length);
                        
                        byte[] returnAuthDataEnc = Base64.getEncoder().encode(tmp);
                        System.out.println("Sending response to server");
                        //System.out.println("Encrypted, encoded response len: " + returnAuthDataEnc.length);
                        //System.out.println(new String(returnAuthDataEnc, StandardCharsets.UTF_8));
                        client.write(returnAuthDataEnc);

                        System.out.println("Returned authentication message. Awaiting response from server...");

                        if (client.readAllBytes() == "SECURED".getBytes()) {
                            System.out.println("Successfully authenticated server.");
                        } else {
                            System.out.println("ERROR: Failed to authenticate server");
                            System.out.println("WARNING: Continuing in plaintext mode.");
                        }
                    } else {
                        System.out.println("Server did not request authentication.");
                        System.out.println("WARNING: Continuing in plaintext mode.");
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
                    localKeyMgr = new RSAKeyPairManager(2048);
                    
                    // If still unable to generate keys, terminate. Something is wrong!
                    if (!localKeyMgr.privateKeySet() || !localKeyMgr.publicKeySet()) {
                        System.out.println("FATAL ERROR: Failed to generate new key " +
                                "pair of size " + 2048 + "bits.");
                        System.out.println("Terminating...");
                        System.exit(1);
                    }
                    
                    System.out.println("Successfully generated new key pair.");
                    System.out.println("Would you like to save these keys?");
                    boolean save = mainClip.yesNo();
                    
                    if (save) {
                        boolean saveSuccess = saveKeys(localKeyMgr);
                        
                        // Retry until success or skip
                        boolean saveRetry = true;
                        while (!saveSuccess && saveRetry) {
                            System.out.println("Failed to save keys. Retry? (y/n)");
                            saveRetry = mainClip.yesNo();
                            
                            if (saveRetry) {
                                saveSuccess = saveKeys(localKeyMgr);   
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
        System.exit(0);  
	}
	
	
	public static RSAKeyPairManager loadKeys() throws IOException {
        CLInputParser fileInput = new CLInputParser(System.in);
        // RSA key pair manager
        RSAKeyPairManager rkpm = new RSAKeyPairManager();
        
	    // If default keyfiles exist, try to use them
	    if (new File(DEFAULT_KEY_FILE_PATH).isDirectory()
	            && new File(DEFAULT_KEY_FILE_PATH + File.separator + DEFAULT_PRIVATE_KEY_FILE_NAME).exists()
	            && new File(DEFAULT_KEY_FILE_PATH + File.separator + DEFAULT_PUBLIC_KEY_FILE_NAME).exists()) {
	        System.out.println("Default key files found. Would you like to use them? (y/n)");
	        boolean useExisting = fileInput.yesNo();
	        
	        if (useExisting) {
	            String keyFilePath = DEFAULT_KEY_FILE_PATH;
	            String privateKeyFilePath = keyFilePath + File.separator + DEFAULT_PRIVATE_KEY_FILE_NAME;
	            String publicKeyFilePath = keyFilePath + File.separator + DEFAULT_PUBLIC_KEY_FILE_NAME;
	            
	            boolean error = false;
	            // Try to load private key
	            try {
	                rkpm.loadPrivate(privateKeyFilePath);
	            } catch (InvalidKeySpecException e) {
	                System.out.println("ERROR: The private key file was not in the correct format.");
	                error = true;
	            } catch (IOException e) {
	                error = true;
	                //System.out.println("ERROR: I/O Error accessing private key file.");
	            }
	            
	            // Try to load public key
	            try {
	                rkpm.loadPublic(publicKeyFilePath);
	            } catch (InvalidKeySpecException e) {
	                System.out.println("ERROR: The public key file was not in the correct format.");
	                error = true;
	            } catch (IOException e) {
	                error = true;
	                //System.out.println("ERROR: I/O Error accessing public key file.");
	            }
	            
	            if (!error) {
	                System.out.println("Default keys loaded successfully.");
	                return rkpm;
	            } else {
	                rkpm = null;
	                System.out.println("Failed to load default keys.");
	            }
	        }
	    }
	    
	    
	    // Get directory in which RSA key files are stored
        System.out.println("RSA key files location? (default: " + DEFAULT_KEY_FILE_PATH + ")");
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

/*  Old encryption test code
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