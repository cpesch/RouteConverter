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

import slash.navigation.rest.exception.DuplicateNameException;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;

import static java.lang.String.format;
import static slash.common.io.Files.toFile;
import static slash.common.io.Transfer.encodeFileName;

/**
 * Represents a route in the file system.
 *
 * @author Christian Pesch
 */
public class LocalRoute implements Route {
    private File file;

    public LocalRoute(File file) {
        this.file = file;
    }

    public String getHref() {
        try {
            return file.toURI().toURL().toExternalForm();
        } catch (IOException e) {
            throw new IllegalStateException(format("Cannot create URL for %s", file));
        }
    }

    public String getName() throws IOException {
        return file.getName();
    }

    public String getDescription() throws IOException {
        if(!file.exists())
            return "broken link: " + file.getName();
        return file.getName();
    }

    public String getCreator() throws IOException {
        if(!file.exists())
            return System.getProperty("user.name");
        Path path = Paths.get(file.getAbsolutePath());
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner = ownerAttributeView.getOwner();
        return owner.getName();
    }

    public String getUrl() throws IOException {
        return getHref();
    }

    public void update(Category parent, String description) throws IOException {
        File category = toFile(new URL(parent.getHref()));
        File newName = new File(category, encodeFileName(description));
        if (newName.exists())
            throw new DuplicateNameException(format("%s %s already exists", newName.isDirectory() ? "Category" : "Route", description), newName.getAbsolutePath());
        if (!file.renameTo(newName))
            throw new IOException(format("Cannot rename %s to %s", file, newName));
        file = newName;
    }

    public void delete() throws IOException {
        if (!file.delete())
            throw new IOException(format("Cannot delete %s", file));
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
        return getClass().getSimpleName() + "[file=" + file + "]";
    }
}
