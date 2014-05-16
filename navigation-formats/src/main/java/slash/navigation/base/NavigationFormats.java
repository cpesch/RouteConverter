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
import slash.navigation.common.NavigationPosition;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.copilot.CoPilot9Format;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gopal.GoPal5RouteFormat;
import slash.navigation.gopal.GoPal7RouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.BrokenGpx10Format;
import slash.navigation.gpx.BrokenGpx11Format;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.klicktel.KlickTelRouteFormat;
import slash.navigation.kml.*;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.BrokenNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.*;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.*;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourFormat;
import slash.navigation.url.GoogleMapsUrlFormat;
import slash.navigation.url.MotoPlanerUrlFormat;
import slash.navigation.url.UrlFormat;
import slash.navigation.viamichelin.ViaMichelinFormat;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.wbt.WintecWbt202TesFormat;
import slash.navigation.zip.ZipFormat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static slash.common.io.Transfer.trim;

/**
 * Contains a list of all navigation formats.
 *
 * @author Christian Pesch
 */

public final class NavigationFormats {
    private static final List<Class<? extends NavigationFormat>> SUPPORTED_FORMATS = new ArrayList<Class<? extends NavigationFormat>>();

    static {
        // self-implemented formats
        addFormat(NmeaFormat.class);
        addFormat(MTP0809Format.class);
        addFormat(MTP0607Format.class);
        addFormat(TomTom8RouteFormat.class);
        addFormat(TomTom5RouteFormat.class);
        addFormat(Igo8RouteFormat.class);
        addFormat(Kml22Format.class);
        addFormat(Kmz22Format.class);
        addFormat(Kml22BetaFormat.class);
        addFormat(Kmz22BetaFormat.class);
        addFormat(Kml21Format.class);
        addFormat(Kmz21Format.class);
        addFormat(Kml20Format.class);
        addFormat(Kmz20Format.class);
        addFormat(Gpx11Format.class);
        addFormat(Gpx10Format.class);
        addFormat(Nmn7Format.class);
        addFormat(Nmn6FavoritesFormat.class);
        addFormat(Nmn6Format.class);
        addFormat(Nmn5Format.class);
        addFormat(Nmn4Format.class);
        addFormat(WebPageFormat.class);
        addFormat(GpsTunerFormat.class);
        addFormat(HaicomLoggerFormat.class);
        addFormat(CoPilot7Format.class);
        addFormat(CoPilot9Format.class);
        addFormat(CoPilot8Format.class);
        addFormat(CoPilot6Format.class);
        addFormat(Route66Format.class);
        addFormat(KompassFormat.class);
        addFormat(GlopusFormat.class);
        addFormat(ColumbusV900ProfessionalFormat.class);
        addFormat(ColumbusV900StandardFormat.class);
        addFormat(QstarzQ1000Format.class);
        addFormat(Iblue747Format.class);
        addFormat(SygicAsciiFormat.class);
        addFormat(SygicUnicodeFormat.class);
        addFormat(MagicMapsPthFormat.class);
        addFormat(GoPal7RouteFormat.class);
        addFormat(GoPal5RouteFormat.class);
        addFormat(GoPal3RouteFormat.class);
        addFormat(OvlFormat.class);
        addFormat(TourFormat.class);
        addFormat(ViaMichelinFormat.class);
        addFormat(MagicMapsIktFormat.class);
        addFormat(MagicMaps2GoFormat.class);
        addFormat(MagellanExploristFormat.class);
        addFormat(MagellanRouteFormat.class);
        addFormat(Tcx2Format.class);
        addFormat(Tcx1Format.class);
        addFormat(NokiaLandmarkExchangeFormat.class);
        addFormat(KlickTelRouteFormat.class);
        addFormat(GarminFlightPlanFormat.class);
        addFormat(WintecWbt201Tk1Format.class);
        addFormat(WintecWbt201Tk2Format.class);
        addFormat(NavilinkFormat.class);
        addFormat(GoRiderGpsFormat.class);
        addFormat(KienzleGpsFormat.class);
        addFormat(GroundTrackFormat.class);
        addFormat(OpelNaviFormat.class);
        addFormat(NavigatingPoiWarnerFormat.class);
        addFormat(NmnRouteFormat.class);
        addFormat(ApeMapFormat.class);
        addFormat(ZipFormat.class);

        // GPSBabel-based formats
        addFormat(GarminMapSource6Format.class);
        addFormat(GarminMapSource5Format.class);
        addFormat(MicrosoftAutoRouteFormat.class);
        addFormat(TourExchangeFormat.class);
        addFormat(NationalGeographicTopo3Format.class);
        addFormat(MagellanMapSendFormat.class);
        addFormat(AlanTrackLogFormat.class);
        addFormat(AlanWaypointsAndRoutesFormat.class);
        addFormat(OziExplorerRouteFormat.class);
        addFormat(OziExplorerTrackFormat.class);
        addFormat(OziExplorerWaypointFormat.class);
        addFormat(CompeGPSDataRouteFormat.class);
        addFormat(CompeGPSDataTrackFormat.class);
        addFormat(CompeGPSDataWaypointFormat.class);
        addFormat(GarminPcx5Format.class);
        addFormat(GeoCachingFormat.class);
        addFormat(GoPalTrackFormat.class);
        addFormat(TomTomPoiFormat.class);
        addFormat(HoluxM241BinaryFormat.class);
        addFormat(FlightRecorderDataFormat.class);
        addFormat(GarminFitFormat.class);
        addFormat(WintecWbt202TesFormat.class);

        addFormat(UrlFormat.class);
        addFormat(NmnUrlFormat.class);
        addFormat(GoogleMapsUrlFormat.class);
        addFormat(MotoPlanerUrlFormat.class);

        // second try for broken files
        addFormat(BrokenColumbusV900StandardFormat.class);
        addFormat(BrokenNmeaFormat.class);
        addFormat(BrokenHaicomLoggerFormat.class);
        addFormat(BrokenGpx10Format.class);
        addFormat(BrokenGpx11Format.class);
        addFormat(BrokenKml21Format.class);
        addFormat(BrokenKml21LittleEndianFormat.class);
        addFormat(BrokenKmz21Format.class);
        addFormat(BrokenKmz21LittleEndianFormat.class);
        addFormat(BrokenKml22BetaFormat.class);
        addFormat(BrokenKml22Format.class);
        addFormat(BrokenNavilinkFormat.class);

        // greedy BabelFormats
        addFormat(GarminPoiFormat.class);
        addFormat(Igo8TrackFormat.class);
        addFormat(GarminPoiDbFormat.class);
    }

    public static void addFormat(Class<? extends NavigationFormat> format) {
        SUPPORTED_FORMATS.add(format);
    }

    private static List<NavigationFormat> getFormatInstances(boolean includeReadableFormats, boolean includeWritableFormats) {
        List<NavigationFormat> formats = new ArrayList<NavigationFormat>();
        for (Class<? extends NavigationFormat> formatClass : SUPPORTED_FORMATS) {
            try {
                NavigationFormat format = formatClass.newInstance();
                if (includeReadableFormats && format.isSupportsReading() ||
                        includeWritableFormats && format.isSupportsWriting())
                    formats.add(format);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate " + formatClass, e);
            }
        }
        return formats;
    }

    public static List<NavigationFormat> getReadFormats() {
        return getFormatInstances(true, false);
    }

    public static List<NavigationFormat> getWriteFormats() {
        return getFormatInstances(false, true);
    }

    private static List<NavigationFormat> sortByName(List<NavigationFormat> formats) {
        NavigationFormat[] formatsArray = formats.toArray(new NavigationFormat[formats.size()]);
        sort(formatsArray, new Comparator<NavigationFormat>() {
            public int compare(NavigationFormat f1, NavigationFormat f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });
        return asList(formatsArray);
    }

    public static List<NavigationFormat> getFormatsSortedByName() {
        return sortByName(getFormatInstances(true, true));
    }

    public static List<NavigationFormat> getReadFormatsSortedByName() {
        return sortByName(getReadFormats());
    }

    public static List<NavigationFormat> getWriteFormatsSortedByName() {
        return sortByName(getWriteFormats());
    }

    public static List<NavigationFormat> getReadFormatsPreferredByExtension(String preferredExtension) {
        List<NavigationFormat> preferredFormats = new ArrayList<NavigationFormat>();
        for(NavigationFormat format : getReadFormats()) {
            if(format.getExtension().equals(preferredExtension))
                preferredFormats.add(format);
        }

        List<NavigationFormat> result = new ArrayList<NavigationFormat>(getReadFormats());
        result.removeAll(preferredFormats);
        result.addAll(0, preferredFormats);
        return result;
    }

    public static List<NavigationFormat> getReadFormatsWithPreferredFormat(NavigationFormat preferredFormat) {
        List<NavigationFormat> formats = new ArrayList<NavigationFormat>(getReadFormats());
        if (preferredFormat != null) {
            formats.remove(preferredFormat);
            formats.add(0, preferredFormat);
        }
        return formats;
    }

    public static List<NavigationFormat> getWriteFormatsWithPreferredFormats(List<NavigationFormat> preferredFormats) {
        List<NavigationFormat> formats = new ArrayList<NavigationFormat>(getWriteFormatsSortedByName());
        formats.removeAll(preferredFormats);
        formats.addAll(0, preferredFormats);
        return formats;
    }


    private static String removeDigits(String string) {
        StringBuilder buffer = new StringBuilder(string);
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (Character.isDigit(c)) {
                buffer.deleteCharAt(i);
                i--;
            }
        }
        return buffer.toString();
    }

    /* package local for tests */static BaseNavigationPosition asFormat(NavigationPosition position, NavigationFormat format) throws IOException {
        BaseNavigationPosition result;
        String formatName = getFormatName(format);
        formatName = formatName.replace("Format", "Position");
        formatName = removeDigits(formatName);
        try {
            Method method = position.getClass().getMethod("as" + formatName, new Class[0]);
            result = (BaseNavigationPosition) method.invoke(position);
        } catch (Exception e) {
            throw new IOException("Cannot call as" + formatName + "() on " + position, e);
        }
        return result;
    }

    public static List<BaseNavigationPosition> asFormatForPositions(List<NavigationPosition> positions, NavigationFormat format) throws IOException {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>(positions.size());
        for (NavigationPosition position : positions) {
            result.add(asFormat(position, format));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static BaseRoute<BaseNavigationPosition, BaseNavigationFormat> asFormat(BaseRoute route, NavigationFormat format) throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> result;
        String formatName = getFormatName(format);
        try {
            Method method = route.getClass().getMethod("as" + formatName, new Class[0]);
            result = (BaseRoute<BaseNavigationPosition, BaseNavigationFormat>) method.invoke(route);
        } catch (Exception e) {
            throw new IOException("Cannot call as" + formatName + "() on " + route, e);
        }
        return result;
    }

    public static List<BaseRoute> asFormatForRoutes(List<BaseRoute> routes, NavigationFormat format) throws IOException {
        List<BaseRoute> result = new ArrayList<BaseRoute>(routes.size());
        for (BaseRoute route : routes) {
            result.add(asFormat(route, format));
        }
        return result;
    }

    private static String getFormatName(NavigationFormat format) {
        Class<? extends NavigationFormat> formatClass = format.getClass();
        String formatName = formatClass.getSimpleName();
        if (trim(formatName) == null && formatClass.getSuperclass() != null)
            formatName = formatClass.getSuperclass().getSimpleName();
        // shortcut to prevent lots of as... methods
        if (format instanceof BabelFormat)
            formatName = "Gpx10Format";
        if (formatName.startsWith("Broken"))
            formatName = formatName.substring(6);
        formatName = formatName.replaceAll("LittleEndian", "");
        return formatName;
    }
}
