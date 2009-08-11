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

import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.*;

import java.io.IOException;
import java.io.InputStream;
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
        client.getParams().setParameter("http.useragent", "RouteConverter Catalog Client/0.1");
        this.method = method;
    }

    public void setAuthentication(String userName, String password) {
        try {
            URI uri = method.getURI();
            client.getState().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort(), "Restricted Access"),
                    new UsernamePasswordCredentials(userName, password)
            );
            client.getParams().setAuthenticationPreemptive(true);
            method.setDoAuthentication(true);
        } catch (URIException e) {
            log.severe("Cannot set authentication: " + e.getMessage());
        }
    }

    public InputStream executeAsStream() throws IOException {
        statusCode = client.executeMethod(method);
        return method.getResponseBodyAsStream();
    }

    void release() {
        method.releaseConnection();
    }

    public String execute() throws IOException {
        return execute(true);
    }

    public String execute(boolean logUnsuccessful) throws IOException {
        try {
            statusCode = client.executeMethod(method);
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
