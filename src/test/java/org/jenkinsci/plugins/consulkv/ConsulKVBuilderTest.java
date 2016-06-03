package org.jenkinsci.plugins.consulkv;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.RequestMode;
import org.jenkinsci.plugins.consulkv.common.exceptions.BuilderException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * This test uses the Jenkins test harness and requires a Consul server be reachable and for the correct ACL entries
 * to be configured.
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public class ConsulKVBuilderTest {

    private static final String ACL_ID = "<ACL_ID>";
    private static final String HOST = "<CONSUL_HOST_URL>";
    private static final String KEY = "test/test-key";
    private static final String VALUE = "test-value";
    private static final String ENV_KEY = "test-key";
    private static final String WRITE_TEST_VALUE = "Raw content:  true";
    private static final String READ_TEST_VALUE_1 = "Consul K/V pair:  test/test-key=test-value";
    private static final String READ_TEST_VALUE_2 = "Stored ENV variable (k,v):  test-key=test-value";
    private static final String IO_EXCEPTION_TEXT = "IO Exception in test case:  ";
    private static final String INTERRUPTED_EXCEPTION_TEXT = "Interrupted Exception in test case:  ";
    private static final String EXECUTION_EXCEPTION_TEXT = "Execution Exception in test case:  ";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void consulWrite() throws BuilderException {

        try {
            FreeStyleProject project = jenkinsRule.createFreeStyleProject();
            project.getBuildersList().add(new ConsulKVBuilder(ConsulKVBuilderTest.ACL_ID,
                    ConsulKVBuilderTest.HOST, ConsulKVBuilderTest.KEY,
                    ConsulKVBuilderTest.VALUE, null, null, RequestMode.WRITE, 30000, 30000, DebugMode.ENABLED));
            FreeStyleBuild build = project.scheduleBuild2(0).get();

            String log = FileUtils.readFileToString(build.getLogFile());
            assertThat(log, containsString(ConsulKVBuilderTest.WRITE_TEST_VALUE));
        } catch (IOException ioe) {
            throw new BuilderException(ConsulKVBuilderTest.IO_EXCEPTION_TEXT, ioe);
        } catch (InterruptedException ie) {
            throw new BuilderException(ConsulKVBuilderTest.INTERRUPTED_EXCEPTION_TEXT, ie);
        } catch (ExecutionException ee) {
            throw new BuilderException(ConsulKVBuilderTest.EXECUTION_EXCEPTION_TEXT, ee);
        }
    }

    @Test
    public void consulRead() throws BuilderException {
        try {
            FreeStyleProject project = jenkinsRule.createFreeStyleProject();
            project.getBuildersList().add(new ConsulKVBuilder(null, ConsulKVBuilderTest.HOST, ConsulKVBuilderTest
                    .KEY, null,
                    null, ConsulKVBuilderTest.ENV_KEY, RequestMode.READ, 30000,
                    30000, DebugMode.ENABLED));
            FreeStyleBuild build = project.scheduleBuild2(0).get();

            String log = FileUtils.readFileToString(build.getLogFile());
            assertThat(log, containsString(ConsulKVBuilderTest.READ_TEST_VALUE_1));
            assertThat(log, containsString(ConsulKVBuilderTest.READ_TEST_VALUE_2));
        } catch (IOException ioe) {
            throw new BuilderException(ConsulKVBuilderTest.IO_EXCEPTION_TEXT, ioe);
        } catch (InterruptedException ie) {
            throw new BuilderException(ConsulKVBuilderTest.INTERRUPTED_EXCEPTION_TEXT, ie);
        } catch (ExecutionException ee) {
            throw new BuilderException(ConsulKVBuilderTest.EXECUTION_EXCEPTION_TEXT, ee);
        }
    }

    @Test
    public void consulDelete() throws BuilderException {
        try {
            FreeStyleProject project = jenkinsRule.createFreeStyleProject();
            project.getBuildersList().add(new ConsulKVBuilder(ConsulKVBuilderTest.ACL_ID,
                    ConsulKVBuilderTest.HOST, ConsulKVBuilderTest.KEY, null, null, null, RequestMode.DELETE, 30000,
                    30000,
                    DebugMode.ENABLED));
            FreeStyleBuild build = project.scheduleBuild2(0).get();

            String log = FileUtils.readFileToString(build.getLogFile());
            assertThat(log, containsString(ConsulKVBuilderTest.WRITE_TEST_VALUE));
        } catch (IOException ioe) {
            throw new BuilderException(ConsulKVBuilderTest.IO_EXCEPTION_TEXT, ioe);
        } catch (InterruptedException ie) {
            throw new BuilderException(ConsulKVBuilderTest.INTERRUPTED_EXCEPTION_TEXT, ie);
        } catch (ExecutionException ee) {
            throw new BuilderException(ConsulKVBuilderTest.EXECUTION_EXCEPTION_TEXT, ee);
        }
    }
}
