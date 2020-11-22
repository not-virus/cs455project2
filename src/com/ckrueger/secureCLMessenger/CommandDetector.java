package com.ckrueger.secureCLMessenger;

import java.io.IOException;
import java.lang.System;
import java.util.Scanner;

import com.ckrueger.secureCLMessenger.CLToken.Commands;

/**
 * @author Cameron Krueger
 * This class, when started as a thread, allows a program to listen for and 
 * process user input on the command line while doing other things
 */
public class CommandDetector extends Thread {

    private boolean commandReceived = false;
    private CLToken command = new CLToken(CLToken.Commands.NONE, null);
    private CLInputParser clip;

    /**
     * Initializes a new CommandDetector. Note, must be started as a thread
     * to begin listening to input.
     */
    public CommandDetector() {
        this.commandReceived = false;
        this.clip = new CLInputParser(System.in); // Yes, I know this is unsafe
    }

    /**
     * Waits for user input or instructions to exit. Reads command from user
     * input and stores in command
     */
    public void waitForInput() {
        CLToken cmd = null;

        try {
            cmd = clip.command();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        // I'm not even sure if this does anything anymore, but it very well
        //  might, so it stays
        // Ultra hacky way of skipping input?
        command = cmd;
        if (command.command == Commands.CANCEL) {
            commandReceived = false; // Remains false, input was cancelled.
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

    @Override
    public void run() {
        waitForInput();
    }

}
