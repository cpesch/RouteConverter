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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Math.max;
import static java.util.logging.Logger.getLogger;
import static slash.navigation.gui.SingleFrameApplication.*;

/**
 * Restores and persists the size and location of a {@link Window} through
 * {@link Preferences}, and enforces a minimum size so a window can never be
 * shrunk small enough to become un-findable. This is the single source of the
 * bounds math shared by {@link SingleFrameApplication} (frames) and
 * {@link SimpleDialog} (dialogs); keys are namespaced by {@code keyPrefix}
 * (empty for the single main frame, {@code "dialogName-"} for dialogs).
 *
 * @author Christian Pesch
 */
public class WindowBounds {
    private static final Logger log = getLogger(WindowBounds.class.getName());

    /**
     * Absolute floor applied on top of the packed size, so even a dialog that
     * packs tiny stays large enough to grab and move.
     */
    static final int ABSOLUTE_MINIMUM_WIDTH = 200;
    static final int ABSOLUTE_MINIMUM_HEIGHT = 120;

    private final Window window;
    private final Preferences preferences;
    private final String keyPrefix;

    public WindowBounds(Window window, Preferences preferences, String keyPrefix) {
        this.window = window;
        this.preferences = preferences;
        this.keyPrefix = keyPrefix;
    }

    /**
     * Clamps {@code position} into [{@code minimum};{@code maximum}], leaving the
     * "unset" sentinel -1 untouched so callers can distinguish "no stored value".
     */
    public static int crop(String name, int position, int minimum, int maximum) {
        int result = position < minimum ? (position == -1 ? -1 : minimum) :
                position > maximum ? (position == -1 ? -1 : maximum) : position;
        log.finer("Cropping value " + position + " for " + name + " to [" + minimum + ";" + maximum + "] gives " + result);
        return result;
    }

    /**
     * Pure size computation: the stored width/height cropped to the usable
     * screen. Returns null when there is no usable stored size (keep the packed
     * size), preserving the historic {@code width > 120 && height > 60} guard.
     */
    public static Dimension computeSize(int savedWidth, int savedHeight, Rectangle bounds, Insets insets) {
        int width = crop("width", savedWidth,
                (int) bounds.getX() - (insets.left + insets.right),
                (int) bounds.getWidth() - (insets.left + insets.right));
        int height = crop("height", savedHeight,
                (int) bounds.getY() - (insets.top + insets.bottom),
                (int) bounds.getHeight() - (insets.top + insets.bottom));
        if (width > 120 && height > 60)
            return new Dimension(width, height);
        return null;
    }

    /**
     * Pure location computation: the stored x/y cropped so the window of the
     * given size stays on the usable screen. Returns null when unset.
     */
    public static Point computeLocation(int savedX, int savedY, int width, int height, Rectangle bounds, Insets insets) {
        int x = crop("x", savedX,
                (int) bounds.getX() + insets.left,
                (int) bounds.getX() + insets.left + (int) bounds.getWidth() - insets.right - width);
        int y = crop("y", savedY,
                (int) bounds.getY() + insets.top,
                (int) bounds.getY() + insets.top + (int) bounds.getHeight() - insets.bottom - height);
        if (x != -1 && y != -1)
            return new Point(x, y);
        return null;
    }

    /**
     * Restores size and location onto the window. The caller must have packed
     * the window first, so its current size is the packed preferred size — that
     * becomes the minimum (every component stays visible), floored by the
     * absolute minimum. A stored size is applied on top, clamped to the minimum;
     * a stored location is applied, else the window is centered on
     * {@code locationRelativeTo}.
     */
    public void restore(Window locationRelativeTo) {
        Dimension packed = window.getSize();
        Dimension minimum = new Dimension(
                max(packed.width, ABSOLUTE_MINIMUM_WIDTH),
                max(packed.height, ABSOLUTE_MINIMUM_HEIGHT));
        window.setMinimumSize(minimum);

        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        Dimension size = computeSize(getInt(WIDTH_PREFERENCE), getInt(HEIGHT_PREFERENCE), bounds, insets);
        if (size != null)
            window.setSize(max(size.width, minimum.width), max(size.height, minimum.height));
        log.info("Window " + keyPrefix + " size is " + window.getSize());

        Point location = computeLocation(getInt(X_PREFERENCE), getInt(Y_PREFERENCE),
                window.getWidth(), window.getHeight(), bounds, insets);
        if (location != null)
            window.setLocation(location);
        else
            window.setLocationRelativeTo(locationRelativeTo);
        log.info("Window " + keyPrefix + " location is " + window.getLocation());
    }

    /**
     * Installs listeners that persist size and location live as the user
     * resizes/moves, matching the main frame's behaviour.
     */
    public void installLivePersistence() {
        window.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                putLocation();
            }

            public void componentResized(ComponentEvent e) {
                putSize();
            }
        });
    }

    public void putLocation() {
        int x = window.getLocation().x;
        int y = window.getLocation().y;
        if (getInt(X_PREFERENCE) == x && getInt(Y_PREFERENCE) == y)
            return;

        preferences.putInt(keyPrefix + X_PREFERENCE, x);
        preferences.putInt(keyPrefix + Y_PREFERENCE, y);
        log.fine("Storing window " + keyPrefix + " location as " + window.getLocation());
    }

    public void putSize() {
        int width = window.getSize().width;
        int height = window.getSize().height;
        if (getInt(WIDTH_PREFERENCE) == width && getInt(HEIGHT_PREFERENCE) == height)
            return;

        preferences.putInt(keyPrefix + WIDTH_PREFERENCE, width);
        preferences.putInt(keyPrefix + HEIGHT_PREFERENCE, height);
        log.fine("Storing window " + keyPrefix + " size as " + window.getSize());
    }

    private int getInt(String key) {
        return preferences.getInt(keyPrefix + key, -1);
    }
}
