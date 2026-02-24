package com.davidr.secureft.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.davidr.secureft.services.AES.AesCipher;

@Service
public class UploadCryptService {

    private static final int BUF_SIZE = 64 * 1024;

    public void cryptStream(boolean encrypt, boolean hmac, InputStream input, OutputStream output, String keyText) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        if (output == null) {
            throw new IllegalArgumentException("output is null");
        }
        if (keyText == null || keyText.isBlank()) {
            throw new IllegalArgumentException("key is empty");
        }

        BufferedInputStream in = new BufferedInputStream(input, BUF_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(output, BUF_SIZE);
       
        byte[] key = keyText.getBytes(StandardCharsets.UTF_8);
        AesCipher.cipherStream(encrypt, hmac, in, out, key);
        out.flush();        
    }

    public String getFileName(boolean mode, String originalFileName){
        return mode ? encryptedFileName(originalFileName) : decryptedFileName(originalFileName);
    }
    private String encryptedFileName(String originalFileName) {
        String safeName = (originalFileName == null || originalFileName.isBlank()) ? "file.bin" : originalFileName;
        return safeName + ".enc";
    }

    private String decryptedFileName(String encryptedFileName) {
        String newName = encryptedFileName;
        if (newName.endsWith(".enc") && newName.length() > 4) {
            return newName.substring(0, newName.length() - 4);
        }
        return newName;
    }
}
