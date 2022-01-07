package slash.navigation.gopal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.gopal.GoPal3RouteFormat.createGoPalFileName;

public class Gopal3RouteFormatTest {
    @Test
    public void testCreateGoPalFileName() {
        assertEquals("EIFELSTERN AACHEN", createGoPalFileName("Eifelstern-Aachen"));
        assertEquals("EIFELSTERN.XML", createGoPalFileName("Eifelstern.xml"));
    }
}
