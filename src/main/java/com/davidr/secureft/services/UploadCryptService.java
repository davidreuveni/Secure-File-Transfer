package com.davidr.secureft.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

import com.davidr.secureft.interfaces.CryptListener;
import com.davidr.secureft.services.AES.AesCipher;
import com.davidr.secureft.utils.utils;

@Service
public class UploadCryptService {

    private static final int BUF_SIZE = 64 * 1024;

    public void cryptStream(boolean encrypt, boolean hmac, InputStream input, OutputStream output, String keyText, CryptListener listener) throws IOException {
        utils.verifyMelacha();
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
        listener.start();

        try {
            AesCipher.cipherStream(encrypt, hmac, in, out, key);
            out.flush();
            listener.end();
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Required cryptography support is unavailable", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unexpected processing failure", e);
        }
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
