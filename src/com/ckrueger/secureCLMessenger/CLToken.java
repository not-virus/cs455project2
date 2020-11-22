package com.ckrueger.secureCLMessenger;

/**
 * @author Cameron Krueger
 * This wouldn't need to exist if Java had structs. This class simply stores
 * three values. That's it. An entire class just so I can return 3 values from
 * one function. I miss programming in C/C++ so much right now.
 * 
 * Used when the user enters a command Initializes a CLToken object with a
 * command and the string parsed from user entry Automatically sets isCommand
 * to true.
 */
public class CLToken {

    // Most of these aren't being used yet
    public static enum Commands {
        HELP,
        CONNECT,
        DISCONNECT,
        HOST,
        AUTH,
        LOAD,
        SAVE,
        GENERATE,
        OVER,
        DONE,
        QUIT,
        CANCEL,
        MESSAGE,
        INVALID,
        NONE
    }

    public boolean isCommand;
    public Commands command;
    public String value;

    /**
     * Creates a new token object for a command. Sets the command type and
     * value.
     * 
     * @param command the command parsed from user entry
     * @param value   the raw text the user entered
     */
    public CLToken(Commands command, String value) {
        this.command = command;
        this.value = value;
        this.isCommand = true;
    }

    /**
     * Creates a new token object for a value only.
     * 
     * @param value the raw text the user entered
     */
    public CLToken(String value) {
        this.value = value;
        this.command = Commands.NONE;
        this.isCommand = false;
    }

}
