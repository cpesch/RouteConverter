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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.gui.helpers.JMenuHelper;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;

public class JMenuHelperTest {
    private Application previousApplication;

    @Before
    public void setUp() throws Exception {
        previousApplication = getApplication();
    }

    @After
    public void tearDown() throws Exception {
        setApplication(previousApplication);
    }

    @Test
    public void createsMenusWithDisplayedMnemonicIndexFromDedicatedMnemonicKey() throws Exception {
        installApplication(new TestBundle(new Object[][]{
                {"file-menu", "File"},
                {"file-menu-mnemonic", "F"}
        }));

        JMenu menu = JMenuHelper.createMenu("file");

        assertEquals("File", menu.getText());
        assertEquals('F', menu.getMnemonic());
        assertEquals(0, menu.getDisplayedMnemonicIndex());
    }

    @Test
    public void createsItemsWithDisplayedMnemonicIndexWithoutAmpersandMarker() throws Exception {
        TestApplication application = installApplication(new TestBundle(new Object[][]{
                {"save-as-action", "Save As"},
                {"save-as-action-mnemonic", "A"}
        }));
        application.getContext().getActionManager().register("save-as", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
            }
        });

        JMenuItem item = JMenuHelper.createItem("save-as");

        assertEquals("Save As", item.getText());
        assertEquals('A', item.getMnemonic());
        assertEquals(5, item.getDisplayedMnemonicIndex());
    }

    private TestApplication installApplication(ResourceBundle bundle) throws Exception {
        TestApplication application = new TestApplication();
        application.getContext().setBundle(bundle);
        setApplication(application);
        return application;
    }

    private static Application getApplication() throws Exception {
        return (Application) getApplicationField().get(null);
    }

    private static void setApplication(Application application) throws Exception {
        getApplicationField().set(null, application);
    }

    private static Field getApplicationField() throws Exception {
        Field field = Application.class.getDeclaredField("application");
        field.setAccessible(true);
        return field;
    }

    private static class TestApplication extends Application {
        protected void startup() {
        }
    }

    private static class TestBundle extends ListResourceBundle {
        private final Object[][] contents;

        private TestBundle(Object[][] contents) {
            this.contents = contents;
        }

        protected Object[][] getContents() {
            return contents;
        }
    }
}

