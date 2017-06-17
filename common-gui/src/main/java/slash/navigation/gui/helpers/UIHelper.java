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

package slash.navigation.gui.helpers;

import javax.swing.*;
import java.awt.*;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.WAIT_CURSOR;
import static java.awt.dnd.DragSource.DefaultMoveDrop;
import static java.util.logging.Logger.getLogger;
import static java.util.prefs.Preferences.userNodeForPackage;
import static slash.common.system.Platform.isWindows;

/**
 * Helpers used throughout the UI
 *
 * @author Christian Pesch
 */

public class UIHelper {
    private static final Preferences preferences = userNodeForPackage(UIHelper.class);
    private static final Logger log = getLogger(UIHelper.class.getName());
    private static final String LOOK_AND_FEEL_CLASS_PREFERENCE = "lookAndFeelClass";

    public static void setLookAndFeel() {
        try {
            String lookAndFeelClass = preferences.get(LOOK_AND_FEEL_CLASS_PREFERENCE, "default");
            if ("default".equals(lookAndFeelClass))
                lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeelClass);
        } catch (Exception e) {
            // intentionally do nothing
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    public static void setUseSystemProxies() {
        System.setProperty("java.net.useSystemProxies", "true");
    }

    public static void startWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        startWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(true);
    }

    private static void startWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(WAIT_CURSOR));
    }

    public static void startDragCursor(Component component) {
        component.setCursor(DefaultMoveDrop);
    }

    public static void stopWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        stopWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(false);
    }

    public static void stopWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(DEFAULT_CURSOR));
    }

    public static JFileChooser createJFileChooser() {
        if(isWindows()) {
            // workaround
            // https://bugs.openjdk.java.net/browse/JDK-8179014
            UIManager.put("FileChooser.useSystemExtensionHiding", false);
        }

        JFileChooser chooser;
        try {
            try {
                chooser = new JFileChooser();
            } catch (NullPointerException npe) {
                log.info("Working around http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6210674 by using Metal UI");
                UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                chooser = new JFileChooser();
            }
        } catch (Exception e) {
            log.info("Working around http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6544857 by using restricted file system view");
            chooser = new JFileChooser(new RestrictedFileSystemView());
        }
        return chooser;
    }

    public static void patchUIManager(ResourceBundle bundle, String... keys) {
        for (String key : keys) {
            try {
                if (bundle.containsKey(key))
                    UIManager.getDefaults().put(key, bundle.getString(key));
            } catch (MissingResourceException e) {
                // intentionally left empty
            }
        }
    }

    private static FontMetrics fontMetrics;

    public static int getMaxWidth(String string, int extraWidth) {
        if (fontMetrics == null) {
            JLabel label = new JLabel();
            fontMetrics = label.getFontMetrics(label.getFont());
        }
        int width = fontMetrics.stringWidth(string);
        return width + extraWidth;
    }
}
