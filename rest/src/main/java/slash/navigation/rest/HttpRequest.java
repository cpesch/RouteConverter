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
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import slash.navigation.rest.ssl.SSLConnectionManagerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.net.Proxy.NO_PROXY;
import static java.net.Proxy.Type.HTTP;
import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.InputOutput.readBytes;

/**
 * Wrapper for a simple HTTP Request.
 *
 * @author Christian Pesch
 */

public abstract class HttpRequest {
    public static final String APPLICATION_JSON = "application/json";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36";

    private final Logger log;
    private final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    private final HttpRequestBase method;
    private HttpResponse response;
    private HttpClientContext context;
    private final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

    HttpRequest(HttpRequestBase method) {
        this.log = Logger.getLogger(getClass().getName());
        requestConfigBuilder.setConnectTimeout(15 * 1000);
        requestConfigBuilder.setSocketTimeout(90 * 1000);
        clientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        try {
            HttpClientConnectionManager connectionManager = new SSLConnectionManagerFactory().createConnectionManager();
            clientBuilder.setConnectionManager(connectionManager);
        } catch (Exception e) {
            log.severe("Cannot create SSL connection manager that supports letsencrypt root certificate: " + getLocalizedMessage(e));
        }
        setUserAgent("RouteConverter REST Client/" + System.getProperty("rest", "2.30")); // versioned preference
        this.method = method;
    }

    HttpRequest(HttpRequestBase method, Credentials credentials) {
        this(method);
        if (credentials != null)
            setAuthentication(credentials);
    }

    HttpRequestBase getMethod() {
        return method;
    }

    private void setAuthentication(String userName, String password, URI uri) {
        int port = uri.getScheme().equals("https") ? 443 : 80;
        HttpHost httpHost = new HttpHost(uri.getHost(), port, uri.getScheme());
        AuthScope authScope = new AuthScope(httpHost, "api", null);
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(authScope, new UsernamePasswordCredentials(userName, password));
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(httpHost, basicAuth);
        context = HttpClientContext.create();
        context.setAuthCache(authCache);
        context.setCredentialsProvider(credentialsProvider);
    }

    private void setAuthentication(Credentials credentials) {
        URI uri = method.getURI();
        setAuthentication(credentials.getUserName(), credentials.getPassword(), uri);
    }

    public void setUserAgent(String userAgent) {
        clientBuilder.setUserAgent(userAgent);
    }

    public void setSocketTimeout(int socketTimeout) {
        requestConfigBuilder.setSocketTimeout(socketTimeout);
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

    private Proxy findHTTPProxy(URI uri) {
        try {
            ProxySelector selector = ProxySelector.getDefault();
            if(selector != null) {
                List<Proxy> proxyList = selector.select(uri);
                for (Proxy proxy : proxyList) {
                    if (proxy.type().equals(HTTP))
                        return proxy;
                }
            }
        } catch (Exception e) {
            log.severe("Exception while finding proxy for " + uri + ": " + getLocalizedMessage(e));
        }
        return NO_PROXY;
    }

    protected HttpResponse execute() throws IOException {
        Proxy proxy = findHTTPProxy(method.getURI());
        if(proxy != NO_PROXY) {
            SocketAddress address = proxy.address();
            if(address instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
                requestConfigBuilder.setProxy(new HttpHost(inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
                log.info(format("Using proxy %s for %s", proxy.toString(), method.getURI()));
            }
        }

        RequestConfig requestConfig = requestConfigBuilder.build();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        try {
            return clientBuilder.build().execute(method, context);
        } catch (SocketException e) {
            if (throwsSocketExceptionIfUnAuthorized())
                return new BasicHttpResponse(HTTP_1_1, SC_UNAUTHORIZED, "socket exception since unauthorized");
            else
                throw e;
        }
    }

    public String executeAsString() throws IOException {
        try {
            this.response = execute();
            HttpEntity entity = response.getEntity();
            // HEAD requests don't have a body
            String body = entity != null ? new String(readBytes(entity.getContent()), StandardCharsets.UTF_8) : null;
            if (!isSuccessful() && body != null)
                log.warning(format("Body of %s not null: %s", response, body));
            return body;
        } finally {
            release();
        }
    }

    public InputStream executeAsStream() throws IOException {
        this.response = execute();
        // no response body then
        HttpEntity entity = response.getEntity();
        InputStream body = entity != null ? entity.getContent() : null;
        if (!isSuccessful() && !isNotModified())
            log.warning(format("Cannot read response body for %s", method.getURI()));
        return body;
    }

    public void release() throws IOException {
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

    public/*for tests*/ List<Header> getHeaders() throws IOException {
        assertExecuted();
        return asList(response.getAllHeaders());
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

    public boolean isNotModified() throws IOException {
        return getStatusCode() == SC_NOT_MODIFIED;
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

    public boolean isBadRequest() throws IOException {
        return getStatusCode() == SC_BAD_REQUEST;
    }

    public boolean isPreconditionFailed() throws IOException {
        return getStatusCode() == SC_PRECONDITION_FAILED;
    }
}
