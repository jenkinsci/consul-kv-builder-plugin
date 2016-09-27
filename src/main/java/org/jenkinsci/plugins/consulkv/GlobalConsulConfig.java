package org.jenkinsci.plugins.consulkv;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.consulkv.common.Constants;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin to set and test Consul global config settings.
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class GlobalConsulConfig extends GlobalPluginConfiguration {

    private static Logger LOGGER = Logger.getLogger(GlobalConsulConfig.class.getName());

    @Extension
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        private String consulHostUrl;
        private String consulAclToken;
        private String consulApiUri;
        private int consulTimeoutConnection;
        private int consulTimeoutResponse;
        private String consulTestUri;
        private DebugMode consulDebugMode;

        public DescriptorImpl() {
            load();
        }

        public String getConsulHostUrl() {
            return consulHostUrl;
        }

        public String getConsulAclToken() {
            return consulAclToken;
        }

        public String getConsulApiUri() {
            return consulApiUri;
        }

        public int getConsulTimeoutConnection() {
            return consulTimeoutConnection;
        }

        public int getConsulTimeoutResponse() {
            return consulTimeoutResponse;
        }

        public String getConsulTestUri() {
            return consulTestUri;
        }

        public DebugMode getConsulDebugMode() {
            return consulDebugMode;
        }

        public ListBoxModel doFillConsulDebugModeItems() {
            return DebugMode.getFillItems();
        }

        public DebugMode getDefaultConsulDebugMode() {
            return DebugMode.DISABLED;
        }

        @Override
        public String getDisplayName() {
            return Constants.PLUGIN_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            JSONObject json = (JSONObject) formData.get("globalConsulConfig");

            consulHostUrl = json.getString("consulHostUrl");
            consulAclToken = json.getString("consulAclToken");
            consulApiUri = json.getString("consulApiUri");

            try {
                consulTimeoutConnection = Integer.parseInt(json.getString("consulTimeoutConnection"));
            } catch (NumberFormatException nfe) {
                LOGGER.warning(String.format("Using default connection timeout of %s.", Constants.TIMEOUT_CONNECTION));
                consulTimeoutConnection = Constants.TIMEOUT_CONNECTION;
            }

            try {
                consulTimeoutResponse = Integer.parseInt(json.getString("consulTimeoutResponse"));
            } catch (NumberFormatException nfe) {
                LOGGER.warning(String.format("Using default response timeout of %s.", Constants.TIMEOUT_RESPONSE));
                consulTimeoutResponse = Constants.TIMEOUT_RESPONSE;
            }

            consulTestUri = json.getString("consulTestUri");
            consulDebugMode = DebugMode.valueOf(json.getString("consulDebugMode"));

            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public FormValidation doTestConnection(@QueryParameter String consulHostUrl, @QueryParameter String
                consulTestUri) {

            LOGGER.info(String.format("Testing Consul connectivity, URL = %s%s", consulHostUrl, consulTestUri));

            //Validation
            if (consulHostUrl.isEmpty()) {
                return FormValidation.warning("Please enter a Consul host with protocol (http/https) and port.");
            }

            final String URL_VALUE = consulHostUrl + consulTestUri;

            URL url;
            try {
                url = new URL(URL_VALUE);
            } catch (MalformedURLException ex) {
                final String ERROR_INVALID_URL = String.format("Supplied Consul URL (%s) was invalid.", URL_VALUE);
                Logger.getLogger(ConsulKVBuilder.class.getName()).log(Level.WARNING, ERROR_INVALID_URL, ex);
                return FormValidation.error(ERROR_INVALID_URL);
            }

            try {
                URLConnection connection = url.openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    httpConnection.setRequestMethod("HEAD");
                    int code = httpConnection.getResponseCode();
                    httpConnection.disconnect();
                    if (code >= 400) {
                        return FormValidation.error("Could not connect to %s, with HEAD request. HTTP Response Code " +
                                "was:  %s", URL_VALUE, code);
                    }
                }
            } catch (IOException ioe) {
                final String ERROR_UNABLE_TO_CONNECT = String.format("Unable to connect to Consul at URL: %Ss",
                        URL_VALUE);
                Logger.getLogger(ConsulKVBuilder.class.getName()).log(Level.WARNING, ERROR_UNABLE_TO_CONNECT, ioe);
                return FormValidation.error("%s - %s", ERROR_UNABLE_TO_CONNECT, ioe.getMessage());
            }

            return FormValidation.ok("Connected to " + consulHostUrl + consulTestUri);
        }
    }
}
