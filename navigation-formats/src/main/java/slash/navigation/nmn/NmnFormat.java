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

package slash.navigation.nmn;

import slash.common.io.Transfer;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.common.NavigationPosition;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The base of all Navigon Mobile Navigator formats.
 *
 * @author Christian Pesch
 */

public abstract class NmnFormat extends SimpleLineBasedFormat<NmnRoute> {
    static final char SEPARATOR = '|';
    static final String REGEX_SEPARATOR = "\\" + SEPARATOR;
    static final String WILDCARD = "[.[^" + SEPARATOR + "]]*";
    static final char LEFT_BRACE = '[';
    static final char RIGHT_BRACE = ']';

    static final Pattern DESCRIPTION_PATTERN = Pattern.compile("(\\d+ )?(.[^,;]+),(.[^ ,;]+)( .[^,;]+)?");

    private static final double DUPLICATE_OFFSET = 0.0001;
    
    public String getExtension() {
        return ".rte";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> NmnRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmnRoute(this, characteristics, null, (List<NmnPosition>) positions);
    }

    public BaseNavigationPosition getDuplicateFirstPosition(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        List<BaseNavigationPosition> positions = route.getPositions();
        if (positions.size() == 0)
            return null;
        NavigationPosition first = positions.get(0);
        return new NmnPosition(first.getLongitude() + DUPLICATE_OFFSET,
                first.getLatitude() + DUPLICATE_OFFSET, (Double)null, null, null, "Start:" + first.getDescription());
    }

    protected String escape(String string) {
        return Transfer.escape(string, SEPARATOR, ';', "-");
    }
}
