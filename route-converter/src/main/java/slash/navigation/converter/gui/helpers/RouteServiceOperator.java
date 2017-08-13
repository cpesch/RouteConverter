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
import slash.navigation.converter.gui.dialogs.LoginDialog;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.rest.exception.UnAuthorizedException;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;

/**
 * Helps to interact with the RemoteCatalog and RouteFeedback service.
 *
 * @author Christian Pesch
 */

public class RouteServiceOperator {
    private static final Logger log = Logger.getLogger(RouteServiceOperator.class.getName());
    private final RouteFeedback routeFeedback;
    private final JFrame frame;

    public RouteServiceOperator(JFrame frame, RouteFeedback routeFeedback) {
        this.frame = frame;
        this.routeFeedback = routeFeedback;
    }

    public RouteFeedback getRouteFeedback() {
        return routeFeedback;
    }

    public boolean showLogin() {
        LoginDialog loginDialog = new LoginDialog(routeFeedback);
        loginDialog.pack();
        loginDialog.restoreLocation();
        loginDialog.setVisible(true);
        return loginDialog.isSuccessful();
    }

    public void handleServiceError(final Throwable t) {
        invokeLater(new Runnable() {
            public void run() {
                log.severe(format("Error while operating on RouteConverter service: %s, %s", t, printStackTrace(t)));
                showMessageDialog(frame,
                        MessageFormat.format(RouteConverter.getBundle().getString("route-service-error"), t.getClass(), getLocalizedMessage(t)),
                        frame.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    public interface Operation {
        String getName();
        void run() throws IOException;
    }

    public void executeOperation(final Operation operation) {
        new Thread(new Runnable() {
            public void run() {
                invokeLater(new Runnable() {
                    public void run() {
                        startWaitCursor(frame.getRootPane());
                    }
                });

                while (true) {
                    try {
                        try {
                            operation.run();
                        } catch (UnAuthorizedException uae) {
                            final boolean[] showedLogin = new boolean[1];
                            showedLogin[0] = false;

                            invokeAndWait(new Runnable() {
                                public void run() {
                                    showedLogin[0] = showLogin();
                                }
                            });

                            if (showedLogin[0])
                                continue;
                        }
                    } catch (Throwable t) {
                        handleServiceError(t);
                    } finally {
                        invokeLater(new Runnable() {
                            public void run() {
                                stopWaitCursor(frame.getRootPane());
                            }
                        });
                    }
                    break;
                }
            }
        }, operation.getName()).start();
    }
}
