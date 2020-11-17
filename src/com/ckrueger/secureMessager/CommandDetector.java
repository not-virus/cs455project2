package com.ckrueger.secureMessager;

import java.lang.System;
import java.util.Scanner;

public class CommandDetector extends Thread {
    
    private boolean commandReceived = false;
    private String command = null;
    private Scanner input = null;
    
    public CommandDetector(String command, Scanner input)
    {
        this.command = command;
        this.commandReceived = false;
        this.input = input; // Yes, I know this is unsafe
    }
    
    public void waitForInput() {
        // Get next token
        String token = input.nextLine();
        
        while (!token.strip().toLowerCase().equals(this.command)) {
            token = input.nextLine();
        }
        
        commandReceived = true;
    }
    
    /**
     * @return true if a valid command has been received
     */
    public boolean success() {
        return commandReceived;
    }
    
   public void run() {
       waitForInput();
   }

}
