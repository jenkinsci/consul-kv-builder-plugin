package org.jenkinsci.plugins.consulkv.common;

import hudson.util.ListBoxModel;

/**
 * @author Jimmy Ray
 */
public enum DebugMode {

    ENABLED, DISABLED;

    public static ListBoxModel getFillItems() {
        ListBoxModel items = new ListBoxModel();
        for (DebugMode debugMode : values()) {
            items.add(debugMode.name());
        }
        return items;
    }
}
