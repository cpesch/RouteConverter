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

import slash.navigation.gui.Application;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;
import static java.lang.Math.min;
import static slash.common.io.Files.shortenPath;

/**
 * Formats a list of items into a localized, length-bounded string for
 * user-facing dialog text, e.g. {@code 'a',\n'b' and\n'c'}. Unlike the
 * uncapped {@link slash.common.io.Files#asLogString log formatter}, this caps
 * the number of shown items with a localized "and N more" suffix so a long
 * list can never grow a dialog past the screen, and shortens only actual file
 * paths.
 *
 * @author Christian Pesch
 */
public class DialogStrings {
    private static final int DEFAULT_MAXIMUM_ITEMS = 10;
    private static final int MAXIMUM_PATH_LENGTH = 60;

    public static String asDialogString(List<?> items) {
        return asDialogString(items, false);
    }

    public static String asDialogString(List<?> items, boolean shortenPaths) {
        ResourceBundle bundle = Application.getInstance().getContext().getBundle();
        return formatList(items, DEFAULT_MAXIMUM_ITEMS, shortenPaths,
                bundle.getString("list-none"), bundle.getString("list-and"), bundle.getString("list-more"));
    }

    /**
     * Pure, bundle-free core so it can be unit-tested headless. {@code moreText}
     * is a {@link java.text.MessageFormat} pattern taking the count of hidden
     * items, e.g. {@code "and {0} more"}.
     */
    static String formatList(List<?> items, int maximumItems, boolean shortenPaths,
                             String noneText, String andText, String moreText) {
        if (items == null || items.isEmpty())
            return noneText;

        int shown = min(items.size(), maximumItems);
        boolean capped = items.size() > maximumItems;

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (i > 0)
                buffer.append(!capped && i == shown - 1 ? " " + andText + "\n" : ",\n");
            buffer.append("'").append(display(items.get(i), shortenPaths)).append("'");
        }
        if (capped)
            buffer.append(",\n").append(format(moreText, items.size() - shown));
        return buffer.toString();
    }

    private static String display(Object item, boolean shortenPaths) {
        String string = item.toString();
        return shortenPaths || item instanceof File ? shortenPath(string, MAXIMUM_PATH_LENGTH) : string;
    }
}
