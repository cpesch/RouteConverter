package slash.navigation.catalog.domain;

import slash.navigation.util.InputOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FilesTest extends BaseRouteServiceTest {

    public void testAddFileWithUmlauts() throws Exception {
        String name = "Category for files with umlauts " + System.currentTimeMillis();
        Category root = adminService.getRootCategory();
        Category category = root.addSubCategory(name);

        File in = File.createTempFile("äöüß", ".file");
        FileInputStream fis = new FileInputStream(new File(TEST_PATH + "filestest.gpx"));
        FileOutputStream fos = new FileOutputStream(in);
        InputOutput output = new InputOutput(fis, fos);
        output.start();
        output.close();
        long inLength = in.length();

        Route route = category.addRoute("File with umlauts", in);
        assertNotNull(route);

        assertTrue(in.delete());

        File out = route.getFile();
        assertEquals(inLength, out.length());
        assertEquals(in.getName(), out.getName());
    }
}
