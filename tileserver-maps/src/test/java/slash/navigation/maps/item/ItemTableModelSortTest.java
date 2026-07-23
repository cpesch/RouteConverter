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

import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ItemTableModelSortTest {
    private static final Comparator<Item> DESCRIPTION_COMPARATOR =
            Comparator.comparing(Item::description, String.CASE_INSENSITIVE_ORDER);

    private static class TestItem implements Item {
        private final String description;

        private TestItem(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public String getUrl() {
            return description;
        }
    }

    @Test
    public void testAddOutOfOrderEndsUpSorted() {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1, DESCRIPTION_COMPARATOR);

        model.addOrUpdateItem(new TestItem("Viamichelin"));
        model.addOrUpdateItem(new TestItem("ArcGIS Topo"));
        model.addOrUpdateItem(new TestItem("bergfex"));
        model.addOrUpdateItem(new TestItem("CyclOSM"));

        List<String> descriptions = model.getItems().stream().map(TestItem::description).toList();
        assertEquals(List.of("ArcGIS Topo", "bergfex", "CyclOSM", "Viamichelin"), descriptions);
    }

    @Test
    public void testInsertIntoEmptyModel() {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1, DESCRIPTION_COMPARATOR);

        model.addOrUpdateItem(new TestItem("Solo"));

        assertEquals(1, model.getItems().size());
        assertEquals("Solo", model.getItems().get(0).description());
    }

    @Test
    public void testInsertAtStartMiddleEnd() {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1, DESCRIPTION_COMPARATOR);

        model.addOrUpdateItem(new TestItem("Mango"));

        model.addOrUpdateItem(new TestItem("Apple"));
        assertEquals(0, model.getIndex(model.getItemByDescription("Apple")));

        model.addOrUpdateItem(new TestItem("Kiwi"));
        assertEquals(1, model.getIndex(model.getItemByDescription("Kiwi")));

        model.addOrUpdateItem(new TestItem("Zebra"));
        assertEquals(3, model.getIndex(model.getItemByDescription("Zebra")));
    }

    @Test
    public void testDuplicateDescriptionLandsAfterExistingEqualItem() {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1, DESCRIPTION_COMPARATOR);

        TestItem first = new TestItem("Duplicate");
        model.addOrUpdateItem(first);
        TestItem second = new TestItem("Duplicate");
        model.addOrUpdateItem(second);

        assertEquals(2, model.getItems().size());
        assertEquals(0, model.getIndex(first));
        assertEquals(1, model.getIndex(second));
    }

    @Test
    public void testNoComparatorKeepsInsertionOrder() {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1);

        model.addOrUpdateItem(new TestItem("Zebra"));
        model.addOrUpdateItem(new TestItem("Apple"));
        model.addOrUpdateItem(new TestItem("Mango"));

        List<String> descriptions = model.getItems().stream().map(TestItem::description).toList();
        assertEquals(List.of("Zebra", "Apple", "Mango"), descriptions);
    }
}
