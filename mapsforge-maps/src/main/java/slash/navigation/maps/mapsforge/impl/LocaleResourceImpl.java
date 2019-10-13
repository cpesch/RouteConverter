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
package slash.navigation.maps.mapsforge.impl;

import slash.navigation.maps.mapsforge.LocalResource;

/**
 * The implementation of a {@link LocalResource}.
 *
 * @author Christian Pesch
 */

abstract class LocaleResourceImpl implements LocalResource {
    private final String description;
    private final String url;
    private final String copyrightText;

    public LocaleResourceImpl(String description, String url, String copyrightText) {
        this.description = description;
        this.url = url;
        this.copyrightText = copyrightText;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public String toString() {
        return getClass().getSimpleName() + "[url=" + getUrl() + "]";
    }
}
