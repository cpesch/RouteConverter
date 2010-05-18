package slash.navigation.converter.gui.dnd;

import slash.navigation.gui.Application;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;

public class ClipboardInteractor {

    private void enable(boolean enable) {
        Application.getInstance().getContext().getActionManager().enable("paste", enable);
    }

    public void putIntoClipboard(final Transferable transferable) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                enable(false);
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
