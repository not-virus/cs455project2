package com.ckrueger.secureMessager.crypto;

import javax.crypto.*;
import java.security.*;
import java.io.*;

public class RSACipher {

    /**
     * @param plainText  data to encrypt
     * @param publicKey  an RSA public key
     * @return encrypted data obtained by encrypting plainText with publicKey using RSA
     * @throws InvalidKeyException
     */
    public byte[] encrypt(byte[] plainText, PublicKey publicKey) throws InvalidKeyException {
        byte[] cipherText = {};
        
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherText = encryptCipher.doFinal(plainText);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();    // Will never get here
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();    // Will (probably?) never get here
        } catch (BadPaddingException e) {
            e.printStackTrace();    // Will (probably?) never get here
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();    // Will (probably?) never get here  multicatch?
        }
        
        return cipherText;
    }

    /**
     * @param cipherText  data to decrypt
     * @param privateKey  an RSA public key 
     * @return decrypted data obtained by decrypting cipherText with publicKey using RSA
     * @throws InvalidKeyException
     */
    public byte[] decrypt(byte[] cipherText, PrivateKey privateKey) throws InvalidKeyException {
        byte[] plainText = {};
        
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            plainText = decryptCipher.doFinal(cipherText);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();    // Will never get here
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();    // Will (probably?) never get here
        } catch (BadPaddingException e) {
            e.printStackTrace();    // Will (probably?) never get here
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();    // Will (probably?) never get here
        }
        
        return plainText;
    }
}
