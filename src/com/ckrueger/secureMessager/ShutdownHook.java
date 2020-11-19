package com.ckrueger.secureMessager;

import java.io.IOException;

import com.ckrueger.secureMessager.comms.Server;

/**
 * This is a simple shutdown hook used to ensure the messaging server releases
 * its port, in the event of unexpected termination
 * 
 * @author Cameron Krueger
 * @created 16 November 2020
 *
 */
public class ShutdownHook extends Thread {

    private Server server = null;
    
    /**
     * This must be provided a non-null Server object!
     * @param server the Server object which should be closed
     */
    public ShutdownHook(Server server) {
        this.server = server;
    }
    
    /**
     * Obligatory run method
     */
    public void run() {
        if (this.server != null) {
            try {
                this.server.close();
            } catch (IOException e) {
                System.err.println("ERROR: Could not close local server");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("Shutdown hook: ERROR: Server null but also not null");
            }
        } else {
            System.out.println("Shutdown hook: Local server closed successfully.");
        }
            
    }

}
