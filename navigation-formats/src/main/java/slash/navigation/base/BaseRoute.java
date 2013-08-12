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

import slash.common.type.CompactCalendar;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.copilot.CoPilot9Format;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.fpl.GarminFlightPlanPosition;
import slash.navigation.fpl.GarminFlightPlanRoute;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gopal.GoPal5RouteFormat;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.klicktel.KlickTelRouteFormat;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22BetaFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.kml.KmlRoute;
import slash.navigation.kml.Kmz20Format;
import slash.navigation.kml.Kmz21Format;
import slash.navigation.kml.Kmz22BetaFormat;
import slash.navigation.kml.Kmz22Format;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.lmx.NokiaLandmarkExchangeRoute;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6FavoritesFormat;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.nmn.NmnRouteFormat;
import slash.navigation.nmn.NmnUrlFormat;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.simple.ApeMapFormat;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GoRiderGpsFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.GroundTrackFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Iblue747Format;
import slash.navigation.simple.KienzleGpsFormat;
import slash.navigation.simple.KompassFormat;
import slash.navigation.simple.NavilinkFormat;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;
import slash.navigation.simple.SygicAsciiFormat;
import slash.navigation.simple.SygicUnicodeFormat;
import slash.navigation.simple.WebPageFormat;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;
import slash.navigation.tour.TourFormat;
import slash.navigation.tour.TourPosition;
import slash.navigation.tour.TourRoute;
import slash.navigation.url.GoogleMapsUrlFormat;
import slash.navigation.viamichelin.ViaMichelinFormat;
import slash.navigation.viamichelin.ViaMichelinRoute;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.wbt.WintecWbt202TesFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Math.max;
import static java.util.Arrays.sort;
import static slash.common.io.Transfer.toArray;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;
import static slash.navigation.base.Positions.contains;
import static slash.navigation.base.Positions.getSignificantPositions;

/**
 * The base of all routes formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseRoute<P extends BaseNavigationPosition, F extends BaseNavigationFormat> {
    private static final String REVERSE_ROUTE_NAME_POSTFIX = " (rev)";
    private F format;
    private RouteCharacteristics characteristics;

    protected BaseRoute(F format, RouteCharacteristics characteristics) {
        this.format = format;
        this.characteristics = characteristics;
    }

    public F getFormat() {
        return format;
    }

    public RouteCharacteristics getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(RouteCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    public abstract String getName();
    public abstract void setName(String name);

    public abstract List<String> getDescription();

    public abstract List<P> getPositions();

    public abstract int getPositionCount();

    private void move(int index, int upOrDown) {
        List<P> positions = getPositions();
        P move = positions.get(index);
        P replace = positions.get(index + upOrDown);
        positions.set(index + upOrDown, move);
        positions.set(index, replace);
    }

    public void top(int index, int topOffset) {
        while (index > topOffset) {
            up(index, index - 1);
            index--;
        }
    }

    public void down(int fromIndex, int toIndex) {
        while (fromIndex < toIndex)
            move(fromIndex++, +1);
    }

    public void up(int fromIndex, int toIndex) {
        while (fromIndex > toIndex)
            move(fromIndex--, -1);
    }

    public void bottom(int index, int bottomOffset) {
        while (index < getPositionCount() - 1 - bottomOffset) {
            down(index, index + 1);
            index++;
        }
    }

    public abstract void add(int index, P position);

    public P remove(int index) {
        List<P> positions = getPositions();
        return positions.remove(index);
    }

    /**
     * Removes duplicate adjacent {@link #getPositions() positions} from this route, leaving
     * only distinct neighbours
     */
    public void removeDuplicates() {
        List<P> positions = getPositions();
        P previous = null;
        int index = 0;
        while (index < positions.size()) {
            P next = positions.get(index);
            if (previous != null && (!next.hasCoordinates() || next.calculateDistance(previous) <= 0.0)) {
                positions.remove(index);
            } else
                index++;
            previous = next;
        }
    }

    public void ensureIncreasingTime() {
        if (getPositionCount() < 2)
            return;

        long completeTime = getTime(); // ms
        double completeDistance = getDistance(); // m
        double averageSpeed = completeTime > 0 ? completeDistance / completeTime * 1000 : 1.0; // m/s

        List<P> positions = getPositions();
        P first = positions.get(0);
        if (!first.hasTime())
            first.setTime(fromCalendar(Calendar.getInstance(UTC)));

        P previous = first;
        for (int i = 1; i < positions.size(); i++) {
            P next = positions.get(i);
            CompactCalendar time = next.getTime();
            if (time == null || time.equals(previous.getTime())) {
                Double distance = next.calculateDistance(previous);
                Long millis = distance != null ? (long) (distance / averageSpeed * 1000) : null;
                if (millis == null || millis < 1000)
                    millis = 1000L;
                next.setTime(fromMillisAndTimeZone(previous.getTime().getTimeInMillis() + millis, previous.getTime().getTimeZoneId()));
            }
            previous = next;
        }
    }

    public int[] getContainedPositions(NavigationPosition northEastCorner,
                                       NavigationPosition southWestCorner) {
        List<Integer> result = new ArrayList<Integer>();
        List<P> positions = getPositions();
        for (int i = 0; i < positions.size(); i++) {
            P position = positions.get(i);
            if (position.hasCoordinates() && contains(northEastCorner, southWestCorner, position))
                result.add(i);
        }
        return toArray(result);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        List<Integer> result = new ArrayList<Integer>();
        List<P> positions = getPositions();
        if (positions.size() <= 2)
            return new int[0];
        P previous = positions.get(0);
        for (int i = 1; i < positions.size() - 1; i++) {
            P next = positions.get(i);
            if (!next.hasCoordinates() || next.calculateDistance(previous) <= distance)
                result.add(i);
            else
                previous = next;
        }
        return toArray(result);
    }

    public int[] getInsignificantPositions(double threshold) {
        int[] significantPositions = getSignificantPositions(getPositions(), threshold);
        BitSet bitset = new BitSet(getPositionCount());
        for (int significantPosition : significantPositions)
            bitset.set(significantPosition);

        int[] result = new int[getPositionCount() - significantPositions.length];
        int index = 0;
        for (int i = 0; i < getPositionCount(); i++)
            if (!bitset.get(i))
                result[index++] = i;
        return result;
    }

    public int getClosestPosition(double longitude, double latitude, double threshold) {
        int closestIndex = -1;
        double closestDistance = MAX_VALUE;

        List<P> positions = getPositions();
        for (int i = 0; i < positions.size(); ++i) {
            P point = positions.get(i);
            Double distance = point.calculateDistance(longitude, latitude);
            if (distance != null && distance < closestDistance && distance < threshold) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    public P getPosition(int index) {
        return getPositions().get(index);
    }

    public int getIndex(P position) {
        return getPositions().indexOf(position);
    }

    public P getSuccessor(P position) {
        List<P> positions = getPositions();
        int index = positions.indexOf(position);
        return index != -1 && index < positions.size() - 1 ? positions.get(index + 1) : null;
    }

    public long getTime() {
        CompactCalendar minimum = null, maximum = null;
        long totalTimeMilliSeconds = 0;
        List<P> positions = getPositions();
        P previous = null;
        for (P next : positions) {
            if (previous != null) {
                Long time = previous.calculateTime(next);
                if (time != null && time > 0)
                    totalTimeMilliSeconds += time;
            }

            CompactCalendar calendar = next.getTime();
            if (calendar == null)
                continue;
            if (minimum == null || calendar.before(minimum))
                minimum = calendar;
            if (maximum == null || calendar.after(maximum))
                maximum = calendar;

            previous = next;
        }

        long maxMinusMin = minimum != null ? maximum.getTimeInMillis() - minimum.getTimeInMillis() : 0;
        return max(maxMinusMin, totalTimeMilliSeconds);
    }

    public double getDistance() {
        return getDistance(0, getPositionCount() - 1);
    }

    public double getDistance(int startIndex, int endIndex) {
        double result = 0;
        List<P> positions = getPositions();
        NavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            NavigationPosition next = positions.get(i);
            if (previous != null) {
                Double distance = previous.calculateDistance(next);
                if (distance != null)
                    result += distance;
            }
            previous = next;
        }
        return result;
    }

    public double[] getDistancesFromStart(int startIndex, int endIndex) {
        double[] result = new double[endIndex - startIndex + 1];
        List<P> positions = getPositions();
        int index = 0;
        double distance = 0.0;
        NavigationPosition previous = positions.size() > 0 ? positions.get(0) : null;
        while (index <= endIndex) {
            NavigationPosition next = positions.get(index);
            if (previous != null) {
                Double delta = previous.calculateDistance(next);
                if (delta != null)
                    distance += delta;
                if (index >= startIndex)
                    result[index - startIndex] = distance;
            }
            index++;
            previous = next;
        }
        return result;
    }

    public double[] getDistancesFromStart(int[] indices) {
        double[] result = new double[indices.length];
        if (indices.length > 0 && getPositionCount() > 0) {
            sort(indices);
            int endIndex = Math.min(indices[indices.length - 1], getPositionCount() - 1);

            int index = 0;
            double distance = 0.0;
            List<P> positions = getPositions();
            NavigationPosition previous = positions.get(0);
            while (index <= endIndex) {
                NavigationPosition next = positions.get(index);
                if (previous != null) {
                    Double delta = previous.calculateDistance(next);
                    if (delta != null)
                        distance += delta;
                    int indexInIndices = Arrays.binarySearch(indices, index);
                    if (indexInIndices >= 0)
                        result[indexInIndices] = distance;
                }
                index++;
                previous = next;
            }
        }
        return result;
    }

    public double getElevationAscend(int startIndex, int endIndex) {
        double result = 0;
        List<P> positions = getPositions();
        NavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            NavigationPosition next = positions.get(i);
            if (previous != null) {
                Double elevation = previous.calculateElevation(next);
                if (elevation != null && elevation > 0)
                    result += elevation;
            }
            previous = next;
        }
        return result;
    }

    public double getElevationDescend(int startIndex, int endIndex) {
        double result = 0;
        List<P> positions = getPositions();
        NavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            NavigationPosition next = positions.get(i);
            if (previous != null) {
                Double elevation = previous.calculateElevation(next);
                if (elevation != null && elevation < 0)
                    result += Math.abs(elevation);
            }
            previous = next;
        }
        return result;
    }

    public void revert() {
        List<P> positions = getPositions();
        List<P> reverted = new ArrayList<P>();
        for (P position : positions) {
            reverted.add(0, position);
        }
        for (int i = 0; i < reverted.size(); i++) {
            positions.set(i, reverted.get(i));
        }

        String routeName = getName();
        if (!routeName.endsWith(REVERSE_ROUTE_NAME_POSTFIX))
            routeName = routeName + REVERSE_ROUTE_NAME_POSTFIX;
        else
            routeName = routeName.substring(0, routeName.length() - REVERSE_ROUTE_NAME_POSTFIX.length());
        setName(routeName);
    }

    public abstract P createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment);

    protected abstract BcrRoute asBcrFormat(BcrFormat format);
    protected abstract GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format);
    protected abstract GpxRoute asGpxFormat(GpxFormat format);
    protected abstract KmlRoute asKmlFormat(BaseKmlFormat format);
    protected abstract NmeaRoute asNmeaFormat(BaseNmeaFormat format);
    protected abstract NmnRoute asNmnFormat(NmnFormat format);
    protected abstract SimpleRoute asSimpleFormat(SimpleFormat format);
    protected abstract TcxRoute asTcxFormat(TcxFormat format);
    protected abstract TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format);

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asApeMapFormat() {
        if (getFormat() instanceof ApeMapFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new ApeMapFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asColumbusV900StandardFormat() {
        if (getFormat() instanceof ColumbusV900StandardFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new ColumbusV900StandardFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asColumbusV900ProfessionalFormat() {
        if (getFormat() instanceof ColumbusV900ProfessionalFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new ColumbusV900ProfessionalFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asCoPilot6Format() {
        if (getFormat() instanceof CoPilot6Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new CoPilot6Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asCoPilot7Format() {
        if (getFormat() instanceof CoPilot7Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new CoPilot7Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asCoPilot8Format() {
        if (getFormat() instanceof CoPilot8Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new CoPilot8Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asCoPilot9Format() {
        if (getFormat() instanceof CoPilot9Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new CoPilot9Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GarminFlightPlanRoute asGarminFlightPlanFormat() {
        if (getFormat() instanceof GarminFlightPlanFormat)
            return (GarminFlightPlanRoute) this;

        List<GarminFlightPlanPosition> flightPlanPositions = new ArrayList<GarminFlightPlanPosition>();
        for (P position : getPositions()) {
            flightPlanPositions.add(position.asGarminFlightPlanPosition());
        }
        return new GarminFlightPlanRoute(getName(), getDescription(), flightPlanPositions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGlopusFormat() {
        if (getFormat() instanceof GlopusFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GlopusFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGoogleMapsUrlFormat() {
        if (getFormat() instanceof GoogleMapsUrlFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GoogleMapsUrlFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GoPalRoute asGoPal3RouteFormat() {
        if (getFormat() instanceof GoPal3RouteFormat)
            return (GoPalRoute) this;
        return asGoPalRouteFormat(new GoPal3RouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GoPalRoute asGoPal5RouteFormat() {
        if (getFormat() instanceof GoPal5RouteFormat)
            return (GoPalRoute) this;
        return asGoPalRouteFormat(new GoPal5RouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGoPalTrackFormat() {
        if (getFormat() instanceof GoPalTrackFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GoPalTrackFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGoRiderGpsFormat() {
        if (getFormat() instanceof GoRiderGpsFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GoRiderGpsFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGpsTunerFormat() {
        if (getFormat() instanceof GpsTunerFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GpsTunerFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GpxRoute asGpx10Format() {
        if (getFormat() instanceof Gpx10Format)
            return (GpxRoute) this;
        return asGpxFormat(new Gpx10Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GpxRoute asGpx11Format() {
        if (getFormat() instanceof Gpx11Format)
            return (GpxRoute) this;
        return asGpxFormat(new Gpx11Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asGroundTrackFormat() {
        if (getFormat() instanceof GroundTrackFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new GroundTrackFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asHaicomLoggerFormat() {
        if (getFormat() instanceof HaicomLoggerFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new HaicomLoggerFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asIblue747Format() {
        if (getFormat() instanceof Iblue747Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new Iblue747Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asIgo8RouteFormat() {
        if (getFormat() instanceof Igo8RouteFormat)
            return (KmlRoute) this;
        return asKmlFormat(new Igo8RouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asKienzleGpsFormat() {
        if (getFormat() instanceof KienzleGpsFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new KienzleGpsFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KlickTelRoute asKlickTelRouteFormat() {
        if (getFormat() instanceof KlickTelRouteFormat)
            return (KlickTelRoute) this;

        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new KlickTelRoute(getName(), wgs84Positions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKml20Format() {
        if (getFormat() instanceof Kml20Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kml20Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKml21Format() {
        if (getFormat() instanceof Kml21Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kml21Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKml22BetaFormat() {
        if (getFormat() instanceof Kml22BetaFormat)
            return (KmlRoute) this;
        return asKmlFormat(new Kml22BetaFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKml22Format() {
        if (getFormat() instanceof Kml22Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kml22Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKmz20Format() {
        if (getFormat() instanceof Kmz20Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kmz20Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKmz21Format() {
        if (getFormat() instanceof Kmz21Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kmz21Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKmz22BetaFormat() {
        if (getFormat() instanceof Kmz22BetaFormat)
            return (KmlRoute) this;
        return asKmlFormat(new Kmz22BetaFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlRoute asKmz22Format() {
        if (getFormat() instanceof Kmz22Format)
            return (KmlRoute) this;
        return asKmlFormat(new Kmz22Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asKompassFormat() {
        if (getFormat() instanceof KompassFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new KompassFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmeaRoute asMagellanExploristFormat() {
        if (getFormat() instanceof MagellanExploristFormat)
            return (NmeaRoute) this;
        return asNmeaFormat(new MagellanExploristFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmeaRoute asMagellanRouteFormat() {
        if (getFormat() instanceof MagellanRouteFormat)
            return (NmeaRoute) this;
        return asNmeaFormat(new MagellanRouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asMagicMaps2GoFormat() {
        if (getFormat() instanceof MagicMaps2GoFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new MagicMaps2GoFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public MagicMapsIktRoute asMagicMapsIktFormat() {
        if (getFormat() instanceof MagicMapsIktFormat)
            return (MagicMapsIktRoute) this;

        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new MagicMapsIktRoute(getName(), getDescription(), wgs84Positions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MagicMapsPthRoute asMagicMapsPthFormat() {
        if (getFormat() instanceof MagicMapsPthFormat)
            return (MagicMapsPthRoute) this;

        List<GkPosition> gkPositions = new ArrayList<GkPosition>();
        for (P position : getPositions()) {
            gkPositions.add(position.asGkPosition());
        }
        return new MagicMapsPthRoute(getCharacteristics(), gkPositions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public BcrRoute asMTP0607Format() {
        if (getFormat() instanceof MTP0607Format)
            return (BcrRoute) this;
        return asBcrFormat(new MTP0607Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public BcrRoute asMTP0809Format() {
        if (getFormat() instanceof MTP0809Format)
            return (BcrRoute) this;
        return asBcrFormat(new MTP0809Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asNavigatingPoiWarnerFormat() {
        if (getFormat() instanceof NavigatingPoiWarnerFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new NavigatingPoiWarnerFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asNavilinkFormat() {
        if (getFormat() instanceof NavilinkFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new NavilinkFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmeaRoute asNmeaFormat() {
        if (getFormat() instanceof NmeaFormat)
            return (NmeaRoute) this;
        return asNmeaFormat(new NmeaFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnRoute asNmn4Format() {
        return asNmnFormat(new Nmn4Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnRoute asNmn5Format() {
        return asNmnFormat(new Nmn5Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnRoute asNmn6Format() {
        return asNmnFormat(new Nmn6Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnRoute asNmn6FavoritesFormat() {
        return asNmnFormat(new Nmn6FavoritesFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnRoute asNmn7Format() {
        return asNmnFormat(new Nmn7Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asNmnRouteFormat() {
        if (getFormat() instanceof NmnRouteFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new NmnRouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asNmnUrlFormat() {
        if (getFormat() instanceof NmnUrlFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new NmnUrlFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NokiaLandmarkExchangeRoute asNokiaLandmarkExchangeFormat() {
        if (getFormat() instanceof NokiaLandmarkExchangeFormat)
            return (NokiaLandmarkExchangeRoute) this;

        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new NokiaLandmarkExchangeRoute(getName(), getDescription(), wgs84Positions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asOpelNaviFormat() {
        if (getFormat() instanceof OpelNaviFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new OpelNaviFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public OvlRoute asOvlFormat() {
        if (getFormat() instanceof OvlFormat)
            return (OvlRoute) this;

        List<Wgs84Position> ovlPositions = new ArrayList<Wgs84Position>();
        for (P position : getPositions()) {
            ovlPositions.add(position.asOvlPosition());
        }
        return new OvlRoute(getCharacteristics(), getName(), ovlPositions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asQstarzQ1000Format() {
        if (getFormat() instanceof QstarzQ1000Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new QstarzQ1000Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asRoute66Format() {
        if (getFormat() instanceof Route66Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new Route66Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asSygicAsciiFormat() {
        if (getFormat() instanceof SygicAsciiFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new SygicAsciiFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asSygicUnicodeFormat() {
        if (getFormat() instanceof SygicUnicodeFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new SygicUnicodeFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public TcxRoute asTcx1Format() {
        if (getFormat() instanceof Tcx1Format)
            return (TcxRoute) this;
        return asTcxFormat(new Tcx1Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public TcxRoute asTcx2Format() {
        if (getFormat() instanceof Tcx2Format)
            return (TcxRoute) this;
        return asTcxFormat(new Tcx2Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public TomTomRoute asTomTom5RouteFormat() {
        if (getFormat() instanceof TomTom5RouteFormat)
            return (TomTomRoute) this;
        return asTomTomRouteFormat(new TomTom5RouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public TomTomRoute asTomTom8RouteFormat() {
        if (getFormat() instanceof TomTom8RouteFormat)
            return (TomTomRoute) this;
        return asTomTomRouteFormat(new TomTom8RouteFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public TourRoute asTourFormat() {
        if (getFormat() instanceof TourFormat)
            return (TourRoute) this;

        List<TourPosition> tourPositions = new ArrayList<TourPosition>();
        for (P position : getPositions()) {
            tourPositions.add(position.asTourPosition());
        }
        return new TourRoute(getName(), tourPositions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ViaMichelinRoute asViaMichelinFormat() {
        if (getFormat() instanceof ViaMichelinFormat)
            return (ViaMichelinRoute) this;

        List<Wgs84Position> wgs84Positions = new ArrayList<Wgs84Position>();
        for (P position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new ViaMichelinRoute(getName(), wgs84Positions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asWebPageFormat() {
        if (getFormat() instanceof WebPageFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new WebPageFormat());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asWintecWbt201Tk1Format() {
        if (getFormat() instanceof WintecWbt201Tk1Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new WintecWbt201Tk1Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asWintecWbt201Tk2Format() {
        if (getFormat() instanceof WintecWbt201Tk2Format)
            return (SimpleRoute) this;
        return asSimpleFormat(new WintecWbt201Tk2Format());
    }

    @SuppressWarnings("UnusedDeclaration")
    public SimpleRoute asWintecWbt202TesFormat() {
        if (getFormat() instanceof WintecWbt202TesFormat)
            return (SimpleRoute) this;
        return asSimpleFormat(new WintecWbt202TesFormat());
    }
}
