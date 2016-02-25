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

package slash.navigation.gui.actions;

import slash.navigation.gui.SimpleDialog;

import java.util.logging.Logger;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Show a dialog at most once.
 *
 * @author Christian Pesch
 */

public abstract class SingletonDialogAction extends FrameAction {
    private static final Logger log = Logger.getLogger(SingletonDialogAction.class.getName());
    private SimpleDialog dialog = null;

    protected abstract SimpleDialog createDialog();

    public void run() {
        try {
            if (dialog == null) {
                dialog = createDialog();
                dialog.pack();
                dialog.restoreLocation();
            }
            if (!dialog.isVisible()) {
                dialog.toFront();
                dialog.setVisible(true);
            }
        } catch(Exception e) {
            e.printStackTrace();
            log.severe("Could not open dialog: " + e);
            showMessageDialog(null, "Could not open dialog: " + getLocalizedMessage(e), "Error", ERROR_MESSAGE);
        }
    }
}
