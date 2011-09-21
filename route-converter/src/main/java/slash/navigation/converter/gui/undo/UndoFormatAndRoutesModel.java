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

package slash.navigation.converter.gui.undo;

import slash.navigation.base.*;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.FormatAndRoutesModelImpl;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.UndoManager;

import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * Implements a undo/redo-supporting {@link FormatAndRoutesModel} for the routes of a file.
 *
 * @author Christian Pesch
 */

public class UndoFormatAndRoutesModel implements FormatAndRoutesModel {
    private FormatAndRoutesModelImpl delegate;
    private UndoManager undoManager;

    public UndoFormatAndRoutesModel(UndoManager undoManager) {
        this.undoManager = undoManager;
        delegate = new FormatAndRoutesModelImpl(new UndoPositionsModel(undoManager));
    }

    // ListModel

    public int getSize() {
        return delegate.getSize();
    }

    public Object getElementAt(int index) {
        return delegate.getElementAt(index);
    }

    public void addListDataListener(ListDataListener l) {
        delegate.addListDataListener(l);
    }

    public void removeListDataListener(ListDataListener l) {
        delegate.removeListDataListener(l);
    }

    // ComboBoxModel

    public Object getSelectedItem() {
        return delegate.getSelectedItem();
    }

    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object anItem) {
        setSelectedRoute((BaseRoute<BaseNavigationPosition, BaseNavigationFormat>) anItem);
    }

    // FormatAndRoutesModel


    public List<BaseRoute> getRoutes() {
        return delegate.getRoutes();
    }

    public void setRoutes(FormatAndRoutes<BaseNavigationFormat, BaseRoute, BaseNavigationPosition> formatAndRoutes) {
        delegate.setRoutes(formatAndRoutes);
    }

    public NavigationFormat<BaseRoute> getFormat() {
        return delegate.getFormat();
    }

    public void setFormat(NavigationFormat<BaseRoute> format) {
        delegate.setFormat(format);
    }

    public PositionsModel getPositionsModel() {
        return delegate.getPositionsModel();
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return delegate.getCharacteristicsModel();
    }

    public boolean isModified() {
        return delegate.isModified();
    }

    public void setModified(boolean modified) {
        delegate.setModified(modified);
    }

    public void addModifiedListener(ChangeListener listener) {
        delegate.addModifiedListener(listener);
    }

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getSelectedRoute() {
        return delegate.getSelectedRoute();
    }

    public void setSelectedRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        setSelectedRoute(route, true);
    }

    public void setSelectedRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route,
                                 boolean trackUndo) {
        if ((getSelectedRoute() != null && !getSelectedRoute().equals(route)) ||
                getSelectedRoute() == null && route != null) {
            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> previousRoute = trackUndo ? getSelectedRoute() : null;
            delegate.setSelectedRoute(route);
            if (trackUndo)
                undoManager.addEdit(new ChangeRoute(this, previousRoute, route));
        }
    }

    public BaseRoute getRoute(int index) {
        return delegate.getRoute(index);
    }

    public void renameRoute(String name) {
        delegate.renameRoute(name);
    }

    public void addRoute(int index, BaseRoute route) {
        delegate.addRoute(index, route);
    }

    public void removeRoute(BaseRoute route) {
        delegate.removeRoute(route);
    }
}
