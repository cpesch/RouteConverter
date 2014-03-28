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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;

/**
 * An {@link ActionListener} that starts and stops the wait cursor on the dialog.
 *
 * @author Christian Pesch
 */

public abstract class DialogAction implements ActionListener {
    private JDialog dialog;

    protected DialogAction(JDialog dialog) {
        this.dialog = dialog;
    }

    protected JDialog getDialog() {
        return dialog;
    }

    public final void actionPerformed(ActionEvent e) {
        startWaitCursor(getDialog().getRootPane());
        try {
            run();
        } finally {
            stopWaitCursor(getDialog().getRootPane());
        }
    }

    public abstract void run();
}