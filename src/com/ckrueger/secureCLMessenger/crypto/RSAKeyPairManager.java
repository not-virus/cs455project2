package com.ckrueger.secureCLMessenger.crypto;

import java.security.*;
import java.security.spec.*;
import java.io.*;
import java.nio.file.*;

/**
 * @author Cameron Krueger
 * Specifically for use with RSA. Stores a private and public key. Similar to
 * the built-in KeyPair class, but adds automatic key generation, key file I/O
 * functionality and is only for use with RSA keys.
 */
public class RSAKeyPairManager {

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;

    /**
     * Manages a public and private RSA key pair.
     * Keys must be individually loaded from key files or
     * generated with the generateKeyPair method before this class can be used.
     */
    public RSAKeyPairManager() {
        this.privateKey = null;
        this.publicKey = null;
    }

    /**
     * Manages a public and private RSA key pair.
     * Will automatically generate keys of size keySize
     * bits upon instantiation and can be used to save and load keys from a
     * file.
     * 
     * @param keySize the desired size of the RSA keys in bits
     */
    public RSAKeyPairManager(int keySize) {
        KeyPair keyPair = generateKeyPair(keySize);

        this.setPrivateKey(keyPair.getPrivate());
        this.setPublicKey(keyPair.getPublic());
    }

    /**
     * @return the private key
     */
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    /**
     * @return the public key
     */
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    /**
     * @return true if the private key has been set, else false
     */
    public boolean privateKeySet() {
        return this.privateKey != null;
    }

    /**
     * @return true if the public key has been set, else false
     */
    public boolean publicKeySet() {
        return this.publicKey != null;
    }

    /**
     * Writes a PKCS8 encoded private key to a file, deleting the file if it
     * already exists, and creating a new file
     * 
     * @param filePath path to write the private key file to
     * @throws IOException if unable to write key to file
     */
    public void writePrivate(String filePath) throws IOException {
        // Create file if it doesn't exist, if it does, delete and create new
        File tmpFileObj = new File(filePath);
        if (!tmpFileObj.exists()) {
            tmpFileObj.createNewFile();
        } else {
            tmpFileObj.delete();
            tmpFileObj.createNewFile();
        }

        // Write key to file
        FileOutputStream file = new FileOutputStream(filePath);
        file.write(this.privateKey.getEncoded());
        file.close();
    }

    /**
     * Writes a X509 encoded public key to a file, deleting the file if 
     * already exists, and creating a new file
     * 
     * @param filePath path to write the public key file to
     * @throws IOException if unable to write key to file
     */
    public void writePublic(String filePath) throws IOException {
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

        // Write key to file
        FileOutputStream file = new FileOutputStream(filePath);
        file.write(this.publicKey.getEncoded());
        file.close();
    }

    /**
     * Loads a PKCS8 encoded private key from a file
     * 
     * @param filePath path to a PKCS8 encoded private keyFile
     * @throws IOException             if unable to load key from file
     * @throws InvalidKeySpecException if the key stored in the file is stored
     *                                 with incorrect keyspec
     */
    public void loadPrivate(String filePath) throws IOException,
            InvalidKeySpecException {
        // Open FileInputStream on the public key file
        FileInputStream keyFile = new FileInputStream(filePath + "");

        // Get file data in bytes
        byte[] fileData = keyFile.readAllBytes();

        // Public key is saved with X.509 encoding, load into KeySpec
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(fileData);

        // Use KeySpec to generate the RSA private key
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            this.privateKey = null;
            e.printStackTrace(); // Will never get here
        }
    }

    /**
     * Loads a X509 encoded public key from a file
     * 
     * @param filePath path to a X509 encoded public keyFile
     * @throws IOException             if unable to load key from file
     * @throws InvalidKeySpecException if the key stored in the file is stored
     *                                 with the incorrect keyspec
     */
    public void loadPublic(String filePath) throws IOException,
            InvalidKeySpecException {
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
            this.publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            this.publicKey = null;
            e.printStackTrace(); // Will never get here
        }
    }

    /**
     * @return a secure RSA key pair
     */
    public KeyPair generateKeyPair(int keySize) {
        KeyPair keyPair = null;

        try {
            // Instantiate and initialize a KeyPairGenerator
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keySize, new SecureRandom());

            // Get the key pair
            keyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(); // Will never get here
            System.exit(1);
        }

        return keyPair;
    }

    /**
     * @param privateKey a private RSA key
     */
    private void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @param publicKey a public RSA key
     */
    private void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}
