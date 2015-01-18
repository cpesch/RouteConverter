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

package slash.navigation.gui.jarinjar;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static java.net.URLDecoder.decode;
import static slash.common.io.Transfer.UTF8_ENCODING;

/**
 * An {@link URLConnection} that reads URLs from the classpath.
 *
 * @author Christian Pesch, inspired from https://code.google.com/p/entail/source/browse/jarinjarloader/org/eclipse/jdt/internal/jarinjarloader/
 */
class ClassPathURLConnection extends URLConnection {
    private ClassLoader classLoader;

    ClassPathURLConnection(URL url, ClassLoader classLoader) {
        super(url);
        this.classLoader = classLoader;
    }

    public void connect() throws IOException {
    }

    public InputStream getInputStream() throws IOException {
        String file = decode(url.getFile(), UTF8_ENCODING);
        InputStream result = classLoader.getResourceAsStream(file);
        if (result == null) {
            throw new MalformedURLException("Could not open InputStream for URL '" + url + "'");
        }
        return result;
    }
}