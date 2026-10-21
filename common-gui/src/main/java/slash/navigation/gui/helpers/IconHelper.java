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

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads icons as {@link ImageIcon}s, attaching their {@code @2x} variant as a
 * {@link BaseMultiResolutionImage} when one exists next to the base image, so that
 * FlatLaf can paint sharp and greyed icons on scaled Windows and Linux displays.
 *
 * @author Christian Pesch
 */

public class IconHelper {
    private static final Map<String, Optional<ImageIcon>> cache = new ConcurrentHashMap<>();

    public static ImageIcon loadIcon(String path) {
        return cache.computeIfAbsent(path, IconHelper::loadIconUncached).orElse(null);
    }

    private static Optional<ImageIcon> loadIconUncached(String path) {
        URL baseUrl = IconHelper.class.getResource(path);
        if (baseUrl == null)
            return Optional.empty();

        ImageIcon baseIcon = new ImageIcon(baseUrl);
        URL retinaUrl = IconHelper.class.getResource(toRetinaPath(path));
        if (retinaUrl == null)
            return Optional.of(baseIcon);

        try {
            BufferedImage retinaImage = ImageIO.read(retinaUrl);
            return Optional.of(new ImageIcon(new BaseMultiResolutionImage(baseIcon.getImage(), retinaImage)));
        } catch (IOException e) {
            return Optional.of(baseIcon);
        }
    }

    private static String toRetinaPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot == -1 ? path + "@2x" : path.substring(0, lastDot) + "@2x" + path.substring(lastDot);
    }
}
