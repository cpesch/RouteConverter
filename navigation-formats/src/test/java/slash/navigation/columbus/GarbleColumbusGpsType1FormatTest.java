package slash.navigation.columbus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GarbleColumbusGpsType1FormatTest {
    private final GarbleColumbusGpsType1Format format = new GarbleColumbusGpsType1Format();

    @Test
    public void testName() {
        assertEquals("Columbus GPS Type 1 Garble (*.csv)", format.getName());
    }

    @Test
    public void testDoesNotSupportWriting() {
        assertFalse(format.isSupportsWriting());
    }

    @Test
    public void testToleratesMuchGarble() {
        assertEquals(1000, format.getGarbleCount());
    }

    @Test
    public void testInheritsType1LineParsing() {
        assertTrue(format.isPosition("2971  ,V,090508,084815,48.132451N,016.321871E,319  ,12  ,207,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX02971"));
    }
}
