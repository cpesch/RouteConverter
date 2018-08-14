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
package slash.navigation.mapview.browser.helpers;

import java.awt.*;

import static slash.common.type.HexadecimalNumber.encodeByte;

/**
 * Provides {@link Color} helpers.
 *
 * @author Christian Pesch
 */

public class ColorHelper {
    private static final float MINIMUM_OPACITY = 0.3f;

    public static float asOpacity(Color color) {
        return MINIMUM_OPACITY + color.getAlpha() / 256f * (1 - MINIMUM_OPACITY);
    }

    public static String asColor(Color color) {
        return encodeByte((byte) color.getRed()) + encodeByte((byte) color.getGreen()) + encodeByte((byte) color.getBlue());
    }
}
