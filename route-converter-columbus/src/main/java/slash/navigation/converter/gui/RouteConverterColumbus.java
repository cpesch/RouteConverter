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
package slash.navigation.converter.gui;

import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.columbus.ColumbusNavigationFormatRegistry;
import slash.navigation.converter.gui.helpers.MapViewImplementation;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.navigation.converter.gui.helpers.MapViewImplementation.EclipseSWT;

/**
 * A small graphical user interface for the Columbus route conversion.
 *
 * @author Christian Pesch
 */

public class RouteConverterColumbus extends RouteConverter {
    private NavigationFormatRegistry navigationFormatRegistry = new ColumbusNavigationFormatRegistry();

    public static void main(String[] args) {
        launch(RouteConverterColumbus.class, new String[]{RouteConverter.class.getPackage().getName() + ".Untranslated", RouteConverter.class.getName()}, args);
    }

    public String getEdition() {
        return "Columbus";
    }

    public NavigationFormatRegistry getNavigationFormatRegistry() {
        return navigationFormatRegistry;
    }

    protected MapViewImplementation getPreferredMapView() {
        return EclipseSWT;
    }

    static {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3 * 60 * 1000);
                } catch (InterruptedException e) {
                }
                showMessageDialog(null, "Thank you for testing.", "RouteConverter for Columbus", ERROR_MESSAGE);
                System.exit(5);
            }
        });
        thread.start();
    }
}
