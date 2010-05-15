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
import slash.navigation.converter.gui.helper.AbstractListDataListener;

import javax.swing.*;
import javax.swing.event.*;
import java.util.List;

/**
 * Acts as a {@link ComboBoxModel} for the routes of a {@link FormatAndRoutes}.
 *
 * @author Christian Pesch
 */

public class FormatAndRoutesModel extends AbstractListModel implements ComboBoxModel {
    private boolean modified = false;
    private FormatAndRoutes formatAndRoutes;
    private PositionsModel positionsModel = new PositionsModel();
    private CharacteristicsModel characteristicsModel = new CharacteristicsModel();

    public FormatAndRoutesModel() {
        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                // ignore events following setSelectedItem()
                if(e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE)
                    return;
                setModified(true);
            }
        });
        addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                // ignore events following setSelectedItem()
                if (e.getType() == ListDataEvent.CONTENTS_CHANGED && e.getIndex0() == -1 && e.getIndex1() == -1)
                    return;
                setModified(true);
            }
        });
        getCharacteristicsModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }
            public void intervalRemoved(ListDataEvent e) {
            }
            public void contentsChanged(ListDataEvent e) {
                fireContentsChanged(this, -1, -1);
            }
        });
    }

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

    public NavigationFormat<BaseRoute> getFormat() {
        return formatAndRoutes != null ? formatAndRoutes.getFormat() : null;
    }

    public void setFormat(NavigationFormat<BaseRoute> format) {
        formatAndRoutes.setFormat(format);
        fireContentsChanged(this, -1, -1);
    }

    public void addRoute(int index, BaseRoute route) {
        getRoutes().add(index, route);
        fireIntervalAdded(this, index, index);
    }

    public void removeRoute(BaseRoute route) {
        int index = getRoutes().indexOf(route);
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

    public void renameRoute(String name) {
        BaseRoute route = getSelectedRoute();
        route.setName(name);
        int index = getRoutes().indexOf(route);
        fireContentsChanged(this, index, index);
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

    public PositionsModel getPositionsModel() {
        return positionsModel;
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return characteristicsModel;
    }

    public BaseRoute getSelectedRoute() {
        return getPositionsModel().getRoute();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        fireModified();
    }

    protected transient ChangeEvent changeEvent = null;

    protected void fireModified() {
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

    public void setSelectedItem(Object anItem) {
        if ((getSelectedItem() != null && !getSelectedItem().equals(anItem)) ||
                getSelectedItem() == null && anItem != null) {
            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = (BaseRoute<BaseNavigationPosition,BaseNavigationFormat>) anItem;
            getPositionsModel().setRoute(route);
            getCharacteristicsModel().setRoute(route);
            fireContentsChanged(this, -1, -1);
        }
    }
}
