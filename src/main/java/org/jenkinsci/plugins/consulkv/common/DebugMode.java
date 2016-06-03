package org.jenkinsci.plugins.consulkv.common;

import hudson.util.ListBoxModel;

/**
 * Debug Mode ENUM
 *
 * @author Jimmy Ray
 * @version 1.0.0
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
