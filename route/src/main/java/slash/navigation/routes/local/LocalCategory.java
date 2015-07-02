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
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static slash.common.io.Files.removeExtension;
import static slash.common.io.InputOutput.copy;
import static slash.common.io.Transfer.decodeUri;
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
    private String name;

    public LocalCategory(LocalCatalog catalog, File directory, String name) {
        this.catalog = catalog;
        this.directory = directory;
        this.name = decodeUri(name);
    }

    public LocalCategory(LocalCatalog catalog, File directory) {
        this(catalog, directory, directory.getName());
    }

    public String getHref() {
        try {
            return directory.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(format("Cannot create URL for %s", directory));
        }
    }

    public String getName() throws IOException {
        return name;
    }

    public List<Category> getCategories() throws IOException {
        List<Category> categories = new ArrayList<>();
        for (File subDirectory : directory.listFiles(new DirectoryFileFilter())) {
            String name = decodeUri(subDirectory.getName());
            if (isPotentialValidLink(subDirectory)) {
                WindowsShortcut shortcut = new WindowsShortcut(subDirectory);
                if (shortcut.isDirectory()) {
                    name = removeExtension(name);
                    subDirectory = new File(shortcut.getRealFilename());
                } else
                    continue;
            }
            categories.add(new LocalCategory(catalog, subDirectory, name));
        }
        return categories;
    }

    public Category create(String name) throws IOException {
        File subDirectory = new File(directory, encodeFileName(name));
        if (subDirectory.exists())
            throw new DuplicateNameException(format("%s %s already exists", subDirectory.isDirectory() ? "Category" : "Route", name), subDirectory.getAbsolutePath());
        if (!subDirectory.mkdir())
            throw new IOException(format("Cannot create category %s", subDirectory));
        return new LocalCategory(catalog, subDirectory);
    }

    public void update(Category parent, String name) throws IOException {
        File newName;
        try {
            newName = new File(parent != null ? new File(new URL(parent.getHref()).toURI()) : directory.getParentFile(), encodeFileName(name));
        } catch (URISyntaxException e) {
            throw new IOException(format("Cannot rename %s for %s and %s", directory, parent, name));
        }
        if (!directory.renameTo(newName))
            throw new IOException(format("Cannot rename %s to %s", directory, newName));
        directory = newName;
    }

    public void delete() throws IOException {
        recursiveDelete(directory);
    }

    private void recursiveDelete(File file) throws IOException {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                recursiveDelete(f);
            }
        }
        if (!file.delete())
            throw new IOException(format("Cannot delete %s", file));
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<>();
        for (File file : directory.listFiles(new FileFileFilter())) {
            if (isPotentialValidLink(file)) {
                WindowsShortcut shortcut = new WindowsShortcut(file);
                if (shortcut.isFile())
                    file = new File(shortcut.getRealFilename());
                else
                    continue;
            }
            routes.add(new LocalRoute(file));
        }
        return routes;
    }

    public Route createRoute(String description, File localFile) throws IOException {
        File destination = new File(directory, encodeFileName(description));
        copy(new FileInputStream(localFile), new FileOutputStream(destination));
        return new LocalRoute(destination);
    }

    public Route createRoute(String description, String url) throws IOException {
        File destination = new File(directory, encodeFileName(description));
        try (PrintWriter writer = new PrintWriter(destination)) {
            writer.println("[InternetShortcut]");
            writer.println("URL=" + url);
        }
        return new LocalRoute(destination);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalCategory that = (LocalCategory) o;

        return catalog.equals(that.catalog) && directory.equals(that.directory);
    }

    public int hashCode() {
        int result = catalog.hashCode();
        result = 31 * result + directory.hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[directory=" + directory + ", name=" + name + "]";
    }
}
