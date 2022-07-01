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
package slash.navigation.maps.item;

import javax.swing.*;
import java.awt.*;

import static java.lang.Math.max;

/**
 * A combobox for {@link Item}s that opens a popup that is wide enough to display the description.
 *
 * Taken from http://www.jroller.com/santhosh/entry/make_jcombobox_popup_wide_enough
 * Got this workaround from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607
 *
 * @author ds10git
 */

public class ItemComboBox<E extends Item> extends JComboBox<E> {
    private static final int SCROLLBAR_WIDTH = 30;
    private boolean layingOut = false;

    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    public Dimension getSize() {
        Dimension size = super.getSize();

        if (!layingOut) {
            FontMetrics fontMetrics = getFontMetrics(getFont());

            for (int i = 0; i < getItemCount(); i++) {
                E item = getItemAt(i);

                if (item.getDescription() != null)
                    size.width = max(size.width, fontMetrics.stringWidth(item.getDescription()) + SCROLLBAR_WIDTH);
            }

            size.width = max(size.width, getPreferredSize().width);
        }

        return size;
    }
}
