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
import slash.navigation.feedback.domain.CrashReportSender;
import slash.navigation.gui.CrashHandler;

import java.io.File;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static slash.common.system.Version.parseVersionFromManifest;

/**
 * Assembles a {@link DiagnosticReport} from a captured crash and spools it to disk.
 * Registered with {@link slash.navigation.gui.Application#setCrashHandler} at startup.
 * <p>
 * Delivery is telemetry-driven and never opens a dialog on its own:
 * <ul>
 * <li><b>Opted in</b> (spec 00011 Phase 2): spooled reports are sent silently in the
 *     background and deleted on success.</li>
 * <li><b>Opted out</b>: reports only stay spooled &mdash; nothing is sent and no dialog
 *     is opened automatically. The user can still review and send a report manually via
 *     Help &rarr; Send Error Report.</li>
 * </ul>
 * Silent sending is always fully asynchronous and fail-silent so telemetry never blocks,
 * slows or crashes the app.
 *
 * @author Christian Pesch
 */

public class CrashReporter implements CrashHandler {
    private static final Logger log = Logger.getLogger(CrashReporter.class.getName());

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

        // opted in: send silently in the background, deleting each on success. opted out:
        // the report only stays spooled -- no dialog is ever opened automatically.
        if (telemetryOptedIn.getAsBoolean())
            flushSpooledReportsAsync();
    }

    /**
     * On a successful launch, delivers unsent spooled reports (crashes from a previous
     * session). When telemetry is opted in they are sent silently in the background;
     * when opted out the backlog is left spooled and silent -- no dialog is opened.
     */
    public void offerSpooledReports() {
        if (spool.newest() == null)
            return;

        if (telemetryOptedIn.getAsBoolean()) {
            log.info("Sending " + spool.list().size() + " spooled crash report(s) from a previous session");
            flushSpooledReportsAsync();
        }
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
}
