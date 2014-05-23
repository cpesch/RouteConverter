/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */
package slash.navigation.common;

import static java.lang.Double.compare;
import static java.lang.Double.doubleToLongBits;

/**
 * Value and {@link Orientation} of GPS coordinates.
 *
 * @author Christian Pesch
 */
public class ValueAndOrientation {
    private double value;
    private Orientation orientation;

    public ValueAndOrientation(double value, Orientation orientation) {
        this.value = value;
        this.orientation = orientation;
    }

    public double getValue() {
        return value;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueAndOrientation that = (ValueAndOrientation) o;

        return compare(that.value, value) == 0 && orientation == that.orientation;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = value != +0.0d ? doubleToLongBits(value) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + orientation.hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[value=" + value + ", orientation=" + orientation + "]";
    }
}
