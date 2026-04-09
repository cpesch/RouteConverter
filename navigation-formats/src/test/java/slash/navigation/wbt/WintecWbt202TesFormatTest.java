package slash.navigation.wbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;

public class WintecWbt202TesFormatTest {
	private final NavigationFormatRegistry registry = new AllNavigationFormatRegistry();

	@Test
	public void testBasicValues() {
		WintecWbt202TesFormat format = new WintecWbt202TesFormat();

		assertTrue("should support reading", format.isSupportsReading());
		assertFalse("should not support writing", format.isSupportsWriting());
		assertTrue("should support multiple routes", format.isSupportsMultipleRoutes());
		assertEquals("unexpected extension", ".tes", format.getExtension());
		assertEquals("unexpected name", "Wintec WBT-202 (*.tes)", format.getName());
		assertEquals("unexpected maximum filename length", 64, format.getMaximumFileNameLength());
		assertEquals("unexpected maximum position count", Integer.MAX_VALUE, format.getMaximumPositionCount());
		assertEquals("unexpected maximum route name length", 64, format.getMaximumRouteNameLength());
	}

	@Test
	public void testGetReadFormatsByExtension() {
		@SuppressWarnings("rawtypes")
		List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".tes");
		assertNotNull("returned formats is null", formats);
		assertFalse("formats are empty", formats.isEmpty());
		assertEquals(WintecWbt202TesFormat.class, formats.get(0).getClass());
	}

	@Test
	public void testIsValidData() {
		WintecWbt202TesFormat format = new WintecWbt202TesFormat();
		BaseNavigationPosition wp1 = format.createWaypoint(1713309531, 481480480, 114599400, (short) 533, 1, false);
		BaseNavigationPosition wp2 = format.createWaypoint(1713309532, 481480480, 114599400, (short) 533, 1, false);
		BaseNavigationPosition wp3 = format.createWaypoint(1713309678, 481480608, 114600512, (short) 533, 1, false);
		BaseNavigationPosition wp4 = format.createWaypoint(1713309678, 881480608, 114600512, (short) 533, 1, false);

		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, -1, 114599400, (short) 533, 1, false), null));
		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, 0, 114599400, (short) 533, 1, false), null));
		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, 100, 114599400, (short) 533, 1, false), null));
		assertTrue("expected to be valid",
				format.isValidData(format.createWaypoint(1713309531, 101, 114599400, (short) 533, 1, false), null));

		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, 481480480, -1, (short) 533, 1, false), null));
		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, 481480480, 0, (short) 533, 1, false), null));
		assertFalse("expected to be invalid",
				format.isValidData(format.createWaypoint(1713309531, 481480480, 100, (short) 533, 1, false), null));
		assertTrue("expected to be valid",
				format.isValidData(format.createWaypoint(1713309531, 481480480, 101, (short) 533, 1, false), null));

		assertFalse("expected to be invalid", format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 15001, 1, false), null));
		assertFalse("expected to be invalid", format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 15000, 1, false), null));
		assertTrue("expected to be valid", format
				.isValidData(format.createWaypoint(1713309531, 481480480, 114599400, (short) 14999, 1, false), null));

		assertTrue("expected to be valid", format.isValidData(wp1, null));
		assertFalse("expected to be invalid", format.isValidData(wp1, wp1));
		assertTrue("expected to be valid", format.isValidData(wp2, wp1));
		assertFalse("expected to be invalid", format.isValidData(wp1, wp2));

		assertTrue("expected to be valid", format.isValidData(wp3, wp1));
		assertFalse("expected to be invalid", format.isValidData(wp4, wp1));
	}
}
