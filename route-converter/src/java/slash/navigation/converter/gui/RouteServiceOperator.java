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

import slash.navigation.catalog.domain.exception.UnAuthorizedException;
import slash.navigation.gui.Constants;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Helps to interact with the route service.
 *
 * @author Christian Pesch
 */

public class RouteServiceOperator {
    private static final Logger log = Logger.getLogger(RouteServiceOperator.class.getName());

    private RouteConverter routeConverter;

    public RouteServiceOperator(RouteConverter routeConverter) {
        this.routeConverter = routeConverter;
    }

    public interface Operation {
        void run() throws IOException;
    }

    boolean showLogin() {
        LoginDialog loginDialog = new LoginDialog(routeConverter);
        loginDialog.pack();
        loginDialog.setLocationRelativeTo(routeConverter.getFrame());
        loginDialog.setVisible(true);
        return loginDialog.isSuccessful();
    }

    void handleServiceError(final Throwable t) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                t.printStackTrace();
                log.severe("Error while operating on RouteService: " + t.getMessage());
                JOptionPane.showMessageDialog(routeConverter.getFrame(),
                        MessageFormat.format(RouteConverter.getBundle().getString("service-error"), t.getClass(), t.getMessage()),
                        routeConverter.getFrame().getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    public void executeOnRouteService(final Operation operation) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.startWaitCursor(routeConverter.getFrame().getRootPane());
                        }
                    });

                    while (true) {
                        try {
                            try {
                                operation.run();
                            } catch (UnAuthorizedException uae) {
                                final boolean[] showedLogin = new boolean[1];
                                showedLogin[0] = false;

                                SwingUtilities.invokeAndWait(new Runnable() {
                                    public void run() {
                                        showedLogin[0] = showLogin();
                                    }
                                });

                                if (showedLogin[0])
                                    continue;
                            }
                        } catch (Throwable t) {
                            handleServiceError(t);
                        }
                        break;
                    }
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(routeConverter.getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "RouteServiceOperator").start();
    }
}
