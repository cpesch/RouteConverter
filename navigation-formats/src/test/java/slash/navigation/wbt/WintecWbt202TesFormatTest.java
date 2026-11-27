package slash.navigation.wbt;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Route;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
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

	private static int time(int year, int month, int day, int hour, int minute, int second) {
		return (year - 2000) << 26 | month << 22 | day << 17 | hour << 12 | minute << 6 | second;
	}

	// a track of count points one second apart, starting at 48.148 N, 11.4599 E, with the records at bad replaced
	private static byte[] track(int count, int... bad) {
		ByteBuffer buffer = ByteBuffer.allocate(count * 16).order(LITTLE_ENDIAN);
		for (int i = 0; i < count; i++) {
			buffer.putShort((short) (i == 0 ? 1 : 0));
			buffer.putInt(time(2024, 4, 17, 10, i / 60 % 60, i % 60));
			buffer.putInt(481480480 + i * 100);
			buffer.putInt(114599400 + i * 100);
			buffer.putShort((short) 533);
		}
		for (int index : bad)
			// month 15
			buffer.putInt(index * 16 + 2, time(2024, 15, 17, 10, 0, 0));
		return buffer.array();
	}

	private static List<Wgs84Route> read(byte[] data) throws IOException {
		ParserContextImpl<Wgs84Route> context = new ParserContextImpl<>();
		new WintecWbt202TesFormat().read(new ByteArrayInputStream(data), context);
		return context.getRoutes();
	}

	@Test
	public void testIsPlausibleRecord() {
		assertTrue(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 0, 0), 481480480, 114599400));
		assertTrue(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 12, 31, 23, 59, 59), -900000000, -1800000000));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 0, 17, 10, 0, 0), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 13, 17, 10, 0, 0), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 0, 10, 0, 0), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 24, 0, 0), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 60, 0), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 0, 60), 481480480, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 0, 0), 900000001, 114599400));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 0, 0), 481480480, -1800000001));
		assertFalse(WintecWbt202TesFormat.isPlausibleRecord(time(2024, 4, 17, 10, 0, 0), 0, 0));
	}

	@Test
	public void testReadsPlausibleTrack() throws IOException {
		List<Wgs84Route> routes = read(track(200));
		assertEquals(1, routes.size());
		assertEquals(200, routes.get(0).getPositionCount());
	}

	@Test
	public void testSkipsADamagedRecordInAHundred() throws IOException {
		List<Wgs84Route> routes = read(track(200, 100, 150));
		assertEquals(1, routes.size());
		assertEquals(198, routes.get(0).getPositionCount());
		assertTrue(read(track(200, 50, 100, 150)).isEmpty());
	}

	@Test
	public void testDeclinesRandomBytes() throws IOException {
		Random random = new Random(42);
		int claimed = 0;
		for (int i = 0; i < 1000; i++) {
			byte[] data = new byte[16 * (3 + random.nextInt(200))];
			random.nextBytes(data);
			if (!read(data).isEmpty())
				claimed++;
		}
		assertEquals(0, claimed);
	}
}
