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

import java.util.List;

/**
 * The result of the parsing of the {@link NavigationFormatParser}.
 *
 * @author Christian Pesch
 */

public class ParserResult {
    private final FormatAndRoutes formatAndRoutes;

    public ParserResult(FormatAndRoutes formatAndRoutes) {
        this.formatAndRoutes = formatAndRoutes;
    }
    
    public boolean isSuccessful() {
        return formatAndRoutes != null;
    }

    public NavigationFormat getFormat() {
        return formatAndRoutes.getFormat();
    }

    @SuppressWarnings("unchecked")
    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getTheRoute() {
        return formatAndRoutes.getRoute();
    }

    @SuppressWarnings("unchecked")
    public List<BaseRoute> getAllRoutes() {
        return formatAndRoutes.getRoutes();
    }

}
