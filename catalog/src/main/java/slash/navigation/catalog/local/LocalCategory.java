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

package slash.navigation.catalog.local;

import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.Route;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static slash.common.io.InputOutput.copy;

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

    public String getUrl() {
        try {
            return directory.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(format("cannot create url for %s", directory));
        }
    }

    public String getName() throws IOException {
        return directory.getName();
    }

    public String getDescription() throws IOException {
        return getName();
    }

    public List<Category> getCategories() throws IOException {
        List<Category> categories = new ArrayList<Category>();
        for (File subDirectory : directory.listFiles(new DirectoryFileFilter())) {
            categories.add(new LocalCategory(catalog, subDirectory));
        }
        return categories;
    }

    public Category create(String name) throws IOException {
        File subDirectory = new File(directory, name);
        if (!subDirectory.mkdir())
            throw new IOException(format("cannot create %s", subDirectory));
        return new LocalCategory(catalog, subDirectory);
    }

    public void update(Category parent, String name) throws IOException {
        File newName = null;
        try {
            newName = new File(parent != null ? new File(new URL(parent.getUrl()).toURI()) : directory.getParentFile(), name);
        } catch (URISyntaxException e) {
            throw new IOException(format("cannot rename %s to %s", directory, newName));
        }
        if (!directory.renameTo(newName))
            throw new IOException(format("cannot rename %s to %s", directory, newName));
        directory = newName;
    }

    public void delete() throws IOException {
        if (!directory.delete())
            throw new IOException(format("cannot delete %s", directory));
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<Route>();
        assert directory != null;
        for (File file : directory.listFiles(new FileFileFilter())) {
            routes.add(new LocalRoute(catalog, file));
        }
        return routes;
    }

    public Route createRoute(String description, File file) throws IOException {
        File destination = new File(directory, description);
        copy(new FileInputStream(file), new FileOutputStream(destination));
        return new LocalRoute(catalog, destination);
    }

    public Route createRoute(String description, String fileUrl) throws IOException {
        File destination = new File(directory, description);
        PrintWriter writer = new PrintWriter(destination);
        try {
            writer.println("[InternetShortcut]");
            writer.println("URL=" + fileUrl);
        } finally {
            writer.close();
        }
        return new LocalRoute(catalog, destination);
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
        return super.toString() + "[directory=" + directory + "]";
    }
}
