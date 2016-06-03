package org.jenkinsci.plugins.consulkv;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.consulkv.common.Constants;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;
import org.jenkinsci.plugins.consulkv.common.VariableInjectionAction;
import org.jenkinsci.plugins.consulkv.common.exceptions.BuilderException;
import org.jenkinsci.plugins.consulkv.common.exceptions.ConsulRequestException;
import org.jenkinsci.plugins.consulkv.common.exceptions.ValidationException;
import org.jenkinsci.plugins.consulkv.common.utils.ConsulRequestUtils;
import org.jenkinsci.plugins.consulkv.common.utils.Strings;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Jenkins Plugin to READ/WRITE/DELETE K/V pairs from/to a Consul cluster, and set a build ENV variable to
 * be used by downstream build steps.
 *
 * @author Jimmy Ray
 * @version 2.0.0
 */
public class ConsulKVBuilder extends Builder implements SimpleBuildStep {
    private final String hostUrl;
    private final String key;
    private String token;
    private String keyValue;
    private String urlOverride;
    private String envVarKey;
    private RequestMode requestMode;
    private Integer timeoutConnection;
    private Integer timeoutResponse;
    private DebugMode debugMode;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @Deprecated
    public ConsulKVBuilder(String token, String hostUrl, String key, String keyValue, String urlOverride, String
            envVarKey, RequestMode requestMode, Integer timeoutConnection, Integer timeoutResponse, DebugMode
                                   debugMode) {
        this.token = token;
        this.hostUrl = hostUrl;
        this.key = key;
        this.keyValue = keyValue;
        this.urlOverride = urlOverride;
        this.envVarKey = envVarKey;
        this.requestMode = requestMode;
        this.timeoutConnection = timeoutConnection;
        this.timeoutResponse = timeoutResponse;
        this.debugMode = debugMode;
    }

    @DataBoundConstructor
    public ConsulKVBuilder(@CheckForNull String hostUrl, @CheckForNull String key) {
        this.hostUrl = hostUrl;
        this.key = key;
    }

    public String getToken() {
        return this.token;
    }

    @DataBoundSetter
    public void setToken(@CheckForNull String token) {
        this.token = token;
    }

    public String getHostUrl() {
        return this.hostUrl;
    }

    public String getKey() {
        return this.key;
    }

    public String getKeyValue() {
        return this.keyValue;
    }

    @DataBoundSetter
    public void setKeyValue(@CheckForNull String keyValue) {
        this.keyValue = keyValue;
    }

    public String getUrlOverride() {
        return this.urlOverride;
    }

    @DataBoundSetter
    public void setUrlOverride(@CheckForNull String urlOverride) {
        this.urlOverride = urlOverride;
    }

    public String getEnvVarKey() {
        return this.envVarKey;
    }

    @DataBoundSetter
    public void setEnvVarKey(@CheckForNull String envVarKey) {
        this.envVarKey = envVarKey;
    }

    public RequestMode getRequestMode() {
        return this.requestMode;
    }

    @DataBoundSetter
    public void setRequestMode(@CheckForNull RequestMode requestMode) {
        this.requestMode = requestMode;
    }

    public Integer getTimeoutConnection() {
        return this.timeoutConnection;
    }

    @DataBoundSetter
    public void setTimeoutConnection(@CheckForNull Integer timeoutConnection) {
        this.timeoutConnection = timeoutConnection;
    }

    public Integer getTimeoutResponse() {
        return this.timeoutResponse;
    }

    @DataBoundSetter
    public void setTimeoutResponse(@CheckForNull Integer timeoutResponse) {
        this.timeoutResponse = timeoutResponse;
    }

    public DebugMode getDebugMode() {
        return this.debugMode;
    }

    @DataBoundSetter
    public void setDebugMode(@CheckForNull DebugMode debugMode) {
        this.debugMode = debugMode;
    }

    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull
    TaskListener listener) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();

        boolean status = true;

        int timeoutConn = (this.timeoutConnection == null || this.timeoutConnection.intValue() == 0) ? Constants
                .TIMEOUT_CONNECTION : this.timeoutConnection;
        int timeoutResp = (this.timeoutResponse == null || this.timeoutResponse.intValue() == 0) ? Constants
                .TIMEOUT_RESPONSE : this.timeoutResponse;

        String url = this.hostUrl;
        String apiUrl = null;

        if (this.urlOverride == null || "".equals(this.urlOverride)) {
            apiUrl = Constants.API_URI;
        } else {
            apiUrl = urlOverride;
        }

        try {
            String responseRaw = null;

            if (this.token == null || "".equals(this.token)) {
                //No token
                url += apiUrl + this.key;
            } else {
                if (this.token.contains("${")) {
                    if (debugMode.equals(DebugMode.ENABLED)) {
                        logger.println("Token=" + this.token);
                    }

                    //Resolve token from supplied build parm
                    List<String> tokenKeys = Strings.parseRegExGroups(this.token, Constants.REGEX_PATTERN_BUILD_PARM);

                    if (tokenKeys == null || tokenKeys.isEmpty()) {
                        throw new BuilderException(String.format("Builder could not parse build parameter from %s.",
                                this.token));
                    }

                    String tokenLocal = build.getEnvironment(listener).get(tokenKeys.get(0));

                    if (debugMode.equals(DebugMode.ENABLED)) {
                        logger.println("Token to be used=" + tokenLocal);
                    }

                    url += apiUrl + this.key + String.format(Constants.TOKEN_URL_PATTERN, tokenLocal);
                } else {
                    //Use token field value
                    url += apiUrl + this.key + String.format(Constants.TOKEN_URL_PATTERN, this.token);
                }
            }

            if (this.debugMode.equals(DebugMode.ENABLED)) {
                logger.println("Consul URL for K/V Lookup:  " + url);
            }

            if (this.requestMode.equals(RequestMode.READ)) {
                //Read
                ConsulRequest consulRequest = ConsulRequestFactory.request().withUrl(url).withTimeoutConnect
                        (timeoutConn).withTimeoutResponse(timeoutResp).withDebugMode(debugMode).withRequestMode
                        (requestMode).withLogger(logger).build();

                responseRaw = ConsulRequestUtils.read(consulRequest);
                String value = ConsulRequestUtils.decodeValue(ConsulRequestUtils.parseJson(responseRaw));
                logger.println(String.format("Consul K/V pair:  %s=%s", this.key, value));

                //Set ENV Variable
                String storageKey = Strings.normalizeStoragekey(this.envVarKey);

                VariableInjectionAction action = new VariableInjectionAction(storageKey, value);
                build.addAction(action);
                build.getEnvironment(listener);

                logger.println(String.format("Stored ENV variable (k,v):  %s=%s", storageKey, build.getEnvironment
                        (listener).get(storageKey)));
            } else if (this.requestMode.equals(RequestMode.WRITE)) {
                //Write
                ConsulRequest consulRequest = ConsulRequestFactory.request().withUrl(url).withValue(this.keyValue)
                        .withTimeoutConnect(timeoutConn).withTimeoutResponse(timeoutResp).withDebugMode(debugMode)
                        .withRequestMode(requestMode).withLogger(logger).build();

                responseRaw = ConsulRequestUtils.write(consulRequest);
            } else {
                //Delete
                ConsulRequest consulRequest = ConsulRequestFactory.request().withUrl(url).withTimeoutConnect
                        (timeoutConn).withTimeoutResponse(timeoutResp).withDebugMode(debugMode).withRequestMode
                        (requestMode).withLogger(logger).build();

                responseRaw = ConsulRequestUtils.delete(consulRequest);
            }

            if (this.debugMode.equals(DebugMode.ENABLED)) {
                logger.printf("Raw content:  %s%n", responseRaw);
            }
        } catch (BuilderException be) {
            logger.printf("Builder exception was detected:  %s%n", be);
            status = false;
        } catch (IOException ioe) {
            logger.printf("IO exception was detected:  %s%n", ioe);
            status = false;
        } catch (InterruptedException ie) {
            logger.printf("Interrupted exception was detected:  %s%n", ie);
            Thread.currentThread().interrupt();
            status = false;
        } catch (ValidationException ve) {
            logger.printf("Validation exception was detected:  %s%n", ve);
            status = false;
        } catch (ConsulRequestException cre) {
            logger.printf("Consul request exception was detected:  %s%n", cre);
        }
    }

    /**
     * Descriptor for {@link ConsulKVBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>src/main/resources/hudson/plugins/ConsulKVBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private RequestMode defaultRequestMode = RequestMode.READ;

        private DebugMode defaultDebugMode = DebugMode.DISABLED;

        public DescriptorImpl() {
            load();
        }

        public ListBoxModel doFillRequestModeItems() {
            return RequestMode.getFillItems();
        }

        public ListBoxModel doFillDefaultRequestModeItems() {
            return RequestMode.getFillItems();
        }

        public ListBoxModel doFillDebugModeItems() {
            return DebugMode.getFillItems();
        }

        public ListBoxModel doFillDefaultDebugModeItems() {
            return DebugMode.getFillItems();
        }

        public RequestMode getDefaultRequestMode() {
            return defaultRequestMode;
        }

        public void setDefaultRequestMode(RequestMode defaultRequestMode) {
            this.defaultRequestMode = defaultRequestMode;
        }

        public DebugMode getDefaultDebugMode() {
            return defaultDebugMode;
        }

        public void setDefaultDebugMode(DebugMode defaultDebugMode) {
            this.defaultDebugMode = defaultDebugMode;
        }

        public FormValidation doCheckToken(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("No token specified.  Call will be made without a token.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckHostUrl(@QueryParameter String value) {
            String message = "Please set a Host URL, including protocol, eg: http/https.";

            if (value.length() == 0) {
                return FormValidation.error(message);
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckKey(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set the key for this request.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckEnvVarKey(@QueryParameter String value) {

            String message = "Please enter an ENV variable storage key that is only RegEx word characters, and " +
                    "hyphens.";
            if (value.length() == 0) {
                return FormValidation.error(message);
            }
            if (value.contains(".") || value.contains("/")) {
                return FormValidation.error(message);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUrlOverride(@QueryParameter String value) {

            String message = "Please set a URL override that begins and ends with a \"/\"., example: " + Constants
                    .API_URI + ".";

            if (value.length() == 0) {
                return FormValidation.error(message);
            }
            if (!Strings.checkPattern(value, Constants.REGEX_PATTERN_API_URI)) {
                return FormValidation.error("Invalid URI pattern supplied.");
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Consul K/V Builder";
        }
    }
}

