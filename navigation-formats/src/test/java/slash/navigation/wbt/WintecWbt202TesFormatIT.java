package slash.navigation.wbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;

import java.io.File;

import org.junit.Test;

import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.common.NavigationPosition;

public class WintecWbt202TesFormatIT {

	@Test
	public void testReadRealFile() throws Exception {
		File source = new File(SAMPLE_PATH, "ZeroSpeedStart.TES");
		NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
		ParserResult result = parser.read(source);
		assertNotNull(result);
		assertEquals(WintecWbt202TesFormat.class, result.getFormat().getClass());
		assertEquals(1, result.getAllRoutes().size());
		@SuppressWarnings("rawtypes")
		BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
		assertEquals(129, route.getPositionCount());
		NavigationPosition position = route.getPositions().get(route.getPositionCount() - 1);
		assertEquals("Trackpoint 129", position.getDescription());
		assertNotNull(position.getLongitude());
		assertNotNull(position.getLatitude());
	}
}
