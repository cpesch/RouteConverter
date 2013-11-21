/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.rest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.util.logging.Logger;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PARTIAL_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static slash.common.io.InputOutput.readBytes;
import static slash.common.io.Transfer.UTF8_ENCODING;

/**
 * Wrapper for a simple HTTP Request.
 *
 * @author Christian Pesch
 */

public abstract class HttpRequest {
    private final Logger log;
    private final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    private final HttpRequestBase method;
    private HttpResponse response;

    HttpRequest(HttpRequestBase method) {
        this.log = Logger.getLogger(getClass().getName());
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setConnectTimeout(15 * 1000);
        requestConfigBuilder.setSocketTimeout(60 * 1000);
        clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
        clientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        setUserAgent("RouteConverter REST Client/" + System.getProperty("rest", "1.6"));
        this.method = method;
    }

    HttpRequest(HttpRequestBase method, Credentials credentials) {
        this(method);
        setAuthentication(credentials);
    }

    HttpRequestBase getMethod() {
        return method;
    }

    private void setAuthentication(String userName, String password, AuthScope authScope) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(authScope, new UsernamePasswordCredentials(userName, password));
        clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        // preemptive authentication
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        HttpHost targetHost = new HttpHost(authScope.getHost(), authScope.getPort(), authScope.getScheme());
        authCache.put(targetHost, basicAuth);
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
    }

    private void setAuthentication(Credentials credentials) {
        URI uri = method.getURI();
        setAuthentication(credentials.getUserName(), credentials.getPassword(), new AuthScope(uri.getHost(), uri.getPort(), "Restricted Access"));
    }

    public void setUserAgent(String userAgent) {
        clientBuilder.setUserAgent(userAgent);
    }

    protected void setHeader(String name, String value) {
        getMethod().setHeader(name, value);
    }

    protected void disableContentCompression() {
        clientBuilder.disableContentCompression();
    }

    protected boolean throwsSocketExceptionIfUnAuthorized() {
        return false;
    }

    protected HttpResponse doExecute() throws IOException {
        try {
            return clientBuilder.build().execute(method);
        } catch (SocketException e) {
            if (throwsSocketExceptionIfUnAuthorized())
                return new BasicHttpResponse(HTTP_1_1, SC_UNAUTHORIZED, "socket exception since unauthorized");
            else
                throw e;
        }
    }

    public String execute() throws IOException {
        return execute(true);
    }

    public String execute(boolean logUnsuccessful) throws IOException {
        try {
            this.response = doExecute();
            // no response body then
            if (isUnAuthorized())
                return null;
            HttpEntity entity = response.getEntity();
            // HEAD requests don't have a body
            String body = entity != null ? new String(readBytes(entity.getContent()), UTF8_ENCODING) : null;
            if (!isSuccessful() && logUnsuccessful && body != null)
                log.warning(body);
            return body;
        } finally {
            release();
        }
    }

    public InputStream executeAsStream(boolean logUnsuccessful) throws IOException {
        this.response = doExecute();
        // no response body then
        if (isUnAuthorized())
            return null;
        InputStream body = response.getEntity().getContent();
        if (!isSuccessful() && logUnsuccessful)
            log.warning("Cannot read response body");
        return body;
    }

    private void release() throws IOException {
        if(response instanceof Closeable)
            ((Closeable)response).close();
        method.reset();
    }

    private void assertExecuted() throws IOException {
        if (response == null)
            throw new IOException("No request executed yet");
    }

    protected String getHeader(String name) throws IOException {
        assertExecuted();
        Header header = response.getFirstHeader(name);
        return header != null ? header.getValue() : null;
    }

    public int getStatusCode() throws IOException {
        assertExecuted();
        return response.getStatusLine().getStatusCode();
    }

    public boolean isSuccessful() throws IOException {
        return getStatusCode() >= SC_OK && getStatusCode() < SC_MULTIPLE_CHOICES;
    }

    public boolean isOk() throws IOException {
        return getStatusCode() == SC_OK;
    }

    public boolean isPartialContent() throws IOException {
        return getStatusCode() == SC_PARTIAL_CONTENT;
    }

    public boolean isUnAuthorized() throws IOException {
        return getStatusCode() == SC_UNAUTHORIZED;
    }

    public boolean isForbidden() throws IOException {
        return getStatusCode() == SC_FORBIDDEN;
    }

    public boolean isNotFound() throws IOException {
        return getStatusCode() == SC_NOT_FOUND;
    }
}
