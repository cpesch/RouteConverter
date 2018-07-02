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

package slash.navigation.routes.local;

import slash.common.io.FileFileFilter;
import slash.common.io.WindowsShortcut;
import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.rest.exception.ForbiddenException;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.copyLarge;
import static slash.common.io.Files.recursiveDelete;
import static slash.common.io.Files.removeExtension;
import static slash.common.io.InputOutput.DEFAULT_BUFFER_SIZE;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.encodeFileName;
import static slash.common.io.WindowsShortcut.isPotentialValidLink;

/**
 * Represents a category in the file system.
 *
 * @author Christian Pesch
 */

public class LocalCategory implements Category {
    private final LocalCatalog catalog;
    private File directory;

    public LocalCategory(LocalCatalog catalog, File directory) {
        this.catalog = catalog;
        this.directory = directory;
    }

    public String getHref() {
        try {
            return directory.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(format("Cannot create URL for %s", directory));
        }
    }

    public String getName() {
        return directory.getName();
    }

    public List<Category> getCategories() throws IOException {
        List<Category> categories = new ArrayList<>();
        File[] directories = directory.listFiles(new DirectoryFileFilter());
        if(directories != null) {
            for (File subDirectory : directories) {
                if (isPotentialValidLink(subDirectory)) {
                    WindowsShortcut shortcut = new WindowsShortcut(subDirectory);
                    if (shortcut.isDirectory()) {
                        subDirectory = new File(removeExtension(shortcut.getRealFilename()));
                    } else
                        continue;
                }
                categories.add(new LocalCategory(catalog, subDirectory));
            }
        }
        return categories;
    }

    public Category create(String name) throws IOException {
        if (name.contains("/") || name.contains(separator))
            throw new ForbiddenException(format("Cannot have slashes in name %s", name), getHref());
        File subDirectory = new File(directory, encodeFileName(name));
        if (subDirectory.exists())
            throw new DuplicateNameException(format("%s %s already exists", subDirectory.isDirectory() ? "Category" : "Route", name), subDirectory.getAbsolutePath());
        if (!subDirectory.mkdir())
            throw new IOException(format("Cannot create category %s", subDirectory));
        return new LocalCategory(catalog, subDirectory);
    }

    public void update(Category parent, String name) throws IOException {
        File newParent;
        String newName = encodeFileName(name);
        try {
            newParent = parent != null ? new File(new URL(parent.getHref()).toURI()) : directory.getParentFile();
        } catch (URISyntaxException e) {
            throw new IOException(format("Cannot rename %s for %s and %s", directory, parent, name));
        }

        File newDirectory = new File(newParent, newName);
        if (!directory.renameTo(newDirectory))
            throw new IOException(format("Cannot rename %s to %s", directory, newDirectory));

        this.directory = newDirectory;
    }

    public void delete() throws IOException {
        recursiveDelete(directory);
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<>();
        File[] files = directory.listFiles(new FileFileFilter());
        if(files != null) {
            for (File file : files) {
                if (isPotentialValidLink(file)) {
                    WindowsShortcut shortcut = new WindowsShortcut(file);
                    if (shortcut.isFile())
                        file = new File(shortcut.getRealFilename());
                    else
                        continue;
                }
                routes.add(new LocalRoute(file));
            }
        }
        return routes;
    }

    public Route createRoute(String description, File localFile) throws IOException {
        File destination = new File(directory, encodeFileName(description));
        try (InputStream inputStream = new FileInputStream(localFile); OutputStream outputStream = new FileOutputStream(destination)) {
            copyLarge(inputStream, outputStream, new byte[DEFAULT_BUFFER_SIZE]);
        }
        return new LocalRoute(destination);
    }

    public Route createRoute(String description, String url) throws IOException {
        File destination = new File(directory, encodeFileName(description));
        try (PrintWriter writer = new PrintWriter(destination, UTF8_ENCODING)) {
            writer.println("[InternetShortcut]");
            writer.println("URL=" + url);
        }
        return new LocalRoute(destination);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCategory that = (LocalCategory) o;
        return Objects.equals(catalog, that.catalog) &&
                Objects.equals(directory, that.directory);
    }

    public int hashCode() {
        return Objects.hash(catalog, directory);
    }

    public String toString() {
        return getClass().getSimpleName() + "[directory=" + directory + "]";
    }
}
