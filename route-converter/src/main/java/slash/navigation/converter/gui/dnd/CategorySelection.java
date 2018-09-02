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

package slash.navigation.converter.gui.dnd;

import slash.navigation.routes.impl.CategoryTreeNode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

/**
 * Acts as a container for drag and drop operations with {@link CategoryTreeNode}s.
 *
 * @author Christian Pesch
 */

public class CategorySelection implements Transferable {
    public static final DataFlavor categoryFlavor = new DataFlavor(CategorySelection.class, "List of Categories");

    private final List<CategoryTreeNode> categories;

    public CategorySelection(List<CategoryTreeNode> categories) {
        this.categories = categories;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{categoryFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return categoryFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor))
            throw new UnsupportedFlavorException(categoryFlavor);
        return categories;
    }
}