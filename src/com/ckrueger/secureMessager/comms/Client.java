package com.ckrueger.secureMessager.comms;

import java.net.*;
import java.io.*;

public class Client {
    
    final int READ_BUFFER_SIZE = 2048;
    private Socket socket = null;
    private String address = null;
    private int port = 0;
    private InputStream dataInput = null;
    private OutputStream dataOutput = null;
    
    /**
     * Creates a client object and initiates a connection to the server
     * @param address the IP address of a remote server
     * @param port the port number of the server
     * @throws UnknownHostException
     * @throws IOException
     */
    public Client(String address, int port) throws UnknownHostException, IOException {
        this.address = address;
        this.port = port;
        
        // Open the connection
        this.open();
    }
    
    /**
     * Initiates a connection to the server 
     * @throws UnknownHostException
     * @throws IOException
     */
    public void open() throws UnknownHostException, IOException, ConnectException {
        // Open client-side connection to server
        this.socket = new Socket(address, port);
        
        // Configure IO streams
        this.dataInput = socket.getInputStream(); //new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dataOutput = socket.getOutputStream(); //new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));   
    }
    
    /**
     * @return the remote port to which the client is connected
     */
    public int getRemotePort() {
        return this.port;
    }
        
    /**
     * @param port a remote port to which the client should connect
     */
    public void setRemotePort(int port) {
        this.port = port;
    }
    
    /**
     * @return the public IP address of the remote server
     */
    public String getRemoteAddress() {
        return this.address;
    }
    
    /**
     * @return returns an approximate number of bytes available for reading
     * from dataInput
     * @throws IOException 
     */
    public int available() throws IOException {
        return dataInput.available();
    }
    
    /**
     * @param data bytes to send to server
     * @throws IOException
     */
    public void write(byte[] data) throws IOException {
        // Write bytes to client
        dataOutput.write(data);
        dataOutput.write(3);
        dataOutput.flush();
    }
    
    /**
     * @param len number of bytes to read from server
     * @return an array of bytes received from the server if available, else null
     * @throws IOException
     */
    public byte[] readBytes(int len) throws IOException {
        // This will remain null unless data is available to read
        byte[] receivedData = new byte[READ_BUFFER_SIZE];
        
        /*if (dataInput.available() != 0)
        {
            // Read available bytes
            receivedData = dataInput.readNBytes(len);
        }*/
        
        
        // Read len bytes from input stream OR up to BUFFER_SIZE
        int readCount = 0;
        int currentByte = 0;
        while (readCount < READ_BUFFER_SIZE && readCount < len && (currentByte = dataInput.read()) != 3) {
            receivedData[readCount] = (byte) currentByte;
            readCount++;
        }
        
        return receivedData;
    }
    
    /**
     * @return an array of bytes received from the server if available, else null
     * @throws IOException
     */
    public byte[] readAllBytes() throws IOException {
        // This will remain null unless data is available to read
        /*byte[] receivedData = null;
        
        if (dataInput.available() != 0)
        {
            // Read available bytes
            receivedData = dataInput.readAllBytes();
        }*/
        
        // This will remain null unless data is available to read
        byte[] receivedData = new byte[READ_BUFFER_SIZE];
        
        // Read available bytes                
        int readCount = 0;
        int currentByte = 0;
        while (readCount < READ_BUFFER_SIZE && (currentByte = dataInput.read()) != 3) {
            receivedData[readCount] = (byte) currentByte;
            readCount++;
        }
        
        return receivedData;
    }
    
    /**
     * @throws IOException
     */
    public void close() throws IOException {
        this.socket.close();
        this.dataInput.close();
        this.dataOutput.close();
    }
    
}
