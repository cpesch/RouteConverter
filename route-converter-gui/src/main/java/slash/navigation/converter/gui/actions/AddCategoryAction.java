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

import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.helpers.WindowHelper;
import slash.navigation.routes.Category;
import slash.navigation.routes.impl.CategoryTreeNode;

import javax.swing.*;

import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.showInputDialog;
import static slash.common.io.Transfer.trim;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedCategoryTreeNode;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.selectCategory;

/**
 * {@link Action} that adds a {@link Category} to the {@link CatalogModel}.
 *
 * @author Christian Pesch
 */

public class AddCategoryAction extends FrameAction {
    private final JTree tree;
    private final CatalogModel catalogModel;

    public AddCategoryAction(JTree tree, CatalogModel catalogModel) {
        this.tree = tree;
        this.catalogModel = catalogModel;
    }

    public void run() {
        final CategoryTreeNode category = getSelectedCategoryTreeNode(tree);
        if (category == null)
            return;

        final String name = showInputDialog(WindowHelper.getFrame(),
                format(getBundle().getString("add-category-label"), category.getName()),
                WindowHelper.getFrame().getTitle(), QUESTION_MESSAGE);
        if (trim(name) == null)
            return;

        catalogModel.addCategories(singletonList(category), singletonList(name),
                new Runnable() {
                    public void run() {
                        selectCategory(tree, catalogModel.getCategoryTreeModel().getChild(category, name));
                    }
                });
    }
}