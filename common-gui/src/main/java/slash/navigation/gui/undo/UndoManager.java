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

package slash.navigation.gui.undo;

import slash.navigation.gui.Application;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoableEdit;
import java.util.prefs.Preferences;

/**
 * Manages the {@link UndoAction} and {@link RedoAction} of an {@link Application}.
 *
 * @author Christian Pesch
 */

public class UndoManager {
    private static final Preferences preferences = Preferences.userNodeForPackage(UndoManager.class);
    private final javax.swing.undo.UndoManager delegate = new javax.swing.undo.UndoManager();
    private final EventListenerList listenerList = new EventListenerList();

    public UndoManager() {
        delegate.setLimit(preferences.getInt("undoLimit", -1));
    }

    public boolean canUndo() {
        return delegate.canUndo();
    }

    public boolean canRedo() {
        return delegate.canRedo();
    }

    public String getUndoPresentationName() {
        return delegate.getUndoPresentationName();
    }

    public String getRedoPresentationName() {
        return delegate.getRedoPresentationName();
    }

    public void undo() {
        delegate.undo();
        fireChanged();
    }

    public void redo() {
        delegate.redo();
        fireChanged();
    }

    public void addEdit(UndoableEdit undoableEdit) {
        delegate.addEdit(undoableEdit);
        fireChanged();
    }

    public void discardAllEdits() {
        delegate.discardAllEdits();
        fireChanged();
    }

    protected void fireChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
}
