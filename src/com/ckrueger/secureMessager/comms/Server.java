package com.ckrueger.secureMessager.comms;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server {

    final int READ_BUFFER_SIZE = 2048;
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private InputStream dataInput = null;
    private OutputStream dataOutput = null;
    private String address = null;
    private int port = 0;
    
    /**
     * Creates a client object and waits for a connection
     * @param port the local port number to which the server should be attached
     * @throws IOException
     */
    public Server(int port) throws IOException {
        // Set basic server params
        this.address = InetAddress.getLocalHost().getHostAddress();
        this.port = port;
        
        // Start server socket on provided port
        this.serverSocket = new ServerSocket(this.port);
        
        // Start server
        open();
    }
    
    /**
     * Waits for a connection
     * @throws IOException
     */
    public void open() throws IOException {
        // Wait for incoming connection
        this.socket = serverSocket.accept();
        
        // Configure IO streams
        this.dataInput = socket.getInputStream();
        this.dataOutput = socket.getOutputStream();        
    }
    
    /**
     * @return the port to which the server socket is attached
     */
    public int getPort() {
        return port;
    }
        
    /**
     * @param port a port to which the server socket should be bound
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * @return the public IP address of this server
     */
    public String getAddress() {
        return address;
    }    
    
    /**
     * @return the IP address of the client connected to this server
     */
    public String getRemoteAddress() {
        return this.serverSocket.getInetAddress().toString();
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
     * @param data bytes to send to client
     * @throws IOException
     */
    public void write(byte[] data) throws IOException {
        // Write bytes to client
        dataOutput.write(data);
        dataOutput.write(3);
        dataOutput.flush();
    }
    
    /**
     * @param len number of bytes to read from client
     * @return an array of bytes received from the client if available, else null
     * @throws IOException
     */
    public byte[] readBytes(int len) throws IOException {
        // This will remain null unless data is available to read
        byte[] receivedData = new byte[READ_BUFFER_SIZE];
        
        // Read len bytes from input stream OR up to BUFFER_SIZE
        int readCount = 0;
        int currentByte = 0;
        while (readCount < READ_BUFFER_SIZE && readCount < len && (currentByte
                = dataInput.read()) != 3) {
            receivedData[readCount] = (byte) currentByte;
            readCount++;
        }
        
        byte[] tmp = Arrays.copyOf(receivedData, readCount);
        
        return tmp;
    }
    
    /**
     * @return an array of bytes received from the client if available, else null
     * @throws IOException
     */
    public byte[] readAllBytes() throws IOException {
        // This will remain null unless data is available to read
        byte[] receivedData = new byte[READ_BUFFER_SIZE];
        
        // Read available bytes                
        int readCount = 0;
        int currentByte = 0;
        while (readCount < READ_BUFFER_SIZE && (currentByte = dataInput.read())
                != 3) {
            receivedData[readCount] = (byte) currentByte;
            readCount++;
        }
        
        byte[] tmp = Arrays.copyOf(receivedData, readCount);
        
        return tmp;
    }
    
    /**
     * @throws IOException
     */
    public void close() throws IOException {
        this.serverSocket.close();
        this.socket.close();
        this.dataInput.close();
        this.dataOutput.close();
    }
    
}
