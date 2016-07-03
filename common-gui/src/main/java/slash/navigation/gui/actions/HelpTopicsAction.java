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

package slash.navigation.gui.actions;

import slash.navigation.gui.Application;

import javax.help.CSH;
import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Open the {@link HelpSet help}.
 *
 * @author Christian Pesch
 */

public class HelpTopicsAction extends FrameAction {
    private static final Logger log = Logger.getLogger(HelpTopicsAction.class.getName());

    public void run() {
        try {
            HelpBroker broker = Application.getInstance().getContext().getHelpBroker();
            CSH.DisplayHelpFromFocus helpFromFocus = new CSH.DisplayHelpFromFocus(broker);
            helpFromFocus.actionPerformed(getEvent());
            final Window window = ((DefaultHelpBroker) broker).getWindowPresentation().getHelpWindow();
            window.setIconImage(getFrame().getIconImage());
            if(window instanceof JFrame) {
                JRootPane rootPane = ((JFrame) window).getRootPane();
                rootPane.registerKeyboardAction(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        window.setVisible(false);
                    }
                }, getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("Could not initialize help: " + e);
            showMessageDialog(null, "Could not initialize help: " + getLocalizedMessage(e), "Error", ERROR_MESSAGE);
        }
    }
}
