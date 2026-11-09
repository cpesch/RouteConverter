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

import org.junit.After;
import org.junit.Test;

import javax.swing.event.ChangeListener;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ItemModelTest {
    private static final Preferences preferences = Preferences.userNodeForPackage(ItemModel.class);

    private static class TestItem implements Item {
        private final String description;
        private final String url;

        private TestItem(String description, String url) {
            this.description = description;
            this.url = url;
        }

        public String description() {
            return description;
        }

        public String getUrl() {
            return url;
        }
    }

    private static class TestItemModel extends ItemModel<TestItem> {
        private TestItemModel(String preferenceName) {
            super(preferenceName, null);
        }

        protected TestItem stringToItem(String value) {
            return value != null ? new TestItem(value, value) : null;
        }

        protected String itemToString(TestItem item) {
            return item != null ? item.getUrl() : null;
        }
    }

    @After
    public void cleanUp() {
        preferences.remove("itemModelTestNullItemUrl");
        preferences.remove("itemModelTestNullItem");
        preferences.remove("itemModelTestNormalItem");
    }

    @Test
    public void setItemWithNullItemToStringDoesNotChangeStoredValueOrFireChange() {
        String preferenceName = "itemModelTestNullItemUrl";
        preferences.put(preferenceName, "unchanged");
        TestItemModel model = new TestItemModel(preferenceName);
        boolean[] fired = {false};
        ChangeListener listener = e -> fired[0] = true;
        model.addChangeListener(listener);

        model.setItem(new TestItem("no url", null));

        assertEquals("unchanged", preferences.get(preferenceName, null));
        assertFalse(fired[0]);
    }

    @Test
    public void setItemWithNullItemDoesNotChangeStoredValueOrFireChange() {
        String preferenceName = "itemModelTestNullItem";
        preferences.put(preferenceName, "unchanged");
        TestItemModel model = new TestItemModel(preferenceName);
        boolean[] fired = {false};
        ChangeListener listener = e -> fired[0] = true;
        model.addChangeListener(listener);

        model.setItem(null);

        assertEquals("unchanged", preferences.get(preferenceName, null));
        assertFalse(fired[0]);
    }

    @Test
    public void setItemWithNormalItemStoresValueAndFiresOnce() {
        String preferenceName = "itemModelTestNormalItem";
        TestItemModel model = new TestItemModel(preferenceName);
        int[] fireCount = {0};
        ChangeListener listener = e -> fireCount[0]++;
        model.addChangeListener(listener);

        model.setItem(new TestItem("desc", "http://example.url"));

        assertEquals("http://example.url", preferences.get(preferenceName, null));
        assertEquals(1, fireCount[0]);
    }
}
