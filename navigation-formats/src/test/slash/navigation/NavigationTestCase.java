/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation;

import junit.framework.AssertionFailedError;
import slash.navigation.babel.*;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.KmlFormat;
import slash.navigation.kml.KmlRoute;
import slash.navigation.kml.KmzFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Route66Format;
import slash.navigation.tour.TourFormat;
import slash.navigation.util.Conversion;
import slash.navigation.util.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

public abstract class NavigationTestCase extends TestCase {
    protected static final Logger log = Logger.getLogger(NavigationTestCase.class.getName());

    public static final String ROUTE_PATH = "..\\routes\\src\\";
    public static final String TEST_PATH = ROUTE_PATH + "test\\";
    public static final String SAMPLE_PATH = ROUTE_PATH + "samples\\";

    public static void assertDescriptionEquals(List<String> expected, List<String> was) {
        List<String> wasFiltered = new ArrayList<String>();
        if (was != null)
            for (String w : was) {
                if (!w.equals(BaseNavigationFormat.GENERATED_BY))
                    wasFiltered.add(w);
            }
        if (wasFiltered.size() == 0)
            wasFiltered = null;
        assertEquals(expected, wasFiltered);
    }

    protected void assertException(Class exceptionClass, ThrowsException runner) {
        try {
            runner.run();
            fail("Worked?");
        } catch (Throwable throwable) {
            assertTrue("Wrong exception: " + throwable.getClass().getName(), exceptionClass.isInstance(throwable));
        }
    }

    protected void assertTestFails(ThrowsException runner) {
      assertException(AssertionFailedError.class, runner);
    }

    protected interface ThrowsException {
        void run() throws Exception;
    }

    static boolean isReallyUnprecise(NavigationFormat format) {
        return format instanceof BaseNmeaFormat || format instanceof TomTomPoiFormat;
    }

    static boolean isSlightlyUnprecise(NavigationFormat format) {
        return isReallyUnprecise(format) ||
                format instanceof GarminPoiFormat || format instanceof GarminMapSource6Format ||
                format instanceof GeoCachingFormat || format instanceof GarminMapSource5Format ||
                format instanceof GarminPcx5Format;
    }

    static boolean mayNotTransformBidirectionally(NavigationFormat first, NavigationFormat second) {
        return (isSlightlyUnprecise(first) || isSlightlyUnprecise(second)) ||
                ((first instanceof GpxFormat) &&
                        (second instanceof AlanTrackLogFormat || second instanceof AlanWaypointsAndRoutesFormat)) ||
                ((first instanceof MicrosoftAutoRouteFormat) &&
                        (second instanceof GarminPcx5Format)) ||
                ((first instanceof KmlFormat) &&
                        (second instanceof BcrFormat)) ||
                ((first instanceof MagicMapsIktFormat) &&
                        (second instanceof CoPilotFormat));
    }

    static boolean isSlightlyUnprecise(NavigationFormat first, NavigationFormat second) {
        return mayNotTransformBidirectionally(first, second) || mayNotTransformBidirectionally(second, first);
    }

    static boolean isBidirectional(NavigationFormat sourceFormat,
                                   NavigationFormat targetFormat,
                                   BaseNavigationPosition sourcePosition,
                                   BaseNavigationPosition targetPosition) {
        if (sourceFormat.getClass().equals(targetFormat.getClass()))
            return !(isSlightlyUnprecise(sourceFormat) || isSlightlyUnprecise(targetFormat));
        return sourcePosition.getClass().equals(targetPosition.getClass()) &&
                !isSlightlyUnprecise(sourceFormat, targetFormat);
    }

    private static String getKmlRouteName(BaseRoute route) {
        String name = route.getName();
        if(name.startsWith("/"))
            name = name.substring(1);
        int index = name.indexOf('/');
        if (index != -1)
            return name.substring(0, index);
        else
            return name;
    }

    public static void compareRouteMetaData(BaseRoute sourceRoute, BaseRoute targetRoute) {
        if (targetRoute instanceof KmlRoute && targetRoute.getCharacteristics().equals(RouteCharacteristics.Waypoints)) {
            String sourceName = getKmlRouteName(sourceRoute);
            String targetName = getKmlRouteName(targetRoute);
            assertEquals(sourceName, targetName);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                sourceRoute.getName().contains(" to ") && sourceRoute.getName().contains("/;") &&
                targetRoute.getName().endsWith("/")) {
            // if AlanWaypointsAndRoutesFormat is converted to AlanWaypointsAndRoutesFormat "EARTH_RADIUS/; Orte to B/; Orte" becomes "EARTH_RADIUS/"
            String sourcePrefix = getKmlRouteName(sourceRoute);
            String targetPrefix = getKmlRouteName(targetRoute);
            assertEquals(sourcePrefix, targetPrefix);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                !targetRoute.getName().contains(" to ") && !targetRoute.getName().contains("Route: ") &&
                !targetRoute.getName().contains("Track: ") && !targetRoute.getName().equals("MapLage"))
            // Test only if this is not the multiple routes per file case & the route has not been named by us
            assertEquals(sourceRoute.getName(), targetRoute.getName());

        // Test only if this is not the multiple routes per file case
        if (sourceRoute.getDescription() != null && targetRoute.getDescription() != null &&
                !(targetRoute.getFormat() instanceof MagicMapsIktFormat))
            assertDescriptionEquals(sourceRoute.getDescription(), targetRoute.getDescription());
    }

    private static void comparePosition(NavigationFormat sourceFormat,
                                        NavigationFormat targetFormat,
                                        int index,
                                        BaseNavigationPosition sourcePosition,
                                        BaseNavigationPosition targetPosition,
                                        boolean numberPositionNames,
                                        boolean commentPositionNames,
                                        RouteCharacteristics targetCharacteristics) {
        assertNotNull("Source longitude " + index + " does not exist", sourcePosition.getLongitude());
        assertNotNull("Source latitude " + index + " does not exist", sourcePosition.getLatitude());
        assertNotNull("Target longitude " + index + " does not exist", targetPosition.getLongitude());
        assertNotNull("Target latitude " + index + " does not exist", targetPosition.getLatitude());

        compareLongitudeAndLatitude(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareElevation(sourceFormat, targetFormat, sourcePosition, targetPosition, targetCharacteristics);
        compareTime(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareComment(sourceFormat, targetFormat, index, sourcePosition, targetPosition, numberPositionNames, commentPositionNames, targetCharacteristics);
    }

    private static void compareLongitudeAndLatitude(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        if (isBidirectional(sourceFormat, targetFormat, sourcePosition, targetPosition)) {
            assertEquals("Longitude " + index + " does not match", sourcePosition.getLongitude(), targetPosition.getLongitude());
            assertEquals("Latitude " + index + " does not match", sourcePosition.getLatitude(), targetPosition.getLatitude());
        } else if (isReallyUnprecise(sourceFormat) || isReallyUnprecise(targetFormat)) {
            // skip silly from.ov2 in tt poi coordinate
            if (targetPosition.getLongitude() != 10.032) {
                assertNearBy(sourcePosition.getLongitude(), targetPosition.getLongitude(), 0.0005);
                assertNearBy(sourcePosition.getLatitude(), targetPosition.getLatitude(), 0.0005);
            }
        } else {
            assertNearBy(sourcePosition.getLongitude(), targetPosition.getLongitude());
            assertNearBy(sourcePosition.getLatitude(), targetPosition.getLatitude());
        }
    }

    private static void compareElevation(NavigationFormat sourceFormat, NavigationFormat targetFormat, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, RouteCharacteristics targetCharacteristics) {
        if (sourcePosition.getElevation() != null && targetPosition.getElevation() != null) {
            if (targetFormat instanceof AlanWaypointsAndRoutesFormat ||
                    targetFormat instanceof TomTomPoiFormat ||
                    (targetFormat instanceof MagellanMapSendFormat && targetCharacteristics.equals(RouteCharacteristics.Route)))
                assertEquals(0.0, targetPosition.getElevation());
            else if (targetFormat instanceof BcrFormat) {
                // skip silly from20.ovl in bcr coordinate
                if (targetPosition.getElevation() != -0.09)
                    assertNearBy(sourcePosition.getElevation(), targetPosition.getElevation(), 0.1);
            } else if (targetFormat instanceof GarminPcx5Format) {
                assertEquals(new Double(Math.round(sourcePosition.getElevation())), targetPosition.getElevation());
            } else if (targetFormat instanceof OziExplorerReadFormat) {
                assertNearBy(sourcePosition.getElevation(), targetPosition.getElevation(), 0.1);
            } else
                assertEquals(sourcePosition.getElevation(), targetPosition.getElevation());

        } else if (sourceFormat instanceof OziExplorerReadFormat) {
            assertNull(targetPosition.getElevation());
        } else if (sourceFormat instanceof CoPilotFormat || sourceFormat instanceof TourFormat)
            assertNull(sourcePosition.getElevation());
        else if (targetFormat instanceof CoPilotFormat || targetFormat instanceof GoPalRouteFormat ||
                targetFormat instanceof GoPalTrackFormat ||
                targetFormat instanceof NavigatingPoiWarnerFormat || targetFormat instanceof NmnFormat ||
                targetFormat instanceof Route66Format || targetFormat instanceof TourFormat)
            assertNull(targetPosition.getElevation());
        else if (sourcePosition.getElevation() == null &&
                (targetFormat instanceof GpxFormat || targetFormat instanceof KmlFormat ||
                        targetFormat instanceof KmzFormat || targetFormat instanceof Nmn5Format))
            assertEquals(0.0, targetPosition.getElevation());
        else if (!(targetPosition instanceof TomTomPosition))
            assertEquals(sourcePosition.getElevation(), targetPosition.getElevation());
    }

    private static String getGarminMapSource6PositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment.startsWith("STATION")) {
            int index = comment.indexOf(';');
            if (index != -1)
                return comment.substring(index);
        }
        return comment;
    }

    private static String getMicrosoftAutoroutePositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment.endsWith(".1")) {
            return comment.substring(0, comment.length() - 2);
        }
        return comment;
    }

    private static void compareComment(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, boolean numberPositionNames, boolean commentPositionNames, RouteCharacteristics targetCharacteristics) {
        // special case numbered positions
        if (numberPositionNames) {
            String number = Integer.toString(index + 1);
            String comment = sourcePosition.getComment();
            String sourceComment = number + comment;
            assertEquals(sourceComment, targetPosition.getComment());

        } else {
            // Test only if a position has not been commented by us
            if (!(sourcePosition.getComment() == null && targetPosition.getComment().startsWith("Position"))) {
                if (targetFormat instanceof AlanTrackLogFormat ||
                        (targetFormat instanceof GarminMapSource6Format && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                        targetFormat instanceof GoPalTrackFormat || targetFormat instanceof GpsTunerFormat ||
                        targetFormat instanceof HaicomLoggerFormat || targetFormat instanceof MagicMapsIktFormat ||
                        targetFormat instanceof MagicMapsPthFormat || targetFormat instanceof OvlFormat ||
                        (targetFormat instanceof OziExplorerReadFormat && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                        (targetFormat instanceof GarminMapSource5Format && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                        ((targetFormat instanceof KmlFormat || targetFormat instanceof KmzFormat) && !targetCharacteristics.equals(RouteCharacteristics.Waypoints) && !commentPositionNames))
                    assertTrue("Comment " + index + " does not match", targetPosition.getComment().startsWith("Position"));
                else if (targetFormat instanceof AlanWaypointsAndRoutesFormat)
                    assertEquals("Comment " + index + " does not match", trim(sourcePosition.getComment(), 12), trim(targetPosition.getComment(), 12));
                else if (sourceFormat instanceof BcrFormat && targetFormat instanceof TomTomRouteFormat) {
                    BcrPosition bcrPosition = (BcrPosition) sourcePosition;
                    assertEquals("Comment " + index + " does not match", escapeBcr(bcrPosition.getCity() + (bcrPosition.getStreet() != null ? "," + bcrPosition.getStreet() : "")), targetPosition.getComment());
                } else if (targetFormat instanceof GarminPoiFormat)
                    assertEquals("Comment " + index + " does not match", trim(sourcePosition.getComment(), 45), trim(targetPosition.getComment(), 45));
                else if (targetFormat instanceof GarminPoiDbFormat ||
                        (targetFormat instanceof OziExplorerReadFormat && targetCharacteristics.equals(RouteCharacteristics.Waypoints)))
                    assertEquals("Comment " + index + " does not match", garminUmlauts(trim(sourcePosition.getComment().replace(",", ""), 50)), trim(targetPosition.getComment(), 50));
                else if (targetFormat instanceof TomTomRouteFormat)
                    assertEquals("Comment " + index + " does not match", sourcePosition.getComment().replaceAll("\\|", ";"), targetPosition.getComment());
                else if (targetFormat instanceof MagellanExploristFormat || targetFormat instanceof NmeaFormat)
                    assertEquals("Comment " + index + " does not match", sourcePosition.getComment().replaceAll(",", ";"), targetPosition.getComment());
                else if (targetFormat instanceof Nmn4Format || targetFormat instanceof Nmn5Format)
                    assertEquals("Comment " + index + " does not match", escapeNmn4and5(sourcePosition.getComment()), targetPosition.getComment());
                else if (targetFormat instanceof Nmn6Format)
                    assertEquals("Comment " + index + " does not match", escapeNmn6(sourcePosition.getComment()), targetPosition.getComment());
                else if (targetFormat instanceof Nmn6FavoritesFormat)
                    assertEquals("Comment " + index + " does not match", escapeNmn6Favorites(sourcePosition.getComment()), targetPosition.getComment());
                else if (targetFormat instanceof Nmn7Format)
                    assertEquals("Comment " + index + " does not match", trimSpaces(sourcePosition.getComment()), trimSpaces(targetPosition.getComment()));
                else if (targetFormat instanceof MagellanMapSendFormat)
                    assertEquals("Comment " + index + " does not match", trim(sourcePosition.getComment(), 30), trim(targetPosition.getComment(), 30));
                else if (targetFormat instanceof GarminPcx5Format)
                    assertEquals("Comment " + index + " does not match", garminUmlauts(trim(sourcePosition.getComment(), 39)), trim(targetPosition.getComment(), 39));
                else if (targetFormat instanceof Route66Format)
                    assertEquals("Comment " + index + " does not match", Conversion.toMixedCase(sourcePosition.getComment()), targetPosition.getComment());
                else if (sourceFormat instanceof GarminMapSource5Format || sourceFormat instanceof GarminMapSource6Format) {
                    String sourceName = getGarminMapSource6PositionComment(sourcePosition);
                    String targetName = getGarminMapSource6PositionComment(targetPosition);
                    assertEquals(sourceName, targetName);
                } else if (sourceFormat instanceof MicrosoftAutoRouteFormat) {
                    String sourceName = getMicrosoftAutoroutePositionComment(sourcePosition);
                    String targetName = getMicrosoftAutoroutePositionComment(targetPosition);
                    assertEquals(sourceName, targetName);
                } else
                    assertEquals("Comment " + index + " does not match", sourcePosition.getComment(), targetPosition.getComment());
            }
        }
    }

    private static void compareTime(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        if (sourcePosition.getTime() != null && targetPosition.getTime() != null) {
            if (sourceFormat instanceof GoPalTrackFormat || targetFormat instanceof GoPalTrackFormat) {
                String sourceTime = DateFormat.getTimeInstance().format(sourcePosition.getTime().getTime());
                String targetTime = DateFormat.getTimeInstance().format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
            } else {
                String sourceTime = DateFormat.getDateTimeInstance().format(sourcePosition.getTime().getTime());
                String targetTime = DateFormat.getDateTimeInstance().format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
                if (!sourcePosition.getTime().equals(targetPosition.getTime()))
                    log.warning("Time " + index + " does not match");
                // too many reasons for an invalid time :-(
                // assertEquals("Time " + index + " does not match", sourcePosition.getTime(), targetPosition.getTime());
            }
        } else if (targetFormat instanceof AlanTrackLogFormat || targetFormat instanceof BcrFormat ||
                targetFormat instanceof TomTomRouteFormat || targetFormat instanceof NmnFormat ||
                targetFormat instanceof GarminMapSource5Format || targetFormat instanceof GarminPcx5Format ||
                targetFormat instanceof GlopusFormat || targetFormat instanceof TourFormat) {
            assertNull(targetPosition.getTime());
        } else if (targetFormat instanceof GoPalTrackFormat) {
            assertNull(sourcePosition.getTime());
            assertNotNull(targetPosition.getTime());
        } else if (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof OziExplorerReadFormat) {
            assertNotNull(sourcePosition.getTime());
            assertNull(targetPosition.getTime());
        } else
            assertEquals(sourcePosition.getTime(), targetPosition.getTime());
    }

    private static String trim(String str, int maximum) {
        return str != null && str.length() > maximum ? str.substring(0, maximum).trim() : str;
    }

    private static String garminUmlauts(String str) {
        return str.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "$").
                replace("Ö", "O");
    }

    private static String escapeBcr(String str) {
        return str != null ? str.replace("|", ";") : null;
    }

    private static String escapeNmn4and5(String str) {
        if (str != null && str.length() > 2 && str.toUpperCase().equals(str))
            str = Conversion.toMixedCase(str);
        return str != null ? str.replaceAll("\\|", ";") : null;
    }

    private static String escapeNmn6(String str) {
        return str != null ? str.replaceAll("[\\[|\\||\\]]", ";") : null;
    }

    private static String escapeNmn6Favorites(String str) {
        return str != null ? Conversion.toMixedCase(str.replaceAll("[\\[|\\||\\]]", "").replaceAll("ß", "ss")) : null;
    }

    private static String trimSpaces(String str) {
        return str != null ? str.replaceAll(" ", "") : null;
    }

    public static void comparePositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute, NavigationFormat sourceFormat,
                                        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute, NavigationFormat targetFormat, boolean commentPositionNames) {
        if (targetFormat instanceof GarminPoiDbFormat) {
            int targetPositionCount = targetRoute.getPositionCount() / 3;
            assertEquals(sourceRoute.getPositionCount(), targetPositionCount);
            comparePositions(sourceRoute.getPositions(), sourceFormat, targetRoute.getPositions().subList(0, targetPositionCount), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
        } else if (targetFormat instanceof TomTomPoiFormat) {
            assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, 1), sourceFormat, targetRoute.getPositions().subList(0, 1), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
            comparePositions(sourceRoute.getPositions().subList(sourceRoute.getPositionCount() - 1, sourceRoute.getPositionCount()), sourceFormat, targetRoute.getPositions().subList(1, 2), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
            // TomTomPoiFormat has no order of the positions except for first and second
            // comparePositions(sourceRoute.getPositions().subList(1, sourceRoute.getPositionCount() - 1), sourceFormat, targetRoute.getPositions().subList(2, targetRoute.getPositionCount() - 2), targetFormat, false, false, targetRoute.getCharacteristics());
        } else if (sourceFormat instanceof GarminPoiDbFormat) {
            int sourcePositionCount = sourceRoute.getPositionCount() / 3;
            assertEquals(sourcePositionCount, targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, sourcePositionCount), sourceFormat, targetRoute.getPositions(), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
        } else if (sourceFormat instanceof MicrosoftAutoRouteFormat &&
                (targetFormat instanceof GarminMapSource5Format || targetFormat instanceof GarminMapSource6Format || targetFormat instanceof KmlFormat) && 
                targetRoute.getCharacteristics().equals(RouteCharacteristics.Waypoints)) {
            int sourcePositionCount = sourceRoute.getPositionCount() - 1;
            assertEquals(sourcePositionCount, targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, sourcePositionCount), sourceFormat, targetRoute.getPositions(), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
        } else {
            assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions(), sourceFormat, targetRoute.getPositions(), targetFormat, false, commentPositionNames, false, targetRoute.getCharacteristics());
        }
    }

    public static void comparePositions(List<BaseNavigationPosition> sourcePositions,
                                        NavigationFormat sourceFormat,
                                        List<BaseNavigationPosition> targetPositions,
                                        NavigationFormat targetFormat,
                                        boolean numberPositionNames,
                                        boolean commentPositionNames,
                                        boolean compareByEquals,
                                        RouteCharacteristics targetCharacteristics) {
        for (int i = 0; i < sourcePositions.size(); i++) {
            BaseNavigationPosition sourcePosition = sourcePositions.get(i);
            BaseNavigationPosition targetPosition = targetPositions.get(i);
            comparePosition(sourceFormat, targetFormat, i, sourcePosition, targetPosition, numberPositionNames, commentPositionNames, targetCharacteristics);
        }
        if (!compareByEquals)
            return;
        for (int i = 0; i < sourcePositions.size(); i++) {
            BaseNavigationPosition sourcePosition = sourcePositions.get(i);
            // don't fail if a position has been commented by us
            if (sourcePosition.getComment() == null)
                sourcePosition.setComment("Position " + (i + 1));
            BaseNavigationPosition targetPosition = targetPositions.get(i);
            assertEquals("Position " + i + " is not equal", sourcePosition, targetPosition);
            assertEquals(sourcePositions, targetPositions);
        }
    }

    public static void compareSplitPositions(List<BaseNavigationPosition> sourcePositions,
                                             NavigationFormat sourceFormat,
                                             List<BaseNavigationPosition> targetPositions,
                                             NavigationFormat targetFormat,
                                             int fileNumber,
                                             int positionsPerFile,
                                             boolean duplicateFirstPosition,
                                             boolean numberPositionNames,
                                             boolean commentPositionNames,
                                             RouteCharacteristics targetCharacteristics) {
        int count = duplicateFirstPosition ? sourcePositions.size() : targetPositions.size();
        for (int i = 0; i < count; i++) {
            int index = i + positionsPerFile * fileNumber;
            BaseNavigationPosition sourcePosition = sourcePositions.get(index);
            BaseNavigationPosition targetPosition = targetPositions.get(i + (duplicateFirstPosition ? 1 : 0));
            comparePosition(sourceFormat, targetFormat, index, sourcePosition, targetPosition, numberPositionNames, commentPositionNames, targetCharacteristics);
        }
    }

    public static Calendar calendar(File file, int hour, int minute, int second) {
        Calendar fileDate = Calendar.getInstance();
        fileDate.setTimeInMillis(file.lastModified());
        return calendar(fileDate.get(Calendar.YEAR), fileDate.get(Calendar.MONTH) + 1, fileDate.get(Calendar.DAY_OF_MONTH),
                hour, minute, second);
    }

    protected void readFile(File source, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        NavigationFileParser parser = new NavigationFileParser();
        parser.read(source);
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getAllRoutes());
        assertEquals(routeCount, parser.getAllRoutes().size());
        for (int i = 0; i < parser.getAllRoutes().size(); i++) {
            BaseRoute route = parser.getAllRoutes().get(i);
            assertNotNull(route);
            assertEquals("Route " + i + " from " + source + " is not " + characteristics[i],
                    characteristics[i], route.getCharacteristics());
            assertTrue(route.getPositionCount() > 0);
            List<BaseNavigationPosition> positions = route.getPositions();
            BaseNavigationPosition previous = null;
            for (int j = 0; j < positions.size(); j++) {
                BaseNavigationPosition position = positions.get(j);
                assertNotNull(position);
                assertNotNull(position.getLongitude());
                assertNotNull(position.getLatitude());
                if (expectElevation)
                    assertNotNull("Position " + j + " has no elevation", position.getElevation());
                if (expectTime) {
                    assertTrue("Position " + j + " has no time", position.getTime() != null);
                    if (previous != null)
                        assertTrue(!position.getTime().before(previous.getTime()));
                }
                previous = position;
            }
        }
    }

    protected void readFiles(String prefix, String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        List<File> files = Files.collectFiles(new File(SAMPLE_PATH), extension);
        for (File file : files) {
            if (file.getName().startsWith(prefix))
                readFile(file, routeCount, expectElevation, expectTime, characteristics);
        }
    }

    protected List<GpxRoute> readSampleGpxFile(GpxFormat format, String fileName) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        return format.read(new FileInputStream(source));
    }

    protected List<TomTomRoute> readSampleTomTomRouteFile(String fileName, boolean setStartDateFromFile) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        Calendar startDate = null;
        if (setStartDateFromFile) {
            startDate = Calendar.getInstance();
            startDate.setTimeInMillis(source.lastModified());
        }
        return new TomTom5RouteFormat().read(new FileInputStream(source), startDate);
    }

    protected List<NmeaRoute> readSampleNmeaFile(String fileName, boolean setStartDateFromFile) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        Calendar startDate = null;
        if (setStartDateFromFile) {
            startDate = Calendar.getInstance();
            startDate.setTimeInMillis(source.lastModified());
        }
        return new NmeaFormat().read(new FileInputStream(source), startDate);
    }

    protected List<SimpleRoute> readSampleGopalTrackFile(String fileName, boolean setStartDateFromFile) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        Calendar startDate = null;
        if (setStartDateFromFile) {
            startDate = Calendar.getInstance();
            startDate.setTimeInMillis(source.lastModified());
        }
        return new GoPalTrackFormat().read(new FileInputStream(source), startDate);
    }
}
