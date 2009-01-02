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
    along with Foobar; if not, write to the Free Software
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
import slash.navigation.itn.ItnFormat;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22Format;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
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
        File source = new File(testFileName);
        assertTrue(parser.read(source));
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

    private void convertSingleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, BaseRoute sourceRoute) throws IOException {
        File target = File.createTempFile("target", targetFormat.getExtension());
        parser.write(sourceRoute, targetFormat, false, false, false, target);
        assertTrue(target.exists());

        NavigationFileParser sourceParser = new NavigationFileParser();
        assertTrue(sourceParser.read(source));
        NavigationFileParser targetParser = new NavigationFileParser();
        assertTrue(targetParser.read(target));

        if (targetFormat instanceof OziExplorerWriteFormat)
            targetFormat = new OziExplorerReadFormat();

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
    }

    private void convertMultipleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, List<BaseRoute> sourceRoutes) throws IOException {
        File target = File.createTempFile("target", targetFormat.getExtension());
        parser.write(sourceRoutes, (MultipleRoutesFormat) targetFormat, target);
        assertTrue(target.exists());

        NavigationFileParser sourceParser = new NavigationFileParser();
        assertTrue(sourceParser.read(source));
        NavigationFileParser targetParser = new NavigationFileParser();
        assertTrue(targetParser.read(target));

        if (targetFormat instanceof OziExplorerWriteFormat)
            targetFormat = new OziExplorerReadFormat();

        assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
        assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
        assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
        assertEquals(targetFormat.getName(), targetParser.getFormat().getName());

        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetParser.getTheRoute();
        compareRouteMetaData(sourceParser.getTheRoute(), targetRoute);

        for (int i = 0; i < targetParser.getAllRoutes().size(); i++) {
            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = targetParser.getAllRoutes().get(i);
            compareRouteMetaData(sourceParser.getTheRoute(), route);
            comparePositions(sourceRoutes.get(i), sourceFormat, route, targetFormat, targetParser.getAllRoutes().size() > 1);
        }

        assertTrue(target.exists());
        assertTrue(target.delete());
    }

    void largeConvertRoundtrip(String testFileName,
                               BaseNavigationFormat sourceFormat,
                               BaseNavigationFormat targetFormat) throws IOException {
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
        assertEquals(fileCount, parser.getNumberOfFilesToWriteFor(sourceRoute, targetFormat, false));

        File[] targets = new File[fileCount];
        for (int i = 0; i < targets.length; i++)
            targets[i] = File.createTempFile("target", targetFormat.getExtension());
        parser.write(sourceRoute, targetFormat, false, false, false, targets);

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
                    targetParser.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount, false, false, false, targetParser.getTheRoute().getCharacteristics());
        }

        for (File target : targets)
            assertTrue(target.delete());
    }

    public void testConvertMTP0607ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new MTP0607Format());
    }

    public void testConvertMTP0809ToMTP0809() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new MTP0809Format());
    }

    public void testConvertMTP0607ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new ItnFormat());
    }

    public void testConvertGpxToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new ItnFormat());
    }

    public void testConvertMTP0607ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new Kml22Format());
    }

    public void testConvertMTP0607ToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0607.bcr", new MTP0607Format(), new CoPilot6Format());
    }

    public void testConvertMTP0809ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-mtp0809.bcr", new MTP0809Format(), new Kml22Format());
    }


    public void testConvertCoPilot6ToCoPilot6() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new CoPilot6Format());
    }

    public void testConvertCoPilot6ToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from6.trp", new CoPilot6Format(), new Gpx11Format());
    }

    public void testConvertCoPilot7ToCoPilot7() throws IOException {
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new CoPilot7Format());
    }

    public void testConvertCoPilot7ToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from7.trp", new CoPilot7Format(), new Gpx11Format());
    }


    public void testConvertItnToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new ItnFormat());
    }

    public void testConvertItnToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new MTP0607Format());
    }

    public void testConvertItnToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new Kml22Format());
    }


    public void testConvertGoogleMapsToGoogleMaps() throws IOException {
        convertRoundtrip(TEST_PATH + "from.url", new GoogleMapsFormat(), new GoogleMapsFormat());
    }

    public void testConvertGoogleMapsToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from.url", new GoogleMapsFormat(), new ItnFormat());
    }


    public void testConvertGdbToGdb() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gdb", new GdbFormat(), new GdbFormat());
        convertRoundtrip(TEST_PATH + "large.gdb", new GdbFormat(), new GdbFormat());
    }

    public void testConvertGdbToKmlFails() throws IOException {
        // TODO positions with the same coordinates are not saved separately within GDB files
        convertRoundtrip(TEST_PATH + "from.gdb", new GdbFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.gdb", new GdbFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.gdb", new GdbFormat(), new Kml22Format());
    }

    public void testConvertGpx10ToGdb() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GdbFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GdbFormat());
    }

    public void testConvertGpx11ToGdb() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new GdbFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GdbFormat());
    }

    public void testConvertAxeToGdbFails() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new GdbFormat());
    }

    public void testConvertTefToGdb() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new GdbFormat());
    }


    public void testConvertGpiToGpi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gpi", new GarminPoiFormat(), new GarminPoiFormat());
    }

    public void testConvertGpiToXcsv() throws IOException {
        convertRoundtrip(TEST_PATH + "from.gpi", new GarminPoiFormat(), new GarminPoiDbFormat());
    }


    public void testConvertXcsvToXcvs() throws IOException {
        convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiDbFormat());
    }

    public void testConvertXcsvToGpi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiFormat());
    }


    public void testConvertMpsToMps() throws IOException {
        convertRoundtrip(TEST_PATH + "from.mps", new MpsFormat(), new MpsFormat());
        convertRoundtrip(TEST_PATH + "large.mps", new MpsFormat(), new MpsFormat());
    }

    public void testConvertGpx10ToMps() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new MpsFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new MpsFormat());
    }

    public void testConvertGpx11ToMps() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new MpsFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new MpsFormat());
    }

    public void testConvertAxeToMpsFails() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new MpsFormat());
    }

    public void testConvertTefToMps() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new MpsFormat());
    }


    public void testConvertGpx10ToGpx10() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Gpx10Format());
    }

    public void testConvertGpx10ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new ItnFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new ItnFormat());
    }

    public void testConvertGpx10ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new MTP0607Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new MTP0607Format());
    }

    public void testConvertGpx11ToGpx11() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Gpx11Format());
    }

    public void testConvertGpx11ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new ItnFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new ItnFormat());
    }

    public void testConvertGpx11ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new MTP0607Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new MTP0607Format());
    }


    public void testConvertKml20ToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new Kml20Format());
    }

    public void testConvertKml20ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new ItnFormat());
    }

    public void testConvertKml20ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new MTP0607Format());
    }

    public void testConvertKml21ToKml21() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new Kml21Format());
    }

    public void testConvertKml21ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new ItnFormat());
    }

    public void testConvertKml21ToMTP0607() throws IOException {
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new MTP0607Format());
    }

    public void testConvertKml22ToKml22() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new Kml22Format());
    }

    public void testConvertKml22ToItn() throws IOException {
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new ItnFormat());
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

    public void testConvertItnToMagicMapsPth() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new MagicMapsPthFormat());
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

    public void testConvertItnToMagicMapsIkt() throws IOException {
        convertRoundtrip(TEST_PATH + "from.itn", new ItnFormat(), new MagicMapsIktFormat());
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

    public void testConvertOziExplorerTrackToTop50Fails() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new OvlFormat());
    }

    public void testConvertOziExplorerWaypointToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new OvlFormat());
    }

    public void testConvertOziExplorerToMagicMapsFails() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new MagicMapsIktFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerReadFormat(), new MagicMapsPthFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new MagicMapsIktFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerReadFormat(), new MagicMapsPthFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new MagicMapsIktFormat());
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new MagicMapsPthFormat());
    }

    public void testConvertGpx10ToOziExplorerTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerTrackFormat());
    }

    public void testConvertGpx10ToOziExplorerRouteFails() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerRouteFormat());
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

    public void testConvertTefToGoPalRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new GoPalRouteFormat());
    }


    public void testConvertGoPalTrackToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx11Format());
    }

    public void testConvertGoPalTrackToGoPal() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new GoPalTrackFormat());
    }

    public void testConvertTefToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new GoPalTrackFormat());
    }


    public void testConvertGpxToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new NmeaFormat());
    }

    public void testConvertGpxToMagellanExplorist() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new MagellanExploristFormat());
    }

    public void testConvertMagellanExploristToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.log", new MagellanExploristFormat(), new Gpx10Format());
    }

    public void testConvertMagellanExploristToMagellanExplorist() throws IOException {
        convertRoundtrip(TEST_PATH + "from.log", new MagellanExploristFormat(), new MagellanExploristFormat());
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

    public void testConvertAxeToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new Nmn4Format());
    }

    public void testConvertNmn4ToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn4.rte", new Nmn4Format(), new Kml20Format());
    }

    public void testConvertTefToNmn4() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new Nmn4Format());
    }


    public void testConvertNmn5ToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn5.rte", new Nmn5Format(), new Nmn5Format());
        convertRoundtrip(TEST_PATH + "large-nmn5.rte", new Nmn5Format(), new Nmn5Format());
    }

    public void testConvertGpx10ToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Nmn5Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Nmn5Format());
    }

    public void testConvertGpx11ToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Nmn5Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Nmn5Format());
    }

    public void testConvertAxeToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new Nmn5Format());
    }

    public void testConvertTefToNmn5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new Nmn5Format());
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

    public void testConvertAxeToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new Nmn6Format());
    }

    public void testConvertTefToNmn6() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new Nmn6Format());
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


    public void testConvertRoute66ToRoute66() throws IOException {
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Route66Format());
    }

    public void testConvertRoute66ToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new Kml22Format());
    }

    public void testConvertNationalGeographicToRoute66() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tpo", new NationalGeographicTopo3Format(), new Route66Format());
    }


    public void testConvertGlopusToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml22Format());
    }

    public void testConvertGpx11ToGlopus() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new GlopusFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GlopusFormat());
    }


    public void testConvertGpsTunerToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Kml22Format());
    }

    public void testConvertGpsTunerToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new PcxFormat());
    }

    public void testConvertKmlToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from20.kml", new Kml20Format(), new GpsTunerFormat());
        convertRoundtrip(TEST_PATH + "from21.kml", new Kml21Format(), new GpsTunerFormat());
        convertRoundtrip(TEST_PATH + "from22.kml", new Kml22Format(), new GpsTunerFormat());
    }


    public void testConvertPcx5ToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from-pcx5.wpt", new PcxFormat(), new PcxFormat());
        convertRoundtrip(TEST_PATH + "large-pcx5.wpt", new PcxFormat(), new PcxFormat());
    }

    public void testConvertGpx10ToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new PcxFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new PcxFormat());
    }

    public void testConvertGpx11ToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new PcxFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new PcxFormat());
    }


    public void testConvertAxeToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new PcxFormat());
    }

    public void testConvertTefToPcx5() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new PcxFormat());
    }


    public void testConvertMapSendToMapSend() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MapSendFormat(), new MapSendFormat());
    }

    public void testConvertMapSendToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MapSendFormat(), new TomTomPoiFormat());
    }

    public void testConvertPcx5ToMapSend() throws IOException {
        convertRoundtrip(TEST_PATH + "from-pcx5.wpt", new PcxFormat(), new MapSendFormat());
    }


    public void testConvertTomTomPoiToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ov2", new TomTomPoiFormat(), new TomTomPoiFormat());
    }


    public void testConvertAlanTrlToAlanTrl() throws IOException {
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrlFormat(), new AlanTrlFormat());
    }

    public void testConvertAlanTrlToGdb() throws IOException {
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrlFormat(), new GdbFormat());
    }

    public void testConvertGpx10ToAlanTrl() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new AlanTrlFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new AlanTrlFormat());
    }

    public void testConvertAlanWprToAlanWpr() throws IOException {
        convertRoundtrip(TEST_PATH + "from.wpr", new AlanWprFormat(), new AlanWprFormat());
    }

    public void testConvertAlanWprToMpsFails() throws IOException {
        // TODO fails since the Garmin Mapsource seems to capture only tracks correctly
        // TODO in routes positions with the same name have the same coordinates
        // TODO in waypoint lists positions with the same coordinates are eliminated
        convertRoundtrip(TEST_PATH + "from.wpr", new AlanWprFormat(), new MpsFormat());
    }

    public void testConvertGpx11ToAlanWpr() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new AlanWprFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new AlanWprFormat());
    }


    /*
    public void testConvertHaicomLoggerToHaicomLogger() throws IOException {
        convertRoundtrip(TEST_PATH + "from-haicomlogger.csv", new HaicomLoggerFormat(), new HaicomLoggerFormat());
    }

    public void testConvertHaicomLoggerToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-haicomlogger.csv", new HaicomLoggerFormat(), new Kml22Format());
    }

    public void testConvertNationalGeographicToHaicomLogger() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tpo", new NationalGeographicTopo3Format(), new HaicomLoggerFormat());
    }
    */


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

    public void testConvertAxeToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from.axe", new AxeFormat(), new TourFormat());
    }

    public void testConvertTourToKml20() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tour", new TourFormat(), new Kml20Format());
    }

    public void testConvertTefToTour() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TefFormat(), new TourFormat());
    }


    public void testConvertViaMichelinToGoPal() throws IOException {
        convertRoundtrip(TEST_PATH + "from-poi.xvm", new ViaMichelinFormat(), new GoPalRouteFormat());
        convertRoundtrip(TEST_PATH + "from-itinerary.xvm", new ViaMichelinFormat(), new GoPalTrackFormat());
    }


    public void testConvertLargeItnToSeveralItns() throws IOException {
        largeConvertRoundtrip(TEST_PATH + "large.itn", new ItnFormat(), new ItnFormat());
    }

    public void testConvertLargeItnToSeveralMTP0607s() throws IOException {
        largeConvertRoundtrip(TEST_PATH + "large.itn", new ItnFormat(), new MTP0607Format());
    }

    public void testConvertLargeMTP0607ToSeveralItns() throws IOException {
        largeConvertRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new ItnFormat());
    }

    public void testConvertLargeMTP0607ToSeveralMTP0607s() throws IOException {
        largeConvertRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new MTP0607Format());
    }
}
