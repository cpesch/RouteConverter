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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static slash.common.jarinjar.JarInJarURLStreamHandler.JAR_IN_JAR_PROTOCOL;

/**
 * Creates a {@link ClassLoader} that allows to include JARs in the classpath and in the file systems.
 * Works fine for Java 7 and 8 but not with Java 9 due to the classpath/module changes.
 *
 * @author Christian Pesch, inspired from https://github.com/mchr3k/swtjar/blob/master/src/org/swtjar/SWTLoader.java
 *         and http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime
 */
public class ClassPathExtender {
    private final ClassLoader classLoader = ClassPathExtender.class.getClassLoader();

    public ClassPathExtender() {
        URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory(classLoader));
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private void addURL(URL url) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(classLoader, url);
    }

    public void addJarInJar(String fileName) throws MalformedURLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        addURL(new URL(JAR_IN_JAR_PROTOCOL + fileName));
    }

    public void addExternalFile(File file) throws MalformedURLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        addURL(file.toURI().toURL());
    }
}