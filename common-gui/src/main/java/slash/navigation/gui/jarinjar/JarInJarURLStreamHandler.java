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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Handle URLs with protocol {@link #JAR_IN_JAR_PROTOCOL}. "jarinjar:path/file.ext" identifies the content
 * accessible as classLoader.getResourceAsStream("path/file.ext"). "jarinjar:path/" identifies a base-path for
 * resources to be searched. The spec "file.ext" is combined to "jarinjar:path/file.ext".
 *
 * @author Christian Pesch, inspired from https://code.google.com/p/entail/source/browse/jarinjarloader/org/eclipse/jdt/internal/jarinjarloader/
 */
class JarInJarURLStreamHandler extends URLStreamHandler {
    public static final String JAR_IN_JAR = "jarinjar";
    public static final String JAR_IN_JAR_PROTOCOL = JAR_IN_JAR + ":";
    private ClassLoader classLoader;

    JarInJarURLStreamHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return new ClassPathURLConnection(url, classLoader);
    }

    protected void parseURL(URL url, String spec, int start, int limit) {
        String file;
        if (spec.startsWith(JAR_IN_JAR_PROTOCOL))
            file = spec.substring(JAR_IN_JAR_PROTOCOL.length());
        else if (url.getFile().equals("./"))
            file = spec;
        else if (url.getFile().endsWith("/"))
            file = url.getFile() + spec;
        else
            file = spec;
        setURL(url, JAR_IN_JAR, "", -1, null, null, file, null, null);
    }
}