package com.ckrueger.secureMessager;

import java.util.Scanner;
import java.util.regex.*;

import com.ckrueger.secureMessager.CLToken.Commands;

import java.io.*;

public class CLInputParser {
    
    private Scanner input = null;
    private final int MESSAGE_MAX_LINES = 2048;
    
    /**
     * Initializes a new CLCommandInterpreter
     * @param is an InputStream to read from (ideally System.in)
     */
    public CLInputParser(InputStream is) {
        this.input = new Scanner(is);
    }
    
    /**
     * Filters a valid IP address from the command line
     * @return an IP address
     */
    public String ipAddress() {
        String in;// = clin();
        boolean match = false;
        CLToken token = null;
        
        String ipRegex = "\\b((1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                            "(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\b";
        
        // Keep trying until valid IP is found        
        while (!match) {
            in = clIn();
            token = parse(in);
            
            // Check if it's a command
            if (token.isCommand) {
                System.out.println(token.value + ": Commands not allowed at " +
                        "this time.");
            } else {
                // Regex for valid IP address
                match = Pattern.matches(ipRegex, token.value);
                
                // If invalid, notify
                if (!match) {
                    System.out.println(token.value + ": Invalid IP address.");
                } else {
                    // Check for broadcast
                    if (Pattern.matches("255.255.255.255", token.value)) {
                        System.out.println(token.value + ": Cannot use " +
                                "the broadcast address.");
                        match = false;
                    }
                    // Check for broadcast
                    if (Pattern.matches("0.0.0.0", token.value)) {
                        System.out.println(token.value + ": Cannot use " +
                                "the meta-address.");
                        match = false;
                    }  
                }
            }
        }
        
        return token.value;
    }
    
    /**
     * Filters a valid port number from the command line
     * @return a port number
     */
    public int port() {
        String in;
        boolean match = false;
        CLToken token = null;
        
        // Regex pattern for valid port numbers (1-65535 inclusive)
        String portRegex = "\\b([1-9]|[1-9][0-9]{1,3}|[1-5][0-9]{4}|" + 
                "6[0-4][0-9][0-9][0-9]|65[0-4][0-9][0-9]|655[0-2][0-9]|" + 
                "6553[0-5])\\b";
        
        //token = parse(in);
        
        // Keep trying until valid port number is found        
        while (!match) {
            in = clIn();
            token = parse(in);
            
            // Check if it's a command
            if (token.isCommand) {
                System.out.println(token.value + ": Commands not allowed at " +
                        "this time.");
            } else {
                // Regex for valid port number
                match = Pattern.matches(portRegex, token.value);
                
                // If invalid, notify
                if (!match) {
                    System.out.println(token.value + ": Invalid port number.");
                }
            }
        }
        
        return Integer.parseInt(token.value);
    }
    
    public boolean yesNo() {
        String in;
        boolean match = false;
        CLToken token = null;
        
        // Regex pattern for valid port numbers (1-65535 inclusive)
        String yesNoRegex = "yes|yep|y|no|nah|n"; // Easter egg ;)
        
        // Keep trying until valid response is obtained        
        while (!match) {
            in = clIn();
            token = parse(in);
            
            // Check if it's a command
            if (token.isCommand) {
                System.out.println(token.value + ": Commands not allowed at " +
                        "this time.");
            } else {
                // Regex for valid choice
                match = Pattern.matches(yesNoRegex, token.value);
                
                // If invalid, notify
                if (!match) {
                    System.out.println(token.value + ": Invalid choice.");
                }
            }
        }
        
        // Check if response was affirmative
        String yesRegex = "yes|yep|y";
        return Pattern.matches(yesRegex, token.value);        
    }
    
    public String message() {
        String[] in;
        String out = "";
        int lineCount = 0;
        
        // Get lines of input
        in = clMulti();
        
        System.out.println("here1");
        
        // Determine message length
        int messageLength = 0;
        while (messageLength < MESSAGE_MAX_LINES && in[messageLength] != null) {
            messageLength++;
        }

        // Remove !done
        if (in[messageLength].strip().toLowerCase().contains("!done")) {
            in[messageLength] = in[messageLength].replace("!done", "");
        } else {
            System.err.println("DEBUG: Did not find !done.");
        }
        
        System.out.println("here2");
        
        // Convert string buffer into big string with newline chars
        while (lineCount < messageLength && in[lineCount] != null) {
            out += in[lineCount];
            out += "\n";
            lineCount++;
        }
        
        System.out.println("here3");
        
        // Last line skipped to avoid appending newline. Add last line without
        //  newline
        out += in[lineCount];
        
        System.out.println("here4");
        
        return out;
                
    }
    
    /**
     * Filters a command from the command line
     * @return a cksm command
     */
    public CLToken command() {
        // Get input
        String in = clIn();
        
        // Parse token
        CLToken token = parse(in);
        
        // Retry until a command is entered
        while (!token.isCommand 
               || token.command == CLToken.Commands.NONE
               || token.command == CLToken.Commands.INVALID) {
            System.out.println(in + ": Not a valid command. (All commands " +
                "start with !)");
            in = clIn();
            token = parse(in);
        }
                
        return token;
    }
    
    /**
     * Parses user input into commands or tokens
     * @param usrInput a raw input string from the user
     * @return a CLToken object representing the user's parsed input
     */
    private CLToken parse(String usrInput) {
        CLToken token = null;
        String in = usrInput.strip().toLowerCase();
        
        if (in.startsWith("!")) {
            
            // Remove !
            String tmp = in.substring(1);
            
            // Parse command and assign token to new "command" instance of
            //  CLToken
            if (tmp.equals("connect")) {
                token = new CLToken(CLToken.Commands.CONNECT, in);
            } else if (tmp.equals("host")) {
                token = new CLToken(CLToken.Commands.HOST, in);
            } else if (tmp.equals("help")) {
                token = new CLToken(CLToken.Commands.HELP, in);
            } else if (tmp.equals("")) {
                token = new CLToken(CLToken.Commands.NONE, in);
            } else {
                token = new CLToken(CLToken.Commands.INVALID, in);
            }
        } else {
            // Instantiate as raw input token
            token = new CLToken(in);
        }
        
        return token;       
    }
    
    /**
     * Takes input from user by prefixing line with "cksm> " prompt
     * @return the line the user entered
     */
    private String clIn() {
        // Print prompt
        System.out.print("cksm> ");
        return input.nextLine();
    }
    
    /**
     * Takes multiple lines of input from user by prefixing block with "cksm#"
     * prompt
     * @return the lines the user entered
     */
    private String[] clMulti() {
        // Print prompt
        System.out.println("cksm#");
        
        // Input string and message buffer
        String in = "";
        String[] mult = new String[MESSAGE_MAX_LINES];
        int messageLineCount = 0;
                
        while (!in.strip().toLowerCase().contains("!done") && messageLineCount < MESSAGE_MAX_LINES) {
            in = input.nextLine();
            mult[messageLineCount] = new String(in);
            messageLineCount++;
        }
        if (messageLineCount >= MESSAGE_MAX_LINES) {
            System.out.println("Message line count limit reached (2048 lines)!");
        }
        
        return mult;
    }
}
