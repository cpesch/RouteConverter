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

import org.junit.Test;
import slash.navigation.columbus.ColumbusGpsBinaryFormat;
import slash.navigation.columbus.ColumbusGpsType1Format;
import slash.navigation.columbus.ColumbusGpsType2Format;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom95RouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.simple.GpsTunerFormat;

import java.io.IOException;

import static slash.navigation.base.ConvertBase.convertRoundtrip;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class AccurracyConvertIT {

    @Test
    public void testConvertColumbusGpsToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new GoPalTrackFormat());
        convertRoundtrip(TEST_PATH + "from-columbusv1000-type2.csv", new ColumbusGpsType2Format(), new GoPalTrackFormat());
    }

    @Test
    public void testConvertColumbusGpsProfessionalToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new GpsTunerFormat());
    }

    @Test
    public void testConvertColumbusGpsToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from-columbusv1000-type2.csv", new ColumbusGpsType2Format(), new Gpx11Format());
        convertRoundtrip(TEST_PATH + "from-columbusv1000-binary.gps", new ColumbusGpsBinaryFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertColumbusGpsProfessionalToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new NmeaFormat());
    }

    @Test
    public void testConvertColumbusGpBinaryToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv1000-binary.gps", new ColumbusGpsBinaryFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertColumbusGpsProfessionalToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusGpsType1Format(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertGoPalTrackToColumbusGps() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new ColumbusGpsType1Format());
    }

    @Test
    public void testConvertGoPalTrackToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new GpsTunerFormat());
    }

    @Test
    public void testConvertGoPalTrackToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertGoPalTrackToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new NmeaFormat());
    }

    @Test
    public void testConvertGoPalTrackToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertGpsTunerToColumbusGps() throws Exception {
        ignoreLocalTimeZone(() -> {
            convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new ColumbusGpsType1Format());
            convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new ColumbusGpsType2Format());
        });
    }

    @Test
    public void testConvertGpsTunerToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new GoPalTrackFormat());
    }

    @Test
    public void testConvertGpsTunerToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new NmeaFormat());
    }

    @Test
    public void testConvertGpsTunerToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertGpsTunerToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertGpxToColumbusGps() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new ColumbusGpsType1Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new ColumbusGpsType1Format());
    }

    @Test
    public void testConvertGpxToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GoPalTrackFormat());
    }

    @Test
    public void testConvertGpxToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GpsTunerFormat());
    }

    @Test
    public void testConvertGpxToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new NmeaFormat());
    }

    @Test
    public void testConvertGpxToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new TomTom5RouteFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertNmeaToColumbusGps() throws Exception {
        ignoreLocalTimeZone(() -> {
            convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new ColumbusGpsType1Format());
            convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new ColumbusGpsType2Format());
        });
    }

    @Test
    public void testConvertNmeaToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new GoPalTrackFormat());
    }

    @Test
    public void testConvertNmeaToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new GpsTunerFormat());
    }

    @Test
    public void testConvertNmeaToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertNmeaToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertLogposTrackToColumbusGps() throws Exception {
        ignoreLocalTimeZone(() -> {
            convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new ColumbusGpsType1Format());
            convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new ColumbusGpsType2Format());
        });
    }

    @Test
    public void testConvertLogposTrackToGoPalTrack() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new GoPalTrackFormat());
    }

    @Test
    public void testConvertLogposTrackToGpsTuner() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new GpsTunerFormat());
    }

    @Test
    public void testConvertLogposTrackToGpx() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new Gpx10Format());
        convertRoundtrip(SAMPLE_PATH + "logpos2.itn", new TomTom5RouteFormat(), new Gpx11Format());
    }

    @Test
    public void testConvertPilogTrackToNmea() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "pilog1.itn", new TomTom5RouteFormat(), new NmeaFormat());
    }
}