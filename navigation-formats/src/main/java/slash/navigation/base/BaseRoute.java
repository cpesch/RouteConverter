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

import slash.common.io.CompactCalendar;
import slash.common.io.Range;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.klicktel.KlickTelRoute;
import slash.navigation.kml.KmlRoute;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.tour.TourRoute;
import slash.navigation.util.Positions;
import slash.navigation.viamichelin.ViaMichelinRoute;

import java.util.*;

/**
 * The base of all routes formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseRoute<P extends BaseNavigationPosition, F extends BaseNavigationFormat> {
    private static final String REVERSE_ROUTE_NAME_POSTFIX = " (rev)";

    private final F format;
    protected RouteCharacteristics characteristics;

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
        if(getPositionCount() < 2)
            return;

        long completeTime = getTime(); // ms
        double completeDistance = getDistance(); // m
        double averageSpeed = completeTime > 0 ? completeDistance / completeTime * 1000 : 1.0; // m/s

        List<P> positions = getPositions();
        P first = positions.get(0);
        if(first.getTime() == null)
            first.setTime(CompactCalendar.fromCalendar(Calendar.getInstance(CompactCalendar.GMT)));

        P previous = first;
        for (int i = 1; i < positions.size(); i++) {
            P next = positions.get(i);
            CompactCalendar time = next.getTime();
            if(time == null || time.equals(previous.getTime())) {
                Double distance = next.calculateDistance(previous);
                Long millis = distance != null ? new Double(distance / averageSpeed * 1000).longValue() : null;
                if(millis == null || millis < 1000)
                    millis = 1000L;
                next.setTime(CompactCalendar.fromMillisAndTimeZone(previous.getTime().getTimeInMillis() + millis, previous.getTime().getTimeZoneId()));
            }
            previous = next;
        }
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
        return Range.toArray(result);
    }

    public int[] getInsignificantPositions(double threshold) {
        int[] significantPositions = Positions.getSignificantPositions(getPositions(), threshold);
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
        Calendar minimum = null, maximum = null;
        long delta = 0;
        List<P> positions = getPositions();
        P previous = null;
        for (P next : positions) {
            if (previous != null) {
                Long time = previous.calculateTime(next);
                if (time != null)
                    delta += time;
            }

            Calendar calendar = next.getTime() != null ? next.getTime().getCalendar() : null;
            if (calendar == null)
                continue;
            if (minimum == null || calendar.before(minimum))
                minimum = calendar;
            if (maximum == null || calendar.after(maximum))
                maximum = calendar;

            previous = next;
        }

        long maxMinusMin = minimum != null ? maximum.getTimeInMillis() - minimum.getTimeInMillis() : 0;
        return Math.max(maxMinusMin, delta);
    }

    public double getDistance() {
        return getDistance(0, getPositionCount() - 1);
    }

    public double getDistance(int startIndex, int endIndex) {
        double result = 0;
        List<P> positions = getPositions();
        BaseNavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            BaseNavigationPosition next = positions.get(i);
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
        BaseNavigationPosition previous = positions.size() > 0 ? positions.get(0) : null;
        while (index <= endIndex) {
            BaseNavigationPosition next = positions.get(index);
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
            Arrays.sort(indices);
            int endIndex = Math.min(indices[indices.length - 1], getPositionCount() - 1);

            int index = 0;
            double distance = 0.0;
            List<P> positions = getPositions();
            BaseNavigationPosition previous = positions.get(0);
            while (index <= endIndex) {
                BaseNavigationPosition next = positions.get(index);
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
        BaseNavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            BaseNavigationPosition next = positions.get(i);
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
        BaseNavigationPosition previous = null;
        for (int i = startIndex; i <= endIndex; i++) {
            BaseNavigationPosition next = positions.get(i);
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

    public abstract SimpleRoute asColumbusV900StandardFormat();

    public abstract SimpleRoute asColumbusV900ProfessionalFormat();

    public abstract SimpleRoute asCoPilot6Format();

    public abstract SimpleRoute asCoPilot7Format();

    public abstract SimpleRoute asGlopusFormat();

    public abstract SimpleRoute asGoogleMapsFormat();

    public abstract GoPalRoute asGoPalRouteFormat();

    public abstract SimpleRoute asGoPalTrackFormat();

    public abstract SimpleRoute asGpsTunerFormat();

    public abstract GpxRoute asGpx10Format();

    public abstract GpxRoute asGpx11Format();

    public abstract GpxRoute asTcx1Format();

    public abstract GpxRoute asTcx2Format();

    public abstract GpxRoute asNokiaLandmarkExchangeFormat();

    public abstract SimpleRoute asHaicomLoggerFormat();

    public abstract KlickTelRoute asKlickTelRouteFormat();

    public abstract KmlRoute asKml20Format();

    public abstract KmlRoute asKml21Format();

    public abstract KmlRoute asKml22BetaFormat();

    public abstract KmlRoute asKml22Format();

    public abstract KmlRoute asKmz20Format();

    public abstract KmlRoute asKmz21Format();

    public abstract KmlRoute asKmz22BetaFormat();

    public abstract KmlRoute asKmz22Format();

    public abstract SimpleRoute asKompassFormat();

    public abstract SimpleRoute asMagicMaps2GoFormat();

    public abstract MagicMapsIktRoute asMagicMapsIktFormat();

    public abstract MagicMapsPthRoute asMagicMapsPthFormat();

    public abstract NmeaRoute asMagellanExploristFormat();

    public abstract NmeaRoute asMagellanRouteFormat();

    public abstract BcrRoute asMTP0607Format();

    public abstract BcrRoute asMTP0809Format();

    public abstract SimpleRoute asNavigatingPoiWarnerFormat();

    public abstract NmeaRoute asNmeaFormat();

    public abstract NmnRoute asNmn4Format();

    public abstract NmnRoute asNmn5Format();

    public abstract NmnRoute asNmn6Format();

    public abstract NmnRoute asNmn6FavoritesFormat();

    public abstract NmnRoute asNmn7Format();

    public abstract OvlRoute asOvlFormat();

    public abstract SimpleRoute asRoute66Format();

    public abstract SimpleRoute asSygicUnicodeFormat();

    public abstract SimpleRoute asWebPageFormat();

    public abstract SimpleRoute asWintecWbt201Tk1Format();

    public abstract SimpleRoute asWintecWbt201Tk2Format();

    public abstract SimpleRoute asWintecWbt202TesFormat();

    public abstract TomTomRoute asTomTom5RouteFormat();

    public abstract TomTomRoute asTomTom8RouteFormat();

    public abstract TourRoute asTourFormat();

    public abstract ViaMichelinRoute asViaMichelinFormat();
}
