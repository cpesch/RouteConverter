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
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrameMenuTest {
    private Application previousApplication;
    private JMenu positionColumns;
    private JMenu routeColumns;

    @Before
    public void setUp() throws Exception {
        previousApplication = getApplication();
        installMenuBar();
    }

    @After
    public void tearDown() throws Exception {
        setApplication(previousApplication);
    }

    @Test
    public void showsRouteColumnsAndHidesPositionColumnsWhenBrowseTabSelected() {
        FrameMenu.updateColumnMenuVisibility(true);

        assertFalse(positionColumns.isVisible());
        assertTrue(routeColumns.isVisible());
    }

    @Test
    public void showsPositionColumnsAndHidesRouteColumnsWhenBrowseTabNotSelected() {
        FrameMenu.updateColumnMenuVisibility(false);

        assertTrue(positionColumns.isVisible());
        assertFalse(routeColumns.isVisible());
    }

    private void installMenuBar() throws Exception {
        positionColumns = new JMenu();
        positionColumns.setName("show-position-column");
        routeColumns = new JMenu();
        routeColumns.setName("show-route-column");

        JMenu viewMenu = new JMenu();
        viewMenu.setName("view");
        viewMenu.add(positionColumns);
        viewMenu.add(routeColumns);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(viewMenu);

        TestApplication application = new TestApplication();
        setApplication(application);
        setMenuBar(application, menuBar);
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

    private static void setMenuBar(Application application, JMenuBar menuBar) throws Exception {
        Method method = application.getContext().getClass().getDeclaredMethod("setMenuBar", JMenuBar.class);
        method.setAccessible(true);
        method.invoke(application.getContext(), menuBar);
    }

    private static class TestApplication extends SingleFrameApplication {
        protected void startup() {
        }
    }
}
