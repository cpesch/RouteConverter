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

package slash.navigation.gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Constants used throughout the UI
 *
 * @author Christian Pesch
 */

public class Constants {
    private static final Logger log = Logger.getLogger(Constants.class.getName());

    // for language support which is not defined by a constant in Locale
    public static final Locale ARABIA = new Locale("ar", "SA");
    public static final Locale NEDERLANDS = new Locale("nl", "NL");
    public static final Locale CROATIA = new Locale("hr", "HR");
    public static final Locale SERBIA = new Locale("sr", "SR");
    public static final Locale SLOVAKIA = new Locale("sk", "SK");
    public static final Locale SPAIN = new Locale("es", "ES");

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void stopWaitCursor(JComponent component) {
        RootPaneContainer root = (RootPaneContainer) component.getTopLevelAncestor();
        stopWaitCursor(root.getGlassPane());
        root.getGlassPane().setVisible(false);
    }

    private static void stopWaitCursor(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public static ImageIcon loadIcon(String name) {
        URL iconURL = Constants.class.getClassLoader().getResource(name);
        return new ImageIcon(iconURL);
    }

    public static JFileChooser createJFileChooser() {
        JFileChooser chooser;
        try {
            try {
                chooser = new JFileChooser();
            }
            catch (NullPointerException npe) {
                log.info("Working around http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6210674 by using Metal UI");
                UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                chooser = new JFileChooser();
            }
        }
        catch (Exception e) {
            log.info("Working around http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6544857 by using restricted file system view");
            chooser = new JFileChooser(new RestrictedFileSystemView());
        }
        return chooser;
    }
}
