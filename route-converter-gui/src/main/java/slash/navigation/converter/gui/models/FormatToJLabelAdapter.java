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
import slash.navigation.base.NavigationFormat;

import javax.swing.*;

/**
 * A bidirectional adapter that extracts the route format name
 * of a {@link PositionsModel} for display.
 *
 * @author Christian Pesch
 */

public class FormatToJLabelAdapter extends FormatAndRoutesListModelToDocumentAdapter {
    private final JLabel label;

    public FormatToJLabelAdapter(FormatAndRoutesModel formatAndRoutesModel,
                                 JLabel label) {
        super(formatAndRoutesModel);
        this.label = label;
    }


    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    protected void updateAdapterFromDelegate() {
        NavigationFormat<BaseRoute> format = getDelegate().getFormat();
        if (format != null) {
            label.setText(format.getName());
        } else {
            label.setText("-");
        }
    }
}
