package slash.navigation.msfs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MSFSFlightPlanFormatTest {
    private final MSFSFlightPlanFormat format = new MSFSFlightPlanFormat();

    @Test
    public void testFormatElevation() {
        assertEquals("+001487.00", format.formatElevation(1487.0));
        assertEquals("-001487.12", format.formatElevation(-1487.123));
    }

    @Test
    public void testParseElevation() {
        assertEquals(1487.0, format.parseElevation("+001487.00"), 0.0);
        assertEquals(-1487.12, format.parseElevation("-001487.12"), 0.0);
    }
}
