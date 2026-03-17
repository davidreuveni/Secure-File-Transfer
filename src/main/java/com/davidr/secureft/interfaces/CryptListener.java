package com.davidr.secureft.interfaces;

public interface CryptListener {
    void start();
    void progress(int percent);
    void end();
}