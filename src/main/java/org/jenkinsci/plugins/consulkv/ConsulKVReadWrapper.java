package org.jenkinsci.plugins.consulkv;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.plugins.consulkv.common.Constants;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;
import org.jenkinsci.plugins.consulkv.common.exceptions.ConsulRequestException;
import org.jenkinsci.plugins.consulkv.common.exceptions.ValidationException;
import org.jenkinsci.plugins.consulkv.common.utils.ConsulRequestUtils;
import org.jenkinsci.plugins.consulkv.common.utils.Strings;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Wrapper plugin to read Consul K/V data and store in ENV variables
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ConsulKVReadWrapper extends SimpleBuildWrapper {
    protected List<ReadBean> reads;

    @DataBoundConstructor
    public ConsulKVReadWrapper(@CheckForNull List<ReadBean> reads) {
        this.reads = reads;
    }

    @Override
    public void setUp(Context context, Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener
            listener, EnvVars envVars) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();

        for (ReadBean read : reads) {

            try {
                if (!read.isIgnoreGlobalSettings()) {
                    //Try to use global settings and backup from constants.
                    read.updateFromGlobalConfiguration();

                    if (Strings.isEmpty(read.getHostUrl())) {
                        throw new ConsulRequestException("Global settings host URL was not found.");
                    }
                }

                //Make Consul Call to get K/V data
                int timeoutConn = (read.getTimeoutConnect() == 0) ? Constants
                        .TIMEOUT_CONNECTION : read.getTimeoutConnect();
                int timeoutResp = (read.getTimeoutResponse() == 0) ? Constants
                        .TIMEOUT_RESPONSE : read.getTimeoutResponse();

                String apiUrl = null;
                if (Strings.isBlank(read.getApiUri())) {
                    apiUrl = Constants.API_URI;
                } else {
                    apiUrl = read.getApiUri();
                }

                String url = read.getHostUrl();


                if (Strings.isBlank(read.getAclToken())) {
                    url += apiUrl + read.getKey();
                } else {
                    if (read.getAclToken().contains("${")) {
                        if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                            logger.println("ACL Token=" + read.getAclToken());
                        }

                        //Resolve token from supplied build parm
                        List<String> tokenKeys = Strings.parseRegExGroups(read.getAclToken(), Constants
                                .REGEX_PATTERN_BUILD_PARM);

                        if (tokenKeys == null || tokenKeys.isEmpty()) {
                            throw new ValidationException(String.format("Wrapper could not parse build parameter from" +
                                            " %s.",
                                    read.getAclToken()));
                        }

                        String tokenLocal = run.getEnvironment(listener).get(tokenKeys.get(0));

                        if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                            logger.println("Token to be used=" + tokenLocal);
                        }

                        url += apiUrl + read.getKey() + String.format(Constants.TOKEN_URL_PATTERN, tokenLocal);
                    } else {
                        //Use token field value
                        url += apiUrl + read.getKey() + String.format(Constants.TOKEN_URL_PATTERN, read.getAclToken());
                    }
                }

                if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                    logger.println("Consul READ URL:  " + url.toString());
                }

                ConsulRequest consulRequest = ConsulRequestFactory.request().withUrl(url).withTimeoutConnect
                        (timeoutConn).withTimeoutResponse(timeoutResp).withRequestMode(RequestMode.READ)
                        .withDebugMode(read.getDebugMode()).withLogger
                                (logger).build();

                String responseRaw = ConsulRequestUtils.read(consulRequest);
                String value = ConsulRequestUtils.decodeValue(ConsulRequestUtils.parseJson(responseRaw, Constants
                        .FIELD_VALUE));

                read.setEnvKey(Strings.normalizeStoragekey(read.getEnvKey()));

                context.env(read.getEnvKey(), value);

                if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                    logger.printf("Raw content:  %s%n", responseRaw);
                    logger.println(String.format("Stored ENV variable (k,v):  %s=%s", read.getEnvKey(),
                            context.getEnv().get(read.getEnvKey())));
                }
            } catch (IOException ioe) {
                run.setResult(Result.FAILURE);
                listener.fatalError("IO exception was detected:  %s%n", ioe);
            } catch (ValidationException ve) {
                run.setResult(Result.FAILURE);
                listener.fatalError("Validation exception was detected:  %s%n", ve);
            } catch (ConsulRequestException cre) {
                run.setResult(Result.FAILURE);
                listener.fatalError("Consul request exception was detected:  %s%n", cre);
            }
        }
    }

    protected boolean doTearDown() throws IOException, InterruptedException {
        return true;
    }

    public List<ReadBean> getReads() {
        return reads;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "Add Consul K/V Read Config(s)";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

    }

}
