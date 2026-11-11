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
import java.awt.datatransfer.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
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

    private final Clipboard clipboard;
    private final Consumer<Boolean> pasteEnabler;

    public ClipboardInteractor() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard(),
                enable -> Application.getInstance().getContext().getActionManager().enable("paste", enable));
    }

    ClipboardInteractor(Clipboard clipboard, Consumer<Boolean> pasteEnabler) {
        this.clipboard = clipboard;
        this.pasteEnabler = pasteEnabler;
    }

    private void enable(boolean enable) {
        pasteEnabler.accept(enable);
    }

    public void watchClipboard() {
        clipboard.addFlavorListener(new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
                enable(isSupportedFlavor());
            }
        });

        // another RouteConverter instance that copied positions while this instance held clipboard
        // ownership may have published the same flavors it lost, which fires no FlavorEvent; re-check
        // when this instance becomes active again, since the other instance has finished writing by then
        getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow", e -> {
            if (e.getNewValue() != null)
                refresh();
        });
    }

    void refresh() {
        enable(isSupportedFlavor());
    }

    private boolean isSupportedFlavor() {
        try {
            return isSupportedFlavor(clipboard.getAvailableDataFlavors());
        }
        catch (IllegalStateException e) {
            log.warning("Cannot get available data flavors: " + e);
        }
        return false;
    }

    static boolean isSupportedFlavor(DataFlavor[] flavors) {
        for (DataFlavor f : flavors) {
            if (f.equals(POSITION_FLAVOR) || f.equals(STRING_FLAVOR))
                return true;
        }
        return false;
    }

    public void putIntoClipboard(final Transferable transferable) {
        clipboard.setContents(transferable, new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                // intentionally left empty: reading the clipboard here races with the new owner
                // still writing its formats on Windows (issue #360) and can observe an empty or
                // partial format list and disable paste; the FlavorListener already reacts to any
                // real change of the format set once the new owner has closed the clipboard, and
                // if the format set is unchanged, the new content is just as pasteable as the old
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
            return clipboard.getContents(null);
        }
        catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        return null;
    }
}
