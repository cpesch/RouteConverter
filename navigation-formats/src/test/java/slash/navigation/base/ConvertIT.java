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

import slash.navigation.babel.*;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gopal.GoPal5RouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.klicktel.KlickTelRouteFormat;
import slash.navigation.kml.*;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourFormat;
import slash.navigation.viamichelin.ViaMichelinFormat;

import java.io.IOException;

public class ConvertIT extends ConvertBase {

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
        convertRoundtrip(TEST_PATH + "from-columbusv900-standard.csv", new ColumbusV900StandardFormat(), new CoPilot6Format());
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new CoPilot7Format());
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new CoPilot8Format());
    }

    public void testConvertiBlue747ToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new CoPilot6Format());
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new CoPilot7Format());
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new CoPilot8Format());
    }

    public void testConvertQstarzQ1000ToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new CoPilot7Format());
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new CoPilot7Format());
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new CoPilot8Format());
    }

    public void testConvertQstarzQ1000ToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new ColumbusV900StandardFormat());
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertQstarzQ1000ToiBlue747() throws IOException {
        convertRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new QstarzQ1000Format(), new iBlue747Format());
    }

    public void testConvertiBlue747ToQstarzQ1000() throws IOException {
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new QstarzQ1000Format());
    }

    public void testConvertiBlue747ToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new ColumbusV900StandardFormat());
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertiBlue747ToCompeGPSData() throws IOException {
        convertRoundtrip(TEST_PATH + "from-iblue747.csv", new iBlue747Format(), new CompeGPSDataFormat());
    }


    public void testConvertCompeGPSDataToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-compegps.trk", new CompeGPSDataFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-compegps.wpt", new CompeGPSDataFormat(), new Gpx11Format());
    }

    public void testConvertCompeGPSDataToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-compegps.trk", new CompeGPSDataFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-compegps.wpt", new CompeGPSDataFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-compegps.trk", new CompeGPSDataFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-compegps.wpt", new CompeGPSDataFormat(), new Kml22Format());
    }


    public void testConvertCoPilotToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-copilot6.trp", new CoPilot6Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-copilot7.trp", new CoPilot7Format(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from-copilot8.trp", new CoPilot8Format(), new Gpx11Format());
    }

    public void testConvertCoPilotToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from-copilot6.trp", new CoPilot6Format(), new ColumbusV900StandardFormat());
        convertRoundtrip(TEST_PATH + "from-copilot6.trp", new CoPilot6Format(), new ColumbusV900ProfessionalFormat());
        convertRoundtrip(TEST_PATH + "from-copilot7.trp", new CoPilot7Format(), new ColumbusV900StandardFormat());
        convertRoundtrip(TEST_PATH + "from-copilot7.trp", new CoPilot7Format(), new ColumbusV900ProfessionalFormat());
        convertRoundtrip(TEST_PATH + "from-copilot8.trp", new CoPilot8Format(), new ColumbusV900StandardFormat());
        convertRoundtrip(TEST_PATH + "from-copilot8.trp", new CoPilot8Format(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertCoPilotToNmn7() throws IOException {
        convertRoundtrip(TEST_PATH + "from-copilot6.trp", new CoPilot6Format(), new Nmn7Format());
        convertRoundtrip(TEST_PATH + "from-copilot7.trp", new CoPilot7Format(), new Nmn7Format());
        convertRoundtrip(TEST_PATH + "from-copilot8.trp", new CoPilot8Format(), new Nmn7Format());
    }

    public void testConvertCoPilotToCoPilot() throws IOException {
        convertRoundtrip(TEST_PATH + "from-copilot6.trp", new CoPilot6Format(), new CoPilot7Format());
        convertRoundtrip(TEST_PATH + "from-copilot7.trp", new CoPilot7Format(), new CoPilot6Format());
        convertRoundtrip(TEST_PATH + "from-copilot8.trp", new CoPilot8Format(), new CoPilot6Format());
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


    public void testConvertFlightRecorderDataToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Gpx11Format());
    }

    public void testConvertFlightRecorderDataToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from.igc", new FlightRecorderDataFormat(), new Kml22Format());
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

    public void testConvertGroundTrackToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-groundtrack.txt", new GroundTrackFormat(), new Gpx10Format());
        convertRoundtrip(SAMPLE_PATH + "groundtrack2011012806Z_F1620229.txt", new GroundTrackFormat(), new Gpx10Format());
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

    public void testConvertOziExplorerWaypointToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerReadFormat(), new OvlFormat());
    }

    public void testConvertGpx10ToOziExplorerTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerTrackFormat());
    }

    public void testConvertGpx10ToOziExplorerWaypoints() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new OziExplorerWaypointFormat());
    }


    public void testConvertGoPalRouteToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal3.xml", new GoPal3RouteFormat(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from-gopal5.xml", new GoPal5RouteFormat(), new Gpx11Format());
    }

    public void testConvertGoPalRouteToGoPalRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal3.xml", new GoPal3RouteFormat(), new GoPal3RouteFormat());
        convertRoundtrip(TEST_PATH + "from-gopal5.xml", new GoPal5RouteFormat(), new GoPal3RouteFormat());
    }

    public void testConvertTourExchangeToGoPalRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GoPal3RouteFormat());
    }


    public void testConvertTourExchangeToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GoPalTrackFormat());
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

    public void testConvertNavigatingPoiWarnerToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc", new NavigatingPoiWarnerFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertNavigatingPoiWarnerToNmn6Favorites() throws IOException {
        convertRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc", new NavigatingPoiWarnerFormat(), new Nmn6FavoritesFormat());
    }

    public void testConvertGpxToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new NavigatingPoiWarnerFormat());
    }


    public void testConvertNavilinkToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-navilink.sbp", new NavilinkFormat(), new NavigatingPoiWarnerFormat());
    }


    public void testConvertNmn6FavoritesToNavigatingPOIWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn6favorites.storage", new Nmn6FavoritesFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertNmnUrlToNavigatingPOIWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-nmn.txt", new NmnUrlFormat(), new NavigatingPoiWarnerFormat());
    }


    public void testConvertOpelNaviToNavigatingPOIWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-opelnavi.poi", new OpelNaviFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertOpelNaviToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-opelnavi.poi", new OpelNaviFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-opelnavi.poi", new OpelNaviFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-opelnavi.poi", new OpelNaviFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-opelnavi.poi", new OpelNaviFormat(), new Kml22Format());
    }

    public void testConvertNationalGeographicToOpelNavi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.tpo", new NationalGeographicTopo3Format(), new OpelNaviFormat());
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


    public void testConvertSygicToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-sygic-ascii.txt", new SygicAsciiFormat(), new Kml22Format());
        convertRoundtrip(TEST_PATH + "from-sygic-unicode.txt", new SygicUnicodeFormat(), new Kml20Format());
    }

    public void testConvertRoute66ToSygic() throws IOException {
        convertRoundtrip(TEST_PATH + "from-route66poi.csv", new Route66Format(), new SygicUnicodeFormat());
    }

    public void testConvertTomTomPoiToSygic() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ov2", new TomTomPoiFormat(), new SygicUnicodeFormat());
    }


    public void testConvertGlopusToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-glopus.tk", new GlopusFormat(), new Kml22Format());
    }

    public void testConvertGpx10ToGlopus() throws IOException {
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


    public void testConvertMagellanMapSendToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MagellanMapSendFormat(), new TomTomPoiFormat());
    }

    public void testConvertGarminPcx5ToMagellanMapSend() throws IOException {
        convertRoundtrip(TEST_PATH + "from-pcx5.wpt", new GarminPcx5Format(), new MagellanMapSendFormat());
    }


    public void testConvertTomTomPoiToTomTomPoi() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ov2", new TomTomPoiFormat(), new TomTomPoiFormat());
    }


    public void testConvertNokiaLandmarkExchangeToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.lmx", new NokiaLandmarkExchangeFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.lmx", new NokiaLandmarkExchangeFormat(), new Gpx11Format());
    }

    public void testConvertNokiaLandmarkExchangeUtf8ToNavigatingPoiWarnerIso88591Fails() throws IOException {
        // source is UTF8 while target is only ISO8859-1
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
            }
        });
    }

    public void testConvertNokiaLandmarkExchangeUtf8ToNavigatingPoiWarnerIso88591() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "file1.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
        convertRoundtrip(SAMPLE_PATH + "file2.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
        convertRoundtrip(SAMPLE_PATH + "file3.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
    }

    public void testConvertOvlToNokiaLandmarkExchange() throws IOException {
        convertRoundtrip(TEST_PATH + "from.ovl", new OvlFormat(), new NokiaLandmarkExchangeFormat());
    }

    public void testConvertViaMichelinToNokiaLandmarkExchange() throws IOException {
        convertRoundtrip(TEST_PATH + "from-poi.xvm", new ViaMichelinFormat(), new NokiaLandmarkExchangeFormat());
        convertRoundtrip(TEST_PATH + "from-itinerary.xvm", new ViaMichelinFormat(), new NokiaLandmarkExchangeFormat());
    }


    public void testConvertAlanTrackLogToAlanTrackLog() throws Exception {
        Thread.sleep(5000); // this seems to help against the errors that only show up on complete runs
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrackLogFormat(), new AlanTrackLogFormat());
    }

    public void testConvertAlanTrackLogToGarminMapSource5() throws Exception {
        Thread.sleep(5000); // this seems to help against the errors that only show up on complete runs
        convertRoundtrip(TEST_PATH + "from.trl", new AlanTrackLogFormat(), new GarminMapSource5Format());
    }

    public void testConvertGpx10ToAlanTrackLog() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new AlanTrackLogFormat());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new AlanTrackLogFormat());
    }

    public void testConvertAlanWaypointsAndRoutesToAlanWaypointsAndRoutes() throws IOException {
        convertRoundtrip(TEST_PATH + "from.wpr", new AlanWaypointsAndRoutesFormat(), new AlanWaypointsAndRoutesFormat());
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


    public void testConvertKompassToKml() throws IOException {
        convertRoundtrip(TEST_PATH + "from-kompass.tk", new KompassFormat(), new Kml20Format());
        convertRoundtrip(TEST_PATH + "from-kompass.tk", new KompassFormat(), new Kml21Format());
        convertRoundtrip(TEST_PATH + "from-kompass.tk", new KompassFormat(), new Kml22BetaFormat());
        convertRoundtrip(TEST_PATH + "from-kompass.tk", new KompassFormat(), new Kml22Format());
    }

    public void testConvertGpx11ToKompass() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new KompassFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new KompassFormat());
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
        convertRoundtrip(TEST_PATH + "from1.crs", new Tcx1Format(), new Tcx2Format());
        convertRoundtrip(TEST_PATH + "from2.tcx", new Tcx2Format(), new Tcx1Format());
    }

    public void testConvertGpx10ToTrainingCenter1Route() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Tcx1Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Tcx1Format());
    }

    public void testConvertGpx10ToTrainingCenter2Route() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new Tcx2Format());
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new Tcx2Format());
    }

    public void testConvertGpx11ToTrainingCenter1Route() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Tcx1Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Tcx1Format());
    }

    public void testConvertGpx11ToTrainingCenter2Route() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new Tcx2Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new Tcx2Format());
    }

    public void testConvertViaMichelinToGoPal() throws IOException {
        convertRoundtrip(TEST_PATH + "from-poi.xvm", new ViaMichelinFormat(), new GoPal3RouteFormat());
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
