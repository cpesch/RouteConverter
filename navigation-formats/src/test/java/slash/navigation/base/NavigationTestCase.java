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
import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.babel.AlanTrackLogFormat;
import slash.navigation.babel.AlanWaypointsAndRoutesFormat;
import slash.navigation.babel.CompeGPSDataFormat;
import slash.navigation.babel.FlightRecorderDataFormat;
import slash.navigation.babel.GarminFitFormat;
import slash.navigation.babel.GarminMapSource5Format;
import slash.navigation.babel.GarminMapSource6Format;
import slash.navigation.babel.GarminPcx5Format;
import slash.navigation.babel.GarminPoiDbFormat;
import slash.navigation.babel.GarminPoiFormat;
import slash.navigation.babel.GeoCachingFormat;
import slash.navigation.babel.MagellanMapSendFormat;
import slash.navigation.babel.MicrosoftAutoRouteFormat;
import slash.navigation.babel.OziExplorerFormat;
import slash.navigation.babel.TomTomPoiFormat;
import slash.navigation.babel.TourExchangeFormat;
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
import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.KmlFormat;
import slash.navigation.kml.KmlRoute;
import slash.navigation.kml.KmzFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6FavoritesFormat;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.ColumbusV900Format;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GoRiderGpsFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.GroundTrackFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Iblue747Format;
import slash.navigation.simple.KompassFormat;
import slash.navigation.simple.NavilinkFormat;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tour.TourFormat;
import slash.navigation.url.GoogleMapsUrlFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.io.File.separator;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.roundFraction;
import static slash.common.io.Transfer.toMixedCase;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.BaseNavigationFormat.GENERATED_BY;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public abstract class NavigationTestCase extends TestCase {
    public static final String ROUTE_PATH = System.getProperty("samples", "routes" + separator + "src") + separator;
    public static final String TEST_PATH = ROUTE_PATH + "test" + separator;
    public static final String SAMPLE_PATH = ROUTE_PATH + "samples" + separator;

    static {
        JaxbUtils.setCacheContexts(true);
    }

    public static void assertDescriptionEquals(List<String> expected, List<String> was) {
        List<String> wasFiltered = new ArrayList<String>();
        if (was != null)
            for (String w : was) {
                if (!w.equals(GENERATED_BY))
                    wasFiltered.add(w);
            }
        if (expected.size() == 0)
            expected = null;
        if (wasFiltered.size() == 0)
            wasFiltered = null;
        assertEquals(expected, wasFiltered);
    }

    public static void assertRouteNameEquals(String expected, String was) {
        assertEquals(trim(expected, 64), trim(was, 64));
    }

    private static void assertException(Class exceptionClass, NavigationTestCaseThrowsException runner) {
        try {
            runner.run();
            fail("Worked?");
        } catch (Throwable throwable) {
            assertTrue("Wrong exception: " + throwable.getClass().getName(), exceptionClass.isInstance(throwable));
        }
    }

    public static void assertTestFails(NavigationTestCaseThrowsException runner) {
        assertException(AssertionFailedError.class, runner);
    }

    static boolean isReallyUnprecise(NavigationFormat format) {
        return format instanceof BaseNmeaFormat || format instanceof TomTomPoiFormat;
    }

    static boolean isSlightlyUnprecise(NavigationFormat format) {
        return isReallyUnprecise(format) || format instanceof CoPilotFormat ||
                format instanceof GarminPoiFormat || format instanceof GarminMapSource6Format ||
                format instanceof GeoCachingFormat || format instanceof GarminMapSource5Format ||
                format instanceof GarminPcx5Format || format instanceof GoogleMapsUrlFormat ||
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
                                   NavigationPosition sourcePosition,
                                   NavigationPosition targetPosition) {
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
        return name.substring(0, min(15 - 4 /* Suffix length */, name.length()));
    }

    @SuppressWarnings("unchecked")
    public static void compareRouteMetaData(BaseRoute sourceRoute, BaseRoute targetRoute) {
        if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                sourceRoute.getName().contains(" to ") && sourceRoute.getName().endsWith("/") &&
                targetRoute.getName().endsWith("/")) {
            String sourcePrefix = getKmlRouteName(sourceRoute);
            String targetPrefix = getKmlRouteName(targetRoute);
            assertRouteNameEquals(sourcePrefix, targetPrefix);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                sourceRoute.getName().contains(" to ") && sourceRoute.getName().contains("/;") &&
                targetRoute.getName().endsWith("/")) {
            // if AlanWaypointsAndRoutesFormat is converted to AlanWaypointsAndRoutesFormat "EARTH_RADIUS/; Orte to B/; Orte" becomes "EARTH_RADIUS/"
            String sourcePrefix = getAlanWaypointsAndRoutesName(sourceRoute);
            String targetPrefix = getAlanWaypointsAndRoutesName(targetRoute);
            assertRouteNameEquals(sourcePrefix, targetPrefix);
        } else if (targetRoute.getFormat() instanceof TcxFormat) {
            // TcxFormat makes route names unique by prefixing "Name" with "1: "
            String sourceName = getTrainingCenterRouteName(sourceRoute);
            String targetName = getTrainingCenterRouteName(targetRoute);
            assertRouteNameEquals(sourceName, targetName);
        } else if (sourceRoute.getName() != null && targetRoute.getName() != null &&
                !targetRoute.getName().contains(" to ") && !targetRoute.getName().contains("Route: ") &&
                !targetRoute.getName().startsWith("/Route") &&
                !targetRoute.getName().endsWith("/Route") &&
                !targetRoute.getName().equals("MapLage") && !targetRoute.getName().contains("Track: ") &&
                !targetRoute.getName().endsWith("/Track") &&
                !targetRoute.getName().endsWith("/Waypoints"))
            // Test only if this is not the multiple routes per file case & the route has not been named by us
            assertRouteNameEquals(sourceRoute.getName(), targetRoute.getName());

        // Test only if this is not the multiple routes per file case
        if (sourceRoute.getDescription() != null && targetRoute.getDescription() != null &&
                !(targetRoute.getFormat() instanceof MagicMapsIktFormat))
            assertDescriptionEquals(sourceRoute.getDescription(), targetRoute.getDescription());
    }

    private static void comparePosition(NavigationFormat sourceFormat,
                                        NavigationFormat targetFormat,
                                        int index,
                                        NavigationPosition sourcePosition,
                                        NavigationPosition targetPosition,
                                        boolean commentPositionNames,
                                        RouteCharacteristics sourceCharacteristics,
                                        RouteCharacteristics targetCharacteristics) {
        if (sourceFormat instanceof GoogleMapsUrlFormat &&
                sourcePosition.getLongitude() == null && sourcePosition.getLatitude() == null) {
            assertNull(sourcePosition.getLongitude());
            assertNull(sourcePosition.getLatitude());
            assertNull(targetPosition.getLongitude());
            assertNull(targetPosition.getLatitude());
        } else {
            assertNotNull("Source longitude " + index + " does not exist", sourcePosition.getLongitude());
            assertNotNull("Source latitude " + index + " does not exist", sourcePosition.getLatitude());
            assertNotNull("Target longitude " + index + " does not exist", targetPosition.getLongitude());
            assertNotNull("Target latitude " + index + " does not exist", targetPosition.getLatitude());
            compareLongitudeAndLatitude(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        }
        compareElevation(sourceFormat, targetFormat, sourcePosition, targetPosition, targetCharacteristics);
        compareHeading(sourceFormat, targetFormat, index, sourcePosition, targetPosition, sourceCharacteristics, targetCharacteristics);
        compareSpeed(sourceFormat, targetFormat, index, sourcePosition, targetPosition, sourceCharacteristics, targetCharacteristics);
        compareTime(sourceFormat, targetFormat, index, sourcePosition, targetPosition, targetCharacteristics);
        compareComment(sourceFormat, targetFormat, index, sourcePosition, targetPosition, commentPositionNames, sourceCharacteristics, targetCharacteristics);
        compareHdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        comparePdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareVdop(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
        compareSatellites(sourceFormat, targetFormat, index, sourcePosition, targetPosition);
    }

    private static void compareLongitudeAndLatitude(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition) {
        if (isBidirectional(sourceFormat, targetFormat, sourcePosition, targetPosition)) {
            assertEquals("Longitude " + index + " does not match", roundFraction(sourcePosition.getLongitude(), 7), roundFraction(targetPosition.getLongitude(), 7));
            assertEquals("Latitude " + index + " does not match", roundFraction(sourcePosition.getLatitude(), 7), roundFraction(targetPosition.getLatitude(), 7));
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

    private static void compareElevation(NavigationFormat sourceFormat, NavigationFormat targetFormat, NavigationPosition sourcePosition, NavigationPosition targetPosition, RouteCharacteristics targetCharacteristics) {
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
            } else if (targetFormat instanceof OziExplorerFormat || targetFormat instanceof NmeaFormat ||
                    targetFormat instanceof CompeGPSDataFormat) {
                assertNearBy(sourcePosition.getElevation(), targetPosition.getElevation(), 0.1);
            } else if (targetFormat instanceof ColumbusV900Format) {
                assertEquals(sourcePosition.getElevation().intValue(), targetPosition.getElevation().intValue());
            } else
                assertNearBy(roundFraction(sourcePosition.getElevation(), 1), roundFraction(targetPosition.getElevation(), 1), 0.1);

        } else if (sourceFormat instanceof OziExplorerFormat) {
            assertNull(targetPosition.getElevation());
        } else if (sourceFormat instanceof CoPilotFormat || sourceFormat instanceof TourFormat)
            assertNull(sourcePosition.getElevation());
        else if (targetFormat instanceof CoPilotFormat || targetFormat instanceof GoPal3RouteFormat ||
                targetFormat instanceof GoPalTrackFormat ||
                targetFormat instanceof NavigatingPoiWarnerFormat || targetFormat instanceof NmnFormat ||
                targetFormat instanceof Route66Format || targetFormat instanceof TourFormat)
            assertNull(targetPosition.getElevation());
        else if (sourcePosition.getElevation() == null &&
                (targetFormat instanceof KmlFormat || targetFormat instanceof KmzFormat))
            assertEquals(0.0, targetPosition.getElevation());
        else if (sourcePosition.getElevation() == null &&
                targetFormat instanceof MagellanMapSendFormat)
            assertTrue(isEmpty(targetPosition.getElevation()));
    }

    private static void compareHeading(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition, RouteCharacteristics sourceCharacteristics, RouteCharacteristics targetCharacteristics) {
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
                    sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(Track) ||
                    sourceFormat instanceof Gpx11Format || sourceFormat instanceof NmeaFormat ||
                    sourceFormat instanceof GpsTunerFormat || sourceFormat instanceof TomTomRouteFormat ||
                    sourceFormat instanceof Iblue747Format) {
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
            assertEquals("Heading " + index + " does not match", roundFraction(sourceHeading, 0), roundFraction(targetHeading, 0));
        } else if ((sourceHeading != null && targetHeading != null) &&
                (sourceFormat instanceof GoPalTrackFormat || sourceFormat instanceof ColumbusV900Format || sourceFormat instanceof GpsTunerFormat ||
                        sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(Track) ||
                        sourceFormat instanceof NmeaFormat || sourceFormat instanceof TomTomRouteFormat) &&
                (targetFormat instanceof GoPalTrackFormat || targetFormat instanceof NmeaFormat || targetFormat instanceof TomTomRouteFormat ||
                        targetFormat instanceof Gpx10Format && targetCharacteristics.equals(Track))) {
            assertEquals("Heading " + index + " does not match", roundFraction(targetHeading, 1), roundFraction(sourceHeading, 1));
        } else if ((targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format ||
                (sourceFormat instanceof Iblue747Format && targetFormat instanceof Iblue747Format)) &&
                (sourceHeading != null && targetHeading != null)) {
            assertEquals("Heading " + index + " does not match", roundFraction(targetHeading, 1), roundFraction(sourceHeading, 1));
        } else if (targetFormat instanceof GoPalTrackFormat ||
                (sourceFormat instanceof QstarzQ1000Format && targetFormat instanceof Iblue747Format)) {
            assertNull(sourceHeading);
            assertNotNull(targetHeading);
        } else
            assertNull("Heading " + index + " is not null: " + targetHeading, targetHeading);
    }

    private static void compareHdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition) {
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
                sourceFormat instanceof GpxFormat || sourceFormat instanceof NmeaFormat ||
                sourceFormat instanceof QstarzQ1000Format) &&
                (targetFormat instanceof ColumbusV900ProfessionalFormat || targetFormat instanceof GoPalTrackFormat ||
                        targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format ||
                        targetFormat instanceof NmeaFormat || targetFormat instanceof QstarzQ1000Format)) {
            assertEquals("Hdop " + index + " does not match", targetHdop, sourceHdop);
        } else if (targetFormat instanceof GoPalTrackFormat ||
                (sourceFormat instanceof CoPilotFormat && targetFormat instanceof ColumbusV900ProfessionalFormat) ||
                (sourceFormat instanceof Iblue747Format && targetFormat instanceof QstarzQ1000Format) ||
                (sourceFormat instanceof Iblue747Format && targetFormat instanceof ColumbusV900ProfessionalFormat) ||
                (sourceFormat instanceof GpsTunerFormat && targetFormat instanceof ColumbusV900Format)) {
            assertNotNull("Hdop " + index + " is not null: " + targetHdop, targetHdop);
        } else
            assertNull("Hdop " + index + " is not null: " + targetHdop, targetHdop);
    }

    private static void comparePdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition) {
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

    private static void compareVdop(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition) {
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

    private static void compareSatellites(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition) {
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

        if ((sourceFormat instanceof GpxFormat || sourceFormat instanceof GoPalTrackFormat ||
                sourceFormat instanceof NmeaFormat || sourceFormat instanceof QstarzQ1000Format) &&
                (targetFormat instanceof Gpx10Format || targetFormat instanceof Gpx11Format ||
                        targetFormat instanceof GoPalTrackFormat || targetFormat instanceof NmeaFormat ||
                        targetFormat instanceof QstarzQ1000Format)) {
            assertEquals("Satellites " + index + " does not match", targetSatellites, sourceSatellites);
        } else if (targetFormat instanceof GoPalTrackFormat || targetFormat instanceof QstarzQ1000Format) {
            assertNotNull("Satellites " + index + " is null", targetSatellites);
        } else
            assertNull("Satellites " + index + " is not null: " + targetSatellites, targetSatellites);
    }

    private static String getAlanWaypointsAndRoutesPositionComment(NavigationPosition position) {
        String comment = position.getComment();
        int index = comment.indexOf(';');
        if (index != -1)
            comment = comment.substring(0, index);
        return trim(comment, 8);
    }

    private static String spaceUmlauts(String str) {
        return str.replaceAll("\u00e4", "  ").replaceAll("\u00fc", "  ").replaceAll("\u00df", "  ")
                .replaceAll("\u00f6", "  ");
    }

    private static String getGarminMapSource6PositionComment(NavigationPosition position) {
        String comment = spaceUmlauts(position.getComment());
        if (comment.startsWith("STATION")) {
            int index = comment.indexOf(';');
            if (index != -1)
                return Transfer.trim(comment.substring(index + 1));
        }
        return comment;
    }

    private static String getGarminPcx5PositionComment(NavigationPosition position) {
        return nameDescription(garminUmlauts(trim(position.getComment(), 39)), 6, 4, true);
    }

    private static String getGarminPoiPositionComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        comment = comment.replaceAll("  ", " ");
        return trim(comment, 45);
    }

    private static String getGarminPoiDbPositionComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return trim(nameDescription(garminUmlauts(comment).replaceAll(",", ""), 24, MAX_VALUE, true), 50);
    }

    private static String getGoRiderGpsComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return comment.replaceAll("\"", ";");
    }

    private static String getMagellanMapSendPositionComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment.startsWith("WPT")) {
            int index = comment.indexOf(';');
            if (index != -1)
                comment = comment.substring(index + 1);
        }
        return trim(trimSpaces(comment), 19);
    }

    private static String getMicrosoftAutoroutePositionComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return trimDot1Substring(trim(comment, 16));
    }

    private static String getTourExchangePositionComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        comment = spaceUmlauts(comment);
        int index = comment.indexOf(";");
        if (index == -1)
            return comment;
        String name = comment.substring(0, index);
        String description = comment.substring(index + 1);
        return trimDot1Substring(name) + ";" + Transfer.trim(description);

    }

    private static String getNavigatingPoiWarnerComment(NavigationPosition position) {
        String comment = position.getComment();
        if (comment == null)
            return null;
        return comment.replaceAll(",", ";").replaceAll("\"", ";");
    }


    private static void compareComment(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition, boolean commentPositionNames, RouteCharacteristics sourceCharacteristics, RouteCharacteristics targetCharacteristics) {
        // Test only if a position has not been commented by us
        if (!(sourcePosition.getComment() == null && targetPosition.getComment().startsWith("Position"))) {
            if (targetFormat instanceof AlanTrackLogFormat || targetFormat instanceof CompeGPSDataFormat ||
                    (targetFormat instanceof GarminMapSource6Format && targetCharacteristics.equals(Track) && sourceFormat instanceof GpxFormat && sourceCharacteristics.equals(Track)) ||
                    (targetFormat instanceof GarminMapSource5Format && targetCharacteristics.equals(Track) && sourceFormat instanceof GpxFormat && sourceCharacteristics.equals(Track)) ||
                    targetFormat instanceof GoPalTrackFormat || targetFormat instanceof GpsTunerFormat ||
                    targetFormat instanceof HaicomLoggerFormat || targetFormat instanceof Igo8RouteFormat ||
                    targetFormat instanceof KompassFormat ||
                    targetFormat instanceof MagicMapsIktFormat || targetFormat instanceof MagicMapsPthFormat ||
                    targetFormat instanceof OvlFormat || targetFormat instanceof Tcx1Format || targetFormat instanceof Tcx2Format ||
                    (targetFormat instanceof OziExplorerFormat && targetCharacteristics.equals(Track)) ||
                    ((targetFormat instanceof KmlFormat || targetFormat instanceof KmzFormat) && !targetCharacteristics.equals(Waypoints) && !commentPositionNames))
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
            } else if (targetFormat instanceof OziExplorerFormat && targetCharacteristics.equals(Waypoints))
                assertEquals("Comment " + index + " does not match", garminUmlauts(trim(sourcePosition.getComment().replace(",", ""), 50)), trim(trimSpeedComment(targetPosition.getComment()), 50));
            else if (targetFormat instanceof OziExplorerFormat && targetCharacteristics.equals(RouteCharacteristics.Route))
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
                assertEquals("Comment " + index + " does not match", escapeNmn6Favorites(sourcePosition.getComment().toUpperCase()), targetPosition.getComment());
            else if (targetFormat instanceof Nmn7Format)
                assertEquals("Comment " + index + " does not match", trimSpaces(sourcePosition.getComment()), trimSpaces(targetPosition.getComment()));
            else if (sourceFormat instanceof GarminPcx5Format && targetFormat instanceof MagellanMapSendFormat) {
                // makes no sense, as the result is "WPT001" from a "D22081..." source
                assertTrue(true);
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
                assertEquals("Comment " + index + " does not match", toMixedCase(sourcePosition.getComment()), targetPosition.getComment());
            else if (sourceFormat instanceof GarminMapSource5Format || sourceFormat instanceof GarminMapSource6Format ||
                    targetFormat instanceof GarminMapSource5Format) {
                String sourceName = getGarminMapSource6PositionComment(sourcePosition);
                String targetName = getGarminMapSource6PositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof NavigatingPoiWarnerFormat || targetFormat instanceof NavigatingPoiWarnerFormat) {
                String sourceName = getNavigatingPoiWarnerComment(sourcePosition);
                String targetName = getNavigatingPoiWarnerComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof GlopusFormat) {
                String sourceName = getNavigatingPoiWarnerComment(sourcePosition);
                String targetName = getNavigatingPoiWarnerComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (targetFormat instanceof GoRiderGpsFormat) {
                String sourceName = getGoRiderGpsComment(sourcePosition);
                String targetName = getGoRiderGpsComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof TourExchangeFormat) {
                String sourceName = getTourExchangePositionComment(sourcePosition);
                String targetName = getTourExchangePositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else if (sourceFormat instanceof GpxFormat) {
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment().trim(), trimSpeedComment(targetPosition.getComment()));
            } else if (sourceFormat instanceof OpelNaviFormat) {
                String sourceName = getTourExchangePositionComment(sourcePosition);
                String targetName = getTourExchangePositionComment(targetPosition);
                assertEquals("Comment " + index + " does not match", sourceName, targetName);
            } else
                assertEquals("Comment " + index + " does not match", sourcePosition.getComment(), targetPosition.getComment());
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static void compareSpeed(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition, RouteCharacteristics sourceCharacteristics, RouteCharacteristics targetCharacteristics) {
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
                    (sourceFormat instanceof Gpx10Format && targetFormat instanceof AlanTrackLogFormat) ||
                    sourceFormat instanceof GpsTunerFormat ||
                    sourceFormat instanceof GoPalTrackFormat ||
                    (sourceFormat instanceof Gpx10Format && sourceCharacteristics.equals(Track)) ||
                    targetFormat instanceof GoPalTrackFormat || targetFormat instanceof Gpx10Format) {
                assertNearBy(sourcePosition.getSpeed(), targetPosition.getSpeed(), 0.025);
            } else if (sourceFormat instanceof QstarzQ1000Format && targetFormat instanceof ColumbusV900Format) {
                assertEquals("Speed " + index + " does not match", sourcePosition.getSpeed().intValue(), targetPosition.getSpeed().intValue());
            } else if (sourceFormat instanceof Iblue747Format && targetFormat instanceof ColumbusV900Format) {
                assertEquals("Speed " + index + " does not match", sourcePosition.getSpeed().intValue(), targetPosition.getSpeed().intValue());
            } else if (sourceFormat instanceof Iblue747Format) {
                assertNearBy(roundFraction(sourcePosition.getSpeed(), 1), roundFraction(targetPosition.getSpeed(), 1), 1.5);
            } else if (sourceFormat instanceof GarminFitFormat && targetFormat instanceof GpxFormat) {
                assertNearBy(roundFraction(sourcePosition.getSpeed(), 1), roundFraction(targetPosition.getSpeed(), 1), 1.5);
            } else {
                assertEquals("Speed " + index + " does not match", roundFraction(sourcePosition.getSpeed(), 1), roundFraction(targetPosition.getSpeed(), 1));
            }
        }
    }

    private static void compareTime(NavigationFormat sourceFormat, NavigationFormat targetFormat, int index, NavigationPosition sourcePosition, NavigationPosition targetPosition, RouteCharacteristics targetCharacteristics) {
        if (sourcePosition.hasTime() && targetPosition.hasTime()) {
            if (targetFormat instanceof KmlFormat && targetCharacteristics.equals(Track)) {
                assertNotNull(sourcePosition.getTime());
                assertNotNull(targetPosition.getTime());
            } else if (sourceFormat instanceof GoPalTrackFormat || targetFormat instanceof GoPalTrackFormat) {
                DateFormat format = DateFormat.getTimeInstance();
                format.setTimeZone(UTC);
                String sourceTime = format.format(sourcePosition.getTime().getTime());
                String targetTime = format.format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
            } else if (sourceFormat instanceof GroundTrackFormat || targetFormat instanceof GroundTrackFormat) {
                DateFormat format = DateFormat.getTimeInstance();
                format.setTimeZone(UTC);
                String sourceTime = format.format(sourcePosition.getTime().getTime());
                String targetTime = format.format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
            } else {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
                format.setTimeZone(UTC);
                String sourceTime = format.format(sourcePosition.getTime().getTime());
                String targetTime = format.format(targetPosition.getTime().getTime());
                assertEquals("Time " + index + " does not match", sourceTime, targetTime);
            }
        } else if ((sourceFormat instanceof Gpx11Format || sourceFormat instanceof TcxFormat) && targetFormat instanceof TcxFormat) {
            assertNull(sourcePosition.getTime());
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
                sourceFormat instanceof FlightRecorderDataFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof GarminFitFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof CompeGPSDataFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof HaicomLoggerFormat && targetFormat instanceof KmlFormat ||
                sourceFormat instanceof NavilinkFormat && targetFormat instanceof NavigatingPoiWarnerFormat ||
                sourceFormat instanceof OziExplorerFormat ||
                sourceFormat instanceof GpxFormat && targetFormat instanceof GoRiderGpsFormat ||
                sourceFormat instanceof ColumbusV900Format && targetFormat instanceof CoPilotFormat ||
                sourceFormat instanceof Iblue747Format && targetFormat instanceof CoPilotFormat ||
                sourceFormat instanceof QstarzQ1000Format && targetFormat instanceof CoPilotFormat) {
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
        return str.replace("\u00e4", "a").replace("\u00f6", "o").replace("\u00fc", "u").replace("\u00df", "$").replace("\u00d6", "O").
                replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "$").replace("Ö", "O");
    }

    private static String nameDescription(String str, int nameMaximum, int descriptionMaximum, boolean trim) {
        if (str == null)
            return null;
        int index = str.indexOf(";");
        if (index == -1)
            return str;
        String name = str.substring(0, min(nameMaximum, index));
        if (trim)
            name = name.trim();
        String description = str.substring(index + 1);
        description = description.substring(0, min(description.length(), descriptionMaximum));
        if (trim)
            description = description.trim();
        return name + ";" + description;
    }

    private static String escapeBcr(String str) {
        return str != null ? str.replace("|", ";") : null;
    }

    private static String escapeNmn4and5(String str) {
        if (str != null && str.length() > 2)
            str = toMixedCase(str);
        return str != null ? str.replaceAll("\\|", ";") : null;
    }

    private static String escapeNmn6(String str) {
        return str != null ? str.replaceAll("[\\[|\\||\\]]", ";") : null;
    }

    private static String escapeNmn6Favorites(String str) {
        return str != null ? toMixedCase(str.replaceAll("[\\[|\\||\\]]", "").replaceAll("\u00df", "ss")) : null;
    }

    private static String trimSpaces(String str) {
        return str != null ? str.replaceAll(" ", "") : null;
    }

    public static void comparePositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute, NavigationFormat sourceFormat,
                                        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute, NavigationFormat targetFormat, boolean commentPositionNames) {
        if (sourceFormat instanceof Route66Format && targetFormat instanceof TomTomPoiFormat) {
            // both formats support no ordering
            assertTrue(true);
        } else if (targetFormat instanceof TomTomPoiFormat) {
            assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());
            comparePositions(sourceRoute.getPositions().subList(0, 1), sourceFormat, targetRoute.getPositions().subList(0, 1), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
            comparePositions(sourceRoute.getPositions().subList(sourceRoute.getPositionCount() - 1, sourceRoute.getPositionCount()), sourceFormat, targetRoute.getPositions().subList(1, 2), targetFormat, commentPositionNames, false, sourceRoute.getCharacteristics(), targetRoute.getCharacteristics());
            // TomTomPoiFormat has no order of the positions except for first and second
            // comparePositions(sourceRoute.getPositions().subList(1, sourceRoute.getPositionCount() - 1), sourceFormat, targetRoute.getPositions().subList(2, targetRoute.getPositionCount() - 2), targetFormat, false, targetRoute.getCharacteristics());
        } else if (sourceFormat instanceof MicrosoftAutoRouteFormat &&
                (targetFormat instanceof GarminMapSource5Format || targetFormat instanceof GarminMapSource6Format || targetFormat instanceof KmlFormat) &&
                targetRoute.getCharacteristics().equals(Waypoints)) {
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
            NavigationPosition sourcePosition = sourcePositions.get(i);
            NavigationPosition targetPosition = targetPositions.get(i);
            comparePosition(sourceFormat, targetFormat, i, sourcePosition, targetPosition, commentPositionNames, sourceCharacteristics, targetCharacteristics);
        }
        if (!compareByEquals)
            return;
        for (int i = 0; i < sourcePositions.size(); i++) {
            NavigationPosition sourcePosition = sourcePositions.get(i);
            // don't fail if a position has been commented by us
            if (sourcePosition.getComment() == null)
                sourcePosition.setComment("Position " + (i + 1));
            NavigationPosition targetPosition = targetPositions.get(i);
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
            NavigationPosition sourcePosition = sourcePositions.get(index);
            NavigationPosition targetPosition = targetPositions.get(i + (duplicateFirstPosition ? 1 : 0));
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
    public static void readFile(File source, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser();
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertNotNull(result.getFormat());
        assertNotNull(result.getAllRoutes());
        assertEquals(routeCount, result.getAllRoutes().size());
        for (int i = 0; i < result.getAllRoutes().size(); i++) {
            BaseRoute route = result.getAllRoutes().get(i);
            assertNotNull(route);
            assertEquals("Route " + i + " from " + source + " is not " + characteristics[i],
                    characteristics[i], route.getCharacteristics());
            assertTrue(route.getPositionCount() > 0);
            List<NavigationPosition> positions = route.getPositions();
            NavigationPosition previous = null;
            for (int j = 0; j < positions.size(); j++) {
                NavigationPosition position = positions.get(j);
                assertNotNull(position);
                assertNotNull(position.getLongitude());
                assertNotNull(position.getLatitude());
                if (expectElevation)
                    assertNotNull("Position " + j + " has no elevation", position.getElevation());
                if (expectTime) {
                    assertTrue("Position " + j + " has no time", position.hasTime());
                    if (previous != null)
                        assertTrue(!position.getTime().getCalendar().before(previous.getTime().getCalendar()));
                }
                previous = position;
            }
        }
    }

    public static void readFiles(String prefix, String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        List<File> files = collectFiles(new File(SAMPLE_PATH), extension);
        for (File file : files) {
            if (file.getName().startsWith(prefix))
                readFile(file, routeCount, expectElevation, expectTime, characteristics);
        }
    }

    public static List<GpxRoute> readGpxFile(GpxFormat format, String fileName) throws Exception {
        File source = new File(fileName);
        ParserContext<GpxRoute> context = new ParserContextImpl<GpxRoute>();
        format.read(new FileInputStream(source), null, context);
        return context.getRoutes();
    }

    public static List<KmlRoute> readKmlFile(BaseKmlFormat format, String fileName) throws Exception {
        File source = new File(fileName);
        NavigationFormatParser parser = new NavigationFormatParser();
        ParserResult result = parser.read(source, asList((NavigationFormat) format));
        List<KmlRoute> routes = new ArrayList<KmlRoute>();
        for (BaseRoute route : result.getAllRoutes()) {
            if (route instanceof KmlRoute)
                routes.add((KmlRoute) route);
        }
        return routes;
    }

    public static List<TomTomRoute> readSampleTomTomRouteFile(String fileName, boolean setStartDateFromFile) throws Exception {
        File source = new File(SAMPLE_PATH + fileName);
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = fromCalendar(calendar);
        }
        ParserContext<TomTomRoute> context = new ParserContextImpl<TomTomRoute>();
        new TomTom5RouteFormat().read(new FileInputStream(source), startDate, context);
        return context.getRoutes();
    }

    public static List<NmeaRoute> readSampleNmeaFile(String fileName, boolean setStartDateFromFile) throws Exception {
        File source = new File(SAMPLE_PATH + fileName);
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = fromCalendar(calendar);
        }
        ParserContext<NmeaRoute> context = new ParserContextImpl<NmeaRoute>();
        new NmeaFormat().read(new FileInputStream(source), startDate, context);
        return context.getRoutes();
    }

    public static List<SimpleRoute> readSampleGopalTrackFile(String fileName, boolean setStartDateFromFile) throws Exception {
        File source = new File(SAMPLE_PATH + fileName);
        CompactCalendar startDate = null;
        if (setStartDateFromFile) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source.lastModified());
            startDate = fromCalendar(calendar);
        }
        ParserContext<SimpleRoute> context = new ParserContextImpl<SimpleRoute>();
        new GoPalTrackFormat().read(new FileInputStream(source), startDate, context);
        return context.getRoutes();
    }
}
