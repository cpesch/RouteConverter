package slash.navigation.catalog.domain;

import org.junit.Test;
import slash.common.io.InputOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class FilesIT extends RouteCatalogServiceBase {

    @Test
    public void testAddFileWithUmlauts() throws Exception {
        String name = "Category for files with umlauts " + System.currentTimeMillis();
        Category root = catalog.getRootCategory();
        Category category = root.create(name);

        File in = File.createTempFile("äöüß", ".file");
        FileInputStream fis = new FileInputStream(new File(TEST_PATH + "filestest.gpx"));
        FileOutputStream fos = new FileOutputStream(in);
        InputOutput.copy(fis, fos);
        long inLength = in.length();

        Route route = category.addRoute("File with umlauts", in);
        assertNotNull(route);

        assertTrue(in.delete());

        InputStream out = route.getDataUrl().openStream();
        assertEquals(inLength, out.available());
    }
}
