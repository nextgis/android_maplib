package com.nextgis.maplib.util;

/**
 * Custom exception for library purposes
 */
public class NGException extends Exception {
    public NGException(String message) {
        super(message);
    }

    public NGException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
