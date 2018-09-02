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
package slash.navigation.converter.gui.dnd;

import slash.navigation.gui.Application;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.dnd.PositionSelection.POSITION_FLAVOR;
import static slash.navigation.converter.gui.dnd.PositionSelection.STRING_FLAVOR;

/**
 * Helps to interact with the system {@link Clipboard}.
 *
 * @author Christian Pesch
 */

public class ClipboardInteractor {
    private static final Logger log = Logger.getLogger(ClipboardInteractor.class.getName());

    private void enable(boolean enable) {
        Application.getInstance().getContext().getActionManager().enable("paste", enable);
    }

    public void watchClipboard() {
        getClipboard().addFlavorListener(new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
                enable(isSupportedFlavor());
            }
        });
    }

    private Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    private boolean isSupportedFlavor() {
        try {
            for (DataFlavor f : getClipboard().getAvailableDataFlavors()) {
                if (f.equals(POSITION_FLAVOR) || f.equals(STRING_FLAVOR))
                    return true;
            }
        }
        catch (IllegalStateException e) {
            log.warning("Cannot get available data flavors: " + e);
        }
        return false;
    }

    public void putIntoClipboard(final Transferable transferable) {
        getClipboard().setContents(transferable, new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                enable(isSupportedFlavor());
            }
        });

        // invoke later to be behind the lost ownership notification that is sent if two or more
        // cut or copy actions are executed by the user in a row
        invokeLater(new Runnable() {
            public void run() {
                enable(true);
            }
        });
    }

    public Transferable getFromClipboard() {
        try {
            return getClipboard().getContents(null);
        }
        catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        return null;
    }
}
