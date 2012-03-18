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

package slash.navigation.converter.gui.actions;

import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.RouteServiceOperator;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.gui.FrameAction;

import javax.swing.*;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.showInputDialog;
import static slash.common.io.Transfer.trim;
import static slash.navigation.converter.gui.helper.JTreeHelper.getSelectedCategoryTreeNode;

/**
 * {@link Action} that renames a category of the {@link RouteServiceOperator}.
 *
 * @author Christian Pesch
 */

public class RenameCategoryAction extends FrameAction {
    private final JTree tree;

    public RenameCategoryAction(JTree tree) {
        this.tree = tree;
    }

    public void run() {
        RouteConverter r = RouteConverter.getInstance();
        CategoryTreeNode selected = getSelectedCategoryTreeNode(tree);
        String name = (String) showInputDialog(r.getFrame(),
                format(RouteConverter.getBundle().getString("rename-category-label"), selected.getName()),
                r.getFrame().getTitle(), QUESTION_MESSAGE, null, null, selected.getName());
        if (trim(name) == null)
            return;

        ((CatalogModel) tree.getModel()).rename(selected, name);
    }
}