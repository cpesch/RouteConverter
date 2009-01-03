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

import slash.navigation.util.Conversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Helpers for comment processing of positions
 *
 * @author Christian Pesch
 */
public abstract class RouteComments {
    private static final String POSITION = "Position";
    private static final Pattern POSITION_PATTERN = Pattern.compile("(.*)" + POSITION + " (\\d+)(.*)");

    public static String createRouteDescription(BaseRoute route) {
        String name = Conversion.trim(route.getName());
        List<String> description = route.getDescription();
        StringBuffer buffer = new StringBuffer();
        if (name != null)
            buffer.append(name);
        if (description != null) {
            for (String line : description)
                buffer.append(line);
        }
        return buffer.toString();
    }

    public static String createRouteName(List<? extends BaseNavigationPosition> positions) {
        if (positions.size() > 0)
            return positions.get(0).getComment() + " to " + positions.get(positions.size() - 1).getComment();
        else
            return "?";
    }

    public static void commentRouteName(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        if (route.getName() == null) {
            route.setName(createRouteName(route.getPositions()));
        }
    }

    private static String getPositionComment(int index) {
        return POSITION + " " + (index + 1);
    }

    public static boolean isPositionComment(String comment) {
        Matcher matcher = POSITION_PATTERN.matcher(comment);
        return matcher.matches();
    }

    public static void commentPositions(List<? extends BaseNavigationPosition> positions) {
        for (int i = 0; i < positions.size(); i++) {
            BaseNavigationPosition position = positions.get(i);
            if (position.getComment() == null || "(null)".equals(position.getComment()))
                position.setComment(getPositionComment(i));
            else {
                Matcher matcher = POSITION_PATTERN.matcher(position.getComment());
                if (matcher.matches()) {
                    String prefix = matcher.group(1);
                    String postfix = matcher.group(3);
                    position.setComment(prefix + getPositionComment(i) + postfix);
                }
            }
        }
    }

    public static String numberPosition(String comment, int number) {
        return number + comment;
    }

    public static void commentRoutePositions(List<? extends BaseRoute> routes) {
        Map<LongitudeAndLatitude, String> comments = new HashMap<LongitudeAndLatitude, String>();

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            for (BaseNavigationPosition position : route.getPositions()) {
                if (position.getComment() == null || !position.hasCoordinates())
                    continue;

                LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                if (comments.get(lal) == null) {
                    comments.put(lal, position.getComment());
                }
            }
        }

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            for (BaseNavigationPosition position : route.getPositions()) {
                if (position.getComment() == null && position.hasCoordinates()) {
                    LongitudeAndLatitude lal = new LongitudeAndLatitude(position);
                    String comment = comments.get(lal);
                    if (comment != null) {
                        position.setComment(comment);
                    }
                }
            }
        }

        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            commentPositions(route.getPositions());
        }
    }


    private static class LongitudeAndLatitude {
        public double longitude, latitude;

        public LongitudeAndLatitude(BaseNavigationPosition position) {
            this.longitude = position.getLongitude();
            this.latitude = position.getLatitude();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final LongitudeAndLatitude that = (LongitudeAndLatitude) o;

            return Double.compare(that.latitude, latitude) == 0 &&
                    Double.compare(that.longitude, longitude) == 0;
        }

        public int hashCode() {
            int result;
            long temp;
            temp = longitude != +0.0d ? Double.doubleToLongBits(longitude) : 0L;
            result = (int) (temp ^ (temp >>> 32));
            temp = latitude != +0.0d ? Double.doubleToLongBits(latitude) : 0L;
            result = 29 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
