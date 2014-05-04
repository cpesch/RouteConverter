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

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;

import javax.swing.*;

import static java.lang.Integer.MIN_VALUE;

/**
 * Acts as a {@link ComboBoxModel} for the characteristics of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class CharacteristicsModel extends AbstractListModel implements ComboBoxModel {
    private BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route;
    public static final int IGNORE = MIN_VALUE;

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getRoute() {
        return route;
    }

    public void setRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        this.route = route;
        fireContentsChanged(this, IGNORE, IGNORE);
    }

    public RouteCharacteristics getSelectedCharacteristics() {
        return getRoute() != null ? route.getCharacteristics() : null;
    }

    public int getSize() {
        return RouteCharacteristics.values().length;
    }

    public Object getElementAt(int index) {
        return RouteCharacteristics.values()[index];
    }

    public Object getSelectedItem() {
        return getSelectedCharacteristics();
    }

    public void setSelectedItem(Object anItem) {
        if ((getSelectedItem() != null && !getSelectedItem().equals(anItem)) ||
                getSelectedItem() == null && anItem != null) {
            route.setCharacteristics((RouteCharacteristics) anItem);
            fireContentsChanged(this, -1, -1);
        }
    }
}
