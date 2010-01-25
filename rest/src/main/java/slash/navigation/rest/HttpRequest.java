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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Wrapper for a simple HTTP Request.
 *
 * @author Christian Pesch
 */

public abstract class HttpRequest {
    private static Logger log = Logger.getLogger(HttpRequest.class.getName());

    private final HttpClient client;
    final HttpMethod method;
    private Integer statusCode;

    HttpRequest(HttpMethod method) {
        log = Logger.getLogger(getClass().getName());
        this.client = new HttpClient();
        client.getParams().setIntParameter("http.connection.timeout", 15 * 1000);
        client.getParams().setIntParameter("http.socket.timeout", 60 * 1000);
        client.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler(0, false));
        client.getParams().setParameter("http.useragent", "RouteConverter Web Client/0.2");
        this.method = method;
    }

    private void setAuthentication(String userName, String password, AuthScope authScope) {
        client.getState().setCredentials(authScope, new UsernamePasswordCredentials(userName, password));
        client.getParams().setAuthenticationPreemptive(true);
        method.setDoAuthentication(true);
    }

    public void setAuthentication(String userName, String password, String authScopeUrl, String authScopeScheme) {
        setAuthentication(userName, password, new AuthScope(authScopeUrl, 80, AuthScope.ANY_REALM, authScopeScheme));  // TODO required?
    }

    public void setAuthentication(String userName, String password) {
        try {
            URI uri = method.getURI();
            setAuthentication(userName, password, new AuthScope(uri.getHost(), uri.getPort(), "Restricted Access")); // TODO required?
        } catch (URIException e) {
            log.severe("Cannot set authentication: " + e.getMessage());
        }
    }

    public void setBody(String body) {
        try {
            ((EntityEnclosingMethod) method).setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE, Helper.UTF8_ENCODING));
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot set body: " + e.getMessage());
        }
    }

    protected void doExecute() throws IOException {
        statusCode = client.executeMethod(method);
    }

    public String execute() throws IOException {
        return execute(true);
    }

    public String execute(boolean logUnsuccessful) throws IOException {
        try {
            doExecute();
            // no response body then
            if (isUnAuthorized())
                return null;
            String body = method.getResponseBodyAsString();
            if (!isSuccessful() && logUnsuccessful)
                log.info(body);
            return body;
        }
        finally {
            release();
        }
    }

    void release() {
        method.releaseConnection();
    }

    public int getResult() throws IOException {
        if(statusCode == null)
            throw new HttpException("No method executed yet");
        return statusCode;
    }

    public boolean isSuccessful() throws IOException {
        return getResult() >= HttpStatus.SC_OK && getResult() < HttpStatus.SC_MULTIPLE_CHOICES; 
    }

    public boolean isUnAuthorized() throws IOException {
        return getResult() == HttpStatus.SC_UNAUTHORIZED;
    }

    public boolean isForbidden() throws IOException {
        return getResult() == HttpStatus.SC_FORBIDDEN;
    }

    public boolean isNotFound() throws IOException {
        return getResult() == HttpStatus.SC_NOT_FOUND;
    }
}
