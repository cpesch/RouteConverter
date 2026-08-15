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
package slash.navigation.converter.gui.helpers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.models.AbstractTableColumnModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.ActionManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.lang.reflect.Field;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mockStatic;

/**
 * Testable seam for issue #99 (spec 00063): {@link AbstractTableHeaderMenu#populateShowColumnMenu}
 * must rebuild the shared menu-bar "Show Column" submenu from scratch for whichever tab is active,
 * reusing the {@link slash.navigation.converter.gui.actions.ToggleColumnVisibilityAction}s that
 * {@link AbstractTableHeaderMenu#initializeShowColumn()} registers once - repeated calls (one per
 * tab switch) must never re-register them with the {@link ActionManager}.
 */
public class AbstractTableHeaderMenuTest {
    private static final ResourceBundle BUNDLE = new ListResourceBundle() {
        protected Object[][] getContents() {
            return new Object[][]{
                    {"column-alpha", "Column Alpha"},
                    {"column-beta", "Column Beta"},
                    {"column-gamma", "Column Gamma"},
                    {"column-delta", "Column Delta"},
            };
        }
    };

    private Application previousApplication;
    private MockedStatic<BaseRouteConverter> mockedBaseRouteConverter;

    @Before
    public void setUp() throws Exception {
        previousApplication = getApplication();
        TestApplication application = new TestApplication();
        application.getContext().setBundle(BUNDLE);
        setApplication(application);

        // AbstractTableHeaderMenu resolves menu item text via BaseRouteConverter.getBundle(),
        // whose default implementation casts Application.getInstance() to BaseRouteConverter -
        // mock the static method directly instead of building a full BaseRouteConverter fixture.
        mockedBaseRouteConverter = mockStatic(BaseRouteConverter.class);
        mockedBaseRouteConverter.when(BaseRouteConverter::getBundle).thenReturn(BUNDLE);
    }

    @After
    public void tearDown() throws Exception {
        mockedBaseRouteConverter.close();
        setApplication(previousApplication);
    }

    @Test
    public void populateShowColumnMenuRebuildsFromTheActiveTabsColumnsOnly() throws Exception {
        ActionManager actionManager = new ActionManager();
        FakeColumnModel modelA = new FakeColumnModel("tabA", "column-alpha", "column-beta");
        FakeColumnModel modelB = new FakeColumnModel("tabB", "column-gamma", "column-delta");

        AbstractTableHeaderMenu menuA = new AbstractTableHeaderMenu(modelA, actionManager, "tabA") {
        };
        AbstractTableHeaderMenu menuB = new AbstractTableHeaderMenu(modelB, actionManager, "tabB") {
        };
        menuA.initializeShowColumn();
        menuB.initializeShowColumn();

        Action alphaAction = actionManager.get("tabA-show-column-alpha");
        Action gammaAction = actionManager.get("tabB-show-column-gamma");

        JMenu showColumnMenu = new JMenu();

        // tab A is selected first
        populateAndWait(menuA, showColumnMenu);
        assertMenuShows(showColumnMenu, "Column Alpha", "Column Beta");

        // switching to tab B must fully replace tab A's items, not append to them
        populateAndWait(menuB, showColumnMenu);
        assertMenuShows(showColumnMenu, "Column Gamma", "Column Delta");

        // switching back to tab A must not re-register its actions or throw
        populateAndWait(menuA, showColumnMenu);
        assertMenuShows(showColumnMenu, "Column Alpha", "Column Beta");

        // the actions registered once at construction are still the very same instances
        assertSame(alphaAction, actionManager.get("tabA-show-column-alpha"));
        assertSame(gammaAction, actionManager.get("tabB-show-column-gamma"));
    }

    // populateShowColumnMenu dispatches via invokeLater when called off the EDT (as this test
    // thread is) - run it through invokeAndWait so the assertion right after sees the rebuilt menu.
    // Mockito's mockStatic is thread-confined, so the @Before mock (registered on this thread)
    // is invisible on the EDT; open a second mock scope inside the very runnable that runs there.
    private static void populateAndWait(AbstractTableHeaderMenu menu, JMenu showColumnMenu) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try (MockedStatic<BaseRouteConverter> onEdt = mockStatic(BaseRouteConverter.class)) {
                onEdt.when(BaseRouteConverter::getBundle).thenReturn(BUNDLE);
                menu.populateShowColumnMenu(showColumnMenu);
            }
        });
    }

    @Test
    public void initializeShowColumnRejectsBeingCalledMoreThanOncePerTable() {
        ActionManager actionManager = new ActionManager();
        FakeColumnModel model = new FakeColumnModel("tabA", "column-alpha");
        AbstractTableHeaderMenu menu = new AbstractTableHeaderMenu(model, actionManager, "tabA") {
        };

        menu.initializeShowColumn();
        try {
            menu.initializeShowColumn();
            fail("expected the second registration of the same action to be rejected");
        } catch (IllegalArgumentException expected) {
            // ActionManager.register() throws on a duplicate key - this is exactly what
            // populateShowColumnMenu (unlike initializeShowColumn) must never trigger
        }
    }

    private static void assertMenuShows(JMenu menu, String... expectedTexts) {
        assertEquals(expectedTexts.length, menu.getItemCount());
        for (int i = 0; i < expectedTexts.length; i++)
            assertEquals(expectedTexts[i], menu.getItem(i).getText());
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

    private static class TestApplication extends SingleFrameApplication {
        protected void startup() {
        }
    }

    private static class FakeColumnModel extends AbstractTableColumnModel {
        FakeColumnModel(String preferencesPrefix, String... columnNames) {
            super(preferencesPrefix, null);
            for (int i = 0; i < columnNames.length; i++)
                predefineColumn(i, columnNames[i], null, true, (TableCellRenderer) null, (TableCellRenderer) null);
            initializeColumns();
        }
    }
}
