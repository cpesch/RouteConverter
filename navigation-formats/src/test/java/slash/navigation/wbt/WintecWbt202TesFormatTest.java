package slash.navigation.wbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Route;

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
	public void testReadRealFile() throws Exception {
		WintecWbt202TesFormat format = new WintecWbt202TesFormat();
		ParserContext<Wgs84Route> context = new ParserContextImpl<>();

		try (InputStream is = getClass().getResource("ZeroSpeedStart.TES").openStream()) {
			format.read(is, context);
		}
		List<Wgs84Route> routes = context.getRoutes();
		assertNotNull("no routes loaded", routes);
		assertEquals("unexpected number of routes", 1, routes.size());
		assertEquals("unexpected number of position", 129, routes.get(0).getPositionCount());
	}
}
