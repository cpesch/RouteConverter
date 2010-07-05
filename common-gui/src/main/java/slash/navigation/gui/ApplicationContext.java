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

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * The context of an application.
 *
 * @author Christian Pesch
 */

public class ApplicationContext {
    private ResourceBundle bundle;
    private ActionManager actionManager = new ActionManager();
    private UndoManager undoManager = new UndoManager();
    private UndoableEditSupport undoableEditSupport = new UndoableEditSupport();
    private JMenuBar menuBar = null;
    private String helpBrokerUrl = null;
    private HelpBroker broker = null;

    public ApplicationContext() {
        undoableEditSupport.addUndoableEditListener(undoManager);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public UndoableEditSupport getUndoableEditSupport() {
        return undoableEditSupport;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    void setMenuBar(JMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    public HelpBroker getHelpBroker() throws HelpSetException {
        if (broker == null) {
            URL url = HelpSet.findHelpSet(null, helpBrokerUrl);
            HelpSet helpSet = new HelpSet(null, url);
            broker = helpSet.createHelpBroker();
        }
        return broker;
    }

    public void setHelpBrokerUrl(String helpBrokerUrl) {
        this.helpBrokerUrl = helpBrokerUrl;
    }
}
