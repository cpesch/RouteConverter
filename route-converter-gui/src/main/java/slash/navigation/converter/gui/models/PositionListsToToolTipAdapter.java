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

import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.BaseRouteConverter;

import javax.swing.*;
import java.text.MessageFormat;

import static slash.navigation.base.RouteCharacteristics.*;

/**
 * An adapter that shows the number of position lists per route characteristics
 * of a {@link FormatAndRoutesModel} as the tool tip of a component.
 *
 * @author Christian Pesch
 */

public class PositionListsToToolTipAdapter extends FormatAndRoutesListModelToDocumentAdapter {
    private final JComponent component;

    public PositionListsToToolTipAdapter(FormatAndRoutesModel formatAndRoutesModel, JComponent component) {
        super(formatAndRoutesModel);
        this.component = component;
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private Integer count(RouteCharacteristics characteristics) {
        int count = 0;
        for (BaseRoute<?, ?> route : getDelegate().getRoutes()) {
            if (characteristics.equals(route.getCharacteristics()))
                count++;
        }
        return count;
    }

    protected void updateAdapterFromDelegate() {
        BaseRoute<?, ?> route = getDelegate().getSelectedRoute();
        component.setToolTipText(route != null ? MessageFormat.format(BaseRouteConverter.getBundle().getString("position-lists"),
                count(Route),
                count(Track),
                count(Waypoints)
        ) : null);
    }
}
