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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static slash.navigation.gui.jarinjar.JarInJarURLStreamHandler.JAR_IN_JAR_PROTOCOL;

/**
 * Return a {@link ClassLoader} which contains a JAR in the classpath.
 *
 * @author Christian Pesch, inspired from https://github.com/mchr3k/swtjar/blob/master/src/org/swtjar/SWTLoader.java
 */
public class JarInJarLoader {
    public static ClassLoader loadJar(String fileName) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
        URLClassLoader classLoader = URLClassLoader.class.cast(JarInJarLoader.class.getClassLoader());
        URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory(classLoader));
        Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        URL fileUrl = new URL(JAR_IN_JAR_PROTOCOL + fileName);
        addUrlMethod.setAccessible(true);
        addUrlMethod.invoke(classLoader, fileUrl);
        return classLoader;
    }
}