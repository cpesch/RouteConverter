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
import java.awt.event.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The base of all single frame graphical user interfaces.
 *
 * @author Christian Pesch
 */

public abstract class SingleFrameApplication extends Application {
    private static final Logger log = Logger.getLogger(SingleFrameApplication.class.getName());
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());

    static final String X_PREFERENCE = "x";
    static final String Y_PREFERENCE = "y";
    static final String WIDTH_PREFERENCE = "width";
    static final String HEIGHT_PREFERENCE = "height";
    private static final String STATE_PREFERENCE = "state";
    private static final String DEVICE_PREFERENCE = "device";
    static final int MAXIMIZE_OFFSET = 4;

    protected JFrame frame;

    public JFrame getFrame() {
        return frame;
    }

    protected void createFrame(String frameTitle, String iconName, JPanel contentPane, JButton defaultButton,
                               JMenuBar menuBar) {
        GraphicsConfiguration gc = null;
        String deviceId = preferences.get(DEVICE_PREFERENCE, null);
        if (deviceId != null) {
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice device : devices) {
                if (deviceId.equals(device.getIDstring())) {
                    gc = device.getDefaultConfiguration();
                    log.info("Graphics device is " + deviceId);
                    break;
                }
            }
        }

        frame = new JFrame(frameTitle, gc);
        frame.setIconImage(Constants.loadIcon(this, iconName).getImage());
        frame.setContentPane(contentPane);
        if (defaultButton != null)
            frame.getRootPane().setDefaultButton(defaultButton);
        if (menuBar != null)
            frame.getRootPane().setJMenuBar(menuBar);
    }

    protected void openFrame(JPanel contentPane) {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit(e);
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit(e);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        frame.pack();
        frame.setLocationRelativeTo(null);

        Rectangle bounds = frame.getGraphicsConfiguration().getBounds();
        log.info("Screen size is " + bounds);

        int width = crop("width", getPreferenceWidth(), -MAXIMIZE_OFFSET, (int) bounds.getWidth() + 2 * MAXIMIZE_OFFSET);
        int height = crop("height", getPreferenceHeight(), -MAXIMIZE_OFFSET, (int) bounds.getHeight() + 2 * MAXIMIZE_OFFSET);
        if (width != -1 && height != -1)
            frame.setSize(width, height);
        log.info("Frame size is " + frame.getSize());

        int x = crop("x", preferences.getInt(X_PREFERENCE, -1),
                (int) bounds.getX() - MAXIMIZE_OFFSET,
                (int) bounds.getX() + (int) bounds.getWidth() + 2 * MAXIMIZE_OFFSET - width);
        int y = crop("y", preferences.getInt(Y_PREFERENCE, -1),
                (int) bounds.getY() - MAXIMIZE_OFFSET,
                (int) bounds.getY() + (int) bounds.getHeight() + 2 * MAXIMIZE_OFFSET - height);
        if (x != -1 && y != -1)
            frame.setLocation(x, y);
        log.info("Frame location is " + frame.getLocation());

        frame.setExtendedState(preferences.getInt(STATE_PREFERENCE, Frame.NORMAL));
        log.info("Frame state is " + frame.getExtendedState());

        frame.setVisible(true);
        frame.toFront();
    }

    private int getPreferenceHeight() {
        return crop("preferencesHeight", preferences.getInt(HEIGHT_PREFERENCE, -1), 0, Integer.MAX_VALUE);
    }

    private int getPreferenceWidth() {
        return crop("preferenceWidth", preferences.getInt(WIDTH_PREFERENCE, -1), 0, Integer.MAX_VALUE);
    }

    static int crop(String name, int position, int minimum, int maximum) {
        int result = position < minimum ? (position == -1 ? -1 : minimum) :
                position > maximum ? (position == -1 ? -1 : maximum) : position;
        log.info("Cropping value " + position + " for " + name + " to [" + minimum + ";" + maximum + "] gives " + result);
        return result;
    }

    void closeFrame() {
        int state = frame.getExtendedState();
        if ((state & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {
            log.info("Storing frame location as " + frame.getLocation());
            log.info("Storing frame size as " + frame.getSize());
            preferences.putInt(X_PREFERENCE, frame.getLocation().x);
            preferences.putInt(Y_PREFERENCE, frame.getLocation().y);
            preferences.putInt(WIDTH_PREFERENCE, frame.getSize().width);
            preferences.putInt(HEIGHT_PREFERENCE, frame.getSize().height);
        }

        String deviceId = frame.getGraphicsConfiguration().getDevice().getIDstring();
        log.info("Storing frame state as " + state);
        log.info("Storing graphics device as " + deviceId);
        preferences.putInt(STATE_PREFERENCE, state);
        preferences.put(DEVICE_PREFERENCE, deviceId);

        frame.dispose();
    }

    protected void shutdown() {
        closeFrame();
    }
}