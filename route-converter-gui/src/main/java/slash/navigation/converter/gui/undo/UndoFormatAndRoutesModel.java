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
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.gui.undo.UndoManager;

import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * Implements a undo/redo-supporting {@link FormatAndRoutesModel} for the routes of a file.
 *
 * @author Christian Pesch
 */

public class UndoFormatAndRoutesModel implements FormatAndRoutesModel {
    private final UndoManager undoManager;
    private final FormatAndRoutesModel delegate;

    public UndoFormatAndRoutesModel(UndoManager undoManager, FormatAndRoutesModel delegate) {
        this.undoManager = undoManager;
        this.delegate = delegate;
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

    public boolean isModified() {
        return delegate.isModified();
    }

    public void setModified(boolean modified) {
        delegate.setModified(modified);
    }

    public void addModifiedListener(ChangeListener l) {
        delegate.addModifiedListener(l);
    }

    public BaseRoute getSelectedRoute() {
        return delegate.getSelectedRoute();
    }

    public void setSelectedRoute(BaseRoute route) {
        setSelectedRoute(route, true);
    }

    public void setSelectedRoute(BaseRoute route, boolean trackUndo) {
        if ((getSelectedRoute() != null && !getSelectedRoute().equals(route)) ||
                getSelectedRoute() == null && route != null) {
            BaseRoute previousRoute = trackUndo ? getSelectedRoute() : null;
            delegate.setSelectedRoute(route);
            if (trackUndo)
                undoManager.addEdit(new ChangeRoute(this, previousRoute, route));
        }
    }

    public BaseRoute getRoute(int index) {
        return delegate.getRoute(index);
    }

    public int getIndex(BaseRoute route) {
        return delegate.getIndex(route);
    }

    public void renamePositionList(String name) {
        renameRoute(name, true);
    }

    public void renameRoute(String name, boolean trackUndo) {
        String previousName = getSelectedRoute().getName();
        delegate.renamePositionList(name);
        if (trackUndo)
            undoManager.addEdit(new RenamePositionList(this, previousName, name));
    }

    public void addPositionList(int index, BaseRoute route) {
        addPositionList(index, route, true);
    }

    public void addPositionList(int index, BaseRoute route, boolean trackUndo) {
        delegate.addPositionList(index, route);
        if (trackUndo)
            undoManager.addEdit(new AddPositionList(this, index, route));
    }

    public void removePositionList(BaseRoute route) {
        removePositionList(route, true);
    }

    public void removePositionList(BaseRoute route, boolean trackUndo) {
        int index = getIndex(route);
        delegate.removePositionList(route);
        if (trackUndo)
            undoManager.addEdit(new RemovePositionList(this, index, route));
    }
}
