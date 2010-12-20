package slash.navigation.converter.gui.dnd;

import slash.navigation.gui.Application;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

public class ClipboardInteractor {
    private static final Logger log = Logger.getLogger(ClipboardInteractor.class.getName());

    private void enable(boolean enable) {
        Application.getInstance().getContext().getActionManager().enable("paste", enable);
    }

    public void watchClipboard() {
        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
                enable(isSupportedFlavor());
            }
        });
    }

    private boolean isSupportedFlavor() {
        try {
            for (DataFlavor f : Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors()) {
                if (f.equals(PositionSelection.positionFlavor) || f.equals(PositionSelection.stringFlavor))
                    return true;
            }
        }
        catch (IllegalStateException e) {
            log.warning("Cannot get available data flavors: " + e.getMessage());
        }
        return false;
    }

    public void putIntoClipboard(final Transferable transferable) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                enable(isSupportedFlavor());
            }
        });

        // invoke later to be behind the lost ownership notification that is sent if two or more
        // cut or copy actions are executed by the user in a row
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                enable(true);
            }
        });
    }

    public Transferable getFromClipboard() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        }
        catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        return null;
    }
}
