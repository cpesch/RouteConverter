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

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the {@link Action}s of an {@link Application}.
 *
 * @author Christian Pesch
 */

public class ActionManager {
    private Map<String, Action> actionMap = new HashMap<String, Action>();
    private Map<String, ProxyAction> proxyActionMap = new HashMap<String, ProxyAction>();

    public Action get(String actionName) {
        Action action = actionMap.get(actionName);
        if (action != null)
            return action;
        ProxyAction proxyAction = proxyActionMap.get(actionName);
        if (proxyAction == null) {
            proxyAction = new ProxyAction();
            proxyActionMap.put(actionName, proxyAction);
        }
        return proxyAction;
    }

    public void register(String actionName, Action action) {
        Action found = actionMap.get(actionName);
        if (found != null)
            throw new IllegalArgumentException("action '" + found + "' for '" + actionName + "' already registered");
        actionMap.put(actionName, action);
        action.putValue(Action.NAME, actionName);
        ProxyAction proxyAction = proxyActionMap.get(actionName);
        if (proxyAction != null)
            proxyAction.setDelegate(action);
    }

    public void run(String actionName) {
        run(actionName, new ActionEvent(this, -1, actionName));
    }

    public void run(String actionName, ActionEvent actionEvent) {
        Action action = actionMap.get(actionName);
        if (action == null)
            throw new IllegalArgumentException("no action registered for '" + actionName + "'");
        action.actionPerformed(actionEvent);
    }

    public void enable(String actionName, boolean enable) {
        Action action = actionMap.get(actionName);
        if (action == null)
            throw new IllegalArgumentException("no action registered for '" + actionName + "'");
        action.setEnabled(enable);
    }


    private static class ProxyAction implements Action, PropertyChangeListener {
        private Action delegate = null;
        private SwingPropertyChangeSupport changeSupport = new SwingPropertyChangeSupport(this);

        public void setDelegate(Action delegate) {
            if (this.delegate != null)
                delegate.removePropertyChangeListener(this);

            this.delegate = delegate;

            delegate.addPropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            changeSupport.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
        }

        public Object getValue(String key) {
            return delegate != null ? delegate.getValue(key) : null;
        }

        public void putValue(String key, Object value) {
            if (delegate != null)
                delegate.putValue(key, value);
        }

        public boolean isEnabled() {
            return delegate == null || delegate.isEnabled();
        }

        public void setEnabled(boolean enabled) {
            if (delegate != null)
                delegate.setEnabled(enabled);
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }

        public void actionPerformed(ActionEvent e) {
            if(delegate != null)
                delegate.actionPerformed(e);
        }
    }
}
