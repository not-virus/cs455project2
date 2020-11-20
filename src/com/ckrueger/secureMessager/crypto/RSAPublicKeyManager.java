package com.ckrueger.secureMessager.crypto;

import java.security.*;
import java.security.spec.*;
import java.io.*;
import java.nio.file.*;

public class RSAPublicKeyManager {

    private PublicKey key = null;

    /**
     * Manages a public RSA key This class is similar to PublicKey, but is
     * specifically for an RSA key.
     */
    public RSAPublicKeyManager() {
        this.key = null;
    }
    
    /**
     * Manages a public RSA key This class is similar to PublicKey, but is
     * specifically for an RSA key.
     * @param key an RSA PublicKey
     */
    public RSAPublicKeyManager(PublicKey key) {
        this.setKey(key);
    }

    /**
     * @return the public key
     */
    public PublicKey getKey() {
        return this.key;
    }
    
    /**
     * @return true if the key has been set, else false
     */
    public boolean keySet() {
        return this.key != null;
    }

    /**
     * @param key a public RSA key
     */
    private void setKey(PublicKey key) {
        this.key = key;
    }

    /**
     * @param filePath path to write the public key file to
     * @throws IOException
     */
    public void writeKey(String filePath) throws IOException {
        // Public key file name must end with .pub
        if (!filePath.endsWith(".pub")) {
            filePath = filePath + ".pub";
        }
        
        // Create file if it doesn't exist, if it does, delete and create new
        File tmpFileObj = new File(filePath);
        if (!tmpFileObj.exists()) {
            tmpFileObj.createNewFile();
        } else {
            tmpFileObj.delete();
            tmpFileObj.createNewFile();
        }
        
        // Open FileOutputStream on the public key file
        FileOutputStream file = new FileOutputStream(filePath);
        file.write(this.key.getEncoded());
        file.close();
    }
    
    /**
     * Loads a X509 encoded public key directly from a byte array
     * @param filePath path to a X509 encoded public key file
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public void loadKey(byte[] keyEncoded) throws InvalidKeySpecException {
        // Public key is saved with X.509 encoding, load into KeySpec
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyEncoded);

        // Use KeySpec to generate the RSA private keys
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            this.key = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            this.key = null;
            e.printStackTrace(); // Will never get here
        }
    }    

    /**
     * Loads a X509 encoded public key from a key file
     * @param filePath path to a X509 encoded public key file
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public void loadKey(String filePath) throws IOException, InvalidKeySpecException {
        // Public key file name must end with .pub
        if (!filePath.endsWith(".pub")) {
            filePath = filePath + ".pub";
        }
        
        // Open FileInputStream on the public key file
        FileInputStream keyFile = new FileInputStream(filePath);
        
        // Get file data in bytes
        byte[] fileData = keyFile.readAllBytes();

        // Public key is saved with X.509 encoding, load into KeySpec
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(fileData);

        // Use KeySpec to generate the RSA private keys
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            this.key = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            this.key = null;
            e.printStackTrace(); // Will never get here
        }
        
        keyFile.close();
    }
}
