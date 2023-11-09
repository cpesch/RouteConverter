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

import slash.navigation.converter.gui.RouteConverter;

import java.awt.*;
import java.awt.desktop.OpenURIEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;

import static java.awt.Desktop.Action.*;
import static java.awt.Desktop.isDesktopSupported;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Creates an application menu for Mac OS X for RouteConverter.
 *
 * @author Christian Pesch
 */

public class ApplicationMenu {
    private static final Logger log = Logger.getLogger(ApplicationMenu.class.getName());

    public void addApplicationMenuItems() {
        if (!isDesktopSupported())
            return;

        if (Desktop.getDesktop().isSupported(APP_ABOUT))
            Desktop.getDesktop().setAboutHandler(this::about);
        if (Desktop.getDesktop().isSupported(APP_OPEN_FILE))
            Desktop.getDesktop().setOpenFileHandler(this::openFiles);
        if (Desktop.getDesktop().isSupported(APP_OPEN_URI))
            Desktop.getDesktop().setOpenURIHandler(this::openUri);
        if (Desktop.getDesktop().isSupported(APP_PREFERENCES))
            Desktop.getDesktop().setPreferencesHandler(this::preferences);
        if (Desktop.getDesktop().isSupported(APP_QUIT_HANDLER))
            Desktop.getDesktop().setQuitHandler(this::quit);
    }

    @SuppressWarnings("unused")
    public void about(EventObject event) {
        run("show-about");
    }

    @SuppressWarnings("unchecked")
    private List<File> extractFiles(EventObject eventObject) {
        try {
            Class<?> eventClass = Class.forName("java.awt.desktop.OpenFilesEvent");
            Method getFilesMethod = eventClass.getMethod("getFiles");
            Object result = getFilesMethod.invoke(eventObject);
            return (List<File>) result;
        }
        catch (Exception e) {
            log.warning("Cannot extract files: " + getLocalizedMessage(e));
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unused")
    public void openFiles(EventObject event) {
        List<URL> urls = new ArrayList<>();
        List<File> files = extractFiles(event);
        if(files == null || files.isEmpty())
            return;

        for(File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                log.warning("Cannot open file " + file + ": " + getLocalizedMessage(e));
            }
        }
        ((RouteConverter)slash.navigation.gui.Application.getInstance()).getConvertPanel().openUrls(urls);
    }

    private void openUri(OpenURIEvent openURIEvent) {
        URI uri = openURIEvent.getURI();
        try {
            URL url = uri.toURL();
            ((RouteConverter)slash.navigation.gui.Application.getInstance()).getConvertPanel().openUrls(List.of(url));
        } catch (MalformedURLException e) {
            log.warning("Cannot open URI " + uri + ": " + getLocalizedMessage(e));
        }
    }

    @SuppressWarnings("unused")
    public void preferences(EventObject event) {
        run("show-options");
    }

    @SuppressWarnings("unused")
    public void quit(EventObject event, Object response) {
        run("exit");
    }

    private void run(String actionName) {
        slash.navigation.gui.Application.getInstance().getContext().getActionManager().run(actionName);
    }
}
