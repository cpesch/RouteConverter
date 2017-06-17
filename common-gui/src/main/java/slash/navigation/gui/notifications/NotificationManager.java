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
package slash.navigation.gui.notifications;

import slash.navigation.gui.Application;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.lang.System.currentTimeMillis;
import static javax.swing.BorderFactory.createEtchedBorder;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.navigation.gui.helpers.WindowHelper.getFrame;

/**
 * Manages the notifications of an {@link Application}.
 *
 * @author Christian Pesch
 */

public class NotificationManager {
    private static final int DISPLAY_TIMEOUT = 2 * 1000;

    private JWindow window;
    private JLabel label = new JLabel();

    private static final Object notificationMutex = new Object();
    private boolean running = true;
    private long lastEvent;
    private String nextMessage;
    private Action nextAction;
    private Thread notificationUpdater;

    public NotificationManager() {
        label.setForeground(new Color(238, 238, 238));
        label.setFont(label.getFont().deriveFont(13f));
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (nextAction != null)
                    nextAction.actionPerformed(new ActionEvent(e, 4711, "notify"));
            }
        });

        window = new JWindow(getFrame());
        JPanel contentPane = (JPanel) window.getContentPane();
        contentPane.add(label);
        contentPane.setBackground(new Color(0, 7 * 16 + 9, 13 * 16));
        contentPane.setBorder(new CompoundBorder(createEtchedBorder(), new EmptyBorder(2, 3, 2, 3)));

        initializeNotificationUpdater();
    }

    private void initializeNotificationUpdater() {
        notificationUpdater = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (notificationMutex) {
                        if (!running)
                            break;

                        if (nextMessage != null) {
                            final String showMessage = nextMessage;
                            nextMessage = null;
                            lastEvent = currentTimeMillis();

                            if (!label.getText().equals(showMessage)) {
                                invokeInAwtEventQueue(new Runnable() {
                                    public void run() {
                                        show(showMessage);
                                    }
                                });
                            }
                        } else if (currentTimeMillis() - lastEvent > DISPLAY_TIMEOUT) {
                            invokeInAwtEventQueue(new Runnable() {
                                public void run() {
                                    hide();
                                }
                            });
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        }, "NotificationUpdater");
        notificationUpdater.start();
    }

    private void show(String message) {
        label.setText(message);
        JFrame frame = getFrame();
        if(frame == null)
            return;

        Point locationOnScreen = frame.getLocationOnScreen();
        Dimension frameSize = getFrame().getSize();
        window.pack();
        window.setLocation(locationOnScreen.x + frameSize.width - label.getWidth() - 25,
                locationOnScreen.y + frameSize.height - label.getHeight() - 25);
        window.setVisible(true);
    }

    private void hide() {
        window.setVisible(false);
        label.setText("");
    }

    public void showNotification(String message, Action action) {
        synchronized (notificationMutex) {
            this.nextMessage = message;
            this.nextAction = action;
            notificationMutex.notifyAll();
        }
    }

    public void dispose() {
        synchronized (notificationMutex) {
            this.running = false;
            notificationMutex.notifyAll();
        }

        if (notificationUpdater != null) {
            try {
                safeJoin(notificationUpdater);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
        }
    }
}
