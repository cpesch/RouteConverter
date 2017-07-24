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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.apache.http.Consts.UTF_8;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
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

    MultipartRequest(HttpEntityEnclosingRequestBase method, Credentials credentials) {
        super(method, credentials);
    }

    private MultipartEntityBuilder getBuilder() {
        if (builder == null)
            builder = MultipartEntityBuilder.create();
        return builder;
    }

    private HttpEntityEnclosingRequestBase getHttpEntityEnclosingRequestBase() {
        return (HttpEntityEnclosingRequestBase) getMethod();
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

    protected HttpResponse execute() throws IOException {
        if (builder != null) {
            HttpEntity entity = builder.build();
            getHttpEntityEnclosingRequestBase().setEntity(entity);
        }
        return super.execute();
    }

    public String getLocation() throws IOException {
        return getHeader(LOCATION);
    }

    public void setAccept(String accept) {
        setHeader(ACCEPT, accept);
    }
}
