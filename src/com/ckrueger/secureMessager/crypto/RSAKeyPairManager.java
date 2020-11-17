package com.ckrueger.secureMessager.crypto;

import java.security.*;
import java.security.spec.*;
import java.io.*;
import java.nio.file.*;

public class RSAKeyPairManager {

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;

    /**
     * Automatically generates and manages a public and private key pair. This class
     * is similar to KeyPair, but specifically for RSA keys, and generates the keys
     * automatically. Will automatically generate keys upon instantiation and can be
     * used to save and load keys from a file. Default key size is 2048 bits
     */
    public RSAKeyPairManager() {
        KeyPair keyPair = generateKeyPair(2048);

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    /**
     * Manages a public and private key pair, much like KeyPair, but specifically
     * for RSA. Will automatically generate keys upon instantiation and can be used
     * to save and load keys from a file.
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
     * @param filePath path to write the private key file to
     * @throws IOException
     */
    public void writePrivate(Path filePath) throws IOException {
        FileOutputStream file = new FileOutputStream(filePath + ".key");
        file.write(this.privateKey.getEncoded());
        file.close();
    }

    /**
     * @param filePath path to write the public key file to
     * @throws IOException
     */
    public void writePublic(Path filePath) throws IOException {
        FileOutputStream file = new FileOutputStream(filePath + ".pub");
        file.write(this.publicKey.getEncoded());
        file.close();
    }

    /**
     * @param filePath path to a PKCS8 encoded private keyFile
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public void loadPrivate(Path filePath) throws IOException, InvalidKeySpecException {
        // Open FileInputStream on the public key file
        FileInputStream keyFile = new FileInputStream(filePath + ".key");

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
     * @param filePath path to a X509 encoded public keyFile
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    public void loadPublic(Path filePath) throws IOException, InvalidKeySpecException {
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
            this.publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            this.publicKey = null;
            e.printStackTrace(); // Will never get here
        }
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

    /**
     * @return a secure RSA key pair
     */
    private KeyPair generateKeyPair(int keySize) {
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

}
