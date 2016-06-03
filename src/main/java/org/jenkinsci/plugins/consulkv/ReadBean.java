package org.jenkinsci.plugins.consulkv;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * Backing bean for Jelly forms in Build Environment Wrapper
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ReadBean extends AbstractDescribableImpl<ReadBean> {

    private String key;
    private String url;
    private String envKey;
    private String token;
    private DebugMode debugMode;
    private String urlOverride;
    private int timeoutConnect;
    private int timeoutResponse;

    @DataBoundConstructor
    public ReadBean(String token, String url,
                    String key, String envKey) {
        super();
        this.token = token;
        this.url = url;
        this.key = key;
        this.envKey = envKey;
    }

    public DebugMode getDebugMode() {
        return debugMode;
    }

    @DataBoundSetter
    public void setDebugMode(DebugMode debugMode) {
        this.debugMode = debugMode;
    }

    public String getUrlOverride() {
        return urlOverride;
    }

    @DataBoundSetter
    public void setUrlOverride(String urlOverride) {
        this.urlOverride = urlOverride;
    }

    public int getTimeoutConnect() {
        return timeoutConnect;
    }

    @DataBoundSetter
    public void setTimeoutConnect(int timeoutConnect) {
        this.timeoutConnect = timeoutConnect;
    }

    public int getTimeoutResponse() {
        return timeoutResponse;
    }

    @DataBoundSetter
    public void setTimeoutResponse(int timeoutResponse) {
        this.timeoutResponse = timeoutResponse;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEnvKey() {
        return envKey;
    }

    public void setEnvKey(String envKey) {
        this.envKey = envKey;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ReadBean> {
        private DebugMode defaultDebugMode = DebugMode.DISABLED;

        @Override
        public String getDisplayName() {
            return "Consul Read";
        }

        public FormValidation doCheckToken(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
                return FormValidation.error("Empty token, no token will be used.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUrl(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
                return FormValidation.error("Empty URL.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckKey(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
                return FormValidation.error("Empty Key.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckEnvKey(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
                return FormValidation.error("Empty ENV key");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillDebugModeItems() {
            return DebugMode.getFillItems();
        }

        public ListBoxModel doFillDefaultDebugModeItems() {
            return DebugMode.getFillItems();
        }

        public DebugMode getDefaultDebugMode() {
            return defaultDebugMode;
        }

        public void setDefaultDebugMode(DebugMode defaultDebugMode) {
            this.defaultDebugMode = defaultDebugMode;
        }

    }
}
