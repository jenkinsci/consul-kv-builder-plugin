package org.jenkinsci.plugins.consulkv.common;

import hudson.util.ListBoxModel;

/**
 * Request Mode ENUM
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public enum RequestMode {

    READ, WRITE, DELETE;

    public static ListBoxModel getFillItems() {
        ListBoxModel items = new ListBoxModel();
        for (RequestMode requestMode : values()) {
            items.add(requestMode.name());
        }
        return items;
    }
}
