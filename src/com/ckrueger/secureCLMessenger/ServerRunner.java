package com.ckrueger.secureCLMessenger;

import java.io.IOException;

import com.ckrueger.secureCLMessenger.comms.Server;

/**
 * @author Cameron Krueger
 * Allows a server to be started in a separate thread and run in the background
 * until an incoming connection is available
 */
public class ServerRunner extends Thread {
    
    private Server server = null;
    public int port = 0;
    private boolean serverReady = false;
    
    /**
     * Initializes the object with a port. Will also reset the serverReady
     * flag to false.
     * 
     * @param port the loca port to which the server should be bound
     */
    public ServerRunner(int port) {
        this.port = port;
        this.serverReady = false;
    }
    
    /**
     * @return the Server object
     */
    public Server getServer() {
        return this.server;
    }
    
    /**
     * @return the local IP address of the Server
     */
    public String getAddress() {
        return this.server.getAddress();
    }
    
    /**
     * @return true if the Server is ready with a connection, else false
     */
    public boolean connectionAvailable() {
        return serverReady;
    }
    
    /**
     * Does almost nothing. Had more functionality earlier and may not be used
     * at all anymore, but I'm waiting to remove it until I'm sure it can't be
     * used.
     */
    public void close() {
        this.serverReady = false;
    }

    @Override
    public void run() {
        this.serverReady = false;
        try {
            this.server = new Server(port);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to start messaging server on"
                    + "port " + Integer.toString(port));
            e.printStackTrace();
        }
        
        this.serverReady = true;
    }
    
}
