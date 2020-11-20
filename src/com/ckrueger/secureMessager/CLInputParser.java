package com.ckrueger.secureMessager;

import java.util.Scanner;
import java.util.regex.*;

import com.ckrueger.secureMessager.CLToken.Commands;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CLInputParser {
    
    private static final int MAX_BUF_SIZE = 64;
    private Scanner input = null;
    private final int MESSAGE_MAX_LINES = 2048;
    private boolean skipInput = false;
    
    /**
     * Initializes a new CLCommandInterpreter
     * @param is an InputStream to read from (ideally System.in)
     */
    public CLInputParser(InputStream is) {
        this.input = new Scanner(is);
    }
    
    public void setSkip(boolean skip) {
        this.skipInput = skip;
    }
    
    public void close() {
        this.skipInput = true;
    }
    
    /**
     * Filters a valid IP address from the command line
     * @return an IP address
     * @throws IOException 
     */
    public String ipAddress() throws IOException {
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
     * @throws IOException 
     */
    public int port() throws IOException {
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
    
    /**
     * @return
     * @throws IOException 
     */
    public boolean yesNo() throws IOException {
        String in;
        boolean match = false;
        CLToken token = null;
        
        // Regex pattern for valid port numbers (1-65535 inclusive)
        String yesNoRegex = "yes|yep|y|no|nah|n"; // Easter egg ;)
        
        // Keep trying until valid response is obtained        
        while (!match) {
            in = clIn().strip();
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
    
    public String message() throws IOException {
        String[] in;
        String out = "";
        int lineCount = 0;
        
        // Get lines of input
        in = clMulti();
        
        // Determine message length
        int messageLength = 0;
        while (messageLength < MESSAGE_MAX_LINES && in[messageLength] != null) {
            messageLength++;
        }

        // Remove !done
        if (in[messageLength - 1].strip().toLowerCase().contains("!done")) {
            in[messageLength - 1] = in[messageLength - 1].replace("!done", "");
        } else {
            System.err.println("ERROR: Did not find !done.");
        }
        
        // Convert string buffer into big string with newline chars
        while (lineCount < messageLength - 1 && in[lineCount] != null) {
            out += in[lineCount];
            out += "\n";
            lineCount++;
        }
        
        // Last line skipped to avoid appending newline. Add last line without
        //  newline
        out += in[lineCount];
        
        return out;
                
    }
    
    /**
     * Filters a command from the command line
     * @return a cksm command
     * @throws IOException 
     */
    public CLToken command() throws IOException {
        // Get input
        String in = clIn();

        // Parse token
        CLToken token = parse(in);
        
        // Retry until a command is entered
        while (!token.isCommand 
               || token.command == CLToken.Commands.NONE
               || token.command == CLToken.Commands.INVALID) {
            if (token.value != "") {
                System.out.println(in + ": Not a valid command. (All " +
                        "commands start with !)");
            }
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
        
        // Check if input is a command
        if (in.startsWith("!")) {
            // Remove !
            String tmp = in.substring(1);

            // Parse command and assign token to new "command" instance of
            //  CLToken
            if (tmp.equals("connect")) {
                token = new CLToken(CLToken.Commands.CONNECT, in);
            } else if (tmp.equals("disconnect") || tmp.equals("disco")) {
                token = new CLToken(CLToken.Commands.DISCONNECT, in);
            } else if (tmp.equals("host")) {
                token = new CLToken(CLToken.Commands.HOST, in);
            } else if (tmp.equals("auth")) {
                token = new CLToken(CLToken.Commands.AUTH, in);
            } else if (tmp.equals("load")) {
                token = new CLToken(CLToken.Commands.LOAD, in);
            } else if (tmp.equals("done")) {
                token = new CLToken(CLToken.Commands.DONE, in);
            } else if (tmp.equals("quit")) {
                token = new CLToken(CLToken.Commands.QUIT, in);
            } else if (tmp.equals("help")) {
                token = new CLToken(CLToken.Commands.HELP, in);
            } else if (tmp.equals("message")) {
                token = new CLToken(CLToken.Commands.MESSAGE, in);
            } else if (tmp.equals("!skip")) {  // Secret and reserved!
                token = new CLToken(CLToken.Commands.CANCEL, in);
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
     * Wrapper for Scanner.hasNext()
     * @return true if there is new user input to be read
     * @throws IOException 
     */
    /*public int available() throws IOException {
        return input.available();
    }*/
    
    /**
     * Takes input from user by prefixing line with "cksm> " prompt
     * @return the line the user entered
     * @throws IOException 
     */
    private String clIn() throws IOException {
        // Print prompt
        System.out.print("cksm> ");
       
        // Waits until a line is available or told to skip
        while (input != null && System.in.available() == 0 && !this.skipInput) {
            ;
        }
        
        // Return skip code if input skipped
        if (this.skipInput || input == null) {;
            this.skipInput = false;
            return new String("!!skip");
        } else {
            /*// Read manually because Java SUCKS
            byte tmp = '\0';
            byte[] buf = new byte[MAX_BUF_SIZE];
            int i = 0;
            while ((tmp = (byte)input.read()) != '\n') {
                // strip LF, CR and vertical tab??
                if (tmp != 12 && tmp != 13 && tmp != 15) {
                    buf[i] = tmp;
                    System.out.println((int)tmp);
                    i++;
                }
            }
            
            // Get rid of unnecessary null bytes
            byte[] out = new byte[i];
            for (int j = 0; j < i; j++) {
                out[j] = buf[j];
            }*/
            
            return input.nextLine();//new String(out, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Takes multiple lines of input from user by prefixing block with "cksm#"
     * prompt
     * @return the lines the user entered
     * @throws IOException 
     */
    private String[] clMulti() throws IOException {
        // Print prompt
        System.out.println("cksm#");
        
        // Input string and message buffer
        String in = "";
        String[] mult = new String[MESSAGE_MAX_LINES];
        int messageLineCount = 0;
        
        // Waits until a line is available or told to skip
        while (input != null && System.in.available() == 0 && !this.skipInput) {
            ;
        }
               
        // Return skip code if input skipped
        if (this.skipInput || input == null) {;
            this.skipInput = false;
            String[] out = {new String("!!skip")};
            return out;
        } else {
            while (!in.strip().toLowerCase().contains("!done") && messageLineCount < MESSAGE_MAX_LINES) {
                in = input.nextLine();//new String(input.readAllBytes(), StandardCharsets.UTF_8);
                mult[messageLineCount] = new String(in);
                messageLineCount++;
            }
            if (messageLineCount >= MESSAGE_MAX_LINES) {
                System.out.println("Message line count limit reached (2048 lines)!");
            }
        }
        
        return mult;
    }
}
