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

import slash.navigation.gui.Application;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.sound.sampled.LineEvent.Type.STOP;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ThreadHelper.safeJoin;

/**
 * Helps to play one audio file after the other.
 *
 * @author Christian Pesch
 */

public class AudioPlayer {
    private static final Logger log = Logger.getLogger(AudioPlayer.class.getName());
    private Clip clip;
    private LineListener clipListener;
    private Thread worker;
    private final Queue<File> queue = new ArrayDeque<>();
    private static final Object notificationMutex = new Object();
    private boolean running = true, playing;

    public AudioPlayer(final JFrame frame) {
        clipListener = new LineListener() {
            public void update(LineEvent event) {
                if (event.getType() != STOP)
                    return;

                clip.removeLineListener(clipListener);
                clip = null;

                synchronized (notificationMutex) {
                    playing = false;
                    notificationMutex.notifyAll();
                }
            }
        };

        worker = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (notificationMutex) {
                        try {
                            notificationMutex.wait(1000);
                        } catch (InterruptedException e) {
                            // ignore this
                        }

                        if (!running)
                            return;
                        if (playing)
                            continue;
                    }

                    File file = null;
                    try {
                        file = queue.poll();
                        if (file != null)
                            playNext(file);
                    } catch (Exception e) {
                        log.severe(format("Cannot play audio file %s: %s", file, getLocalizedMessage(e)));
                        showMessageDialog(frame,
                                MessageFormat.format(Application.getInstance().getContext().getBundle().getString("cannot-play-voice"), file, e), frame.getTitle(),
                                ERROR_MESSAGE);
                    }
                }
            }
        }, "AudioPlayer");
        worker.start();
    }

    public void interrupt() {
        if (clip != null)
            clip.stop();
        synchronized (notificationMutex) {
            playing = false;
            queue.clear();
            notificationMutex.notifyAll();
        }
    }

    public void dispose() {
        interrupt();
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }
        try {
            safeJoin(worker);
        } catch (InterruptedException e) {
            // intentionally left empty
        }
        clipListener = null;
        worker = null;
    }

    public void play(File file) {
        synchronized (notificationMutex) {
            queue.add(file);
            notificationMutex.notifyAll();
        }
    }

    private void playNext(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        synchronized (notificationMutex) {
            playing = true;
        }

        CancelAction cancelAction = new CancelAction();
        Application.getInstance().getContext().getNotificationManager().showNotification(
                MessageFormat.format(Application.getInstance().getContext().getBundle().getString("play-voice-started"), file.getName()), cancelAction);

        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(file)) {
            clip = AudioSystem.getClip();
            clip.addLineListener(clipListener);
            clip.open(inputStream);
            clip.start();
        }
    }

    private class CancelAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            interrupt();
        }
    }
}
