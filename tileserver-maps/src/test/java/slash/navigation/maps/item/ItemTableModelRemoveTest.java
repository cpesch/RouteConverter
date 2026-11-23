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

import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemTableModelRemoveTest {
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

    private static ItemTableModel<TestItem> modelOfSize(int size) {
        ItemTableModel<TestItem> model = new ItemTableModel<>(1);
        for (int i = 0; i < size; i++)
            model.addOrUpdateItem(new TestItem("Item " + i));
        return model;
    }

    @Test
    public void testRemoveSingleRowAtStart() {
        ItemTableModel<TestItem> model = modelOfSize(5);

        model.remove(0, 0);

        List<String> descriptions = model.getItems().stream().map(TestItem::description).toList();
        assertEquals(4, model.getItems().size());
        assertEquals(List.of("Item 1", "Item 2", "Item 3", "Item 4"), descriptions);
    }

    @Test
    public void testRemoveRangeInMiddle() {
        ItemTableModel<TestItem> model = modelOfSize(5);

        model.remove(1, 3);

        List<String> descriptions = model.getItems().stream().map(TestItem::description).toList();
        assertEquals(2, model.getItems().size());
        assertEquals(List.of("Item 0", "Item 4"), descriptions);
    }

    @Test
    public void testRemoveFiresOneDeleteEventWithGivenBounds() {
        ItemTableModel<TestItem> model = modelOfSize(5);
        TableModelEvent[] received = new TableModelEvent[1];
        int[] eventCount = new int[1];
        TableModelListener listener = e -> {
            received[0] = e;
            eventCount[0]++;
        };
        model.addTableModelListener(listener);

        model.remove(1, 3);

        assertEquals(1, eventCount[0]);
        assertEquals(TableModelEvent.DELETE, received[0].getType());
        assertEquals(1, received[0].getFirstRow());
        assertEquals(3, received[0].getLastRow());
    }
}
