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
import slash.navigation.base.FormatAndRoutes;
import slash.navigation.base.NavigationFormat;
import slash.navigation.converter.gui.helpers.AbstractListDataListener;
import slash.navigation.gui.events.IgnoreEvent;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.List;

import static slash.navigation.gui.events.IgnoreEvent.IGNORE;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * Acts as a {@link ComboBoxModel} for the routes of a {@link FormatAndRoutes}.
 *
 * @author Christian Pesch
 */

public class FormatAndRoutesModelImpl extends AbstractListModel implements FormatAndRoutesModel {
    private final PositionsModel positionsModel;
    private final CharacteristicsModel characteristicsModel;
    private boolean modified;
    private FormatAndRoutes formatAndRoutes;

    public FormatAndRoutesModelImpl(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;
        this.characteristicsModel = characteristicsModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                // ignore events following setSelectedRoute()
                if (isFirstToLastRow(e))
                    return;
                // ignore distance column updates from the overlay position model
                if (e.getColumn() == DISTANCE_COLUMN_INDEX)
                    return;
                setModified(true);
            }
        });
        addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                // ignore events following setSelectedRoute()
                if (IgnoreEvent.isIgnoreEvent(e))
                    return;
                setModified(true);
            }
        });
        characteristicsModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }

            public void intervalRemoved(ListDataEvent e) {
            }

            public void contentsChanged(ListDataEvent e) {
                // ignore events following setRoute()
                if (IgnoreEvent.isIgnoreEvent(e))
                    return;
                if (formatAndRoutes.getFormat().isWritingRouteCharacteristics())
                    setModified(true);
                fireContentsChanged(this, IGNORE, IGNORE);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<BaseRoute> getRoutes() {
        return formatAndRoutes != null ? formatAndRoutes.getRoutes() : null;
    }

    public void setRoutes(FormatAndRoutes<BaseNavigationFormat, BaseRoute, BaseNavigationPosition> formatAndRoutes) {
        int index1 = getRoutes() != null ? getRoutes().size() - 1 : 0;
        if (index1 != -1)
            fireIntervalRemoved(this, 0, index1);
        this.formatAndRoutes = formatAndRoutes;
        index1 = getRoutes().size() - 1;
        if (index1 != -1) {
            setSelectedItem(getRoutes().get(0));
            fireIntervalAdded(this, 0, index1);
        } else {
            setSelectedItem(null);
        }
        setModified(false);
    }

    @SuppressWarnings("unchecked")
    public NavigationFormat<BaseRoute> getFormat() {
        return formatAndRoutes != null ? formatAndRoutes.getFormat() : null;
    }

    @SuppressWarnings("unchecked")
    public void setFormat(NavigationFormat<BaseRoute> format) {
        formatAndRoutes.setFormat(format);
        fireContentsChanged(this, IGNORE, IGNORE);
    }

    public void addPositionList(int index, BaseRoute route) {
        getRoutes().add(index, route);
        fireIntervalAdded(this, index, index);
    }

    public void renamePositionList(String name) {
        BaseRoute route = getSelectedRoute();
        route.setName(name);
        int index = getRoutes().indexOf(route);
        fireContentsChanged(this, index, index);
    }

    public void removePositionList(BaseRoute route) {
        int index = getIndex(route);
        if (index != -1) {
            if (getElementAt(index) == getSelectedRoute()) {
                if (index == 0) {
                    setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
                } else {
                    setSelectedItem(getElementAt(index - 1));
                }
            }
            getRoutes().remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    public int getSize() {
        return getRoutes() != null ? getRoutes().size() : 0;
    }

    public Object getElementAt(int index) {
        return getRoute(index);
    }

    public BaseRoute getRoute(int index) {
        return getRoutes().get(index);
    }

    public int getIndex(BaseRoute route) {
        return getRoutes().indexOf(route);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        if (this.modified != modified) {
            this.modified = modified;
            fireModified();
        }
    }

    private void fireModified() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
            }
        }
    }

    public void addModifiedListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public Object getSelectedItem() {
        return getSelectedRoute();
    }

    public BaseRoute getSelectedRoute() {
        return positionsModel.getRoute();
    }

    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object anItem) {
        setSelectedRoute((BaseRoute) anItem);
    }

    @SuppressWarnings("unchecked")
    public void setSelectedRoute(BaseRoute route) {
        if ((getSelectedRoute() != null && !getSelectedRoute().equals(route)) ||
                getSelectedRoute() == null && route != null) {
            positionsModel.setRoute(route);
            characteristicsModel.setRoute(route);
            fireContentsChanged(this, IGNORE, IGNORE);
        }
    }
}
