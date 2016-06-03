package org.jenkinsci.plugins.consulkv;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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

            //Make Consul Call to get K/V data
            int timeoutConn = (read.getTimeoutConnect() == 0) ? Constants
                    .TIMEOUT_CONNECTION : read.getTimeoutConnect();
            int timeoutResp = (read.getTimeoutResponse() == 0) ? Constants
                    .TIMEOUT_RESPONSE : read.getTimeoutResponse();

            String apiUrl = null;
            if (Strings.isBlank(read.getUrlOverride())) {
                apiUrl = Constants.API_URI;
            } else {
                apiUrl = read.getUrlOverride();
            }

            String url = read.getUrl();

            try {
                if (!Strings.isBlank(read.getToken())) {
                    url += apiUrl + read.getKey();
                } else {
                    if (read.getToken().contains("${")) {
                        if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                            logger.println("Token=" + read.getToken());
                        }

                        //Resolve token from supplied build parm
                        List<String> tokenKeys = Strings.parseRegExGroups(read.getToken(), Constants
                                .REGEX_PATTERN_BUILD_PARM);

                        if (tokenKeys == null || tokenKeys.isEmpty()) {
                            throw new ValidationException(String.format("Wrapper could not parse build parameter from" +
                                            " %s.",
                                    read.getToken()));
                        }

                        String tokenLocal = run.getEnvironment(listener).get(tokenKeys.get(0));

                        if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                            logger.println("Token to be used=" + tokenLocal);
                        }

                        url += apiUrl + read.getKey() + String.format(Constants.TOKEN_URL_PATTERN, tokenLocal);
                    } else {
                        //Use token field value
                        url += apiUrl + read.getKey() + String.format(Constants.TOKEN_URL_PATTERN, read.getToken());
                    }
                }

                ConsulRequest consulRequest = ConsulRequestFactory.request().withUrl(url).withTimeoutConnect
                        (timeoutConn).withTimeoutResponse(timeoutResp).withRequestMode(RequestMode.READ)
                        .withDebugMode(read.getDebugMode()).withLogger
                                (logger).build();

                String responseRaw = ConsulRequestUtils.read(consulRequest);
                String value = ConsulRequestUtils.decodeValue(ConsulRequestUtils.parseJson(responseRaw));

                read.setEnvKey(Strings.normalizeStoragekey(read.getEnvKey()));

                context.env(read.getEnvKey(), value);

                if (read.getDebugMode().equals(DebugMode.ENABLED)) {
                    logger.printf("Raw content:  %s%n", responseRaw);
                    logger.println(String.format("Stored ENV variable (k,v):  %s=%s", read.getEnvKey(),
                            context.getEnv().get(read.getEnvKey())));
                }
            } catch (IOException ioe) {
                logger.printf("IO exception was detected:  %s%n", ioe);
            } catch (ValidationException ve) {
                logger.printf("Validation exception was detected:  %s%n", ve);
            } catch (ConsulRequestException cre) {
                logger.printf("Consul request exception was detected:  %s%n", cre);
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
            return "Add Consul Read Config(s)";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

    }

}
