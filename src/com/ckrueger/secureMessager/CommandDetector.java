package com.ckrueger.secureMessager;

import java.io.IOException;
import java.lang.System;
import java.util.Scanner;

import com.ckrueger.secureMessager.CLToken.Commands;

/**
 * @author black
 *
 */
public class CommandDetector extends Thread {
    
    private boolean commandReceived = false;
    private CLToken command = new CLToken(CLToken.Commands.NONE, null);
    private CLInputParser clip;
    
    
    public CommandDetector()
    {
        this.commandReceived = false;
        this.clip = new CLInputParser(System.in); // Yes, I know this is unsafe
    }
    
    /**
     * Waits for user input or instructions to exit.
     * Reads command from user input and stores in command
     */
    public void waitForInput() {
        CLToken cmd = null;
        
        try {
            cmd = clip.command();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        // Ultra hacky way of skipping input
        command = cmd;
        if (command.command == Commands.CANCEL) {
            commandReceived = false;  // remains false, input was cancelled.
        } else {
            commandReceived = true;
        }
    }
    
    /**
     * Sets a flag which allows the thread to die
     */
    public void close() {
        this.clip.close();
    }
    
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

    public void retrigger() {
        waitForInput();
    }
    
   public void run() {
       waitForInput();
   }

}
