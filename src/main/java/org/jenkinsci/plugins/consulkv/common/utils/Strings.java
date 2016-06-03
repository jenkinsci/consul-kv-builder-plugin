package org.jenkinsci.plugins.consulkv.common.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String utilities
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public final class Strings extends StringUtils {
    private static final Logger LOG = LoggerFactory.getLogger(Strings.class);

    private Strings() {

    }

    /**
     * Replaces null strings with out parm.
     *
     * @param in  String
     * @param out String
     * @return
     */
    public static String nvl(final String in, final String out) {
        if (null == in) {
            return out;
        }

        return in;
    }

    public static String nvlOrEmpty(final String in, final String out) {
        if (in == null || "".equals(in)) {
            return out;
        }

        return in;
    }

    public static String nvlWithLeftPad(final String in, final String out, final String pad) {
        if (null == in) {
            return out;
        }

        return in + pad;
    }

    /**
     * Encodes String with SUPPLIED encoding.
     *
     * @param data
     * @return
     */
    public static String encode(final String data, final String encoding) {
        String encoded = null;

        try {
            encoded = URLEncoder.encode(data, encoding);
        } catch (UnsupportedEncodingException uee) {
            LOG.error(String.format("There was a problem encoding the data.  %s", uee));
        }

        return encoded;
    }

    public static boolean isEmptyAfterTrim(final String in) {
        if (in == null) {
            return true;
        }

        if ("".equals(in.trim())) {
            return true;
        }

        return false;
    }

    public static String maybeGetMappedValue(final ResourceBundle bundle, final String prefix, final String key) {
        String out = null;

        if (Strings.isEmptyAfterTrim(key)) {
            return key;
        }

        if (bundle != null && bundle.containsKey(Strings.nvl(prefix, "") + key)) {
            out = bundle.getString(Strings.nvl(prefix, "") + key);
        } else {
            return key;
        }

        return out;
    }

    public static List<String> parseRegExGroups(String test, String regEx) {
        List<String> groups = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(test);

        while (matcher.find()) {
            groups.add(matcher.group().substring(2, matcher.group().length() - 1));
        }

        return groups;
    }

    public static boolean checkPattern(String test, String regExPattern) {
        Pattern pattern = Pattern.compile(regExPattern);
        Matcher matcher = pattern.matcher(test);
        return matcher.matches();
    }

    public static String normalizeStoragekey(String storageKey) {
        return storageKey.replace('.', '_').replace('/', '_');
    }
}

