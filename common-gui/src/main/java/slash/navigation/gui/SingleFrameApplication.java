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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.awt.Frame.MAXIMIZED_HORIZ;
import static java.awt.Frame.MAXIMIZED_VERT;
import static java.awt.Frame.NORMAL;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.lang.Integer.MAX_VALUE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static slash.navigation.gui.helpers.UIHelper.loadIcon;

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
        frame.setIconImage(loadIcon(iconName).getImage());
        frame.setContentPane(contentPane);
        if (defaultButton != null)
            frame.getRootPane().setDefaultButton(defaultButton);
        if (menuBar != null) {
            frame.getRootPane().setJMenuBar(menuBar);
            getContext().setMenuBar(menuBar);
        }
    }

    protected void openFrame(JPanel contentPane) {
        frame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit(e);
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit(e);
            }
        }, KeyStroke.getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        frame.pack();
        frame.setLocationRelativeTo(null);

        Rectangle bounds = frame.getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        log.info("Screen size is " + bounds + ", insets are " + insets);

        int state = preferences.getInt(STATE_PREFERENCE, NORMAL);
        int width = crop("width", getPreferenceWidth(),
                (int) bounds.getX() - (insets.left + insets.right),
                (int) bounds.getWidth() - (insets.left + insets.right));
        int height = crop("height", getPreferenceHeight(),
                (int) bounds.getY() - (insets.top + insets.bottom),
                (int) bounds.getHeight() - (insets.top + insets.bottom));
        if((state & MAXIMIZED_HORIZ) == MAXIMIZED_HORIZ)
            width = (int)bounds.getWidth() - (insets.left + insets.right);
        if((state & MAXIMIZED_VERT) == MAXIMIZED_VERT)
            height = (int)bounds.getHeight() - (insets.top + insets.bottom);
        if (width != -1 && height != -1)
            frame.setSize(width, height);
        log.info("Frame size is " + frame.getSize());

        int x = crop("x", preferences.getInt(X_PREFERENCE, -1),
                (int) bounds.getX() + insets.left,
                (int) bounds.getX() + insets.left + (int) bounds.getWidth() - insets.right - width);
        int y = crop("y", preferences.getInt(Y_PREFERENCE, -1),
                (int) bounds.getY() + insets.top,
                (int) bounds.getY() + insets.top + (int) bounds.getHeight() - insets.bottom - height);
        if ((state & MAXIMIZED_HORIZ) == MAXIMIZED_HORIZ)
            x = insets.left;
        if ((state & MAXIMIZED_VERT) == MAXIMIZED_VERT)
            y = insets.top;
        if (x != -1 && y != -1)
            frame.setLocation(x, y);
        log.info("Frame location is " + frame.getLocation());

        frame.setExtendedState(state);
        log.info("Frame state is " + frame.getExtendedState());

        frame.setVisible(true);
        frame.toFront();
    }

    private int getPreferenceHeight() {
        return crop("preferencesHeight", preferences.getInt(HEIGHT_PREFERENCE, -1), 0, MAX_VALUE);
    }

    private int getPreferenceWidth() {
        return crop("preferenceWidth", preferences.getInt(WIDTH_PREFERENCE, -1), 0, MAX_VALUE);
    }

    static int crop(String name, int position, int minimum, int maximum) {
        int result = position < minimum ? (position == -1 ? -1 : minimum) :
                position > maximum ? (position == -1 ? -1 : maximum) : position;
        log.fine("Cropping value " + position + " for " + name + " to [" + minimum + ";" + maximum + "] gives " + result);
        return result;
    }

    void closeFrame() {
        preferences.putInt(X_PREFERENCE, frame.getLocation().x);
        preferences.putInt(Y_PREFERENCE, frame.getLocation().y);
        log.info("Storing frame location as " + frame.getLocation());
        preferences.putInt(WIDTH_PREFERENCE, frame.getSize().width);
        preferences.putInt(HEIGHT_PREFERENCE, frame.getSize().height);
        log.info("Storing frame size as " + frame.getSize());

        int state = frame.getExtendedState();
        preferences.putInt(STATE_PREFERENCE, state);
        log.info("Storing frame state as " + state);

        String deviceId = frame.getGraphicsConfiguration().getDevice().getIDstring();
        preferences.put(DEVICE_PREFERENCE, deviceId);
        log.info("Storing graphics device as " + deviceId);

        frame.dispose();
    }

    protected void shutdown() {
        closeFrame();
    }
}