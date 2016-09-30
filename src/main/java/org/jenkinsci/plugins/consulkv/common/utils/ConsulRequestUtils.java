package org.jenkinsci.plugins.consulkv.common.utils;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Base64;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.consulkv.ConsulRequest;
import org.jenkinsci.plugins.consulkv.common.Constants;
import org.jenkinsci.plugins.consulkv.common.DebugMode;
import org.jenkinsci.plugins.consulkv.common.exceptions.ConsulRequestException;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for HTTP requests to Hashicorp Consul REST API.
 *
 * @author Jimmy Ray
 * @version 1.0.0
 */
public final class ConsulRequestUtils {

    private ConsulRequestUtils() {

    }

    public static String read(final ConsulRequest consulRequest) throws ConsulRequestException {
        HttpGet httpGet = new HttpGet(consulRequest.getUrl());

        CloseableHttpClient httpclient = ConsulRequestUtils.getHttpClient(consulRequest, httpGet);

        ResponseHandler<String> responseHandler = ConsulRequestUtils.getResponseHandler(consulRequest.getDebugMode(),
                consulRequest.getLogger());

        String responseBody = null;
        try {
            responseBody = httpclient.execute(httpGet, responseHandler);
        } catch (IOException ioe) {
            consulRequest.getLogger().println(ExceptionUtils.getFullStackTrace(ioe));
            throw new ConsulRequestException("Consul Request Failed.");
        } finally {
            ConsulRequestUtils.closeHttpClient(httpclient, consulRequest);
        }

        return responseBody;
    }

    public static String write(final ConsulRequest consulRequest) throws ConsulRequestException {
        HttpPut httpPut = new HttpPut(consulRequest.getUrl());

        CloseableHttpClient httpclient = ConsulRequestUtils.getHttpClient(consulRequest, httpPut);

        httpPut.addHeader(Constants.LABEL_CONTENT_TYPE, Constants.MEDIA_TYPE_PLAIN_TEXT);
        httpPut.addHeader(Constants.LABEL_ACCEPT, Constants.MEDIA_TYPE_APP_JSON);
        String responseBody = null;

        try {
            StringEntity input = new StringEntity(consulRequest.getValue());
            httpPut.setEntity(input);

            ResponseHandler<String> responseHandler = ConsulRequestUtils.getResponseHandler(consulRequest
                    .getDebugMode(), consulRequest.getLogger());


            responseBody = httpclient.execute(httpPut, responseHandler);
        } catch (IOException ioe) {
            consulRequest.getLogger().println(ExceptionUtils.getFullStackTrace(ioe));
            throw new ConsulRequestException("Consul Request Failed.");
        } finally {
            ConsulRequestUtils.closeHttpClient(httpclient, consulRequest);
        }
        return responseBody;
    }

    public static String delete(final ConsulRequest consulRequest) throws ConsulRequestException {
        HttpDelete httpDelete = new HttpDelete(consulRequest.getUrl());

        CloseableHttpClient httpclient = ConsulRequestUtils.getHttpClient(consulRequest, httpDelete);

        httpDelete.addHeader(Constants.LABEL_CONTENT_TYPE, Constants.MEDIA_TYPE_PLAIN_TEXT);
        httpDelete.addHeader(Constants.LABEL_ACCEPT, Constants.MEDIA_TYPE_APP_JSON);

        ResponseHandler<String> responseHandler = ConsulRequestUtils.getResponseHandler(consulRequest.getDebugMode(),
                consulRequest.getLogger());

        String responseBody = null;
        try {
            responseBody = httpclient.execute(httpDelete, responseHandler);
        } catch (IOException ioe) {
            consulRequest.getLogger().println(ExceptionUtils.getFullStackTrace(ioe));
            throw new ConsulRequestException("Consul Request Failed.");
        } finally {
            ConsulRequestUtils.closeHttpClient(httpclient, consulRequest);
        }
        return responseBody;
    }

    private static ResponseHandler<String> getResponseHandler(final DebugMode debugMode, final PrintStream logger) {
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(final HttpResponse response) throws IOException {
                if (debugMode.equals(DebugMode.ENABLED)) {
                    logger.println("Response Headers: ");
                    Header[] headers = response.getAllHeaders();
                    for (Header header : headers) {
                        logger.println(String.format("%s=%s", header.getName(), header.getValue()));
                    }
                }

                int status = response.getStatusLine().getStatusCode();
                if (status >= Constants.HTTP_OK && status < Constants.HTTP_MULTI_CHOICES) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        return EntityUtils.toString(entity);
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }

                return null;
            }
        };

        return responseHandler;
    }

    private static CloseableHttpClient getHttpClient(final ConsulRequest consulRequest, final HttpRequestBase
            httpRequestBase) {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(consulRequest.getTimeoutConnect())
                .setConnectTimeout(consulRequest.getTimeoutConnect())
                .setConnectionRequestTimeout(consulRequest.getTimeoutResponse())
                .build();

        httpRequestBase.setConfig(requestConfig);

        return httpclient;
    }

    private static void closeHttpClient(final CloseableHttpClient httpclient, final ConsulRequest consulRequest) {
        if (httpclient != null) {
            try {
                httpclient.close();
            } catch (IOException ioe) {
                consulRequest.getLogger().printf("IO Exception was encountered when closing HTTP client.  %s%n",
                        ioe);
            }
        }
    }

    public static String parseJson(String data, String field) throws IOException {
        List<String> fields = Arrays.asList(field);
        return parseJson(data, fields, 0).get(field);
    }

    public static Map<String, String> parseJson(String data, List<String> fields, int index) throws IOException {
        Map<String, String> map = new HashMap<String, String>();

        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getJsonFactory();
        JsonParser jsonParser = factory.createParser(data);
        JsonNode jsonObj = mapper.readTree(jsonParser);

        for (String field : fields) {
            map.put(field, jsonObj.get(index).get(field).toString());
        }


        return map;
    }

    public static String decodeValue(String value) throws UnsupportedEncodingException {
        byte[] valueDecoded = Base64.decodeBase64(value);
        return new String(valueDecoded, Constants.DEFAULT_ENCODING);
    }
}
