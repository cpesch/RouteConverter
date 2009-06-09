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

import slash.navigation.bcr.BcrRoute;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.kml.KmlRoute;
import slash.navigation.mm.MagicMapsIktRoute;
import slash.navigation.mm.MagicMapsPthRoute;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.ovl.OvlRoute;
import slash.navigation.tour.TourRoute;
import slash.navigation.util.Calculation;
import slash.navigation.util.Range;
import slash.navigation.util.CompactCalendar;
import slash.navigation.viamichelin.ViaMichelinRoute;
import slash.navigation.klicktel.KlickTelRoute;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;

/**
 * The base of all routes formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseRoute<P extends BaseNavigationPosition, F extends BaseNavigationFormat> {
    private static final String REVERSE_ROUTE_NAME_POSTFIX = " (rev)";

    private F format;
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

    public abstract void add(int index, P position);

    private void move(int index, int upOrDown) {
        List<P> positions = getPositions();
        P move = positions.get(index);
        P replace = positions.get(index + upOrDown);
        positions.set(index + upOrDown, move);
        positions.set(index, replace);
    }

    public void top(int index, int topOffset) {
        while (index > topOffset)
            up(index--);
    }

    public void up(int index) {
        move(index, -1);
    }

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

    public int[] getDuplicatesWithinDistance(double distance) {
        List<Integer> result = new ArrayList<Integer>();
        List<P> positions = getPositions();
        P previous = null;
        for (int i = 0; i < positions.size(); i++) {
            P next = positions.get(i);
            if (previous != null && (!next.hasCoordinates() || next.calculateDistance(previous) <= distance))
                result.add(i);
            previous = next;
        }
        return Range.toArray(result);
    }

    public int[] getPositionsThatRemainingHaveDistance(double distance) {
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
        int[] significantPositions = Calculation.getSignificantPositions(getPositions(), threshold);
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

    public P getPredecessor(P position) {
        List<P> positions = getPositions();
        int index = positions.indexOf(position);
        return index > 0 && index < positions.size() ? positions.get(index - 1) : null;
    }

    public P getPosition(int index) {
        return getPositions().get(index);
    }

    public P getSuccessor(P position) {
        List<P> positions = getPositions();
        int index = positions.indexOf(position);
        return index != -1 && index < positions.size() -1 ? positions.get(index + 1) : null;
    }

    public double getLength() {
        double result = 0;
        List<P> positions = getPositions();
        P previous = null;
        for (P next : positions) {
            if (previous != null) {
                Double distance = previous.calculateDistance(next);
                if (distance != null)
                    result += distance;
            }
            previous = next;
        }
        return result;
    }

    public long getDuration() {
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

    public void down(int index) {
        move(index, +1);
    }

    public void bottom(int index, int bottomOffset) {
        while (index < getPositionCount() - 1 - bottomOffset)
            down(index++);
    }

    public abstract P createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment);

    public abstract SimpleRoute asColumbusV900Format();

    public abstract SimpleRoute asCoPilot6Format();

    public abstract SimpleRoute asCoPilot7Format();

    public abstract SimpleRoute asGlopusFormat();

    public abstract SimpleRoute asGoogleMapsFormat();

    public abstract GoPalRoute asGoPalRouteFormat();

    public abstract SimpleRoute asGoPalTrackFormat();

    public abstract SimpleRoute asGpsTunerFormat();

    public abstract GpxRoute asGpx10Format();

    public abstract GpxRoute asGpx11Format();

    public abstract TomTomRoute asTomTom5RouteFormat();

    public abstract TomTomRoute asTomTom8RouteFormat();

    public abstract KlickTelRoute asKlickTelRouteFormat();

    public abstract KmlRoute asKml20Format();

    public abstract KmlRoute asKml21Format();

    public abstract KmlRoute asKml22BetaFormat();

    public abstract KmlRoute asKml22Format();

    public abstract KmlRoute asKmz20Format();

    public abstract KmlRoute asKmz21Format();

    public abstract KmlRoute asKmz22BetaFormat();

    public abstract KmlRoute asKmz22Format();

    public abstract MagicMapsIktRoute asMagicMapsIktFormat();

    public abstract MagicMapsPthRoute asMagicMapsPthFormat();

    public abstract NmeaRoute asMagellanExploristFormat();

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

    public abstract TourRoute asTourFormat();

    public abstract ViaMichelinRoute asViaMichelinFormat();
}
