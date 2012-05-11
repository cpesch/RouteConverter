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

package slash.navigation.base;

import slash.common.io.CompactCalendar;
import slash.common.io.NotClosingUnderlyingInputStream;
import slash.navigation.babel.BabelFormat;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.tcx.TcxFormat;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.CompactCalendar.UTC;
import static slash.common.io.CompactCalendar.fromCalendar;
import static slash.common.io.Transfer.ceiling;
import static slash.navigation.base.NavigationFormats.asFormatForRoutes;
import static slash.navigation.base.NavigationFormats.getReadFormats;
import static slash.navigation.simple.GoogleMapsUrlFormat.isGoogleMapsUrl;
import static slash.navigation.util.RouteComments.commentPositions;
import static slash.navigation.util.RouteComments.commentRouteName;
import static slash.navigation.util.RouteComments.createRouteName;

/**
 * Parses byte streams with navigation information via {@link NavigationFormat} classes.
 *
 * @author Christian Pesch
 */

public class NavigationFormatParser {
    private static final Logger log = Logger.getLogger(NavigationFormatParser.class.getName());
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    private final List<NavigationFormatParserListener> listeners = new CopyOnWriteArrayList<NavigationFormatParserListener>();

    public void addNavigationFileParserListener(NavigationFormatParserListener listener) {
        listeners.add(listener);
    }

    public void removeNavigationFileParserListener(NavigationFormatParserListener listener) {
        listeners.remove(listener);
    }

    private void notifyReading(NavigationFormat<BaseRoute> format) {
        for (NavigationFormatParserListener listener : listeners) {
            listener.reading(format);
        }
    }

    private List<Integer> getPositionCounts(List<BaseRoute> routes) {
        List<Integer> positionCounts = new ArrayList<Integer>();
        for (BaseRoute route : routes)
            positionCounts.add(route.getPositionCount());
        return positionCounts;
    }

    @SuppressWarnings("unchecked")
    private void internalRead(InputStream buffer, int readBufferSize, CompactCalendar startDate,
                              List<NavigationFormat> formats, ParserContext context) throws IOException {
        int routeCountBefore = context.getRoutes().size();
        try {
            for (NavigationFormat<BaseRoute> format : formats) {
                notifyReading(format);

                try {
                    format.read(buffer, startDate, context);
                } catch (Exception e) {
                    log.fine(format("Error reading with %s: %s, %s", format, e.getClass(), e.getMessage()));
                }

                if (context.getRoutes().size() > routeCountBefore) {
                    context.addFormat(format);
                    break;
                }

                try {
                    buffer.reset();
                } catch (IOException e) {
                    // Resetting to invalid mark - if the read buffer is not large enough
                    log.severe(format("No known format found within %d bytes; increase the read buffer", readBufferSize));
                    break;
                }
            }
        } finally {
            buffer.close();
        }
    }

    public ParserResult read(File source, List<NavigationFormat> formats) throws IOException {
        log.info("Reading '" + source.getAbsolutePath() + "' by " + formats.size() + " formats");
        Calendar startDate = Calendar.getInstance(UTC);
        startDate.setTimeInMillis(source.lastModified());
        FileInputStream fis = new FileInputStream(source);
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(fis, (int) source.length() + 1));
        buffer.mark((int) source.length() + 1);
        try {
            return read(buffer, (int) source.length(), startDate, formats);
        } finally {
            buffer.closeUnderlyingInputStream();
        }
    }

    public ParserResult read(File source) throws IOException {
        return read(source, getReadFormats());
    }

    private NavigationFormat determineFormat(List<BaseRoute> routes, NavigationFormat preferredFormat) {
        NavigationFormat result = preferredFormat;
        for (BaseRoute route : routes) {
            // more than one route: the same result
            if (result.equals(route.getFormat()))
                continue;

            // result is capable of storing multiple routes
            if (result.isSupportsMultipleRoutes())
                continue;

            // result from GPSBabel-based format which allows only one route but is represented by GPX 1.0
            if (result instanceof BabelFormat)
                continue;

            // default for multiple routes is GPX 1.1
            result = new Gpx11Format();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void commentRoutes(List<BaseRoute> routes) {
        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            commentPositions(route.getPositions());
            commentRouteName(route);
        }
    }

    @SuppressWarnings("unchecked")
    private ParserResult createResult(ParserContext<BaseRoute> context) throws IOException {
        List<BaseRoute> routes = context.getRoutes();
        if (routes != null && routes.size() > 0) {
            NavigationFormat format = determineFormat(routes, context.getFormats().get(0));
            List<BaseRoute> result = asFormatForRoutes(routes, format);
            log.info("Detected '" + format.getName() + "' with " + result.size() + " route(s) and " +
                    getPositionCounts(result) + " positions");
            commentRoutes(result);
            return new ParserResult(new FormatAndRoutes(format, routes));
        } else
            return new ParserResult(null);
    }

    private class InternalParserContext<R extends BaseRoute> extends ParserContextImpl<R> {
        public void parse(InputStream inputStream, int readBufferSize, CompactCalendar startDate, List<NavigationFormat> formats) throws IOException {
            internalRead(inputStream, readBufferSize, startDate, formats, this);
        }
    }

    private ParserResult read(InputStream source, int readBufferSize, Calendar startDate,
                              List<NavigationFormat> formats) throws IOException {
        log.fine("Reading '" + source + "' with a buffer of " + readBufferSize + " bytes by " + formats.size() + " formats");
        CompactCalendar compactStartDate = startDate != null ? fromCalendar(startDate) : null;
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(source, readBufferSize + 1));
        buffer.mark(readBufferSize + 1);
        try {
            ParserContext<BaseRoute> context = new InternalParserContext<BaseRoute>();
            internalRead(buffer, readBufferSize, compactStartDate, formats, context);
            return createResult(context);
        } finally {
            buffer.closeUnderlyingInputStream();
        }
    }

    public ParserResult read(String source) throws IOException {
        return read(new ByteArrayInputStream(source.getBytes()));
    }

    public ParserResult read(InputStream source) throws IOException {
        return read(source, READ_BUFFER_SIZE, null, getReadFormats());
    }

    private int getSize(URL url) throws IOException {
        try {
            if (url.getProtocol().equals("file"))
                return (int) new File(url.toURI()).length();
            else
                return READ_BUFFER_SIZE;
        } catch (URISyntaxException e) {
            throw new IOException("Cannot determine file from URL: " + e.getMessage());
        }
    }

    private Calendar getStartDate(URL url) throws IOException {
        try {
            if (url.getProtocol().equals("file")) {
                Calendar startDate = Calendar.getInstance();
                startDate.setTimeInMillis(new File(url.toURI()).lastModified());
                return startDate;
            } else
                return null;
        } catch (URISyntaxException e) {
            throw new IOException("Cannot determine file from URL: " + e.getMessage());
        }
    }

    public ParserResult read(URL url, List<NavigationFormat> formats) throws IOException {
        if (isGoogleMapsUrl(url)) {
            url = new URL(url.toExternalForm() + "&output=kml");
        }
        int readBufferSize = getSize(url);
        log.info("Reading '" + url + "' with a buffer of " + readBufferSize + " bytes");
        return read(url.openStream(), readBufferSize, getStartDate(url), formats);
    }


    public static int getNumberOfFilesToWriteFor(BaseRoute route, NavigationFormat format, boolean duplicateFirstPosition) {
        return ceiling(route.getPositionCount() + (duplicateFirstPosition ? 1 : 0), format.getMaximumPositionCount(), true);
    }

    @SuppressWarnings("unchecked")
    private void write(BaseRoute route, NavigationFormat format,
                       boolean duplicateFirstPosition,
                       boolean ignoreMaximumPositionCount,
                       OutputStream... targets) throws IOException {
        log.info("Writing '" + format.getName() + "' position lists with 1 route and " + route.getPositionCount() + " positions");

        BaseRoute routeToWrite = NavigationFormats.asFormat(route, format);
        preprocessRoute(routeToWrite, format, duplicateFirstPosition);

        int positionsToWrite = routeToWrite.getPositionCount();
        int writeInOneChunk = format.getMaximumPositionCount();

        // check if the positions to write fit within the given files
        if (positionsToWrite > targets.length * writeInOneChunk) {
            if (ignoreMaximumPositionCount)
                writeInOneChunk = positionsToWrite;
            else
                throw new IOException("Found " + positionsToWrite + " positions, " + format.getName() +
                        " format may only contain " + writeInOneChunk + " positions in one position list.");
        }

        int startIndex = 0;
        for (int i = 0; i < targets.length; i++) {
            OutputStream target = targets[i];
            int endIndex = Math.min(startIndex + writeInOneChunk, positionsToWrite);
            renameRoute(route, routeToWrite, startIndex, endIndex, i, targets);
            format.write(routeToWrite, target, startIndex, endIndex);
            log.info("Wrote position list from " + startIndex + " to " + endIndex);
            startIndex += writeInOneChunk;
        }

        postProcessRoute(routeToWrite, format, duplicateFirstPosition);
    }

    public void write(BaseRoute route, NavigationFormat format,
                      boolean duplicateFirstPosition,
                      boolean ignoreMaximumPositionCount,
                      File... targets) throws IOException {
        OutputStream[] targetStreams = new OutputStream[targets.length];
        for (int i = 0; i < targetStreams.length; i++)
            targetStreams[i] = new FileOutputStream(targets[i]);
        write(route, format, duplicateFirstPosition, ignoreMaximumPositionCount, targetStreams);
        for (File target : targets)
            log.info("Wrote '" + target.getAbsolutePath() + "'");
    }


    @SuppressWarnings("unchecked")
    private void preprocessRoute(BaseRoute routeToWrite, NavigationFormat format, boolean duplicateFirstPosition) {
        if (format instanceof NmnFormat)
            routeToWrite.removeDuplicates();
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((NmnFormat) format).getDuplicateFirstPosition(routeToWrite));
        if (format instanceof CoPilotFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((CoPilotFormat) format).getDuplicateFirstPosition(routeToWrite));
        if (format instanceof TcxFormat)
            routeToWrite.ensureIncreasingTime();
    }

    @SuppressWarnings("unchecked")
    private void renameRoute(BaseRoute route, BaseRoute routeToWrite, int startIndex, int endIndex, int trackIndex, OutputStream... targets) {
        // gives splitted TomTomRoute and SimpleRoute routes a more useful name for the fragment
        if (route.getFormat() instanceof TomTomRouteFormat || route.getFormat() instanceof SimpleFormat ||
                route.getFormat() instanceof GpxFormat && routeToWrite.getFormat() instanceof BcrFormat) {
            String name = createRouteName(routeToWrite.getPositions().subList(startIndex, endIndex));
            if (targets.length > 1)
                name = "Track" + (trackIndex + 1) + ": " + name;
            routeToWrite.setName(name);
        }
    }

    private void postProcessRoute(BaseRoute routeToWrite, NavigationFormat format, boolean duplicateFirstPosition) {
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.remove(0);
    }


    @SuppressWarnings("unchecked")
    public void write(List<BaseRoute> routes, MultipleRoutesFormat format, File target) throws IOException {
        log.info("Writing '" + format.getName() + "' with with " + routes.size() + " routes and " +
                getPositionCounts(routes) + " positions");

        List<BaseRoute> routesToWrite = new ArrayList<BaseRoute>(routes.size());
        for (BaseRoute route : routes) {
            BaseRoute routeToWrite = NavigationFormats.asFormat(route, format);
            preprocessRoute(routeToWrite, format, false);
            routesToWrite.add(routeToWrite);
            postProcessRoute(routeToWrite, format, false);
        }

        format.write(routesToWrite, new FileOutputStream(target));
        log.info("Wrote '" + target.getAbsolutePath() + "'");
    }
}
