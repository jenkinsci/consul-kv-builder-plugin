package org.jenkinsci.plugins.consulkv;

import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;
import org.jenkinsci.plugins.consulkv.common.exceptions.ValidationException;
import org.jenkinsci.plugins.consulkv.common.utils.Strings;

import java.io.PrintStream;

/**
 * Factory to create <code>ConsulRequest</code> objects
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ConsulRequestFactory {

    private final ConsulRequest consulRequest;

    private ConsulRequestFactory() {
        this.consulRequest = new ConsulRequest();
    }

    public static ConsulRequestFactory request() {
        return new ConsulRequestFactory();
    }

    public ConsulRequestFactory withUrl(final String url) {
        consulRequest.setUrl(url);
        return this;
    }

    public ConsulRequestFactory withValue(final String value) {
        consulRequest.setValue(value);
        return this;
    }

    public ConsulRequestFactory withTimeoutConnect(final int timeoutConnect) {
        consulRequest.setTimeoutConnect(timeoutConnect);
        return this;
    }

    public ConsulRequestFactory withTimeoutResponse(final int timeoutResponse) {
        consulRequest.setTimeoutResponse(timeoutResponse);
        return this;
    }

    public ConsulRequestFactory withLogger(final PrintStream logger) {
        consulRequest.setLogger(logger);
        return this;
    }

    public ConsulRequestFactory withRequestMode(final RequestMode requestMode) {
        consulRequest.setRequestMode(requestMode);
        return this;
    }

    public ConsulRequestFactory withDebugMode(final DebugMode debugMode) {
        consulRequest.setDebugMode(debugMode);
        return this;
    }

    public ConsulRequest build() throws ValidationException {
        this.validate();
        return this.consulRequest;
    }

    private boolean validate() throws ValidationException {
        if (Strings.isBlank(this.consulRequest.getUrl())) {
            throw new ValidationException("Empty url");
        }

        if (null == this.consulRequest.getRequestMode()) {
            throw new ValidationException("Null request mode");
        }

        if (null == this.consulRequest.getDebugMode()) {
            throw new ValidationException("Null debug mode");
        }

        if (null == this.consulRequest.getLogger()) {
            throw new ValidationException("Null logger");
        }

        if (0 == this.consulRequest.getTimeoutConnect()) {
            throw new ValidationException("0 connection timeout");
        }

        if (0 == this.consulRequest.getTimeoutResponse()) {
            throw new ValidationException("0 response timeout");
        }

        switch (this.consulRequest.getRequestMode()) {
            case WRITE:
                if (Strings.isBlank(this.consulRequest.getValue())) {
                    throw new ValidationException("Empty value");
                }
                break;
            default:
        }

        return true;
    }
}
