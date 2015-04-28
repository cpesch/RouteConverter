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

import slash.navigation.routes.impl.CategoryTreeNode;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.List;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.asParents;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedCategoryTreeNodes;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.selectCategoryTreePath;

/**
 * {@link Action} that renames {@link CategoryTreeNode}s of the {@link CatalogModel}.
 *
 * @author Christian Pesch
 */

public class RemoveCategoriesAction extends FrameAction {
    private final JTree tree;
    private final CatalogModel catalogModel;

    public RemoveCategoriesAction(JTree tree, CatalogModel catalogModel) {
        this.tree = tree;
        this.catalogModel = catalogModel;
    }

    public void run() {
        RouteConverter r = RouteConverter.getInstance();

        List<CategoryTreeNode> categories = getSelectedCategoryTreeNodes(tree);
        if (categories.size() == 0)
            return;

        StringBuilder categoryNames = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            CategoryTreeNode category = categories.get(i);

            if(category.isLocalRoot() || category.isRemoteRoot()) {
                showMessageDialog(getFrame(),
                        RouteConverter.getBundle().getString("remove-category-cannot-delete-root"), getFrame().getTitle(),
                        ERROR_MESSAGE);
                return;
            }

            categoryNames.append(category.getName());
            if (i < categories.size() - 1)
                categoryNames.append(", ");
        }

        final List<CategoryTreeNode> parents = asParents(categories);

        int confirm = showConfirmDialog(r.getFrame(),
                format(RouteConverter.getBundle().getString("confirm-remove-category"), categoryNames),
                r.getFrame().getTitle(), YES_NO_OPTION);
        if (confirm != YES_OPTION)
            return;

        catalogModel.removeCategories(categories, new Runnable() {
            public void run() {
                for (CategoryTreeNode parent : parents) {
                    TreePath treePath = new TreePath(catalogModel.getCategoryTreeModel().getPathToRoot(parent));
                    selectCategoryTreePath(tree, treePath);
                }
            }
        });
    }
}