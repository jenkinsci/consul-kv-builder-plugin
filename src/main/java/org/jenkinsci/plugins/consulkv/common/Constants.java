package org.jenkinsci.plugins.consulkv.common;

/**
 * @author Jimmy Ray
 * @version 1.0.0
 */
public final class Constants {
    // \' is left
    public static final char[] SPECIAL_CHARS = {'(', ')', '^', '[', ']', '{', '}', '~', '*', '?', '|', '&', '!', '-',
            '\"', ' '};
    public static final Integer TIMEOUT_CONNECTION = 10000;
    public static final Integer TIMEOUT_RESPONSE = 30000;
    public static final String LABEL_CONTENT_TYPE = "Content-Type";
    public static final String LABEL_ACCEPT = "Accept";
    public static final String API_URI = "/v1/kv/";
    public static final String REGEX_PATTERN_API_URI = "^([\\/]\\w+)+\\/$";
    public static final String MEDIA_TYPE_APP_JSON = "application/json; charset=utf-8";
    public static final String MEDIA_TYPE_PLAIN_TEXT = "plain/text; charset=utf-8";
    public static final String MEDIA_TYPE_APP_FORM = "application/x-www-form-urlencoded; charset=utf-8";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String REGEX_PATTERN_URL = "(((http|https):\\/{2})+(([0-9a-z_-]+\\.)+(com)(:[0-9]+)?((\\/([~0-9a-zA-Z\\#\\+\\%@\\.\\/_-]+))?(\\?[0-9a-zA-Z\\+\\%@\\/&\\[\\];=_-]+)?)?))\\b";
    public static final String REGEX_PATTERN_BUILD_PARM = "(\\$\\{[\\w+-]+\\})";
    public static final String USER_AGENT_DEFAULT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36";
    public static final int HTTP_OK = 200;
    public static final int HTTP_MULTI_CHOICES = 300;

    private Constants() {

    }
}
