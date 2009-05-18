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

import slash.navigation.BaseRoute;
import slash.navigation.RouteCharacteristics;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.mapview.MapViewListener;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * A bidirectional adapter that extracts the route length
 * of a {@link FormatAndRoutesModel} for display.
 *
 * @author Christian Pesch
 */

public class LengthToJLabelAdapter extends FormatAndRoutesListModelToDocumentAdapter {
    private JLabel label;

    public LengthToJLabelAdapter(FormatAndRoutesModel formatAndRoutesModel,
                                 JLabel label) {
        super(formatAndRoutesModel);
        this.label = label;

        RouteConverter r = RouteConverter.getInstance();
        r.addMapViewListener(new MapViewListener() {
            public void calculatedDistance(int meters, int seconds) {
                updateLabel(meters, seconds * 1000);
            }

            public void receivedCallback(int port) {
            }
        });
    }


    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private void updateLabel(int meters, long milliSeconds) {
        label.setText(meters > 0 ? MessageFormat.format(RouteConverter.getBundle().getString("length-value"), meters / 1000.0 ) : "-");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(milliSeconds);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 1);
        Date date = calendar.getTime();
        label.setToolTipText(MessageFormat.format(RouteConverter.getBundle().getString("duration-value"), date));
    }

    protected void updateAdapterFromDelegate() {
        BaseRoute route = getDelegate().getSelectedRoute();
        if (route != null && route.getCharacteristics() != RouteCharacteristics.Waypoints) {
            updateLabel((int)route.getLength(), route.getDuration());
        } else {
            updateLabel(0, 0);
        }
    }
}