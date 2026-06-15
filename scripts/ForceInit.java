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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Forces static initialization of every class in a (shaded) jar under whatever
 * JRE runs this program. Run it on the stripped/minimized JRE that ships with
 * the app: a class whose static initializer touches a JDK class that the
 * minimized runtime dropped (e.g. jdk.net.Sockets) throws
 * NoClassDefFoundError / ExceptionInInitializerError here -- the exact failure
 * a full-JDK test never reproduces.
 *
 * Usage: ForceInit <jar> [includePrefixesCsv]
 *   includePrefixesCsv limits the sweep to classes whose binary name starts
 *   with one of the comma-separated prefixes (default: all). Scope to library
 *   packages (org.apache.,org.sqlite.) to avoid initializing GUI/app classes
 *   that open windows or need a display.
 *
 * Exit code 0 = every targeted class initialized; 1 = at least one failed.
 */
public final class ForceInit {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ForceInit <jar> [includePrefixesCsv]");
            System.exit(2);
        }
        File jar = new File(args[0]);
        if (!jar.isFile()) {
            System.err.println("not a file: " + jar);
            System.exit(2);
        }
        String[] prefixes = args.length > 1 && !args[1].isEmpty() ? args[1].split(",") : new String[0];

        TreeSet<String> classNames = collectClassNames(jar, prefixes);
        System.out.println("ForceInit: " + classNames.size() + " classes from " + jar.getName()
                + (prefixes.length == 0 ? " (all packages)" : " (prefixes: " + String.join(",", prefixes) + ")")
                + " on JRE " + System.getProperty("java.version"));

        URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()},
                ClassLoader.getSystemClassLoader());

        List<String> moduleFailures = new ArrayList<>();      // real: a JDK module class is absent
        List<String> environmentalFailures = new ArrayList<>(); // noise: native lib / headless / optional bundled code
        int initialized = 0;
        for (String name : classNames) {
            try {
                Class.forName(name, true, loader);
                initialized++;
            } catch (Throwable t) {
                String missing = missingJdkClass(t);
                String line = name + "  ->  " + describe(t);
                if (missing != null) {
                    moduleFailures.add(line + "   [missing JDK class: " + missing + "]");
                } else {
                    environmentalFailures.add(line);
                }
            }
        }

        System.out.println("ForceInit: initialized " + initialized
                + ", module-missing " + moduleFailures.size()
                + ", environmental " + environmentalFailures.size());

        if (!environmentalFailures.isEmpty()) {
            // informational only -- native libraries, headless display, and
            // optional bundled code legitimately fail to init in this harness.
            System.out.println("---- environmental (ignored) ----");
            for (String f : environmentalFailures) {
                System.out.println("  " + f);
            }
        }
        if (!moduleFailures.isEmpty()) {
            System.out.println("---- module-missing (FAILURE: stripped JRE lacks a needed JDK module) ----");
            for (String f : moduleFailures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    /**
     * If this throwable chain is a class-resolution failure for a JDK-owned
     * class (a module dropped from the stripped JRE), return that class name;
     * otherwise null. Distinguishes the real signal (NoClassDefFoundError:
     * jdk/net/Sockets) from environmental noise (UnsatisfiedLinkError,
     * HeadlessException, or a missing *non*-JDK bundled class).
     */
    private static String missingJdkClass(Throwable t) {
        for (Throwable c = t; c != null && c != c.getCause(); c = c.getCause()) {
            if (c instanceof NoClassDefFoundError || c instanceof ClassNotFoundException) {
                String name = c.getMessage();
                if (name == null) {
                    continue;
                }
                name = name.replace('/', '.').trim();
                // strip a wrapping message if present, keep the dotted type
                int sp = name.indexOf(' ');
                if (sp > 0) {
                    name = name.substring(0, sp);
                }
                if (isJdkClass(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private static boolean isJdkClass(String name) {
        if (name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("com.sun.") || name.startsWith("org.w3c.")
                || name.startsWith("org.xml.sax") || name.startsWith("org.ietf.")
                || name.startsWith("org.jcp.")) {
            return true;
        }
        // javax.* is split: some packages are JDK modules (script, xml, crypto,
        // ...), others are third-party APIs (servlet, mail, validation, inject)
        // that are bundled, not JDK-owned. Only treat the JDK-owned ones as
        // "missing module" signals.
        for (String jdkJavax : JDK_JAVAX_PACKAGES) {
            if (name.startsWith(jdkJavax)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] JDK_JAVAX_PACKAGES = {
            "javax.script.", "javax.xml.", "javax.crypto.", "javax.net.",
            "javax.sql.", "javax.naming.", "javax.management.", "javax.security.",
            "javax.swing.", "javax.imageio.", "javax.sound.", "javax.print.",
            "javax.accessibility.", "javax.smartcardio.", "javax.lang.model.",
            "javax.annotation.processing.", "javax.tools.", "javax.transaction.xa."
    };

    private static String describe(Throwable t) {
        Throwable cause = rootCauseOf(t);
        String message = cause.getMessage();
        if (message != null) {
            message = message.split("\\R", 2)[0];   // first line only
        }
        return cause.getClass().getName() + (message != null ? ": " + message : "");
    }

    private static TreeSet<String> collectClassNames(File jar, String[] prefixes) throws Exception {
        TreeSet<String> names = new TreeSet<>();
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String path = entries.nextElement().getName();
                if (!path.endsWith(".class")) {
                    continue;
                }
                // base name of a multi-release entry: strip META-INF/versions/<n>/
                if (path.startsWith("META-INF/versions/")) {
                    int slash = path.indexOf('/', "META-INF/versions/".length());
                    if (slash < 0) {
                        continue;
                    }
                    path = path.substring(slash + 1);
                }
                if (path.startsWith("META-INF/") || path.endsWith("module-info.class")
                        || path.endsWith("package-info.class")) {
                    continue;
                }
                String binary = path.substring(0, path.length() - ".class".length()).replace('/', '.');
                if (matches(binary, prefixes)) {
                    names.add(binary);
                }
            }
        }
        return names;
    }

    private static boolean matches(String binary, String[] prefixes) {
        if (prefixes.length == 0) {
            return true;
        }
        for (String prefix : prefixes) {
            if (binary.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable rootCauseOf(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
