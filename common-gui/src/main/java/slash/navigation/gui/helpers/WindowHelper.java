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
package slash.navigation.gui.helpers;

import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.system.Platform.getMaximumMemory;

/**
 * A helper for {@link JFrame} and {@link JDialog} operations.
 *
 * @author Christian Pesch
 */
public class WindowHelper {
    public static final Logger log = getLogger(FrameAction.class.getName());

    public static JFrame getFrame() {
        Application application = Application.getInstance();
        if (application instanceof SingleFrameApplication)
            return ((SingleFrameApplication) application).getFrame();
        throw new UnsupportedOperationException("FrameAction only works on SingleFrameApplication");
    }

    public static void handleOutOfMemoryError(OutOfMemoryError e) {
        // get some air to breath
        System.gc();
        System.runFinalization();

        final long limitBefore = getMaximumMemory();
        final long limitAfter = limitBefore * 2;
        log.severe(String.format("Out of memory with %d maximum memory: %s", limitBefore, e));

        invokeLater(new Runnable() {
            public void run() {
                showMessageDialog(getFrame(),
                        MessageFormat.format(Application.getInstance().getContext().getBundle().
                                getString("out-of-memory-error"), limitBefore, limitAfter),
                        getFrame().getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public static void handleThrowable(Class clazz, Throwable throwable) {
        log.severe(format("Unhandled throwable in action %s: %s, %s", clazz.getSimpleName(), throwable, printStackTrace(throwable)));
        showMessageDialog(getFrame(),
                MessageFormat.format(Application.getInstance().getContext().getBundle().
                        getString("unhandled-throwable-error"), clazz.getSimpleName(), getLocalizedMessage(throwable), printStackTrace(throwable)),
                getFrame().getTitle(), ERROR_MESSAGE);
    }
}
