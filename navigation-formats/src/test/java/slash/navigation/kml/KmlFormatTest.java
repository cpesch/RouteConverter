package slash.navigation.kml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KmlFormatTest {
    private final KmlFormat format = new Kml22Format();

    @Test
    public void testConcatPath() throws Exception {
        assertEquals("a/b/c", format.concatPath("a/b", "c"));
        assertEquals("a/b", format.concatPath("a", "b"));
        assertEquals("a", format.concatPath("a", null));
        assertEquals("b", format.concatPath(null, "b"));
    }
}
