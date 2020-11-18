package com.ckrueger.secureMessager.comms;

import java.io.*;
import java.net.*;

public class Server {
    
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
        
        
        System.out.println("Server listening for connections on port " + this.port);
        
        // Start server
        this.open();
    }
    
    /**
     * Waits for a connection
     * @throws IOException
     */
    public void open() throws IOException {
        // Wait for incoming connection
        this.socket = serverSocket.accept();
        
        // Configure IO streams
        this.dataInput = socket.getInputStream(); //new DataInputStream(socket.getInputStream());
        this.dataOutput = socket.getOutputStream(); //new DataOutputStream(socket.getOutputStream());        
    }
    
    /**
     * @return the port to which the server socket is attached
     */
    public int getPort() {
        return this.port;
    }
        
    /**
     * @param port a port to which the server socket should be attached
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * @return the public IP address of the server
     */
    public String getAddress() {
        return this.address;
    }    
    
    /**
     * @param data bytes to send to client
     * @throws IOException
     */
    public void write(byte[] data) throws IOException {
        // Write bytes to client
        dataOutput.write(data);
        dataOutput.flush();
    }
    
    /**
     * @param len number of bytes to read from client
     * @return an array of bytes received from the client if available, else null
     * @throws IOException
     */
    public byte[] readBytes(int len) throws IOException {
        // This will remain null unless data is available to read
        byte[] receivedData = null;
        
        if (dataInput.available() != 0)
        {
            // Read available bytes
            receivedData = dataInput.readNBytes(len);
        }
        
        return receivedData;
    }
    
    /**
     * @return an array of bytes received from the client if available, else null
     * @throws IOException
     */
    public byte[] readAllBytes() throws IOException {
        // This will remain null unless data is available to read
        final int BUFFER_SIZE = 1024;
        byte[] receivedData = new byte[BUFFER_SIZE];
        
        /*sSystem.out.println(dataInput.available());
        
        if (dataInput.available() != 0)
        {
            // Read available bytes
            receivedData = dataInput.readAllBytes();
        }*/
        
        // Read available bytes
        //receivedData = dataInput.readAllBytes();
        
        int count = 0;
        int current = 0;
        while (count < BUFFER_SIZE && (current = dataInput.read()) != -1) {
            receivedData[count] = (byte) current;
            count++;
        }
        
        return receivedData;
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
