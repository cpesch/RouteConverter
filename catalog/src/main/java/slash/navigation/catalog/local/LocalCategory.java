package slash.navigation.catalog.local;

import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.Route;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static java.lang.String.format;

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
            throw new IllegalStateException(format("cannot create url for file %s", directory));
        }
    }

    public String getName() throws IOException {
        return directory.getName();
    }

    public String getDescription() throws IOException {
        throw new UnsupportedOperationException();
    }

    public List<Category> getSubCategories() throws IOException {
        throw new UnsupportedOperationException();
    }

    public Category addSubCategory(String name) throws IOException {
        File subDirectory = new File(directory, name);
        if(!subDirectory.mkdir())
            throw new IOException(format("cannot create %s", subDirectory));
        return new LocalCategory(catalog, subDirectory);
    }

    public void updateCategory(Category parent, String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void delete() throws IOException {
        if(!directory.delete())
            throw new IOException(format("cannot delete %s", directory));
    }

    public List<Route> getRoutes() throws IOException {
        throw new UnsupportedOperationException();
    }

    public Route addRoute(String description, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Route addRoute(String description, String fileUrl) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void updateRoute(Route route, Category category, String description) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void deleteRoute(Route route) throws IOException {
        throw new UnsupportedOperationException();
    }
}
