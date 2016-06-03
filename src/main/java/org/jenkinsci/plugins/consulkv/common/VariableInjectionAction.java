package org.jenkinsci.plugins.consulkv.common;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.EnvironmentContributingAction;

/**
 * Used to inject ENV variables
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public final class VariableInjectionAction implements Action, EnvironmentContributingAction {

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