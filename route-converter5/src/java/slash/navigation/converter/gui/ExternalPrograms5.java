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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui;

import slash.navigation.util.Platform;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * A small graphical user interface for the route conversion.
 *
 * @author Christian Pesch, Michel
 */

public class ExternalPrograms5 extends ExternalPrograms {

    private void startIE(Window window, String uri) {
        try {
            Runtime.getRuntime().exec("\"c:\\programme\\internet explorer\\iexplore.exe\" \"" + uri + "\"");
        } catch (IOException e) {
            try {
                Runtime.getRuntime().exec("\"c:\\program files\\internet explorer\\iexplore.exe\" \"" + uri + "\"");
            } catch (IOException e1) {
                log.severe("Start IE error " + uri + ": " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.BUNDLE.getString("start-browser-error"), e.getMessage()),
                        RouteConverter.BUNDLE.getString("title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startFirefox(Window window, String uri) {
        try {
            Runtime.getRuntime().exec("firefox \"" + uri + "\"");
        } catch (IOException e) {
            try {
                Runtime.getRuntime().exec("mozilla \"" + uri + "\"");
            } catch (IOException e1) {
                log.severe("Start Firefox error " + uri + ": " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.BUNDLE.getString("start-browser-error"), e.getMessage()),
                        RouteConverter.BUNDLE.getString("title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected void startBrowser(Window window, String uri) {
        if (Platform.isWindows())
            startIE(window, uri);
        else
            startFirefox(window, uri);
    }

    protected void startMail(Window window, String uri) {
        startBrowser(window, uri);
    }

    private void openFileByCmd(File file, Window window) {
        try {
            Runtime.getRuntime().exec("cmd /c \"" + file.getAbsolutePath() + "\"");
        } catch (IOException e) {
            log.severe("Open file by cmd error " + file + ": " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window,
                    MessageFormat.format(RouteConverter.BUNDLE.getString("start-google-earth-error"), file.getAbsolutePath(), e.getMessage()),
                    RouteConverter.BUNDLE.getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFileByRun(File file, Window window) {
        try {
            Runtime.getRuntime().exec("run \"" + file.getAbsolutePath() + "\"");
        } catch (IOException e) {
            log.severe("Open file by run error " + file + ": " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window,
                    MessageFormat.format(RouteConverter.BUNDLE.getString("start-google-earth-error"), file.getAbsolutePath(), e.getMessage()),
                    RouteConverter.BUNDLE.getString("title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void openFile(Window window, File file) {
        if (Platform.isWindows())
            openFileByCmd(file, window);
        else
            openFileByRun(file, window);
    }
}
