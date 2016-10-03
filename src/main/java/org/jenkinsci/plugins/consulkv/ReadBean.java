package org.jenkinsci.plugins.consulkv;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Backing bean for Jelly forms in Build Environment Wrapper
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ReadBean extends AbstractDescribableImpl<ReadBean> {
    private static Logger LOGGER = Logger.getLogger(ReadBean.class.getName());

    private String key;
    private String hostUrl;
    private String envKey;
    private String aclToken;
    private DebugMode debugMode;
    private String apiUri;
    private int timeoutConnect;
    private int timeoutResponse;
    private boolean ignoreGlobalSettings;

    @DataBoundConstructor
    public ReadBean(String aclToken, String hostUrl,
                    String key, String envKey) {
        super();
        this.aclToken = aclToken;
        this.hostUrl = hostUrl;
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

    public String getApiUri() {
        return apiUri;
    }

    @DataBoundSetter
    public void setApiUri(String apiUri) {
        this.apiUri = apiUri;
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

    public boolean isIgnoreGlobalSettings() {
        return this.ignoreGlobalSettings;
    }

    @DataBoundSetter
    public void setIgnoreGlobalSettings(boolean ignoreGlobalSettings) {
        this.ignoreGlobalSettings = ignoreGlobalSettings;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getEnvKey() {
        return envKey;
    }

    public void setEnvKey(String envKey) {
        this.envKey = envKey;
    }

    public String getAclToken() {
        return aclToken;
    }

    public void setAclToken(String aclToken) {
        this.aclToken = aclToken;
    }

    @Override
    public String toString() {
        return "ReadBean{" +
                "key='" + key + '\'' +
                ", hostUrl='" + hostUrl + '\'' +
                ", envKey='" + envKey + '\'' +
                ", aclToken='" + aclToken + '\'' +
                ", debugMode=" + debugMode +
                ", apiUri='" + apiUri + '\'' +
                ", timeoutConnect=" + timeoutConnect +
                ", timeoutResponse=" + timeoutResponse +
                ", ignoreGlobalSettings=" + ignoreGlobalSettings +
                '}';
    }

    /**
     * Loads global settings
     */
    public void updateFromGlobalConfiguration() {
        Jenkins jenkins = Jenkins.getInstance();

        if (jenkins !=  null) {
            GlobalConsulConfig.DescriptorImpl globalDescriptor = (GlobalConsulConfig.DescriptorImpl)
                    jenkins.getDescriptor(GlobalConsulConfig.class);

            if (globalDescriptor != null) {
                this.hostUrl = globalDescriptor.getConsulHostUrl();
                this.apiUri = globalDescriptor.getConsulApiUri();
                this.aclToken = globalDescriptor.getConsulAclToken();
                this.timeoutConnect = globalDescriptor.getConsulTimeoutConnection();
                this.timeoutResponse = globalDescriptor.getConsulTimeoutResponse();
                this.debugMode = globalDescriptor.getConsulDebugMode();
            } else {
                LOGGER.warning("Could not load global settings.");
            }
        } else {
            LOGGER.warning("Could not load global settings.");
        }
    }

    /**
     * Descriptor for {@link ReadBean}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<ReadBean> {
        private String hostUrl;
        private String aclToken;
        private String apiUri;
        private String timeoutConnect;
        private String timeoutResponse;
        private DebugMode consulDebugMode;
        private boolean ignoreGlobalSettings;

        public String getHostUrl() {
            return hostUrl;
        }

        public void setHostUrl(String hostUrl) {
            this.hostUrl = hostUrl;
        }

        public String getAclToken() {
            return aclToken;
        }

        public void setAclToken(String aclToken) {
            this.aclToken = aclToken;
        }

        public String getApiUri() {
            return apiUri;
        }

        public void setApiUri(String apiUri) {
            this.apiUri = apiUri;
        }

        public String getTimeoutConnect() {
            return timeoutConnect;
        }

        public void setTimeoutConnect(String timeoutConnect) {
            this.timeoutConnect = timeoutConnect;
        }

        public String getTimeoutResponse() {
            return timeoutResponse;
        }

        public void setTimeoutResponse(String timeoutResponse) {
            this.timeoutResponse = timeoutResponse;
        }

        public DebugMode getConsulDebugMode() {
            return consulDebugMode;
        }

        public void setConsulDebugMode(DebugMode consulDebugMode) {
            this.consulDebugMode = consulDebugMode;
        }

        public boolean isIgnoreGlobalSettings() {
            return ignoreGlobalSettings;
        }

        public void setIgnoreGlobalSettings(boolean ignoreGlobalSettings) {
            this.ignoreGlobalSettings = ignoreGlobalSettings;
        }

        @Override
        public String getDisplayName() {
            return "Consul Read";
        }

        public FormValidation doCheckAclToken(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
                return FormValidation.error("Empty token, no token will be used.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckHostUrl(
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
            return DebugMode.DISABLED;
        }

    }
}
