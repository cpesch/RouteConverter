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

package slash.navigation.base;

import junit.framework.AssertionFailedError;
import slash.common.TestCase;
import slash.common.io.CompactCalendar;
import slash.common.io.Files;
import slash.common.io.Transfer;
import slash.navigation.babel.*;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
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
import slash.navigation.nmea.*;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tour.TourFormat;

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

    public static final String ROUTE_PATH = "routes" + File.separator + "src" + File.separator;
    public static final String TEST_PATH = ROUTE_PATH + "test" + File.separator;
    public static final String SAMPLE_PATH = ROUTE_PATH + "samples" + File.separator;

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
                format instanceof GarminPcx5Format || format instanceof GoogleMapsFormat ||
                format instanceof GpsTunerFormat;
    }

    static boolean mayNotTransformBidirectionally(NavigationFormat first, NavigationFormat second) {
        return (isSlightlyUnprecise(first) || isSlightlyUnprecise(second)) ||
                ((first instanceof GpxFormat) &&
                        (second instanceof AlanTrackLogFormat || second instanceof AlanWaypointsAndRoutesFormat)) ||
                ((first instanceof MicrosoftAutoRouteFormat) &&
                        (second instanceof GarminPcx5Format)) ||
                ((first instanceof KmlFormat) &&
                        (second instanceof BcrFormat)) ||
                ((first instanceof ColumbusV900Format) &&
                        (second instanceof CoPilotFormat)) ||
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
        if (name.startsWith("/"))
            name = name.substring(1);
        int index = name.indexOf('/');
        if (index != -1)
            return name.substring(0, index);
        else
            return name;
    }

    private static String getAlanWaypointsAndRoutesName(BaseRoute route) {
        String name = route.getName();
        int index = name.indexOf(';');
        if (index != -1)
            return name.substring(0, index);
        else
            return name;
    }

    private static String getTrainingCenterRouteName(BaseRoute route) {
        String name = route.getName();
        name = name.replaceAll("\\d+: ", "");
        return name.substring(0, Math.min(15 - 3, name.length()));
    }

    @SuppressWarnings("unchecked")
    public static void compareRouteMetaData(BaseRoute sourceRoute, BaseRoute targetRoute) {
        if (targetRoute instanceof KmlRoute && targetRoute.getCharacteristics().equals(RouteCharacteristics.Waypoints)) {
            String sourceName = getKmlRouteName(sourceRoute);
            String targetName = getKmlRouteName(targetRoute);
            assertEquals(sourceName, targetName);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                sourceRoute.getName().contains(" to ") && sourceRoute.getName().endsWith("/") &&
                targetRoute.getName().endsWith("/")) {
            String sourcePrefix = getKmlRouteName(sourceRoute);
            String targetPrefix = getKmlRouteName(targetRoute);
            assertEquals(sourcePrefix, targetPrefix);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                sourceRoute.getName().contains(" to ") && sourceRoute.getName().contains("/;") &&
                targetRoute.getName().endsWith("/")) {
            // if AlanWaypointsAndRoutesFormat is converted to AlanWaypointsAndRoutesFormat "EARTH_RADIUS/; Orte to B/; Orte" becomes "EARTH_RADIUS/"
            String sourcePrefix = getAlanWaypointsAndRoutesName(sourceRoute);
            String targetPrefix = getAlanWaypointsAndRoutesName(targetRoute);
            assertEquals(sourcePrefix, targetPrefix);
        } else if (targetRoute.getFormat() instanceof TcxFormat) {
            // TcxFormat makes route names unique by prefixing "Name" with "1: "
            String sourceName = getTrainingCenterRouteName(sourceRoute);
            String targetName = getTrainingCenterRouteName(targetRoute);
            assertEquals(sourceName, targetName);
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
                                        boolean commentPositionNames,
                                        RouteCharacteristics sourceCharacteristics,
                                        RouteCharacteristics targetCharacteristics) {
        assertNotNull("Source longitude " + index + " does not exist", sourcePosition.getLongitude());
        assertNotNull("Source latitude " + index + " does not exist", sourcePosition.getLatitude());
        assertNotNull("Target longitude " + index + " does not exist", targetPosition.getLongitude());
        assertNotNull("Target latitude " + index + " does not exist", targetPosition.getLatitude());

        compareLongitudeAndLatitude(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareElevation(sourceFormat, targetFormat, sourcePosition, targetPosition, targetCharacteristics);
        compareHeading(sourceFormat, targetFormat, index, sourcePosition, targetPosition, sourceCharacteristics, targetCharacteristics);
        compareSpeed(sourceFormat, targetFormat, index, sourcePosition, targetPosition, sourceCharacteristics, targetCharacteristics);
        compareTime(sourceFormat, targetFormat, index, sourcePosition, targetPosition, targetCharacteristics);
        compareComment(sourceFormat, targetFormat, index, sourcePosition, targetPosition, commentPositionNames, targetCharacteristics);
        compareHdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        comparePdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareVdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareSatellites(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
    }

    private static void compareLongitudeAndLatitude(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        if (isBidirectional(sourceFormat, targetFormat, sourcePosition, targetPosition)) {
            assertEquals("Longitude " + index + " does not match", sourcePosition.getLongitude(), targetPosition.getLongitude());
            assertEquals("Latitude " + index + " does not match", sourcePosition.getLatitude(), targetPosition.getLatitude());
        } else if (isReallyUnprecise(sourceFormat) || isReallyUnprecise(targetFormat)) {
            // skip silly from.ov2 in tt poi coordinate
            if (targetPosition.getLongitude() != 10.032 && targetPosition.getLongitude() != 11.0206) {
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
                assertEquals((double) Math.round(sourcePosition.getElevation()), targetPosition.getElevation());
            } else if (targetFormat instanceof OziExplorerReadFormat || targetFormat instanceof NmeaFormat) {
                assertNearBy(sourcePosition.getElevation(), targetPosition.getElevation(), 0.1);
            } else if (targetFormat instanceof ColumbusV900Format) {
                assertEquals(sourcePosition.getElevation().intValue(), targetPosition.getElevation().intValue());
            } else
                assertEquals(sourcePosition.getElevation(), targetPosition.getElevation());

        } else if (sourceFormat instanceof OziExplorerReadFormat) {
            assertNull(targetPosition.getElevation());
        } else if (sourceFormat instanceof CoPilotFormat || sourceFormat instanceof TourFormat)
            assertNull(sourcePosition.getElevation());
        else if (targetFormat instanceof CoPilotFormat || targetFormat instanceof GoPal3RouteFormat ||
                targetFormat instanceof GoPalTrackFormat ||
                targetFormat instanceof NavigatingPoiWarnerFormat || targetFormat instanceof NmnFormat ||
                targetFormat instanceof Route66Format || targetFormat instanceof TourFormat)
            assertNull(targetPosition.getElevation());
        else if (sourcePosition.getElevation() == null &&
                (targetFormat instanceof KmlFormat || targetFormat instanceof KmzFormat ||
                        targetFormat instanceof Nmn5Format))
            assertEquals(0.0, targetPosition.getElevation());
        else if (sourcePosition.getElevation() == null &&
                targetFormat instanceof MagellanMapSendFormat)
            assertTrue(Transfer.isEmpty(targetPosition.getElevation()));
    }

    private static void compareHeading(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, RouteCharacteristics sourceCharacteristics, RouteCharacteristics targetCharacteristics) {
        Double sourceHeading = null;
        if (sourcePosition instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) sourcePosition;
            sourceHeading = wgs84Position.getHeading();
        }
        if (sourcePosition instanceof TomTomPosition) {
            TomTomPosition tomTomPosition = (TomTomPosition) sourcePosition;
            sourceHeading = tomTomPosition.getHeading();
        }
        if (sourcePosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) sourcePosition;
            sourceHeading = nmeaPosition.getHeading();
        }

        Double targetHeading = null;
        if (targetPosition instanceof Wgs84Position) {
            Wgs84Position wgs84TargetPosition = (Wgs84Position) targetPosition;
            targetHeading = wgs84TargetPosition.getHeading();
        }
        if (targetPosition instanceof TomTomPosition) {
            TomTomPosition tomTomPosition = (TomTomPosition) targetPosition;
            targetHeading = tomTomPosition.getHeading();
        }
        if (targetPosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) targetPosition;
            targetHeading = nmeaPosition.getHeading();
        }

        if (targetFormat instanceof ColumbusV900Format || targetFormat instanceof GpsTunerFormat) {
            if (sourceFormat instanceof GoPalTrackFormat || sourceFormat instanceof ColumbusV900Format ||
                sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(RouteCharacteristics.Track) ||
                sourceFormat instanceof Gpx11Format || sourceFormat instanceof NmeaFormat || 
                sourceFormat instanceof GpsTunerFormat || sourceFormat instanceof TomTomRouteFormat) {
                assertNotNull(sourceHeading);
                assertNotNull(targetHeading);
                assertEquals("Heading " + index + " does not match", targetHeading.intValue(), sourceHeading.intValue());
            } else {
                assertNull(sourceHeading);
                assertNotNull(targetHeading);
            }
        } else if ((sourceFormat instanceof GoPalTrackFormat && targetFormat instanceof NmeaFormat) ||
                (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof NmeaFormat) ||
                (sourceFormat instanceof GpxFormat && targetFormat instanceof NmeaFormat) ||
                (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof GoPalTrackFormat)) {
            assertEquals("Heading " + index + " does not match", Transfer.roundFraction(sourceHeading, 0), Transfer.roundFraction(targetHeading, 0));
        } else if ((sourceFormat instanceof GoPalTrackFormat || sourceFormat instanceof ColumbusV900Format || sourceFormat instanceof GpsTunerFormat ||
                sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(RouteCharacteristics.Track) ||
                sourceFormat instanceof NmeaFormat || sourceFormat instanceof TomTomRouteFormat) &&
                (targetFormat instanceof GoPalTrackFormat || targetFormat instanceof NmeaFormat || targetFormat instanceof TomTomRouteFormat ||
                 targetFormat instanceof Gpx10Format && targetCharacteristics.equals(RouteCharacteristics.Track))) {
            assertEquals("Heading " + index + " does not match", targetHeading, sourceHeading);
        } else if (targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format) {
            assertEquals("Heading " + index + " does not match", targetHeading, sourceHeading);
        } else if (targetFormat instanceof GoPalTrackFormat) {
            assertNull(sourceHeading);
            assertNotNull(targetHeading);
        } else
            assertNull("Heading " + index + " is not null: " + targetHeading, targetHeading);
    }

    private static void compareHdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        Double sourceHdop = null;
        if (sourcePosition instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) sourcePosition;
            sourceHdop = wgs84Position.getHdop();
        }
        if (sourcePosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) sourcePosition;
            sourceHdop = nmeaPosition.getHdop();
        }

        Double targetHdop = null;
        if (targetPosition instanceof Wgs84Position) {
            Wgs84Position wgs84TargetPosition = (Wgs84Position) targetPosition;
            targetHdop = wgs84TargetPosition.getHdop();
        }
        if (targetPosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) targetPosition;
            targetHdop = nmeaPosition.getHdop();
        }

        if ((sourceFormat instanceof ColumbusV900ProfessionalFormat || sourceFormat instanceof GoPalTrackFormat ||
             sourceFormat instanceof GpxFormat || sourceFormat instanceof NmeaFormat) &&
            (targetFormat instanceof ColumbusV900ProfessionalFormat || targetFormat instanceof GoPalTrackFormat ||
             targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format ||
             targetFormat instanceof NmeaFormat)) {
            assertEquals("Hdop " + index + " does not match", targetHdop, sourceHdop);
        } else if (targetFormat instanceof GoPalTrackFormat ||
            (sourceFormat instanceof CoPilotFormat && targetFormat instanceof ColumbusV900ProfessionalFormat) ||
            (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof ColumbusV900Format)) {
            assertNotNull("Hdop " + index + " is not null: " + targetHdop, targetHdop);
        } else
            assertNull("Hdop " + index + " is not null: " + targetHdop, targetHdop);
    }

    private static void comparePdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        Double sourcePdop = null;
        if (sourcePosition instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) sourcePosition;
            sourcePdop = wgs84Position.getPdop();
        }
        if (sourcePosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) sourcePosition;
            sourcePdop = nmeaPosition.getPdop();
        }

        Double targetPdop = null;
        if (targetPosition instanceof Wgs84Position) {
            Wgs84Position wgs84TargetPosition = (Wgs84Position) targetPosition;
            targetPdop = wgs84TargetPosition.getPdop();
        }
        if (targetPosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) targetPosition;
            targetPdop = nmeaPosition.getPdop();
        }

        if ((sourceFormat instanceof ColumbusV900ProfessionalFormat || sourceFormat instanceof GpxFormat || sourceFormat instanceof NmeaFormat) &&
                (targetFormat instanceof ColumbusV900ProfessionalFormat || targetFormat instanceof Gpx10Format ||
                 targetFormat instanceof Gpx11Format || targetFormat instanceof NmeaFormat)) {
            assertEquals("Pdop " + index + " does not match", targetPdop, sourcePdop);
        } else if (sourceFormat instanceof GoPalTrackFormat || targetFormat instanceof ColumbusV900Format) {
            assertNull("Pdop " + index + " is not null: " + sourcePdop, sourcePdop);
        } else
            assertNull("Pdop " + index + " is not null: " + targetPdop, targetPdop);
    }

    private static void compareVdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        Double sourceVdop = null;
        if (sourcePosition instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) sourcePosition;
            sourceVdop = wgs84Position.getVdop();
        }
        if (sourcePosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) sourcePosition;
            sourceVdop = nmeaPosition.getVdop();
        }

        Double targetVdop = null;
        if (targetPosition instanceof Wgs84Position) {
            Wgs84Position wgs84TargetPosition = (Wgs84Position) targetPosition;
            targetVdop = wgs84TargetPosition.getVdop();
        }
        if (targetPosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) targetPosition;
            targetVdop = nmeaPosition.getVdop();
        }

        if ((sourceFormat instanceof ColumbusV900ProfessionalFormat || sourceFormat instanceof GpxFormat || sourceFormat instanceof NmeaFormat) &&
            (targetFormat instanceof ColumbusV900ProfessionalFormat || targetFormat instanceof Gpx10Format ||
             targetFormat instanceof Gpx11Format || targetFormat instanceof NmeaFormat)) {
            assertEquals("Vdop " + index + " does not match", targetVdop, sourceVdop);
        } else if (sourceFormat instanceof GoPalTrackFormat || targetFormat instanceof ColumbusV900Format) {
            assertNull("Vdop " + index + " is not null: " + sourceVdop, sourceVdop);
        } else
            assertNull("Vdop " + index + " is not null: " + targetVdop, targetVdop);
    }

    private static void compareSatellites(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition) {
        Integer sourceSatellites = null;
        if (sourcePosition instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) sourcePosition;
            sourceSatellites = wgs84Position.getSatellites();
        }
        if (sourcePosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) sourcePosition;
            sourceSatellites = nmeaPosition.getSatellites();
        }

        Integer targetSatellites = null;
        if (targetPosition instanceof Wgs84Position) {
            Wgs84Position wgs84TargetPosition = (Wgs84Position) targetPosition;
            targetSatellites = wgs84TargetPosition.getSatellites();
        }
        if (targetPosition instanceof NmeaPosition) {
            NmeaPosition nmeaPosition = (NmeaPosition) targetPosition;
            targetSatellites = nmeaPosition.getSatellites();
        }

        if ((sourceFormat instanceof GpxFormat || sourceFormat instanceof GoPalTrackFormat || sourceFormat instanceof NmeaFormat) &&
            (targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format ||
             targetFormat instanceof GoPalTrackFormat || targetFormat instanceof NmeaFormat)) {
            assertEquals("Satellites " + index + " does not match", targetSatellites, sourceSatellites);
        } else if (targetFormat instanceof GoPalTrackFormat) {
            assertNotNull("Satellites " + index + " is null", targetSatellites);
        }  else
            assertNull("Satellites " + index + " is not null: " + targetSatellites, targetSatellites);
    }

    private static String getAlanWaypointsAndRoutesPositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        int index = comment.indexOf(';');
        if (index != -1)
            comment = comment.substring(0, index);
        return trim(comment, 8);
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

    private static String getGarminPcx5PositionComment(BaseNavigationPosition position) {
        return nameDescription(garminUmlauts(trim(position.getComment(), 39)), 6, 4, true);
    }

    private static String getGarminPoiPositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return trim(comment, 45);
    }

    private static String getGarminPoiDbPositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return trim(nameDescription(garminUmlauts(comment).replaceAll(",", ""), 24, Integer.MAX_VALUE, true), 50);
    }

    private static String getMagellanMapSendPositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment.startsWith("WPT")) {
            int index = comment.indexOf(';');
            if (index != -1)
                comment = comment.substring(index + 1);
        }
        return trim(trimSpaces(comment), 19);
    }

    private static String getMicrosoftAutoroutePositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return trimDot1Substring(trim(comment, 16));
    }

    private static String getTourExchangePositionComment(BaseNavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        int index = comment.indexOf(";");
        if (index == -1)
            return comment;
        String name = comment.substring(0, index);
        String description = comment.substring(index + 1);
        return trimDot1Substring(name) + ";" + description;

    }


    private static void compareComment(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, boolean commentPositionNames, RouteCharacteristics targetCharacteristics) {
        // Test only if a position has not been commented by us
        if (!(sourcePosition.getComment() == null && targetPosition.getComment().startsWith("Position"))) {
            if (targetFormat instanceof AlanTrackLogFormat ||
                    (targetFormat instanceof GarminMapSource6Format && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                    targetFormat instanceof GoPalTrackFormat || targetFormat instanceof GpsTunerFormat ||
                    targetFormat instanceof HaicomLoggerFormat || targetFormat instanceof KompassFormat ||
                    targetFormat instanceof MagicMapsIktFormat || targetFormat instanceof MagicMapsPthFormat ||
                    targetFormat instanceof OvlFormat || targetFormat instanceof Tcx1Format || targetFormat instanceof Tcx2Format ||
                    (targetFormat instanceof OziExplorerReadFormat && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                    (targetFormat instanceof GarminMapSource5Format && targetCharacteristics.equals(RouteCharacteristics.Track)) ||
                    ((targetFormat instanceof KmlFormat || targetFormat instanceof KmzFormat) && !targetCharacteristics.equals(RouteCharacteristics.Waypoints) && !commentPositionNames))
                assertTrue("Comment " + index + " does not match", targetPosition.getComment().startsWith("Position"));
            else if (sourceFormat instanceof AlanTrackLogFormat)
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment(), targetPosition.getComment());
            else if (targetFormat instanceof AlanWaypointsAndRoutesFormat)
                assertEquals("Comment " + index + " does not match", getAlanWaypointsAndRoutesPositionComment(sourcePosition), getAlanWaypointsAndRoutesPositionComment(targetPosition));
            else if (sourceFormat instanceof BcrFormat && targetFormat instanceof TomTomRouteFormat) {
                BcrPosition bcrPosition = (BcrPosition) sourcePosition;
                assertEquals("Comment " + index + " does not match", escapeBcr(bcrPosition.getCity() + (bcrPosition.getStreet() != null ? "," + bcrPosition.getStreet() : "")), targetPosition.getComment());
            } else if (sourceFormat instanceof GarminPoiFormat && targetFormat instanceof GarminPoiDbFormat) {
                String sourceName = getGarminPoiDbPositionComment(sourcePosition);
                String targetName = getGarminPoiDbPositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof GarminPoiDbFormat) {
                String sourceName = getGarminPoiDbPositionComment(sourcePosition);
                String targetName = getGarminPoiDbPositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof GarminPoiFormat) {
                String sourceName = getGarminPoiPositionComment(sourcePosition);
                String targetName = getGarminPoiPositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof OziExplorerReadFormat && targetCharacteristics.equals(RouteCharacteristics.Waypoints))
                assertEquals("Comment " + index + " does not match", garminUmlauts(trim(sourcePosition.getComment().replace(",", ""), 50)), trim(trimSpeedComment(targetPosition.getComment()), 50));
            else if (targetFormat instanceof OziExplorerReadFormat && targetCharacteristics.equals(RouteCharacteristics.Route))
                assertEquals("Comment " + index + " does not match", garminUmlauts(trim(sourcePosition.getComment().replace(",", ""), 8)), trim(trimSpeedComment(targetPosition.getComment()), 8));
            else if (targetFormat instanceof TomTomRouteFormat) {
                String targetComment = targetPosition.getComment();
                // strip tripmaster prefix if required
                int colonIndex = targetComment.lastIndexOf(" : ");
                if (colonIndex == -1)
                    colonIndex = targetComment.lastIndexOf(" - ");
                if (colonIndex != -1) 
                    targetComment = targetComment.substring(colonIndex + 3);
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment().replaceAll("\\|", ";"), targetComment);
            } else if (targetFormat instanceof ColumbusV900Format || targetFormat instanceof MagellanExploristFormat ||
                    targetFormat instanceof MagellanRouteFormat || targetFormat instanceof NmeaFormat)
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment().replaceAll(",", ";"), targetPosition.getComment());
            else if (targetFormat instanceof Nmn4Format || targetFormat instanceof Nmn5Format)
                assertEquals("Comment " + index + " does not match", escapeNmn4and5(sourcePosition.getComment()), targetPosition.getComment());
            else if (targetFormat instanceof Nmn6Format)
                assertEquals("Comment " + index + " does not match", escapeNmn6(sourcePosition.getComment()), targetPosition.getComment());
            else if (targetFormat instanceof Nmn6FavoritesFormat)
                assertEquals("Comment " + index + " does not match", escapeNmn6Favorites(sourcePosition.getComment()), targetPosition.getComment());
            else if (targetFormat instanceof Nmn7Format)
                assertEquals("Comment " + index + " does not match", trimSpaces(sourcePosition.getComment()), trimSpaces(targetPosition.getComment()));
            else if (sourceFormat instanceof GarminPcx5Format && targetFormat instanceof MagellanMapSendFormat) {
                // makes no sense, as the result is "WPT001" from a "D22081..." source
            } else if (targetFormat instanceof MagellanMapSendFormat) {
                String sourceName = getMagellanMapSendPositionComment(sourcePosition);
                String targetName = getMagellanMapSendPositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof GarminPcx5Format) {
                String sourceName = getGarminPcx5PositionComment(sourcePosition);
                String targetName = getGarminPcx5PositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof MicrosoftAutoRouteFormat) {
                String sourceName = getMicrosoftAutoroutePositionComment(sourcePosition);
                String targetName = getMicrosoftAutoroutePositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof Route66Format)
                assertEquals("Comment " + index + " does not match", Transfer.toMixedCase(sourcePosition.getComment()), targetPosition.getComment());
            else if (sourceFormat instanceof GarminMapSource5Format || sourceFormat instanceof GarminMapSource6Format) {
                String sourceName = getGarminMapSource6PositionComment(sourcePosition);
                String targetName = getGarminMapSource6PositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof TourExchangeFormat) {
                String sourceName = getTourExchangePositionComment(sourcePosition);
                String targetName = getTourExchangePositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof GpxFormat)
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment().trim(), trimSpeedComment(targetPosition.getComment()));
            else
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment(), targetPosition.getComment());
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static void compareSpeed(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, RouteCharacteristics sourceCharacteristics, RouteCharacteristics targetCharacteristics) {
        if (sourcePosition.getSpeed() != null && targetPosition.getSpeed() != null) {
            if ((sourceFormat instanceof GpsTunerFormat && targetFormat instanceof NmeaFormat) ||
                (sourceFormat instanceof GpxFormat && targetFormat instanceof NmeaFormat)) {
                assertNearBy(sourcePosition.getSpeed(), targetPosition.getSpeed(), 0.05);
            } else if ((sourceFormat instanceof ColumbusV900Format || sourceFormat instanceof GoPalTrackFormat ||
                    sourceFormat instanceof GpsTunerFormat || sourceFormat instanceof GpxFormat || sourceFormat instanceof NmeaFormat) &&
                    targetFormat instanceof ColumbusV900Format) {
                assertEquals("Speed " + index + " does not match", sourcePosition.getSpeed().intValue(), targetPosition.getSpeed().intValue());
            } else if (sourceFormat instanceof ColumbusV900Format && targetFormat instanceof NmeaFormat) {
                assertNearBy(sourcePosition.getSpeed(), targetPosition.getSpeed(), 0.1);
            } else if (sourceFormat instanceof NmeaFormat || targetFormat instanceof NmeaFormat ||
                    (sourceFormat instanceof Gpx10Format && targetFormat instanceof AlanTrackLogFormat)) {
                assertNearBy(sourcePosition.getSpeed(), targetPosition.getSpeed(), 0.025);
            } else if (sourceFormat instanceof GoPalTrackFormat || sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(RouteCharacteristics.Track) ||
                    targetFormat instanceof GoPalTrackFormat || targetFormat instanceof Gpx10Format) {
                assertEquals("Speed " + index + " does not match", Transfer.roundFraction(sourcePosition.getSpeed(), 1), Transfer.roundFraction(targetPosition.getSpeed(), 1));
            } else {
                assertEquals("Speed " + index + " does not match", sourcePosition.getSpeed(), targetPosition.getSpeed());
            }
        }
    }

    private static void compareTime(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, BaseNavigationPosition sourcePosition, BaseNavigationPosition targetPosition, RouteCharacteristics targetCharacteristics) {
        if (sourcePosition.getTime() != null && targetPosition.getTime() != null) {
            if (targetFormat instanceof KmlFormat && targetCharacteristics.equals(RouteCharacteristics.Track)) {
                assertNotNull(sourcePosition.getTime());
                assertNotNull(targetPosition.getTime());
            } else if (sourceFormat instanceof GoPalTrackFormat || targetFormat instanceof GoPalTrackFormat) {
                DateFormat format = DateFormat.getTimeInstance();
                format.setTimeZone(CompactCalendar.UTC);
                String sourceTime = format.format(sourcePosition.getTime().getTime());
                String targetTime = format.format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
            } else {
                DateFormat format = DateFormat.getDateTimeInstance();
                format.setTimeZone(CompactCalendar.UTC);
                String sourceTime = format.format(sourcePosition.getTime().getTime());
                String targetTime = format.format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
                if (!sourcePosition.getTime().equals(targetPosition.getTime()))
                    log.warning("Time " + index + " does not match");
                // too many reasons for an invalid time :-(
                // assertEquals("Time " + index + " does not match", sourcePosition.getTime(), targetPosition.getTime());
            }
        } else if ((sourceFormat instanceof Gpx11Format || sourceFormat instanceof Tcx1Format) && targetFormat instanceof Tcx1Format) {
            assertNull(sourcePosition.getTime());
            assertNotNull(targetPosition.getTime());
        } else if (targetFormat instanceof AlanTrackLogFormat || targetFormat instanceof BcrFormat ||
                targetFormat instanceof GarminMapSource5Format || targetFormat instanceof GarminPcx5Format ||
                targetFormat instanceof GlopusFormat || targetFormat instanceof MagellanRouteFormat ||
                targetFormat instanceof NmnFormat || targetFormat instanceof TomTomRouteFormat ||
                targetFormat instanceof TourFormat) {
            assertNull(targetPosition.getTime());
        } else if (targetFormat instanceof GoPalTrackFormat) {
            assertNull(sourcePosition.getTime());
            assertNotNull(targetPosition.getTime());
        } else if (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof HaicomLoggerFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof OziExplorerReadFormat ||
                sourceFormat instanceof ColumbusV900Format && targetFormat instanceof CoPilotFormat) {
            assertNotNull(sourcePosition.getTime());
            assertNull(targetPosition.getTime());
        } else
            assertEquals("Time " + index + " does not match", sourcePosition.getTime(), targetPosition.getTime());
    }

    private static String trim(String str, int maximum) {
        return str != null && str.length() > maximum ? str.substring(0, maximum).trim() : str;
    }

    private static String trimSuffix(String str, String suffix) {
        if (str == null)
            return null;
        int index = str.indexOf(suffix);
        return index != -1 ? str.substring(0, index) : str;
    }

    private static String trimSpeedComment(String str) {
        return trimSuffix(str, "; Speed");
    }

    private static String trimDot1Substring(String str) {
        return trimSuffix(str, ".1");
    }

    private static String garminUmlauts(String str) {
        return str.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "$").replace("Ö", "O");
    }

    private static String nameDescription(String str, int nameMaximum, int descriptionMaximum, boolean trim) {
        if (str == null)
            return null;
        int index = str.indexOf(";");
        if (index == -1)
            return str;
        String name = str.substring(0, Math.min(nameMaximum, index));
        if (trim)
            name = name.trim();
        String description = str.substring(index + 1);
        description = description.substring(0, Math.min(description.length(), descriptionMaximum));
        if (trim)
            description = description.trim();
        return name + ";" + description;
    }

    private static String escapeBcr(String str) {
        return str != null ? str.replace("|", ";") : null;
    }

    private static String escapeNmn4and5(String str) {
        if (str != null && str.length() > 2 && str.toUpperCase().equals(str))
            str = Transfer.toMixedCase(str);
        return str != null ? str.replaceAll("\\|", ";") : null;
    }

    private static String escapeNmn6(String str) {
        return str != null ? str.replaceAll("[\\[|\\||\\]]", ";") : null;
    }

    private static String escapeNmn6Favorites(String str) {
        return str != null ? Transfer.toMixedCase(str.replaceAll("[\\[|\\||\\]]", "").replaceAll("ß", "ss")) : null;
    }

    private static String trimSpaces(String str) {
        return str != null ? str.replaceAll(" ", "") : null;
    }

    public static void comparePositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute, NavigationFormat sourceFormat,
                                        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute, NavigationFormat targetFormat, boolean commentPositionNames) {
        if (sourceFormat instanceof Route66Format && targetFormat instanceof TomTomPoiFormat) {
            // both formats support no ordering
        } else if (targetFormat instanceof TomTomPoiFormat) {
            assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, 1), sourceFormat, targetRoute.getPositions().subList(0, 1), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
            comparePositions(sourceRoute.getPositions().subList(sourceRoute.getPositionCount() - 1, sourceRoute.getPositionCount()), sourceFormat, targetRoute.getPositions().subList(1, 2), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
            // TomTomPoiFormat has no order of the positions except for first and second
            // comparePositions(sourceRoute.getPositions().subList(1, sourceRoute.getPositionCount() - 1), sourceFormat, targetRoute.getPositions().subList(2, targetRoute.getPositionCount() - 2), targetFormat, false, targetRoute.getCharacteristics());
        } else if (sourceFormat instanceof MicrosoftAutoRouteFormat &&
                (targetFormat instanceof GarminMapSource5Format || targetFormat instanceof GarminMapSource6Format || targetFormat instanceof KmlFormat) &&
                targetRoute.getCharacteristics().equals(RouteCharacteristics.Waypoints)) {
            int sourcePositionCount = sourceRoute.getPositionCount() - 1;
            assertEquals(sourcePositionCount, targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, sourcePositionCount), sourceFormat, targetRoute.getPositions(), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
        } else {
            assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions(), sourceFormat, targetRoute.getPositions(), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
        }
    }

    public static void comparePositions(List<BaseNavigationPosition> sourcePositions,
                                        NavigationFormat sourceFormat,
                                        List<BaseNavigationPosition> targetPositions,
                                        NavigationFormat targetFormat,
                                        boolean commentPositionNames,
                                        boolean compareByEquals,
                                        RouteCharacteristics sourceCharacteristics,
                                        RouteCharacteristics targetCharacteristics) {
        for (int i = 0; i < sourcePositions.size(); i++) {
            BaseNavigationPosition sourcePosition = sourcePositions.get(i);
            BaseNavigationPosition targetPosition = targetPositions.get(i);
            comparePosition(sourceFormat, targetFormat, i, sourcePosition, targetPosition, commentPositionNames, sourceCharacteristics, targetCharacteristics);
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
                                             boolean commentPositionNames,
                                             RouteCharacteristics sourceCharacteristics,
                                             RouteCharacteristics targetCharacteristics) {
        int count = duplicateFirstPosition ? sourcePositions.size() : targetPositions.size();
        for (int i = 0; i < count; i++) {
            int index = i + positionsPerFile * fileNumber;
            BaseNavigationPosition sourcePosition = sourcePositions.get(index);
            BaseNavigationPosition targetPosition = targetPositions.get(i + (duplicateFirstPosition ? 1 : 0));
            comparePosition(sourceFormat, targetFormat, index, sourcePosition, targetPosition, commentPositionNames, sourceCharacteristics, targetCharacteristics);
        }
    }

    public static CompactCalendar calendar(File file, int hour, int minute, int second) {
        Calendar fileDate = Calendar.getInstance();
        fileDate.setTimeInMillis(file.lastModified());
        return calendar(fileDate.get(Calendar.YEAR), fileDate.get(Calendar.MONTH) + 1, fileDate.get(Calendar.DAY_OF_MONTH),
                hour, minute, second);
    }

    @SuppressWarnings("unchecked")
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
                        assertTrue(!position.getTime().getCalendar().before(previous.getTime().getCalendar()));
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
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = CompactCalendar.fromCalendar(calendar);
        }
        return new TomTom5RouteFormat().read(new FileInputStream(source), startDate);
    }

    protected List<NmeaRoute> readSampleNmeaFile(String fileName, boolean setStartDateFromFile) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = CompactCalendar.fromCalendar(calendar);
        }
        return new NmeaFormat().read(new FileInputStream(source), startDate);
    }

    protected List<SimpleRoute> readSampleGopalTrackFile(String fileName, boolean setStartDateFromFile) throws IOException {
        File source = new File(SAMPLE_PATH + fileName);
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = CompactCalendar.fromCalendar(calendar);
        }
        return new GoPalTrackFormat().read(new FileInputStream(source), startDate);
    }
}
