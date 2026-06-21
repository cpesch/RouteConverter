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

import slash.navigation.base.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.util.List;

/**
 * Acts as a {@link ComboBoxModel} for the routes of a {@link FormatAndRoutes}.
 *
 * @author Christian Pesch
 */

public interface FormatAndRoutesModel extends ComboBoxModel {
    List<BaseRoute> getRoutes();
    void setRoutes(FormatAndRoutes<BaseNavigationFormat, BaseRoute, BaseNavigationPosition> formatAndRoutes);
    NavigationFormat<BaseRoute> getFormat();
    void setFormat(NavigationFormat<BaseRoute> format);

    boolean isModified();
    void setModified(boolean modified);
    void addModifiedListener(ChangeListener l);

    BaseRoute getSelectedRoute();
    void setSelectedRoute(BaseRoute route);
    BaseRoute getRoute(int index);
    int getIndex(BaseRoute route);

    void addPositionList(int index, BaseRoute route);
    void renamePositionList(String name);
    void removePositionList(BaseRoute route);
}
