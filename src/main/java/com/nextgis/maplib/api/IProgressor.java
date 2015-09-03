package com.nextgis.maplib.api;

/**
 * Interface for progress indication
 */
public interface IProgressor {
    void setMax(int maxValue);
    boolean isCanceled();
    void setValue(int value);
    void setIndeterminate(boolean indeterminate);
    void setMessage(String message);
}
