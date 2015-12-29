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
package slash.navigation.mapview.mapsforge.models;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * Joins a {@link ComboBoxModel} and a {@link List}.
 *
 * @author Christian Pesch
 */

public class JoinedListComboBoxModel<E> implements ComboBoxModel<E> {
    private final ComboBoxModel<E> delegate;
    private final List<E> list;

    public JoinedListComboBoxModel(ComboBoxModel<E> delegate, List<E> list) {
        this.delegate = delegate;
        this.list = list;
    }

    public int getSize() {
        return delegate.getSize() + list.size();
    }

    public E getElementAt(int index) {
        return index < delegate.getSize() ? delegate.getElementAt(index) : list.get(index - delegate.getSize());
    }

    public void addListDataListener(ListDataListener l) {
        delegate.addListDataListener(l);
    }

    public void removeListDataListener(ListDataListener l) {
        delegate.removeListDataListener(l);
    }

    public Object getSelectedItem() {
        return delegate.getSelectedItem();
    }

    public void setSelectedItem(Object anItem) {
        delegate.setSelectedItem(anItem);
    }
}
