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
package slash.navigation.geocoding;

import slash.common.type.CompactCalendar;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.Objects;

/**
 * A simple navigation position with an optional category in addition to the description.
 *
 * @author Christian Pesch
 */
public class SimpleCategorizedNavigationPosition extends SimpleNavigationPosition implements CategorizedNavigationPosition {
    private String category;

    public SimpleCategorizedNavigationPosition(Double longitude, Double latitude, Double elevation,
                                               String description, String category, CompactCalendar time) {
        super(longitude, latitude, elevation, description, time);
        this.category = category;
    }

    public SimpleCategorizedNavigationPosition(Double longitude, Double latitude, Double elevation,
                                               String description, String category) {
        this(longitude, latitude, elevation, description, category, null);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleCategorizedNavigationPosition that)) return false;
        return super.equals(o) && Objects.equals(category, that.category);
    }

    public int hashCode() {
        return 31 * super.hashCode() + (category != null ? category.hashCode() : 0);
    }

    public String toString() {
        return getClass().getSimpleName() + "[longitude=" + getLongitude() + ", latitude=" + getLatitude() +
                (getDescription() != null ? ", description=" + getDescription() : "") +
                (category != null ? ", category=" + category : "") +
                "]";
    }
}

