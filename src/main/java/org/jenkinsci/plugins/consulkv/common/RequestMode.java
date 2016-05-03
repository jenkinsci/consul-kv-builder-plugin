package org.jenkinsci.plugins.consulkv.common;

import hudson.util.ListBoxModel;

/**
 * @author Jimmy Ray
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
