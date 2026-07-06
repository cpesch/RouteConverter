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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.*;
import static slash.common.system.Platform.getMaximumMemory;

/**
 * A helper for {@link JFrame} and {@link JDialog} operations, and the single
 * choke point for {@link JOptionPane} message/confirm/input dialogs. Every
 * wrapper defaults the parent to the main {@link #getFrame frame} and the
 * title to the owner window's title, and bounds long plain-text messages
 * (e.g. stacktraces) in a scroll pane so a dialog can never grow past the
 * screen.
 *
 * @author Christian Pesch
 */
public class WindowHelper {
    public static final Logger log = getLogger(FrameAction.class.getName());

    private static final int MAXIMUM_MESSAGE_LINES = 12;
    private static final int MAXIMUM_MESSAGE_CHARACTERS = 800;
    private static final int SCROLL_WIDTH = 600;
    private static final int SCROLL_HEIGHT = 400;

    public static JFrame getFrame() {
        Application application = Application.getInstance();
        if (application instanceof SingleFrameApplication)
            return ((SingleFrameApplication) application).getFrame();
        throw new UnsupportedOperationException("FrameAction only works on SingleFrameApplication");
    }

    // --- message dialogs -----------------------------------------------------

    public static void showError(Object message) {
        showMessage(getFrame(), message, getFrame().getTitle(), ERROR_MESSAGE);
    }

    public static void showError(Window owner, Object message) {
        showMessage(owner, message, titleOf(owner), ERROR_MESSAGE);
    }

    /** Null-safe: {@code owner} may be null (e.g. before the frame exists). */
    public static void showError(Window owner, Object message, String title) {
        showMessage(owner, message, title, ERROR_MESSAGE);
    }

    public static void showWarning(Object message) {
        showMessage(getFrame(), message, getFrame().getTitle(), WARNING_MESSAGE);
    }

    public static void showWarning(Window owner, Object message) {
        showMessage(owner, message, titleOf(owner), WARNING_MESSAGE);
    }

    public static void showInformation(Object message) {
        showMessage(getFrame(), message, getFrame().getTitle(), INFORMATION_MESSAGE);
    }

    public static void showInformation(Window owner, Object message) {
        showMessage(owner, message, titleOf(owner), INFORMATION_MESSAGE);
    }

    public static void showInformation(Window owner, Object message, String title) {
        showMessage(owner, message, title, INFORMATION_MESSAGE);
    }

    private static void showMessage(Window owner, Object message, String title, int messageType) {
        showMessageDialog(owner, boundMessage(message), title, messageType);
    }

    // --- confirm / input -----------------------------------------------------

    public static int showConfirm(Object message, String title, int optionType) {
        return showConfirm(getFrame(), message, title, optionType);
    }

    public static int showConfirm(Window owner, Object message, String title, int optionType) {
        return showConfirmDialog(owner, boundMessage(message), title, optionType);
    }

    public static String showInput(Object message, String title) {
        return showInput(getFrame(), message, title);
    }

    public static String showInput(Window owner, Object message, String title) {
        Object result = showInputDialog(owner, boundMessage(message), title, QUESTION_MESSAGE);
        return result != null ? result.toString() : null;
    }

    /** Free-text input pre-filled with {@code initialValue}. */
    public static String showInput(Window owner, Object message, String title, Object initialValue) {
        Object result = showInputDialog(owner, boundMessage(message), title, QUESTION_MESSAGE, null, null, initialValue);
        return result != null ? result.toString() : null;
    }

    // --- content bounding ----------------------------------------------------

    /**
     * Wraps a long plain-text message in a bounded scroll pane so it cannot
     * grow the dialog past the screen; short text, HTML, and non-String
     * (component) messages are passed through to {@link JOptionPane} unchanged.
     */
    static Object boundMessage(Object message) {
        if (!(message instanceof String))
            return message;
        String text = (String) message;
        if (!needsScrolling(text))
            return text;

        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(SCROLL_WIDTH, SCROLL_HEIGHT));
        return scrollPane;
    }

    /** Pure decision: does this plain text exceed the message-size limits? */
    static boolean needsScrolling(String text) {
        // HTML renders/wraps itself; let JOptionPane size it
        if (text.regionMatches(true, 0, "<html", 0, 5))
            return false;
        if (text.length() > MAXIMUM_MESSAGE_CHARACTERS)
            return true;
        int lines = 1;
        for (int i = 0; i < text.length(); i++)
            if (text.charAt(i) == '\n')
                lines++;
        return lines > MAXIMUM_MESSAGE_LINES;
    }

    private static String titleOf(Window owner) {
        if (owner instanceof Frame)
            return ((Frame) owner).getTitle();
        if (owner instanceof Dialog)
            return ((Dialog) owner).getTitle();
        return getFrame().getTitle();
    }

    // --- centralized error handling (dogfoods the wrappers) ------------------

    public static void handleOutOfMemoryError(OutOfMemoryError e) {
        // get some air to breath
        System.gc();
        System.runFinalization();

        final long limitBefore = getMaximumMemory();
        final long limitAfter = limitBefore * 2;
        log.severe(String.format("Out of memory with %d maximum memory: %s", limitBefore, e));

        invokeLater(() -> showError(MessageFormat.format(Application.getInstance().getContext().getBundle().
                getString("out-of-memory-error"), limitBefore, limitAfter)));
    }

    public static void handleThrowable(Class clazz, ActionEvent e, Throwable throwable) {
        boolean offline = isComputerOffline(throwable);
        String stacktrace = offline ? "" : printStackTrace(throwable);
        // for a wrapped/opaque failure show the whole cause chain (the real
        // fault is often hidden behind a generic outer exception); keep the
        // friendly single-line message when the computer is simply offline
        String message = offline ? getLocalizedMessage(throwable) : getMessageWithCauses(throwable);
        log.severe(format("Unhandled throwable in action %s from event %s: %s, %s", clazz.getName(), e, message, stacktrace));
        showError(MessageFormat.format(Application.getInstance().getContext().getBundle().
                getString("unhandled-throwable-error"), clazz.getName(), message, stacktrace));
    }
}
