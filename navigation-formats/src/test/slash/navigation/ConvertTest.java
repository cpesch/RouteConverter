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

import slash.navigation.babel.*;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.klicktel.KlickTelRouteFormat;
import slash.navigation.kml.*;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.*;
import slash.navigation.tcx.*;
import slash.navigation.tour.TourFormat;
import slash.navigation.viamichelin.ViaMichelinFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConvertTest extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

    void convertRoundtrip(String testFileName,
                          BaseNavigationFormat sourceFormat,
                          BaseNavigationFormat targetFormat) throws IOException {
        assertTrue(sourceFormat.isSupportsReading());
        assertTrue(targetFormat.isSupportsWriting());

        File source = new File(testFileName);
        assertTrue("Cannot read route from " + source, parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getTheRoute());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        // check append
        BaseNavigationPosition sourcePosition = parser.getTheRoute().getPositions().get(0);
        BaseNavigationPosition targetPosition = NavigationFormats.asFormat(sourcePosition, targetFormat);
        assertNotNull(targetPosition);

        convertSingleRouteRoundtrip(sourceFormat, targetFormat, source, parser.getTheRoute());

        if (targetFormat.isSupportsMultipleRoutes()) {
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, new ArrayList<BaseRoute>(Arrays.asList(parser.getTheRoute())));
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, parser.getAllRoutes());
        }
    }

    private BaseNavigationFormat handleWriteOnlyFormats(BaseNavigationFormat targetFormat) {
        if (targetFormat instanceof OziExplorerWriteFormat)
            targetFormat = new OziExplorerReadFormat();
        if (targetFormat instanceof Crs1Format)
            targetFormat = new Tcx1Format();
        return targetFormat;
    }

    private void convertSingleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, BaseRoute sourceRoute) throws IOException {
        File target = File.createTempFile("singletarget", targetFormat.getExtension());
        try {
            parser.write(sourceRoute, targetFormat, false, false, target);
            assertTrue(target.exists());

            NavigationFileParser sourceParser = new NavigationFileParser();
            assertTrue(sourceParser.read(source));
            NavigationFileParser targetParser = new NavigationFileParser();
            assertTrue(targetParser.read(target));

            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
            assertEquals(targetFormat.getName(), targetParser.getFormat().getName());

            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetParser.getTheRoute();
            compareRouteMetaData(sourceRoute, targetRoute);
            comparePositions(sourceRoute, sourceFormat, targetRoute, targetFormat, targetParser.getAllRoutes().size() > 0);

            for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : targetParser.getAllRoutes()) {
                compareRouteMetaData(sourceRoute, route);
                comparePositions(sourceRoute, sourceFormat, route, targetFormat, targetParser.getAllRoutes().size() > 0);
            }

            assertTrue(target.exists());
            assertTrue(target.delete());
        } finally {
            // avoid to clutter the temp directory
            assert target.delete();
        }
    }

    private void convertMultipleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, List<BaseRoute> sourceRoutes) throws IOException {
        File target = File.createTempFile("multitarget", targetFormat.getExtension());
        try {
            parser.write(sourceRoutes, (MultipleRoutesFormat) targetFormat, target);
            assertTrue(target.exists());

            NavigationFileParser sourceParser = new NavigationFileParser();
            assertTrue(sourceParser.read(source));
            NavigationFileParser targetParser = new NavigationFileParser();
            assertTrue(targetParser.read(target));

            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
            assertEquals(targetFormat.getName(), targetParser.getFormat().getName());

            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetParser.getTheRoute();
            compareRouteMetaData(sourceParser.getTheRoute(), targetRoute);

            for (int i = 0; i < targetParser.getAllRoutes().size(); i++) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = targetParser.getAllRoutes().get(i);
                compareRouteMetaData(sourceParser.getTheRoute(), route);
                BaseRoute sourceRoute = sourceFormat instanceof MicrosoftAutoRouteFormat ? sourceRoutes.get(0) : sourceRoutes.get(i);
                comparePositions(sourceRoute, sourceFormat, route, targetFormat, targetParser.getAllRoutes().size() > 1);
            }

            assertTrue(target.exists());
            assertTrue(target.delete());
        } finally {
            // avoid to clutter the temp directory
            assert target.delete();
        }
    }

    void convertSplitRoundtrip(String testFileName, BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat) throws IOException {
        File source = new File(testFileName);
        assertTrue(parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getTheRoute());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        BaseRoute sourceRoute = parser.getTheRoute();
        int maximumPositionCount = targetFormat.getMaximumPositionCount();
        int positionCount = parser.getTheRoute().getPositionCount();
        int fileCount = (int) Math.ceil((double) positionCount / maximumPositionCount);
        assertEquals(fileCount, NavigationFileParser.getNumberOfFilesToWriteFor(sourceRoute, targetFormat, false));

        File[] targets = new File[fileCount];
        for (int i = 0; i < targets.length; i++)
            targets[i] = File.createTempFile("splittarget", targetFormat.getExtension());
        try {
            parser.write(sourceRoute, targetFormat, false, false, targets);

            NavigationFileParser sourceParser = new NavigationFileParser();
            sourceParser.read(source);

            for (int i = 0; i < targets.length; i++) {
                NavigationFileParser targetParser = new NavigationFileParser();
                targetParser.read(targets[i]);
                assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
                assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
                assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
                assertEquals(targetFormat.getName(), targetParser.getFormat().getName());
                assertEquals(i != targets.length - 1 ? maximumPositionCount : (positionCount - i * maximumPositionCount),
                        targetParser.getTheRoute().getPositionCount());

                compareSplitPositions(sourceParser.getTheRoute().getPositions(), sourceFormat,
                        targetParser.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount, false, false,
                        sourceParser.getTheRoute().getCharacteristics(), targetParser.getTheRoute().getCharacteristics());
            }

            for (File target : targets) {
                assertTrue(target.exists());
                assertTrue(target.delete());
            }
        } finally {
            // avoid to clutter the temp directory
            for (File target : targets) {
                assert target.delete();
            }
        }
    }

    public void testConvertMTP0607ToMTP0809() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new MTP0809Format());
    }

    public void testConvertMTP0809ToMTP0809() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new MTP0809Format());
    }

    public void testConvertMTP0607ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new TomTom5RouteFormat());
    }

    public void testConvertGpxToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new TomTom8RouteFormat());
    }

    public void testConvertMTP0607ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml22Format());
    }

    public void testConvertMTP0607ToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new CoPilot6Format());
    }

    public void testConvertMTP0809ToKmz() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kmz20Format());
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kmz21Format());
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kmz22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kmz22Format());
    }


    public void testConvertColumbusV900ToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-standard.csv", new ColumbusV900Format(), new CoPilot6Format());
    }


    public void testConvertCoPilot6ToCoPilot7() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new CoPilot7Format());
    }

    public void testConvertCoPilot6ToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new Gpx11Format());
    }

    public void testConvertCoPilot6ToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new ColumbusV900Format());
    }

    public void testConvertCoPilot7ToCoPilot6() throws IOException {
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new CoPilot6Format());
    }

    public void testConvertCoPilot7ToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new Gpx11Format());
    }


    public void testConvertTomTomRouteToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new TomTom5RouteFormat());
        convertRoundtrip(TEST_PATH + "from5.itn", new TomTom5RouteFormat(), new TomTom5RouteFormat());
        convertRoundtrip(TEST_PATH + "from8.itn", new TomTom8RouteFormat(), new TomTom8RouteFormat());
    }

    public void testConvertTomTomRouteToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new MTP0607Format());
        // contain umlauts currently not read by MTP:
        // convertRoundtrip(TEST_PATH + "from5.itn", new TomTom5RouteFormat(), new MTP0607Format());
        // convertRoundtrip(TEST_PATH + "from8.itn", new TomTom8RouteFormat(), new MTP0607Format());
    }

    public void testConvertTomTomRouteToMTP0809() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new MTP0809Format());
        // contain umlauts currently not read by MTP:
        // convertRoundtrip(TEST_PATH + "from5.itn", new TomTom5RouteFormat(), new MTP0809Format());
        // convertRoundtrip(TEST_PATH + "from8.itn", new TomTom8RouteFormat(), new MTP0809Format());
    }

    public void testConvertTomTomRouteToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new Kml22Format());
    }


    public void testConvertGoogleMapsToGoogleMaps() throws IOException {
        convertRoundtrip(TEST_PATH + "from.url", new GoogleMapsFormat(), new GoogleMapsFormat());
    }

    public void testConvertGoogleMapsToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.url", new GoogleMapsFormat(), new TomTom5RouteFormat());
    }


    public void testConvertGarminMapSource6ToGarminMapSource5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gdb", new GarminMapSource6Format(), new GarminMapSource5Format());
        convertRoundtrip(TEST_PATH + "large.gdb", new GarminMapSource6Format(), new GarminMapSource5Format());
    }

    public void testConvertGarminMapSource6ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gdb", new GarminMapSource6Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.gdb", new GarminMapSource6Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.gdb", new GarminMapSource6Format(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from.gdb", new GarminMapSource6Format(), new Kml22Format());
    }

    public void testConvertGpx10ToGarminMapSource6() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GarminMapSource6Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GarminMapSource6Format());
    }

    public void testConvertGpx11ToGarminMapSource6() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new GarminMapSource6Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GarminMapSource6Format());
    }

    public void testConvertMicrosoftAutoRouteToGarminMapSource6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new GarminMapSource6Format());
    }

    public void testConvertTourExchangeToGarminMapSource6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GarminMapSource6Format());
    }


    public void testConvertGpiToGpi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gpi", new GarminPoiFormat(), new GarminPoiFormat());
    }

    public void testConvertGpiToXcsv() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gpi", new GarminPoiFormat(), new GarminPoiDbFormat());
    }


    public void testConvertHoluxM241BinaryToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.bin", new HoluxM241BinaryFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.bin", new HoluxM241BinaryFormat(), new Gpx11Format());
    }


    public void testConvertXcsvToXcvs() throws IOException {
        convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiDbFormat());
    }

    public void testConvertXcsvToGpiFails() throws IOException {
        // TODO Garmin file contains 400 instead of expected 216 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiFormat());
            }
        });
    }


    public void testConvertGarminMapSource5ToGarminMapSource5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new GarminMapSource5Format());
        convertRoundtrip(TEST_PATH + "large.mps", new GarminMapSource5Format(), new GarminMapSource5Format());
    }

    public void testConvertGarminMapSource5ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new Kml22Format());
    }

    public void testConvertGpx10ToGarminMapSource5() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GarminMapSource5Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GarminMapSource5Format());
    }

    public void testConvertGpx11ToGarminMapSource5() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new GarminMapSource5Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GarminMapSource5Format());
    }

    public void testConvertMicrosoftAutoRouteToGarminMapSource5() throws IOException {
        // TODO Garmin file contains only 41 instead of expected 45 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new GarminMapSource5Format());
            }
        });
    }

    public void testConvertTourExchangeToGarminMapSource5Fails() throws IOException {
        // TODO Garmin file contains only 47 instead of expected 49 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GarminMapSource5Format());
            }
        });
    }


    public void testConvertGpx10ToGpx10() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Gpx10Format());
    }

    public void testConvertGpx10ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new TomTom8RouteFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new TomTom8RouteFormat());
    }

    public void testConvertGpx10ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new MTP0607Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new MTP0607Format());
    }

    public void testConvertGpx11ToGpx11() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Gpx11Format());
    }

    public void testConvertGpx11ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new TomTom8RouteFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new TomTom8RouteFormat());
    }

    public void testConvertGpx11ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new MTP0607Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new MTP0607Format());
    }


    public void testConvertKlickTelRouteToKlickTelRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.krt", new KlickTelRouteFormat(), new KlickTelRouteFormat());
    }

    public void testConvertKlickTelRouteToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.krt", new KlickTelRouteFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.krt", new KlickTelRouteFormat(), new Gpx11Format());
    }


    public void testConvertKml20ToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new Kml20Format());
    }

    public void testConvertKml20ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new TomTom8RouteFormat());
    }

    public void testConvertKml20ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new MTP0607Format());
    }

    public void testConvertKml21ToKml21() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new Kml21Format());
    }

    public void testConvertKml21ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new TomTom8RouteFormat());
    }

    public void testConvertKml21ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new MTP0607Format());
    }

    public void testConvertKml22BetaToKml22Beta() throws IOException {
        convertRoundtrip(TEST_PATH + "from22beta.kml", new Kml22BetaFormat(), new Kml22BetaFormat());
    }

    public void testConvertKml22BetaToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from22beta.kml", new Kml22BetaFormat(), new TomTom8RouteFormat());
    }

    public void testConvertKml22BetaToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from22beta.kml", new Kml22BetaFormat(), new MTP0607Format());
    }

    public void testConvertKml22ToKml22() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new Kml22Format());
    }

    public void testConvertKml22ToKml22Beta() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new Kml22BetaFormat());
    }

    public void testConvertKml22ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new TomTom8RouteFormat());
    }

    public void testConvertKml22ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new MTP0607Format());
    }


    public void testConvertMagicMapsPthToMagicMapsPth() throws IOException {
        convertRoundtrip(TEST_PATH + "from.pth", new MagicMapsPthFormat(), new MagicMapsPthFormat());
    }

    public void testConvertMagicMapsPthToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from.pth", new MagicMapsPthFormat(), new CoPilot6Format());
    }

    public void testConvertTomTomRouteToMagicMapsPth() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new MagicMapsPthFormat());
        convertRoundtrip(TEST_PATH + "from5.itn", new TomTom5RouteFormat(), new MagicMapsPthFormat());
        convertRoundtrip(TEST_PATH + "from8.itn", new TomTom8RouteFormat(), new MagicMapsPthFormat());
    }

    public void testConvertTop50ToMagicMapsPth() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ovl", new OvlFormat(), new MagicMapsPthFormat());
    }

    public void testConvertMagicMapsPthToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from.pth", new MagicMapsPthFormat(), new OvlFormat());
    }


    public void testConvertMagicMapsIktToMagicMapsIkt() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ikt", new MagicMapsIktFormat(), new MagicMapsIktFormat());
    }

    public void testConvertMagicMapsIktToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ikt", new MagicMapsIktFormat(), new CoPilot6Format());
    }

    public void testConvertTomTomRouteToMagicMapsIkt() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new TomTom5RouteFormat(), new MagicMapsIktFormat());
        convertRoundtrip(TEST_PATH + "from5.itn", new TomTom5RouteFormat(), new MagicMapsIktFormat());
        convertRoundtrip(TEST_PATH + "from8.itn", new TomTom8RouteFormat(), new MagicMapsIktFormat());
    }

    public void testConvertTop50ToMagicMapsIkt() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ovl", new OvlFormat(), new MagicMapsIktFormat());
    }

    public void testConvertMagicMapsIktToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ikt", new MagicMapsIktFormat(), new OvlFormat());
    }


    public void testConvertOziExplorerToOziExplorer() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new OziExplorerTrackFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new OziExplorerRouteFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new OziExplorerWaypointFormat());
    }

    public void testConvertOziExplorerRouteToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new OvlFormat());
    }

    public void testConvertOziExplorerTrackToTop50() throws IOException {
        // TODO differences in conversion:
        // TODO Target longitude 0 does not exist
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new OvlFormat());
            }
        });
    }

    public void testConvertOziExplorerWaypointToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new OvlFormat());
    }

    public void testConvertOziExplorerToMagicMaps() throws IOException {
        // TODO differences in conversion:
        // TODO 2.6141469644200224 is not within +5.0E-6 of -17.954639 to -17.954728773195
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new MagicMapsPthFormat());
            }
        });
    }

    public void testConvertGpx10ToOziExplorerTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerTrackFormat());
    }

    public void testConvertGpx10ToOziExplorerRoute() throws IOException {
        // TODO differences in conversion:
        // TODO Longitude 0 does not match expected:<-10.76617> but was:<-53.69928>
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerRouteFormat());
            }
        });
    }

    public void testConvertGpx10ToOziExplorerWaypoints() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new OziExplorerWaypointFormat());
    }


    public void testConvertGoPalRouteToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.xml", new GoPalRouteFormat(), new Gpx11Format());
    }

    public void testConvertGoPalRouteToGoPalRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.xml", new GoPalRouteFormat(), new GoPalRouteFormat());
    }

    public void testConvertTourExchangeToGoPalRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GoPalRouteFormat());
    }


    public void testConvertGoPalTrackToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx11Format());
    }

    public void testConvertGoPalTrackToGoPal() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new GoPalTrackFormat());
    }

    public void testConvertTourExchangeToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GoPalTrackFormat());
    }


    public void testConvertGpxToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new NmeaFormat());
    }

    public void testConvertGpxToMagellanExplorist() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new MagellanExploristFormat());
    }

    public void testConvertGpxToMagellanRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new MagellanRouteFormat());
    }

    public void testConvertMagellanExploristToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-magellan.log", new MagellanExploristFormat(), new Gpx10Format());
    }

    public void testConvertMagellanRouteToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-magellan.rte", new MagellanRouteFormat(), new Gpx10Format());
    }

    public void testConvertMagellanToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-magellan.log", new MagellanExploristFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-magellan.rte", new MagellanRouteFormat(), new Gpx10Format());
    }

    public void testConvertNmn4ToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn4.rte", new Nmn4Format(), new Nmn4Format());
        convertRoundtrip(TEST_PATH + "large-nmn4.rte", new Nmn4Format(), new Nmn4Format());
    }

    public void testConvertGpx10ToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Nmn4Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Nmn4Format());
    }

    public void testConvertGpx11ToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Nmn4Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Nmn4Format());
    }

    public void testConvertNmn4ToGpx10() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn4.rte", new Nmn4Format(), new Gpx10Format());
    }

    public void testConvertMicrosoftAutoRouteToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new Nmn4Format());
    }

    public void testConvertNmn4ToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn4.rte", new Nmn4Format(), new Kml20Format());
    }

    public void testConvertTourExchangeToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new Nmn4Format());
    }


    public void testConvertNmn5ToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn5.rte", new Nmn5Format(), new Nmn6Format());
        convertRoundtrip(TEST_PATH + "large-nmn5.rte", new Nmn5Format(), new Nmn6Format());
    }

    public void testConvertGpx10ToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Nmn5Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Nmn5Format());
    }

    public void testConvertGpx11ToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Nmn5Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Nmn5Format());
    }

    public void testConvertMicrosoftAutoRouteToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new Nmn5Format());
    }

    public void testConvertTourExchangeToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new Nmn5Format());
    }


    public void testConvertNmn6ToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn6.rte", new Nmn6Format(), new Nmn6Format());
        convertRoundtrip(TEST_PATH + "large-nmn6.rte", new Nmn6Format(), new Nmn6Format());
    }

    public void testConvertGpx10ToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Nmn6Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Nmn6Format());
    }

    public void testConvertGpx11ToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Nmn6Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Nmn6Format());
    }

    public void testConvertMicrosoftAutoRouteToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new Nmn6Format());
    }

    public void testConvertTourExchangeToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new Nmn6Format());
    }


    public void testConvertNmn7ToNmn7() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn7.freshroute", new Nmn7Format(), new Nmn7Format());
    }

    public void testConvertGpx11ToNmn7() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Nmn7Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Nmn7Format());
    }

    public void testConvertKml22BetaToNmn7() throws IOException {
        convertRoundtrip(TEST_PATH + "from22beta.kml", new Kml22BetaFormat(), new Nmn7Format());
        convertRoundtrip(TEST_PATH + "from22beta.kmz", new Kmz22BetaFormat(), new Nmn7Format());
    }

    public void testConvertCoPilotToNmn7() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new Nmn7Format());
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new Nmn7Format());
    }


    public void testConvertNavigatingPoiWarnerToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc", new NavigatingPoiWarnerFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertNavigatingPoiWarnerToNmn6Favorites() throws IOException {
        convertRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc", new NavigatingPoiWarnerFormat(), new Nmn6FavoritesFormat());
    }

    public void testConvertGpxToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new NavigatingPoiWarnerFormat());
    }


    public void testConvertNmn6FavoritesToNavigatingPOIWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn6favorites.storage", new Nmn6FavoritesFormat(), new NavigatingPoiWarnerFormat());
    }


    public void testConvertRoute66ToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new TomTomPoiFormat());
    }

    public void testConvertRoute66ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Kml22Format());
    }

    public void testConvertNationalGeographicToRoute66() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tpo", new NationalGeographicTopo3Format(), new Route66Format());
    }


    public void testConvertGlopusToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml22Format());
    }

    public void testConvertGpx11ToGlopus() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GlopusFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GlopusFormat());
    }


    public void testConvertGpsTunerToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml22BetaFormat());
    }

    public void testConvertGpsTunerToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new GarminPcx5Format());
    }

    public void testConvertKmlToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new GpsTunerFormat());
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new GpsTunerFormat());
        convertRoundtrip(TEST_PATH + "from22beta.kml", new Kml22BetaFormat(), new GpsTunerFormat());
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new GpsTunerFormat());
    }


    public void testConvertGarminPcx5ToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from-pcx5.wpt", new GarminPcx5Format(), new GarminPcx5Format());
        convertRoundtrip(TEST_PATH + "large-pcx5.wpt", new GarminPcx5Format(), new GarminPcx5Format());
    }

    public void testConvertGpx10ToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GarminPcx5Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GarminPcx5Format());
    }

    public void testConvertGpx11ToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new GarminPcx5Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GarminPcx5Format());
    }


    public void testConvertMicrosoftAutoRouteToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new GarminPcx5Format());
    }

    public void testConvertTourExchangeToGarminPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GarminPcx5Format());
    }


    public void testConvertMagellanMapSendToMagellanMapSendFails() throws IOException {
        // TODO roundtrip fails since name and description are mangled
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MagellanMapSendFormat(), new MagellanMapSendFormat());
            }
        });
    }

    public void testConvertMagellanMapSendToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MagellanMapSendFormat(), new TomTomPoiFormat());
    }

    public void testConvertGarminPcx5ToMagellanMapSend() throws IOException {
        convertRoundtrip(TEST_PATH + "from-pcx5.wpt", new GarminPcx5Format(), new MagellanMapSendFormat());
    }


    public void testConvertTomTomPoiToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ov2", new TomTomPoiFormat(), new TomTomPoiFormat());
    }


    public void testConvertNokiaLandmarkExchangeToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertOvlToNokiaLandmarkExchange() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ovl", new OvlFormat(), new NokiaLandmarkExchangeFormat());
    }


    public void testConvertAlanTrackLogToAlanTrackLog() throws Exception {
        Thread.sleep(5000); // TODO maybe this helps against the errors that only show up on complete runs
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrackLogFormat(), new AlanTrackLogFormat());
    }

    public void testConvertAlanTrackLogToGarminMapSource5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrackLogFormat(), new GarminMapSource5Format());
    }

    public void testConvertGpx10ToAlanTrackLog() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new AlanTrackLogFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new AlanTrackLogFormat());
    }

    public void testConvertAlanWaypointsAndRoutesToAlanWaypointsAndRoutes() throws IOException {
        convertRoundtrip(TEST_PATH + "from.wpr", new AlanWaypointsAndRoutesFormat(), new AlanWaypointsAndRoutesFormat());
    }

    public void testConvertAlanWaypointsAndRoutesToGarminMapSource5() throws IOException {
        // TODO fails since the Garmin Mapsource seems to capture only tracks correctly
        // TODO in routes positions with the same name have the same coordinates
        // TODO in waypoint lists positions with the same coordinates are eliminated
        // TODO Garmin file contains only 37 instead of expected 46 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.wpr", new AlanWaypointsAndRoutesFormat(), new GarminMapSource5Format());
            }
        });
    }

    public void testConvertGpx11ToAlanWaypointsAndRoutes() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new AlanWaypointsAndRoutesFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new AlanWaypointsAndRoutesFormat());
    }


    public void testConvertHaicomLoggerToHaicomLogger() throws IOException {
        convertRoundtrip(TEST_PATH + "from-haicomlogger.csv", new HaicomLoggerFormat(), new HaicomLoggerFormat());
    }

    public void testConvertHaicomLoggerToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-haicomlogger.csv", new HaicomLoggerFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-haicomlogger.csv", new HaicomLoggerFormat(), new Kml22Format());
    }

    public void testConvertNationalGeographicToHaicomLogger() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tpo", new NationalGeographicTopo3Format(), new HaicomLoggerFormat());
    }


    public void testConvertTourToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tour", new TourFormat(), new TourFormat());
    }

    public void testConvertGpx10ToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new TourFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new TourFormat());
    }

    public void testConvertGpx11ToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new TourFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new TourFormat());
    }

    public void testConvertTourToGpx10() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tour", new TourFormat(), new Gpx10Format());
    }

    public void testConvertMicrosoftAutoRouteToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new TourFormat());
    }

    public void testConvertTourToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tour", new TourFormat(), new Kml20Format());
    }

    public void testConvertTourExchangeToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new TourFormat());
    }

    public void testConvertTrainingCenterDatabaseToTrainingCenterRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from1.crs", new Tcx1Format(), new Crs1Format());
        convertRoundtrip(TEST_PATH + "from2.tcx", new Tcx2Format(), new Crs1Format());
    }

    public void testConvertGpx10ToTrainingCenterRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Crs1Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Crs1Format());
    }

    public void testConvertGpx11ToTrainingCenterRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Crs1Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Crs1Format());
    }

    public void testConvertViaMichelinToGoPal() throws IOException {
        convertRoundtrip(TEST_PATH + "from-poi.xvm", new ViaMichelinFormat(), new GoPalRouteFormat());
        convertRoundtrip(TEST_PATH + "from-itinerary.xvm", new ViaMichelinFormat(), new GoPalTrackFormat());
    }


    public void testConvertLargeTomTomRouteToSeveralTomTomRoutes() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.itn", new TomTom5RouteFormat(), new TomTom8RouteFormat());
    }

    public void testConvertLargeTomTomRouteToSeveralMTP0607s() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.itn", new TomTom5RouteFormat(), new MTP0607Format());
    }

    public void testConvertLargeMTP0607ToSeveralTomTomRoutes() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new TomTom8RouteFormat());
    }

    public void testConvertLargeMTP0607ToSeveralMTP0607s() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new MTP0607Format());
    }
}
