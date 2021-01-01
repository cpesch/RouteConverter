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

package slash.common.jarinjar;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import static slash.common.jarinjar.JarInJarURLStreamHandler.JAR_IN_JAR;

/**
 * Factory that creates a {@link JarInJarURLStreamHandler} for each {@link URL} with the
 * {@link JarInJarURLStreamHandler#JAR_IN_JAR} protocol.
 *
 * @author Christian Pesch, inspired from https://code.google.com/p/entail/source/browse/jarinjarloader/org/eclipse/jdt/internal/jarinjarloader/
 */
class JarInJarURLStreamHandlerFactory implements URLStreamHandlerFactory {
    private ClassLoader classLoader;

    JarInJarURLStreamHandlerFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (JAR_IN_JAR.equals(protocol))
            return new JarInJarURLStreamHandler(classLoader);
        return null;
    }
}