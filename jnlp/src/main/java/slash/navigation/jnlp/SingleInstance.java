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

package slash.navigation.jnlp;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;

/**
 * Encapsulates access to the {@link SingleInstanceService} of the JNLP API
 *
 * @author Christian Pesch
 */

public class SingleInstance {
    private SingleInstanceCallback callback;
    private SingleInstanceService service;
    private SingleInstanceListener listener;

    public SingleInstance(SingleInstanceCallback callback) {
        this.callback = callback;
        try {
            service = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            listener = new SingleInstanceListener() {
                public void newActivation(String[] args) {
                    SingleInstance.this.callback.newActivation(args);
                }
            };
            service.addSingleInstanceListener(listener);
        } catch (Exception e) {
            service = null;
        }
    }

    public void dispose() {
        if (service != null)
            service.removeSingleInstanceListener(listener);
    }
}