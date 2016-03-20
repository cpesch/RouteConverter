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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.Transparency.TRANSLUCENT;

/**
 * A helper for simplified {@link BufferedImage} operations.
 *
 * @author Christian Pesch
 */
public class ImageHelper {
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, TRANSLUCENT);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.addRenderingHints(new RenderingHints(KEY_RENDERING, VALUE_RENDER_QUALITY));
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return bufferedImage;
    }

    public static BufferedImage resize(File file, int height) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null)
                return null;
            double factor = (double) height / image.getHeight();
            return resize(image, (int) (image.getWidth() * factor), height);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
