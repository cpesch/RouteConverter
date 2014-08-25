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

import slash.common.io.Files;
import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.Route;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static java.lang.String.format;
import static slash.common.io.Transfer.decodeUri;
import static slash.common.io.Transfer.encodeFileName;

/**
 * Represents a route in the file system.
 *
 * @author Christian Pesch
 */
public class LocalRoute implements Route {
    private final LocalCatalog catalog;
    private File file;
    private String name;

    public LocalRoute(LocalCatalog catalog, File file, String name) {
        this.catalog = catalog;
        this.file = file;
        this.name = decodeUri(name);
    }

    public LocalRoute(LocalCatalog catalog, File file) {
        this(catalog, file, file.getName());
    }

    public String getUrl() {
        try {
            return getDataUrl().toString();
        } catch (IOException e) {
            throw new IllegalStateException(format("cannot create url for %s", file));
        }
    }

    public String getName() throws IOException {
        return name;
    }

    public String getDescription() throws IOException {
        return null;
    }

    public String getCreator() throws IOException {
        // with Java 7 there is an API for the owner of a file
        // FileRef file = Paths.get("/path/to/file.ext");
        // UserPrincipal principal = Attributes.getOwner(file);
        // String username = principal.getName();
        return catalog.getUserName();
    }

    public URL getDataUrl() throws IOException {
        return file.toURI().toURL();
    }

    public void update(Category parent, String description) throws IOException {
        File category = Files.toFile(new URL(parent.getUrl()));
        File newName = new File(category, encodeFileName(description));
        if (!file.renameTo(newName))
            throw new IOException(format("cannot rename %s to %s", file, newName));
        file = newName;
    }

    public void delete() throws IOException {
        if (!file.delete())
            throw new IOException(format("cannot delete %s", file));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalRoute that = (LocalRoute) o;

        return file.equals(that.file);
    }

    public int hashCode() {
        return file.hashCode();
    }

    public String toString() {
        return getClass().getSimpleName() + "[file=" + file + ", name=" + name + "]";
    }
}
