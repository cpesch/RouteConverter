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

package slash.navigation.converter.gui.models;

import slash.navigation.gui.actions.ActionManager;

/**
 * Contains constants for all {@link ActionManager#registerLocal(String, String, String) local names}
 * of the {@link ActionManager}.
 *
 * @author Christian Pesch
 */

public interface LocalActionConstants {
    String POSITIONS = "positions";
    String POINTS_OF_INTEREST = "points-of-interest";
    String PHOTOS = "photos";

    String CATEGORIES = "categories";
    String ROUTES = "routes";

    String MAPS = "maps";
    String THEMES = "themes";

    String DOWNLOADS = "downloads";
    String DOWNLOADABLE_MAPS = "downloadable-maps";
    String DOWNLOADABLE_THEMES = "downloadable-themes";
}
