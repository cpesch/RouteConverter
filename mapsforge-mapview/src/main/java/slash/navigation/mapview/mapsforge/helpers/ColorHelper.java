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
package slash.navigation.mapview.mapsforge.helpers;

import slash.navigation.converter.gui.models.ColorModel;

import java.awt.*;

/**
 * Provides {@link Color} helpers.
 *
 * @author Christian Pesch
 */

public class ColorHelper {
    private static final int MINIMUM_ALPHA = (int)(256 * 0.3);

    public static int asRGBA(ColorModel colorModel) {
        return asRGBA(colorModel.getColor());
    }

    static int asRGBA(Color color) {
        int rgba = color.getRGB();
        int alpha = MINIMUM_ALPHA + (int)(color.getAlpha() * (256.0 / MINIMUM_ALPHA) * 256.0);
        alpha = alpha << 24;
        return alpha | rgba;
    }
}
