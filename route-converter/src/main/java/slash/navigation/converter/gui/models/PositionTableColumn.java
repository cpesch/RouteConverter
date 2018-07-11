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
import slash.navigation.common.NavigationPosition;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;

/**
 * Acts as a {@link TableColumn} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionTableColumn extends TableColumn {
    public static final String VISIBLE_PROPERTY_NAME = "visible";
    private boolean visible;
    private final String name;
    private final Comparator<NavigationPosition> comparator;

    public PositionTableColumn(int modelIndex, String name, boolean visible,
                               TableCellRenderer cellRenderer, TableCellEditor cellEditor,
                               Comparator<NavigationPosition> comparator) {
        super(modelIndex, 75, cellRenderer, cellEditor);
        this.name = name;
        this.visible = visible;
        this.comparator = comparator;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean newVisible) {
        boolean oldVisible = this.visible;
        this.visible = newVisible;

        PropertyChangeEvent changeEvent = new PropertyChangeEvent(this, VISIBLE_PROPERTY_NAME, oldVisible, newVisible);
        for(PropertyChangeListener changeListener : getPropertyChangeListeners()) {
            changeListener.propertyChange(changeEvent);
        }
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public Comparator<NavigationPosition> getComparator() {
        return comparator;
    }
}
