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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The base of all navigation graphical user interfaces.
 *
 * @author Christian Pesch
 */

public abstract class BaseNavigationGUI {
    private static final Logger log = Logger.getLogger(BaseNavigationGUI.class.getName());
    protected Preferences preferences = Preferences.userNodeForPackage(getClass());

    private static final String PREFERRED_LANGUAGE_PREFERENCE = "preferredLanguage";
    private static final String PREFERRED_COUNTRY_PREFERENCE = "preferredCountry";
    private static final String X_PREFERENCE = "x";
    private static final String Y_PREFERENCE = "y";
    private static final String WIDTH_PREFERENCE = "width";
    private static final String HEIGHT_PREFERENCE = "height";
    private static final int MAXIMIZE_OFFSET = 4;

    protected JFrame frame;


    public void setLocale(Locale locale) {
        if (!Constants.ROOT_LOCALE.equals(locale)) {
            preferences.put(PREFERRED_LANGUAGE_PREFERENCE, locale.getLanguage());
            preferences.put(PREFERRED_COUNTRY_PREFERENCE, locale.getCountry());
        } else {
            preferences.remove(PREFERRED_LANGUAGE_PREFERENCE);
            preferences.remove(PREFERRED_COUNTRY_PREFERENCE);
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    protected static void setDefaultLocale(Preferences preferences) {
        String language = preferences.get(PREFERRED_LANGUAGE_PREFERENCE, Locale.getDefault().getLanguage());
        String country = preferences.get(PREFERRED_COUNTRY_PREFERENCE, Locale.getDefault().getCountry());
        Locale.setDefault(new Locale(language, country));
    }

    protected ImageIcon loadIcon(String name) {
        URL iconURL = getClass().getResource(name);
        return new ImageIcon(iconURL);
    }

    protected void createFrame(String frameTitle, String iconName, JPanel contentPane, JButton defaultButton) {
        frame = new JFrame();
        frame.setIconImage(loadIcon(iconName).getImage());
        frame.setTitle(frameTitle);
        frame.setContentPane(contentPane);
        frame.getRootPane().setDefaultButton(defaultButton);
    }

    protected void openFrame(JPanel contentPane) {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onExit();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        frame.pack();
        frame.setLocationRelativeTo(null);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        log.info("screen size is " + screenSize);

        int width = crop("width", getPreferenceWidth(), -MAXIMIZE_OFFSET, (int) screenSize.getWidth() + 2 * MAXIMIZE_OFFSET);
        int height = crop("height", getPreferenceHeight(), -MAXIMIZE_OFFSET, (int) screenSize.getHeight() + 2 * MAXIMIZE_OFFSET);
        if (width != -1 && height != -1)
            frame.setSize(width, height);
        log.info("frame size is " + frame.getSize());

        int x = crop("x", preferences.getInt(X_PREFERENCE, -1), -MAXIMIZE_OFFSET, screenSize.width + 2 * MAXIMIZE_OFFSET - width);
        int y = crop("y", preferences.getInt(Y_PREFERENCE, -1), -MAXIMIZE_OFFSET, screenSize.height + 2 * MAXIMIZE_OFFSET - height);
        if (x != -1 && y != -1)
            frame.setLocation(x, y);
        log.info("frame location is " + frame.getLocation());

        frame.setVisible(true);
        frame.toFront();
    }

    protected int getPreferenceHeight() {
        return crop("preferencesHeight", preferences.getInt(HEIGHT_PREFERENCE, -1), 0, Integer.MAX_VALUE);
    }

    protected int getPreferenceWidth() {
        return crop("preferenceWidth", preferences.getInt(WIDTH_PREFERENCE, -1), 0, Integer.MAX_VALUE);
    }

    private int crop(String name, int position, int minimum, int maximum) {
        int result = position < minimum ? (position == -1 ? -1 : minimum) :
                position > maximum ? (position == -1 ? -1 : maximum) : position;
        log.info("Cropping value " + position + " for " + name + " to [" + minimum + ";" + maximum + "] gives " + result);
        return result;
    }

    protected abstract void onExit();

    protected void closeFrame() {
        log.info("Storing frame location as " + frame.getLocation());
        log.info("Storing frame size as " + frame.getSize());
        preferences.putInt(X_PREFERENCE, frame.getLocation().x);
        preferences.putInt(Y_PREFERENCE, frame.getLocation().y);
        preferences.putInt(WIDTH_PREFERENCE, frame.getSize().width);
        preferences.putInt(HEIGHT_PREFERENCE, frame.getSize().height);

        frame.dispose();
    }

}
