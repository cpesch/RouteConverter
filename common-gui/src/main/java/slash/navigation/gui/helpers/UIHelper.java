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
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.WAIT_CURSOR;
import static java.util.logging.Logger.getLogger;
import static java.util.prefs.Preferences.userNodeForPackage;

/**
 * Helpers used throughout the UI
 *
 * @author Christian Pesch
 */

public class UIHelper {
    private static final Preferences preferences = userNodeForPackage(UIHelper.class);
    private static final Logger log = getLogger(UIHelper.class.getName());
    private static final String LOOK_AND_FEEL_CLASS_PREFERENCE = "lookAndFeelClass";

    // for language support which is not defined by a constant in Locale
    public static final Locale ARABIA = new Locale("ar", "SA");
    public static final Locale CZECH = new Locale("cs", "CZ");
    public static final Locale CROATIA = new Locale("hr", "HR");
    public static final Locale NEDERLANDS = new Locale("nl", "NL");
    public static final Locale POLAND = new Locale("pl", "PL");
    public static final Locale RUSSIA = new Locale("ru", "RU");
    public static final Locale SERBIA = new Locale("sr", "SR");
    public static final Locale SLOVAKIA = new Locale("sk", "SK");
    public static final Locale SPAIN = new Locale("es", "ES");

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

    public static void startWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        startWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(true);
    }

    private static void startWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(WAIT_CURSOR));
    }

    public static void stopWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        stopWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(false);
    }

    private static void stopWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(DEFAULT_CURSOR));
    }

    public static ImageIcon loadIcon(String name) {
        URL iconURL = UIHelper.class.getClassLoader().getResource(name);
        return new ImageIcon(iconURL);
    }

    public static JFileChooser createJFileChooser() {
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
                String text = bundle.getString(key);
                if (text != null)
                    UIManager.getDefaults().put(key, text);
            } catch (MissingResourceException e) {
                // intentionally left empty
            }
        }
    }

    private static FontMetrics fontMetrics = null;

    public static int getMaxWidth(String string, int extraWidth) {
        if (fontMetrics == null) {
            JLabel label = new JLabel();
            fontMetrics = label.getFontMetrics(label.getFont());
        }
        int width = fontMetrics.stringWidth(string);
        return width + extraWidth;
    }
}
