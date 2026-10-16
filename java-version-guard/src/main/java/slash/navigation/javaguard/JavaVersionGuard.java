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
package slash.navigation.javaguard;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Main-Class of the jars that run on the user's own Java (Linux): compiled for
 * Java 8, it checks the running Java against the jar's Required-Java-Version
 * manifest entry and only then hands over to its Guarded-Main-Class. On a too
 * old Java the real main class would fail with an UnsupportedClassVersionError
 * that a desktop launch never shows; the guard says what to install instead.
 *
 * @author Christian Pesch
 */
public class JavaVersionGuard {
    static final String GUARDED_MAIN_CLASS = "Guarded-Main-Class";
    static final String REQUIRED_JAVA_VERSION = "Required-Java-Version";
    static final String APPLICATION_NAME = "Application-Name";
    static final String DOWNLOAD_URL = "https://adoptium.net/temurin/releases/?version=";

    public static void main(String[] args) throws Throwable {
        Attributes attributes = readGuardAttributes();
        int required = parseMajor(attributes.getValue(REQUIRED_JAVA_VERSION));
        int running = parseMajor(System.getProperty("java.specification.version"));
        if (!isSupported(running, required)) {
            reportTooOld(attributes.getValue(APPLICATION_NAME), required, running);
            System.exit(1);
        }
        delegate(attributes.getValue(GUARDED_MAIN_CLASS), args);
    }

    /**
     * The major version of a java.specification.version or java.version value:
     * "1.8" is 8, "21" is 21, "25-ea" is 25; 0 when it cannot be parsed.
     */
    static int parseMajor(String version) {
        if (version == null)
            return 0;
        String trimmed = version.trim();
        if (trimmed.startsWith("1."))
            trimmed = trimmed.substring(2);
        int end = 0;
        while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end)))
            end++;
        if (end == 0)
            return 0;
        try {
            return Integer.parseInt(trimmed.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * An unknown version on either side never blocks the start: the guard only
     * stops what it knows is too old.
     */
    static boolean isSupported(int running, int required) {
        return running == 0 || required == 0 || running >= required;
    }

    /**
     * The manifest attributes that carry the guard entries; not simply the first
     * META-INF/MANIFEST.MF on the class path, which may belong to another jar.
     */
    static Attributes readGuardAttributes() throws IOException {
        Enumeration<URL> manifests = JavaVersionGuard.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            try (InputStream inputStream = manifests.nextElement().openStream()) {
                Attributes attributes = new Manifest(inputStream).getMainAttributes();
                if (attributes.getValue(GUARDED_MAIN_CLASS) != null)
                    return attributes;
            }
        }
        throw new IllegalStateException("No manifest with " + GUARDED_MAIN_CLASS + " on the class path");
    }

    static void delegate(String mainClass, String[] args) throws Throwable {
        try {
            Class.forName(mainClass).getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static void reportTooOld(String application, int required, int running) {
        ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/javaguard/JavaVersionGuard");
        String name = application != null ? application : "RouteConverter";
        String message = MessageFormat.format(bundle.getString("java-too-old"),
                name, String.valueOf(required), String.valueOf(running));
        System.err.println(message);
        if (GraphicsEnvironment.isHeadless())
            return;

        String download = MessageFormat.format(bundle.getString("download-java"), String.valueOf(required));
        Object[] options = {download, bundle.getString("close")};
        int choice = JOptionPane.showOptionDialog(null, message, name, JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE, null, options, options[0]);
        if (choice == 0)
            browse(DOWNLOAD_URL + required);
    }

    // xdg-open first: Desktop.browse() is unreliable on Linux desktops, where this guard runs
    private static void browse(String url) {
        try {
            new ProcessBuilder("xdg-open", url).start();
            return;
        } catch (IOException e) {
            // no xdg-open, try the AWT way below
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException | RuntimeException e) {
            System.err.println(url);
        }
    }
}
