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

package slash.navigation.gui;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HelpManagerTest {
    @Test
    public void namedComponentReturnsItsOwnName() {
        JPanel panel = new JPanel();
        panel.setName("options");

        assertEquals("options", HelpManager.resolveTopicId(panel));
    }

    @Test
    public void unnamedChildReturnsNearestNamedAncestor() {
        JPanel parent = new JPanel();
        parent.setName("options");
        JPanel child = new JPanel();
        parent.add(child);

        assertEquals("options", HelpManager.resolveTopicId(child));
    }

    @Test
    public void namedLayeredPaneInChainIsSkipped() {
        JPanel root = new JPanel();
        root.setName("options");
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setName("some-layered-pane");
        root.add(layeredPane);
        JPanel child = new JPanel();
        layeredPane.add(child);

        assertEquals("options", HelpManager.resolveTopicId(child));
    }

    @Test
    public void emptyStringNameIsTreatedAsAbsent() {
        JPanel parent = new JPanel();
        parent.setName("options");
        JPanel child = new JPanel();
        child.setName("");
        parent.add(child);

        assertEquals("options", HelpManager.resolveTopicId(child));
    }

    @Test
    public void fullyUnnamedChainReturnsNull() {
        JPanel parent = new JPanel();
        JPanel child = new JPanel();
        parent.add(child);

        assertNull(HelpManager.resolveTopicId(child));
    }

    @Test
    public void syntheticRootPaneNamesAreSkipped() {
        JRootPane rootPane = new JRootPane();
        assertEquals("null.contentPane", rootPane.getContentPane().getName());
        assertEquals("null.glassPane", rootPane.getGlassPane().getName());

        assertNull(HelpManager.resolveTopicId(rootPane.getContentPane()));
    }

    @Test
    public void reportedBugShapeResolvesToDialogName() {
        Container dialog = new Container();
        dialog.setName("options");
        JRootPane rootPane = new JRootPane();
        dialog.add(rootPane);
        Container contentPane = rootPane.getContentPane();
        JButton button = new JButton("?");
        contentPane.add(button);

        assertEquals("options", HelpManager.resolveTopicId(button));
    }

    @Test
    public void realMenuPathResolvesToHighlightedItem() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem open = new JMenuItem();
        open.setName("open");

        assertEquals("open", HelpManager.resolveMenuTopicId(path(bar, file, popup, open)));
    }

    @Test
    public void menuPathEndingAtMenuReturnsNull() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");

        assertNull(HelpManager.resolveMenuTopicId(path(bar, file)));
    }

    @Test
    public void menuPathEndingAtPopupMenuReturnsNull() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");
        JPopupMenu popup = new JPopupMenu();

        assertNull(HelpManager.resolveMenuTopicId(path(bar, file, popup)));
    }

    @Test
    public void unnamedLeafWithNamedMenuEarlierReturnsNull() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem unnamed = new JMenuItem();

        assertNull(HelpManager.resolveMenuTopicId(path(bar, file, popup, unnamed)));
    }

    @Test
    public void twoLeafItemsResolveToLast() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem first = new JMenuItem();
        first.setName("first");
        JMenuItem last = new JMenuItem();
        last.setName("last");

        assertEquals("last", HelpManager.resolveMenuTopicId(path(bar, file, popup, first, last)));
    }

    @Test
    public void nullAndEmptyPathsReturnNull() {
        assertNull(HelpManager.resolveMenuTopicId(null));
        assertNull(HelpManager.resolveMenuTopicId(new MenuElement[0]));
    }

    @Test
    public void syntheticLeafNameReturnsNull() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu();
        file.setName("file");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem synthetic = new JMenuItem();
        synthetic.setName("some.dotted");

        assertNull(HelpManager.resolveMenuTopicId(path(bar, file, popup, synthetic)));
    }

    private static MenuElement[] path(Component... components) {
        MenuElement[] elements = new MenuElement[components.length];
        for (int i = 0; i < components.length; i++)
            elements[i] = (MenuElement) components[i];
        return elements;
    }
}
