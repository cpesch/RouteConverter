package slash.navigation.catalog.domain;

import org.junit.Test;
import slash.common.io.InputOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FilesIT extends RouteCatalogServiceBase {

    @Test
    public void testAddFileWithUmlauts() throws Exception {
        String name = "Category for files with umlauts " + System.currentTimeMillis();
        Category root = routeCatalog.getRootCategory();
        Category category = root.addSubCategory(name);

        File in = File.createTempFile("äöüß", ".file");
        FileInputStream fis = new FileInputStream(new File(TEST_PATH + "filestest.gpx"));
        FileOutputStream fos = new FileOutputStream(in);
        InputOutput.copy(fis, fos);
        long inLength = in.length();

        Route route = category.addRoute("File with umlauts", in);
        assertNotNull(route);

        assertTrue(in.delete());

        InputStream out = route.getUrl().openStream();
        assertEquals(inLength, out.available());
    }
}
