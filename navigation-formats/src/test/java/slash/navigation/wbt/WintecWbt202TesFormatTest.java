package slash.navigation.wbt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;

public class WintecWbt202TesFormatTest {
	private final NavigationFormatRegistry registry = new AllNavigationFormatRegistry();

	@Test
	public void testBasicValues() {
		WintecWbt202TesFormat format = new WintecWbt202TesFormat();

		assertTrue(format.isSupportsReading(), "should support reading");
		assertFalse(format.isSupportsWriting(), "should not support writing");
		assertTrue(format.isSupportsMultipleRoutes(), "should support multiple routes");
		assertEquals(".tes", format.getExtension(), "unexpected extension");
		assertEquals("Wintec WBT-202 (*.tes)", format.getName(), "unexpected name");
		assertEquals(255, format.getMaximumFileNameLength(), "unexpected maximum filename length");
		assertEquals(Integer.MAX_VALUE, format.getMaximumPositionCount(), "unexpected maximum position count");
		assertEquals(64, format.getMaximumRouteNameLength(), "unexpected maximum route name length");
	}

	@Test
	public void testGetReadFormatsByExtension() {
		@SuppressWarnings("rawtypes")
		List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".tes");
		assertNotNull(formats, "returned formats is null");
		assertFalse(formats.isEmpty(), "formats are empty");
		assertEquals(WintecWbt202TesFormat.class, formats.get(0).getClass());
	}

	@Test
	public void testIsValidData() {
		WintecWbt202TesFormat format = new WintecWbt202TesFormat();
		BaseNavigationPosition wp1 = format.createWaypoint(1713309531, 481480480, 114599400, (short) 533, 1, false);
		BaseNavigationPosition wp2 = format.createWaypoint(1713309532, 481480480, 114599400, (short) 533, 1, false);
		BaseNavigationPosition wp3 = format.createWaypoint(1713309678, 481480608, 114600512, (short) 533, 1, false);
		BaseNavigationPosition wp4 = format.createWaypoint(1713309678, 881480608, 114600512, (short) 533, 1, false);

		assertFalse(format.isValidData(format.createWaypoint(1713309531, -1, 114599400, (short) 533, 1, false), null),
				"expected to be invalid");
		assertFalse(format.isValidData(format.createWaypoint(1713309531, 0, 114599400, (short) 533, 1, false), null),
				"expected to be invalid");
		assertFalse(format.isValidData(format.createWaypoint(1713309531, 100, 114599400, (short) 533, 1, false), null),
				"expected to be invalid");
		assertTrue(format.isValidData(format.createWaypoint(1713309531, 101, 114599400, (short) 533, 1, false), null),
				"expected to be valid");

		assertFalse(format.isValidData(format.createWaypoint(1713309531, 481480480, -1, (short) 533, 1, false), null),
				"expected to be invalid");
		assertFalse(format.isValidData(format.createWaypoint(1713309531, 481480480, 0, (short) 533, 1, false), null),
				"expected to be invalid");
		assertFalse(format.isValidData(format.createWaypoint(1713309531, 481480480, 100, (short) 533, 1, false), null),
				"expected to be invalid");
		assertTrue(format.isValidData(format.createWaypoint(1713309531, 481480480, 101, (short) 533, 1, false), null),
				"expected to be valid");

		assertFalse(format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 15001, 1, false), null), "expected to be invalid");
		assertFalse(format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 15000, 1, false), null), "expected to be invalid");
		assertTrue(format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 14999, 1, false), null), "expected to be valid");

		assertTrue(format.isValidData(wp1, null), "expected to be valid");
		assertFalse(format.isValidData(wp1, wp1), "expected to be invalid");
		assertTrue(format.isValidData(wp2, wp1), "expected to be valid");
		assertFalse(format.isValidData(wp1, wp2), "expected to be invalid");

		assertTrue(format.isValidData(wp3, wp1), "expected to be valid");
		assertFalse(format.isValidData(wp4, wp1), "expected to be invalid");
	}
}
