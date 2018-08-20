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
import slash.navigation.converter.gui.actions.ShowAboutTimeAlbumProAction;
import slash.navigation.gui.actions.SingletonDialogAction;

/**
 * A graphical user interface for Columbus devices.
 *
 * @author Christian Pesch
 */

public class TimeAlbumPro extends RouteConverterGoogle {
    private NavigationFormatRegistry navigationFormatRegistry = new ColumbusNavigationFormatRegistry();

    public static void main(String[] args) {
        launch(TimeAlbumPro.class, new String[]{RouteConverter.class.getPackage().getName() + ".Untranslated", RouteConverter.class.getName()}, args);
    }

    protected String getProduct() {
        return "TimeAlbumPro";
    }

    public String getEdition() {
        return "TimeAlbum Pro";
    }

    public String getEditionId() {
        return "timealbum";
    }

    public NavigationFormatRegistry getNavigationFormatRegistry() {
        return navigationFormatRegistry;
    }

    protected boolean isPointsOfInterestEnabled() {
        return true;
    }

    protected boolean isPhotosEnabled() {
        return true;
    }

    protected void checkForMissingTranslator() {}

    protected SingletonDialogAction createAboutAction() {
        return new ShowAboutTimeAlbumProAction();
    }
}
