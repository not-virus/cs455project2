package com.ckrueger.secureMessager;

import java.lang.System;
import java.util.Scanner;

/**
 * @author black
 *
 */
public class CommandDetector extends Thread {
    
    private boolean commandReceived = false;
    //private boolean canExit = false;
    private CLToken command = new CLToken(CLToken.Commands.NONE, null);
    private CLInputParser clcli;
    
    
    public CommandDetector(CLInputParser clcli)
    {
        this.commandReceived = false;
        this.clcli = clcli; // Yes, I know this is unsafe
    }
    
    public void waitForInput() {
        // Get next token
        CLToken cmd;
        cmd = clcli.command();
        
        /*while (cmd.command == CLToken.Commands.NONE
                || cmd.command == CLToken.Commands.INVALID) {
            if (cmd.command == CLToken.Commands.INVALID) {
                System.out.println(cmd.value + ": command not recognized.");
            }
            cmd = clcli.command();
        }*/
        
        command = cmd;
        commandReceived = true;
    }
    
    /**
     * Sets a flag which allows the thread to die
     */
    /*public void close() {
        this.canExit = true;
    }*/
    
    /**
     * @return true if a valid command has been received
     */
    public boolean available() {
        return commandReceived;
    }
    

    /**
     * @return a command issued by the user
     */
    public CLToken getCommand() {
        return this.command;
    }
    
   public void run() {
       waitForInput();
       
       // Loop until closed
       /*while (!canExit) {
           waitForInput();
       }*/
       
   }

}
