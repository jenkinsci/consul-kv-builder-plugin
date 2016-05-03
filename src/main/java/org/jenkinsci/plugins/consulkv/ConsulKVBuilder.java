package org.jenkinsci.plugins.consulkv;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Base64;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.consulkv.common.Constants;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;
import org.jenkinsci.plugins.consulkv.common.exceptions.BuilderException;
import org.jenkinsci.plugins.consulkv.common.utils.Strings;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author Jimmy Ray
 * @version 1.0.1
 *          <p>
 *          Jenkins Plugin to READ/WRITE/DELETE K/V pairs from/to a Consul cluster, and set a build ENV variable to
 *          be used by
 *          downstream build steps.
 */
public class ConsulKVBuilder extends Builder {
    private static final String TOKEN_URL_PATTERN = "?token=%s";

    private final String token;
    private final String hostUrl;
    private final String key;
    private final String keyValue;
    private final String urlOverride;
    private final String envVarKey;
    private final RequestMode requestMode;
    private final Integer timeoutConnection;
    private final Integer timeoutResponse;
    private final DebugMode debugMode;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
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

    public String getToken() {
        return this.token;
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

    public String getUrlOverride() {
        return this.urlOverride;
    }

    public String getEnvVarKey() {
        return this.envVarKey;
    }

    public RequestMode getRequestMode() {
        return this.requestMode;
    }

    public Integer getTimeoutConnection() {
        return this.timeoutConnection;
    }

    public Integer getTimeoutResponse() {
        return this.timeoutResponse;
    }

    public DebugMode getDebugMode() {
        return this.debugMode;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

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

        CloseableHttpClient httpclient = null;

        try {
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

                    String tokenLocal = build.getBuildVariableResolver().resolve(tokenKeys.get(0));

                    if (debugMode.equals(DebugMode.ENABLED)) {
                        logger.println("Token to be used=" + tokenLocal);
                    }

                    url += apiUrl + this.key + String.format(ConsulKVBuilder.TOKEN_URL_PATTERN, tokenLocal);
                } else {
                    //Use token field value
                    url += apiUrl + this.key + String.format(ConsulKVBuilder.TOKEN_URL_PATTERN, this.token);
                }
            }

            logger.println("Consul URL for K/V Lookup:  " + url);

            httpclient = HttpClients.createDefault();
            HttpDelete httpDelete = null;
            HttpGet httpGet = null;
            HttpPut httpPut = null;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeoutConn)
                    .setConnectTimeout(timeoutConn)
                    .setConnectionRequestTimeout(timeoutResp)
                    .build();

            String responseBody = null;

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws IOException {
                    if (debugMode.equals(DebugMode.ENABLED)) {
                        logger.println("Response Headers: ");
                        Header[] headers = response.getAllHeaders();
                        for (Header header : headers) {
                            logger.println(String.format("%s=%s", header.getName(), header.getValue()));
                        }
                    }

                    int status = response.getStatusLine().getStatusCode();
                    if (status >= Constants.HTTP_OK && status < Constants.HTTP_MULTI_CHOICES) {
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            return EntityUtils.toString(entity);
                        }
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }

                    return null;
                }
            };

            if (this.requestMode.equals(RequestMode.READ)) {
                httpGet = new HttpGet(url);
                httpGet.setConfig(requestConfig);

                responseBody = httpclient.execute(httpGet, responseHandler);
                String value = this.decodeValue(this.parseJson(responseBody));

                logger.println(String.format("Consul K/V pair:  %s=%s", this.key, value));

                //Set ENV Variable
                String storageKey = this.envVarKey.replace('.', '_').replace('/', '_');

                VariableInjectionAction action = new VariableInjectionAction(storageKey, value);
                build.addAction(action);
                build.getEnvironment(listener);

                logger.println(String.format("Stored ENV variable (k,v):  %s=%s", storageKey, build.getEnvironment
                        (listener).get
                        (storageKey)));
            } else if (this.requestMode.equals(RequestMode.WRITE)) {
                httpPut = new HttpPut(url);
                httpPut.setConfig(requestConfig);
                httpPut.addHeader(Constants.LABEL_CONTENT_TYPE, Constants.MEDIA_TYPE_PLAIN_TEXT);
                httpPut.addHeader(Constants.LABEL_ACCEPT, Constants.MEDIA_TYPE_APP_JSON);
                StringEntity input = new StringEntity(this.keyValue);
                httpPut.setEntity(input);
                responseBody = httpclient.execute(httpPut, responseHandler);
            } else {
                //Delete
                httpDelete = new HttpDelete(url);
                httpDelete.setConfig(requestConfig);
                httpDelete.addHeader(Constants.LABEL_CONTENT_TYPE, Constants.MEDIA_TYPE_PLAIN_TEXT);
                httpDelete.addHeader(Constants.LABEL_ACCEPT, Constants.MEDIA_TYPE_APP_JSON);
                responseBody = httpclient.execute(httpDelete, responseHandler);
            }

            if (this.debugMode.equals(DebugMode.ENABLED)) {
                logger.printf("Raw content:  %s%n", responseBody);
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
        } finally {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException ioe) {
                    logger.printf("IO Exception was encountered when closing HTTP client.  %s%n", ioe);
                    status = false;
                }
            }
        }

        return status;
    }

    private String parseJson(String data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getJsonFactory();

        JsonParser jsonParser = factory.createParser(data);
        JsonNode actualObj = mapper.readTree(jsonParser);

        return actualObj.get(0).get("Value").toString();
    }

    private String decodeValue(String value) throws UnsupportedEncodingException {
        byte[] valueDecoded = Base64.decodeBase64(value);
        return new String(valueDecoded, Constants.DEFAULT_ENCODING);
    }

    // Overridden for better type safety.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
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

    static final class VariableInjectionAction implements EnvironmentContributingAction {

        private String key;
        private String value;

        public VariableInjectionAction(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void buildEnvVars(AbstractBuild build, EnvVars envVars) {

            if (envVars != null && key != null && value != null) {
                envVars.put(key, value);
            }
        }

        @Override
        public String getDisplayName() {
            return "VariableInjectionAction";
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return null;
        }
    }
}

