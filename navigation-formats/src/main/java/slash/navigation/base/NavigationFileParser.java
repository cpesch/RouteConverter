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

import slash.navigation.bcr.BcrFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.util.*;
import slash.navigation.simple.GoogleMapsFormat;
import slash.common.io.NotClosingUnderlyingInputStream;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Parses files with navigation information via NavigationFormat classes.
 *
 * @author Christian Pesch
 */

public class NavigationFileParser {
    private static final Logger log = Logger.getLogger(NavigationFileParser.class.getName());
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    private final List<NavigationFileParserListener> navigationFileParserListeners = new CopyOnWriteArrayList<NavigationFileParserListener>();
    private FormatAndRoutes formatAndRoutes;

    public void addNavigationFileParserListener(NavigationFileParserListener listener) {
        navigationFileParserListeners.add(listener);
    }

    public void removeNavigationFileParserListener(NavigationFileParserListener listener) {
        navigationFileParserListeners.remove(listener);
    }

    public NavigationFormat getFormat() {
        return formatAndRoutes.getFormat();
    }

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getTheRoute() {
        return formatAndRoutes.getRoute();
    }

    public List<BaseRoute> getAllRoutes() {
        return formatAndRoutes.getRoutes();
    }


    List<BaseRoute> getRouteCharacteristics(RouteCharacteristics characteristics) {
        List<BaseRoute> result = new ArrayList<BaseRoute>();
        for (BaseRoute route : getAllRoutes()) {
            if (route.getCharacteristics().equals(characteristics))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }

    private List<Integer> getPositionCounts(List<BaseRoute> routes) {
        List<Integer> positionCounts = new ArrayList<Integer>();
        for (BaseRoute route : routes)
            positionCounts.add(route.getPositionCount());
        return positionCounts;
    }

    private void notifyReading(NavigationFormat<BaseRoute> format) {
        for (NavigationFileParserListener listener : navigationFileParserListeners) {
            listener.reading(format);
        }
    }

    private FormatAndRoutes internalRead(InputStream source, int readBufferSize, Calendar startDate,
                                         List<NavigationFormat> formats) throws IOException {
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(source, readBufferSize + 1));
        try {
            buffer.mark(readBufferSize + 1);
            CompactCalendar compactStartDate = startDate != null ? CompactCalendar.fromCalendar(startDate) : null;
            for (NavigationFormat<BaseRoute> format : formats) {
                notifyReading(format);

                List<BaseRoute> routes = format.read(buffer, compactStartDate);
                if (routes != null && routes.size() > 0) {
                    log.info("Detected '" + format.getName() + "' file with " + routes.size() + " route(s) and " +
                            getPositionCounts(routes) + " positions");
                    commentRoutes(routes);
                    return new FormatAndRoutes(format, routes);
                }

                try {
                    buffer.reset();
                } catch (IOException e) {
                    // Resetting to invalid mark - if the read buffer is not large enough
                    log.severe("No known format found within " + readBufferSize + " bytes; increase the read buffer");
                    break;
                }
            }
        }
        finally {
            buffer.close();
        }
        return null;
    }

    private void commentRoutes(List<BaseRoute> routes) {
        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            RouteComments.commentPositions(route.getPositions(), route.getFormat() instanceof GoogleMapsFormat);
            RouteComments.commentRouteName(route);
        }
    }

    public boolean read(File source, List<NavigationFormat> formats) throws IOException {
        log.info("Reading '" + source.getAbsolutePath() + "' by " + formats.size() + " formats");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        FileInputStream fis = new FileInputStream(source);
        try {
            this.formatAndRoutes = internalRead(fis, (int) source.length(), startDate, formats);
            return formatAndRoutes != null;
        }
        finally {
            fis.close();
        }
    }

    public boolean read(File source) throws IOException {
        return read(source, NavigationFormats.getReadFormats());
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

    public boolean read(InputStream source, int readBufferSize, Calendar startDate,
                        List<NavigationFormat> formats) throws IOException {
        log.fine("Reading '" + source + "' with a buffer of " + readBufferSize + " bytes by " + formats.size() + " formats");
        try {
            this.formatAndRoutes = internalRead(source, readBufferSize, startDate, formats);
            return formatAndRoutes != null;
        }
        finally {
            source.close();
        }
    }

    public boolean read(InputStream source) throws IOException {
        return read(source, READ_BUFFER_SIZE, null, NavigationFormats.getReadFormats());
    }

    public boolean read(URL url, List<NavigationFormat> formats) throws IOException {
        InputStream in;
        int readBufferSize;
        Calendar startDate;
        if (GoogleMapsFormat.isGoogleMapsUrl(url)) {
            byte[] bytes = url.toExternalForm().getBytes();
            in = new ByteArrayInputStream(bytes);
            readBufferSize = bytes.length;
            startDate = Calendar.getInstance();
        } else {
            in = url.openStream();
            readBufferSize = getSize(url);
            startDate = getStartDate(url);
        }
        log.info("Reading '" + url + "' with a buffer of " + readBufferSize + " bytes");
        return read(in, readBufferSize, startDate, formats);
    }

    public boolean read(URL url) throws IOException {
        return read(url, NavigationFormats.getReadFormats());
    }


    public static int getNumberOfFilesToWriteFor(BaseRoute route, NavigationFormat format, boolean duplicateFirstPosition) {
        return Transfer.ceiling(route.getPositionCount() + (duplicateFirstPosition ? 1 : 0), format.getMaximumPositionCount(), true);
    }

    public void write(BaseRoute route, NavigationFormat format,
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

        this.formatAndRoutes = new FormatAndRoutes(format, Arrays.asList(routeToWrite));
    }

    public void write(BaseRoute route, NavigationFormat format,
                      boolean duplicateFirstPosition,
                      boolean ignoreMaximumPositionCount,
                      File... targets) throws IOException {
        OutputStream[] targetStreams = new OutputStream[targets.length];
        for (int i = 0; i < targetStreams.length; i++)
            targetStreams[i] = new FileOutputStream(targets[i]);
        write(route, format, duplicateFirstPosition, ignoreMaximumPositionCount, targetStreams);
    }


    private void preprocessRoute(BaseRoute routeToWrite, NavigationFormat format, boolean duplicateFirstPosition) {
        if (format instanceof NmnFormat)
            routeToWrite.removeDuplicates();
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((NmnFormat) format).getDuplicateFirstPosition(routeToWrite));
    }

    private void renameRoute(BaseRoute route, BaseRoute routeToWrite, int startIndex, int endIndex, int trackIndex, OutputStream... targets) {
        // gives splitted TomTomRoute and SimpleRoute routes a more useful name for the fragment
        if (route.getFormat() instanceof TomTomRouteFormat || route.getFormat() instanceof SimpleFormat ||
                route.getFormat() instanceof GpxFormat && routeToWrite.getFormat() instanceof BcrFormat) {
            String name = RouteComments.createRouteName(routeToWrite.getPositions().subList(startIndex, endIndex));
            if (targets.length > 1)
                name = "Track" + (trackIndex + 1) + ": " + name;
            routeToWrite.setName(name);
        }
    }

    private void postProcessRoute(BaseRoute routeToWrite, NavigationFormat format, boolean duplicateFirstPosition) {
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.remove(0);
    }


    public void write(List<BaseRoute> routes, MultipleRoutesFormat format, File target) throws IOException {
        log.info("Writing '" + format.getName() + "' file with with " + routes.size() + " routes and " +
                getPositionCounts(routes) + " positions");

        List<BaseRoute> routesToWrite = new ArrayList<BaseRoute>(routes.size());
        for (BaseRoute route : routes) {
            BaseRoute routeToWrite = NavigationFormats.asFormat(route, format);
            routesToWrite.add(routeToWrite);
        }

        format.write(routesToWrite, new FileOutputStream(target));
        log.info("Wrote '" + target.getAbsolutePath() + "'");

        this.formatAndRoutes = new FormatAndRoutes(format, routesToWrite);
    }
}
