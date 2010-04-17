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

import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;
import slash.navigation.simple.GpsTunerFormat;

import java.io.IOException;

public class AccurracyConvertIT extends ConvertBase {

    public void testConvertColumbusV900ToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new GoPalTrackFormat());
    }

    public void testConvertColumbusV900ToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new GpsTunerFormat());
    }

    public void testConvertColumbusV900ToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new Gpx11Format());
    }

    public void testConvertColumbusV900ToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new NmeaFormat());
    }

    public void testConvertColumbusV900ToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ColumbusV900ProfessionalFormat(), new TomTom8RouteFormat());
    }


    public void testConvertGoPalTrackToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertGoPalTrackToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new GpsTunerFormat());
    }

    public void testConvertGoPalTrackToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new Gpx11Format());
    }

    public void testConvertGoPalTrackToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new NmeaFormat());
    }

    public void testConvertGoPalTrackToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gopal.trk", new GoPalTrackFormat(), new TomTom8RouteFormat());
    }


    public void testConvertGpsTunerToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertGpsTunerToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new GoPalTrackFormat());
    }

    public void testConvertGpsTunerToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new NmeaFormat());
    }

    public void testConvertGpsTunerToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new Gpx11Format());
    }

    public void testConvertGpsTunerToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-gpstuner.trk", new GpsTunerFormat(), new TomTom8RouteFormat());
    }


    public void testConvertGpxToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new ColumbusV900ProfessionalFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertGpxToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new GoPalTrackFormat());
    }

    public void testConvertGpxToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GpsTunerFormat());
    }

    public void testConvertGpxToNmea() throws IOException {
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new NmeaFormat());
    }

    public void testConvertGpxToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new TomTom5RouteFormat());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new TomTom8RouteFormat());
    }


    public void testConvertNmeaToColumbusV900() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new ColumbusV900ProfessionalFormat());
    }

    public void testConvertNmeaToGoPalTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new GoPalTrackFormat());
    }

    public void testConvertNmeaToGpsTuner() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new GpsTunerFormat());
    }

    public void testConvertNmeaToGpx() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new Gpx10Format());
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new Gpx11Format());
    }

    public void testConvertNmeaToTomTomRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from.nmea", new NmeaFormat(), new TomTom8RouteFormat());
    }


    public void testConvertLogposTrackToColumbusV900() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new ColumbusV900StandardFormat());
    }

    public void testConvertLogposTrackToGoPalTrack() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new GoPalTrackFormat());
    }

    public void testConvertLogposTrackToGpsTuner() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new GpsTunerFormat());
    }

    public void testConvertLogposTrackToGpx() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "logpos1.itn", new TomTom5RouteFormat(), new Gpx10Format());
        convertRoundtrip(SAMPLE_PATH + "logpos2.itn", new TomTom5RouteFormat(), new Gpx11Format());
    }

    public void testConvertPilogTrackToNmea() throws IOException {
        convertRoundtrip(SAMPLE_PATH + "pilog1.itn", new TomTom5RouteFormat(), new NmeaFormat());
    }
}