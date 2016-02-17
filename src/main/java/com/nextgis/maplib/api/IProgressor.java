package com.nextgis.maplib.api;

/**
 * Interface for progress indication
 */
public interface IProgressor {
    /**
     * Set maximum value of progress
     * @param maxValue maximum value
     */
    void setMax(int maxValue);

    /**
     * The process should execute this method if it should be canceled.
     * @return true if progress must be canceled, or false
     */
    boolean isCanceled();

    /**
     * Set current progress.
     * @param value current progress
     */
    void setValue(int value);

    /**
     * If progress cannopt determinate the max value and current value, it can be set as indeterminate.
     * @param indeterminate true to set indeterminate state, or false
     */
    void setIndeterminate(boolean indeterminate);

    /**
     * Set some message to show in progress dialog or somewhere else.
     * @param message a message
     */
    void setMessage(String message);
}
