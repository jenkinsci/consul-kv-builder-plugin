package org.jenkinsci.plugins.consulkv.common.exceptions;

/**
 * ConsulRequestException
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ConsulRequestException extends Exception {

    private static final long serialVersionUID = 2161055784155502757L;

    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public ConsulRequestException() {
        super();
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for later
     *                retrieval by the {@link #getMessage()} method.
     */
    public ConsulRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance of this class with the specified detail message
     * and root cause.
     *
     * @param message   the detail message. The detail message is saved for later
     *                  retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public ConsulRequestException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public ConsulRequestException(Throwable rootCause) {
        super(rootCause);
    }
}
