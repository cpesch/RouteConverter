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

import slash.common.log.CrashReportSpool;
import slash.common.log.DiagnosticReport;
import slash.common.system.Version;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.dialogs.SendErrorReportDialog;
import slash.navigation.feedback.domain.CrashReportSender;
import slash.navigation.gui.CrashHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.system.Version.parseVersionFromManifest;

/**
 * Assembles a {@link DiagnosticReport} from a captured crash, spools it to disk and
 * either offers it through the existing {@link SendErrorReportDialog} or -- when the
 * user has opted in to anonymous crash telemetry (spec 00011 Phase 2) -- sends it
 * silently to the crash-report endpoint. Registered with
 * {@link slash.navigation.gui.Application#setCrashHandler} at startup.
 * <p>
 * <b>Opted-in vs manual (no double-send):</b> a spooled report is delivered through
 * exactly one path. When telemetry is opted in it is sent silently and the spool file
 * is deleted on success; the manual review dialog is <i>not</i> opened for it. When
 * telemetry is off the Phase 1 behaviour is unchanged: the newest report is offered in
 * the dialog and deleted once the user sends it. So a report is never both silently
 * sent and dialog-offered.
 * <p>
 * At most one report dialog is opened per session (a crash-loop latch): further
 * crashes are only spooled and offered on the next successful launch. Silent sending is
 * always fully asynchronous and fail-silent so telemetry never blocks, slows or crashes
 * the app.
 *
 * @author Christian Pesch
 */

public class CrashReporter implements CrashHandler {
    private static final Logger log = Logger.getLogger(CrashReporter.class.getName());
    private static final AtomicBoolean dialogShown = new AtomicBoolean(false);

    private final CrashReportSpool spool;
    private final CrashReportSender sender;
    private final BooleanSupplier telemetryOptedIn;
    private final Supplier<String> apiUrl;

    public CrashReporter() {
        this(CrashReportSpool.createDefault(), new CrashReportSender(),
                RouteConverter::isSendCrashReportsEnabled,
                () -> RouteConverter.getInstance().getApiUrl());
    }

    CrashReporter(CrashReportSpool spool, CrashReportSender sender,
                  BooleanSupplier telemetryOptedIn, Supplier<String> apiUrl) {
        this.spool = spool;
        this.sender = sender;
        this.telemetryOptedIn = telemetryOptedIn;
        this.apiUrl = apiUrl;
    }

    public void handleCrash(Thread thread, Throwable throwable) {
        Version version = parseVersionFromManifest();
        DiagnosticReport report = new DiagnosticReport(thread.getName(), throwable, version.getVersion(), version.getDate());
        String json = report.toJson();

        try {
            spool.write(json);
        } catch (IOException e) {
            log.log(WARNING, "Cannot spool crash report", e);
        }

        // opted in: send silently in the background and delete on success -- no dialog.
        if (telemetryOptedIn.getAsBoolean()) {
            flushSpooledReportsAsync();
            return;
        }

        // not opted in: the Phase 1 manual review dialog is the only send path.
        if (claimDialogSlot())
            invokeLater(() -> showReportDialog(json));
    }

    /**
     * On a successful launch, delivers unsent spooled reports (crashes before the dialog
     * could be shown, or before login). When telemetry is opted in they are sent
     * silently in the background; otherwise the newest is offered in the dialog and
     * deleted once the user sends it.
     */
    public void offerSpooledReports() {
        if (spool.newest() == null)
            return;

        if (telemetryOptedIn.getAsBoolean()) {
            log.info("Sending " + spool.list().size() + " spooled crash report(s) from a previous session");
            flushSpooledReportsAsync();
            return;
        }

        log.info("Offering " + spool.list().size() + " spooled crash report(s) from a previous session");
        if (!claimDialogSlot())
            return;

        File newest = spool.newest();
        invokeLater(() -> {
            try {
                String json = spool.read(newest);
                SendErrorReportDialog dialog = new SendErrorReportDialog(json);
                dialog.showWithPreferences();
                if (dialog.isSent())
                    spool.delete(newest);
            } catch (IOException e) {
                log.log(WARNING, "Cannot read spooled crash report " + newest, e);
            }
        });
    }

    /**
     * Sends every spooled report silently on a daemon thread, deleting each on a
     * successful send and leaving the rest for the next launch. Fail-silent: never
     * throws, never blocks startup or shutdown.
     */
    void flushSpooledReportsAsync() {
        Thread thread = new Thread(this::flushSpooledReports, "CrashReportSender");
        thread.setDaemon(true);
        thread.start();
    }

    void flushSpooledReports() {
        if (!telemetryOptedIn.getAsBoolean())
            return;

        String url = apiUrl.get();
        for (File file : spool.list()) {
            try {
                String json = spool.read(file);
                if (sender.send(url, json))
                    spool.delete(file);
            } catch (Exception e) {
                log.log(WARNING, "Cannot send spooled crash report " + file, e);
            }
        }
    }

    private void showReportDialog(String json) {
        RouteConverter instance = RouteConverter.getInstance();
        // no frame yet (a very early crash): the report is spooled and offered on the
        // next successful launch instead
        if (instance == null || instance.getFrame() == null)
            return;

        SendErrorReportDialog dialog = new SendErrorReportDialog(json);
        dialog.showWithPreferences();
    }

    /**
     * Returns true for the first caller in this session, false afterwards, so at most
     * one report dialog is opened.
     */
    static boolean claimDialogSlot() {
        return dialogShown.compareAndSet(false, true);
    }

    static void resetForTesting() {
        dialogShown.set(false);
    }
}
