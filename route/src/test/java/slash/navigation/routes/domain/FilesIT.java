package slash.navigation.routes.domain;

import org.junit.Test;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import static java.io.File.createTempFile;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.io.InputOutput.copy;

public class FilesIT extends RouteCatalogServiceBase {

    @Test
    public void testAddFileWithUmlauts() throws Exception {
        String name = "Category for File with Umlauts " + UMLAUTS + " " + currentTimeMillis();
        Category root = catalog.getRootCategory();
        Category category = root.create(name);

        File in = createTempFile("File with Umlauts " + UMLAUTS, ".file");

        FileInputStream input = new FileInputStream(new File(TEST_PATH + "filestest.gpx"));
        FileOutputStream output = new FileOutputStream(in);
        copy(input, output);
        long inLength = in.length();

        Route route = category.createRoute("File with Umlauts " + UMLAUTS + " " + currentTimeMillis(), in);
        assertNotNull(route);

        assertTrue(in.delete());

        InputStream out = route.getDataUrl().openStream();
        assertEquals(inLength, out.available());
    }
}
