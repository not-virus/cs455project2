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
     * @param key a public RSA key
     */
    public void setKey(PublicKey key) {
        this.key = key;
    }

    /**
     * @param filePath path to write the public key file to
     * @throws IOException
     */
    public void writeKey(Path filePath) throws IOException {
        // Open FileOutputStream on the public key file
        FileOutputStream file = new FileOutputStream(filePath + ".pub");
        file.write(this.key.getEncoded());
        file.close();
    }

    /**
     * @param filePath path to a X509 encoded public key file
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public void loadKey(Path filePath) throws IOException, InvalidKeySpecException {
        // Open FileInputStream on the public key file
        FileInputStream keyFile = new FileInputStream(filePath + ".pub");
        
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
