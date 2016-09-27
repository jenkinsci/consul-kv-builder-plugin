package org.jenkinsci.plugins.consulkv;


import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;

import java.io.PrintStream;

/**
 * Consul Request domain type
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ConsulRequest {
    private String value;
    private String url;
    private int timeoutConnect;
    private int timeoutResponse;
    private PrintStream logger;
    private DebugMode debugMode;
    private RequestMode requestMode;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeoutConnect() {
        return timeoutConnect;
    }

    public void setTimeoutConnect(int timeoutConnect) {
        this.timeoutConnect = timeoutConnect;
    }

    public PrintStream getLogger() {
        return logger;
    }

    public void setLogger(PrintStream logger) {
        this.logger = logger;
    }

    public DebugMode getDebugMode() {
        return debugMode;
    }

    public void setDebugMode(DebugMode debugMode) {
        this.debugMode = debugMode;
    }

    public int getTimeoutResponse() {
        return timeoutResponse;
    }

    public void setTimeoutResponse(int timeoutResponse) {
        this.timeoutResponse = timeoutResponse;
    }

    public RequestMode getRequestMode() {
        return requestMode;
    }

    public void setRequestMode(RequestMode requestMode) {
        this.requestMode = requestMode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ConsulRequest{" +
                "value='" + value + '\'' +
                ", url='" + url + '\'' +
                ", timeoutConnect=" + timeoutConnect +
                ", timeoutResponse=" + timeoutResponse +
                ", debugMode=" + debugMode +
                ", requestMode=" + requestMode +
                '}';
    }
}
