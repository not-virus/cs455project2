package com.ckrueger.secureMessager;

/**
 * @author Cameron Krueger
 * This wouldn't need to exist if Java had structs
 */
public class CLToken {

    public static enum Commands {
        HELP,
        CONNECT,
        HOST,
        DONE,
        QUIT,
        INVALID,
        NONE
    }
    
    //public boolean isValidCommand;
    public boolean isCommand;
    public Commands command;
    public String value;
    
    /**
     * Used when the user enters a command
     * Initializes a CLToken object with a command and the string parsed from user entry
     * Automatically sets isCommand to true.
     * @param command the command parsed from user entry
     * @param value the raw text the user entered
     */
    public CLToken(Commands command, String value) {
        this.command = command;
        this.value = value;
        this.isCommand = true;
        /*if (command != Commands.NONE && command != Commands.INVALID) {
            this.isValidCommand = true;
        } else {
            this.isValidCommand = false;
        }*/
    }
    
    /**
     * Used when the user enters a token
     * Initializes a CLToken object with the string parsed from user entry. Automatically
     * sets internal command to Commands.NONE and isCommand to false.
     * @param value the raw text the user entered
     */
    public CLToken(String value) {
        this.value = value;
        this.command = Commands.NONE;
        this.isCommand = false;
        //this.isValidCommand = false;
    }
      
}
