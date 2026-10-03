package slash.navigation.msfs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MSFSFlightPlanFormatTest {
    private final MSFSFlightPlanFormat format = new MSFSFlightPlanFormat();

    @Test
    public void testFormatElevationWritesFeet() {
        assertEquals("+000364.17", format.formatElevation(111.0));
        assertEquals("+036000.00", format.formatElevation(10972.8));
        assertEquals("-001487.12", format.formatElevation(-453.274176));
        assertEquals("+000000.00", format.formatElevation(null));
    }

    @Test
    public void testParseElevationReadsFeet() {
        assertEquals(110.9472, format.parseElevation("+000364.00"), 0.00001);
        assertEquals(10972.8, format.parseElevation("+036000.00"), 0.00001);
        assertEquals(-453.274176, format.parseElevation("-001487.12"), 0.00001);
    }

    @Test
    public void testFlightLevelRoundTrip() {
        String fl360 = "+036000.00";
        assertEquals(fl360, format.formatElevation(format.parseElevation(fl360)));
    }
}
