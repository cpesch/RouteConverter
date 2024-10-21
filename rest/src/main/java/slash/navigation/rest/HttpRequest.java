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

import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.net.Proxy.NO_PROXY;
import static java.net.Proxy.Type.HTTP;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hc.core5.http.HttpStatus.*;
import static org.apache.hc.core5.util.TimeValue.ZERO_MILLISECONDS;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Wrapper for a simple HTTP Request.
 *
 * @author Christian Pesch
 */

public abstract class HttpRequest {
    public static final String APPLICATION_JSON = "application/json";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";

    private final Logger log;
    private final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    private final HttpUriRequestBase method;
    private Credentials credentials;
    private ClassicHttpResponse response;
    private final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

    HttpRequest(HttpUriRequestBase method) {
        this.log = Logger.getLogger(getClass().getName());
        requestConfigBuilder.setConnectionRequestTimeout(15, SECONDS);
        requestConfigBuilder.setResponseTimeout(30, SECONDS);
        clientBuilder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(0, ZERO_MILLISECONDS));
        setUserAgent("RouteConverter REST Client/" + System.getProperty("rest", "3.0"));
        this.method = method;
    }

    HttpRequest(HttpUriRequestBase method, Credentials credentials) {
        this(method);
        this.credentials = credentials;
    }

    HttpUriRequestBase getMethod() {
        return method;
    }

    private URI getURI() {
        try {
            return method.getUri();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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

    public <T> T execute(HttpClientResponseHandler<T> responseHandler) throws IOException {
        URI uri = getURI();
        Proxy proxy = findHTTPProxy(uri);
        if(proxy != NO_PROXY) {
            SocketAddress address = proxy.address();
            if(address instanceof InetSocketAddress inetSocketAddress) {
                clientBuilder.setProxy(new HttpHost(inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
                log.info(format("Using proxy %s for %s", proxy, uri));
            }
        }

        RequestConfig requestConfig = requestConfigBuilder.build();
        clientBuilder.setDefaultRequestConfig(requestConfig);


        HttpClientContext context = HttpClientContext.create();
        if(credentials != null && credentials.getUserName() != null && credentials.getPassword() != null) {
            UsernamePasswordCredentials preemptiveCredentials = new UsernamePasswordCredentials(credentials.getUserName(), credentials.getPassword());
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), preemptiveCredentials);
            BasicScheme authScheme = new BasicScheme();
            authScheme.initPreemptive(preemptiveCredentials);
            context.setCredentialsProvider(credentialsProvider);

            HttpHost httpHost = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
            AuthCache authCache = new BasicAuthCache();
            authCache.put(httpHost, authScheme);
            context.setAuthCache(authCache);
        }

        try(CloseableHttpClient httpClient = clientBuilder.build()) {
            return httpClient.execute(method, context, response -> {
                HttpRequest.this.response = response;
                try {
                    return responseHandler.handleResponse(response);
                } catch (HttpException e) {
                    throw new IOException(e);
                }
            });
        } catch (SocketException e) {
            if (throwsSocketExceptionIfUnAuthorized())
                this.response = new BasicClassicHttpResponse(SC_UNAUTHORIZED, "socket exception since unauthorized");
            else
                throw e;
        }
        return null;
    }

    public String executeAsString() throws IOException {
        String body = execute(response -> {
            try {
                HttpEntity entity = response.getEntity();
                // HEAD requests don't have a body
                return entity != null ? EntityUtils.toString(entity) : null;
            } catch (ParseException e) {
                throw new IOException(e);
            }
        });
        if (!isSuccessful() && body != null)
            log.warning(format("Body of %s is not null: %s", getURI(), body));
        return body;
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
        return asList(response.getHeaders());
    }

    public int getStatusCode() throws IOException {
        assertExecuted();
        return response.getCode();
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
