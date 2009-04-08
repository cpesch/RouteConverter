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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.models;

import slash.navigation.BaseRoute;
import slash.navigation.RouteCharacteristics;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.text.MessageFormat;

/**
 * A bidirectional adapter that extracts the route format name and the number
 * of position lists per route characteristics of a {@link FormatAndRoutesModel} for display.
 *
 * @author Christian Pesch
 */

public class RouteFormatToJLabelAdapter extends FormatAndRoutesListModelToDocumentAdapter {
    private JLabel labelFormat, labelPositionLists;

    public RouteFormatToJLabelAdapter(FormatAndRoutesModel formatAndRoutesModel,
                                      JLabel labelFormat,
                                      JLabel labelPositionLists) {
        super(formatAndRoutesModel);
        this.labelFormat = labelFormat;
        this.labelPositionLists = labelPositionLists;
    }


    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private Integer count(RouteCharacteristics characteristics) {
        int count = 0;
        for (BaseRoute route : getDelegate().getRoutes()) {
            if (characteristics.equals(route.getCharacteristics()))
                count++;
        }
        return count;
    }

    protected void updateAdapterFromDelegate() {
        BaseRoute route = getDelegate().getSelectedRoute();
        if (route != null) {
            labelFormat.setText(getDelegate().getFormat().getName());
            labelPositionLists.setText(MessageFormat.format(RouteConverter.getBundle().getString("position-lists"),
                    count(RouteCharacteristics.Route),
                    count(RouteCharacteristics.Track),
                    count(RouteCharacteristics.Waypoints)
            ));
        } else {
            labelFormat.setText("-");
            labelPositionLists.setText("-");
        }
    }
}
