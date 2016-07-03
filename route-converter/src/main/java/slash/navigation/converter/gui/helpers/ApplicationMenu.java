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

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.Application;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

/**
 * Creates an application menu for Mac OS X for RouteConverter.
 *
 * @author Christian Pesch
 */

public class ApplicationMenu {
    public void addApplicationMenuItems() {
        Application application = Application.getApplication();
        application.setAboutHandler(new AboutHandler() {
            public void handleAbout(AppEvent.AboutEvent aboutEvent) {
                run("show-about");
            }
        });
        application.setPreferencesHandler(new PreferencesHandler() {
            public void handlePreferences(AppEvent.PreferencesEvent preferencesEvent) {
                run("show-options");
            }
        });
        application.setQuitHandler(new QuitHandler() {
            public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent,
                    QuitResponse quitResponse) {
                run("exit");
            }
        });
    }

    private void run(String actionName) {
        slash.navigation.gui.Application.getInstance().getContext().getActionManager().run(actionName);
    }
}
