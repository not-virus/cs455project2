package com.ckrueger.secureMessager;

import java.io.IOException;
import com.ckrueger.secureMessager.comms.Server;

public class ServerRunner extends Thread {
    
    public Server server = null;
    public int port = 0;
    private boolean serverReady = false;
    private boolean end = false;
    
    public ServerRunner(int port) {
        this.port = port;
        this.serverReady = false;
    }
    
    public Server getServer() {
        return this.server;
    }
    
    public String getAddress() {
        return this.server.getAddress();
    }
    
    public boolean connectionAvailable() {
        return serverReady;
    }
    
    public void close() {
        this.end = true;
        this.serverReady = false;
    }

    @Override
    public void run() {
        this.serverReady = false;
        try {
            this.server = new Server(port);
            //System.out.println("Connection received from remote host");
        } catch (IOException e) {
            System.err.println("ERROR: Failed to start messaging server on port " + Integer.toString(port));
            /*try {
                server.closeSocket();
                server.close();
            } catch (IOException ioe) {
                System.err.println("ERROR: An exception occurred while attempting to close the server");
                ioe.printStackTrace();
            }*/
            e.printStackTrace();
        }
        
        this.serverReady = true;
        
        /*while (this.end == false) {
            ;
        }*/
    }
    
}
