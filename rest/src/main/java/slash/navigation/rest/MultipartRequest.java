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

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpHeaders.LOCATION;
import static slash.common.io.Transfer.encodeUriButKeepSlashes;

/**
 * Wrapper for a HTTP Multipart Request.
 *
 * @author Christian Pesch
 */

abstract class MultipartRequest extends HttpRequest {
    private static final ContentType TEXT_PLAIN_UTF8 = ContentType.create("text/plain", UTF_8);
    private MultipartEntityBuilder builder;
    private boolean containsFileLargerThan4k;

    MultipartRequest(HttpUriRequestBase method, Credentials credentials) {
        super(method, credentials);
    }

    private MultipartEntityBuilder getBuilder() {
        if (builder == null)
            builder = MultipartEntityBuilder.create();
        return builder;
    }

    public void addString(String name, String value) {
        getBuilder().addTextBody(name, value, TEXT_PLAIN_UTF8);
    }

    public void addFile(String name, File value) {
        if (value.exists() && value.length() > 4096)
            containsFileLargerThan4k = true;
        getBuilder().addBinaryBody(name, value, APPLICATION_OCTET_STREAM, encodeUriButKeepSlashes(value.getName()));
    }

    public void addFile(String name, byte[] value) {
        if (value.length > 4096)
            containsFileLargerThan4k = true;
        getBuilder().addBinaryBody(name, value, APPLICATION_OCTET_STREAM, encodeUriButKeepSlashes(name + ".xml"));
    }

    protected boolean throwsSocketExceptionIfUnAuthorized() {
        return containsFileLargerThan4k;
    }

    public <T> T execute(HttpClientResponseHandler<T> responseHandler) throws IOException {
        if (builder != null) {
            HttpEntity entity = builder.build();
            getMethod().setEntity(entity);
        }
        return super.execute(responseHandler);
    }

    public String getLocation() throws IOException {
        return getHeader(LOCATION);
    }

    public void setAccept(String accept) {
        setHeader(ACCEPT, accept);
    }
}
