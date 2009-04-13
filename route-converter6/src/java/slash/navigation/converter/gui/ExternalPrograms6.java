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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;

/**
 * Knows how to cope with external programs like mail when
 * running under Java 6.
 *
 * @author Christian Pesch
 */

public class ExternalPrograms6 extends ExternalPrograms {
    protected void startBrowser(Window window, String uri) {
        if (Desktop.isDesktopSupported())
            try {
                Desktop.getDesktop().browse(new URI(uri));
            } catch (Exception e) {
                log.severe("Start Browser error: " + e.getMessage());

                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-browser-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
    }

    protected void startMail(Window window, String uri) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().mail(new URI(uri));
            } catch (Exception e) {
                log.severe("Start Mail error: " + e.getMessage());

                JOptionPane.showMessageDialog(window,
                        MessageFormat.format(RouteConverter.getBundle().getString("start-mail-error"), e.getMessage()),
                        RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected void openFile(Window window, File file) {
        if (Desktop.isDesktopSupported())
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException ie) {
                log.severe("Open File error " + file + ": " + ie.getMessage());

                try {
                    Runtime.getRuntime().exec("cmd /c \"" + file.getAbsolutePath() + "\"");
                } catch (IOException e) {
                    log.severe("Open File error " + file + ": " + e.getMessage());

                    JOptionPane.showMessageDialog(window,
                            MessageFormat.format(RouteConverter.getBundle().getString("start-google-earth-error"), file.getAbsolutePath(), e.getMessage()),
                            RouteConverter.getTitle(), JOptionPane.ERROR_MESSAGE);
                }
            }
    }

}

