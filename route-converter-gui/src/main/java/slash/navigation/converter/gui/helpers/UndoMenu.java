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

package slash.navigation.converter.gui.helpers;

import slash.navigation.gui.Application;
import slash.navigation.gui.undo.UndoManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Synchronizes the texts of {@link JMenuItem}s with the {@link UndoManager}.
 *
 * @author Christian Pesch
 */

public class UndoMenu {
    private final JMenuItem undoMenuItem;
    private final JMenuItem redoMenuItem;
    private final UndoManager undoManager;

    public UndoMenu(JMenuItem undoMenuItem, JMenuItem redoMenuItem, UndoManager undoManager) {
        this.undoMenuItem = undoMenuItem;
        this.redoMenuItem = redoMenuItem;
        this.undoManager = undoManager;
        initializeMenu();
    }

    private void setText(JMenuItem menuItem, String undoText) {
        String text = menuItem.getText();
        int index = text.indexOf(": ");
        if (index != -1)
            text = text.substring(0, index);
        if (undoText != null)
            text = text + ": " + undoText;
        menuItem.setText(text);
    }

    private void initializeMenu() {
        undoManager.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setText(undoMenuItem, undoManager.canUndo() ?
                        Application.getInstance().getContext().getBundle().getString(undoManager.getUndoPresentationName()) :
                        null);
                setText(redoMenuItem, undoManager.canRedo() ?
                        Application.getInstance().getContext().getBundle().getString(undoManager.getRedoPresentationName()) :
                        null);
            }
        });
    }
}