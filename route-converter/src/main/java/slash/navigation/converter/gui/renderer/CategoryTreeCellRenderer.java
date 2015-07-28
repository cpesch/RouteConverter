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

package slash.navigation.converter.gui.renderer;

import slash.navigation.routes.impl.CategoryTreeNode;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static slash.navigation.gui.helpers.UIHelper.loadIcon;

/**
 * Renders the {@link CategoryTreeNode} names.
 *
 * @author Christian Pesch
 */

public class CategoryTreeCellRenderer extends DefaultTreeCellRenderer {
    public CategoryTreeCellRenderer() {
        setOpenIcon(loadIcon("slash/navigation/converter/gui/16/folder-open.png"));
        setClosedIcon(loadIcon("slash/navigation/converter/gui/16/folder.png"));
        setLeafIcon(getClosedIcon());
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if(value instanceof CategoryTreeNode) {
            CategoryTreeNode categoryTreeNode = (CategoryTreeNode) value;
            String name = categoryTreeNode.getName();
            if (name == null)
                name = RouteConverter.getBundle().getString("no-name");
            else if(categoryTreeNode.isRemoteRoot())
                name = RouteConverter.getBundle().getString("remote-catalog");

            label.setText(name);
        } else
            label.setText(RouteConverter.getBundle().getString("loading"));
        return label;
    }
}