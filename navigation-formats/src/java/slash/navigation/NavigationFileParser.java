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

package slash.navigation;

import slash.navigation.bcr.BcrFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.util.Conversion;
import slash.navigation.util.NotClosingUnderlyingInputStream;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses files with navigation information via BaseNavigationFormat plugins.
 *
 * @author Christian Pesch
 */

public class NavigationFileParser {
    private static final Logger log = Logger.getLogger(NavigationFileParser.class.getName());
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    private FormatAndRoutes formatAndRoutes;


    public NavigationFormat getFormat() {
        return formatAndRoutes.getFormat();
    }

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getTheRoute() {
        return formatAndRoutes.getRoute();
    }

    public List<BaseRoute> getAllRoutes() {
        return formatAndRoutes.getRoutes();
    }


    public List<BaseRoute> getRouteCharacteristics(RouteCharacteristics characteristics) {
        List<BaseRoute> result = new ArrayList<BaseRoute>();
        for (BaseRoute route : getAllRoutes()) {
            if (route.getCharacteristics().equals(characteristics))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }

    public List<BaseRoute> getWaypoints() {
        return getRouteCharacteristics(RouteCharacteristics.Waypoints);
    }

    public List<BaseRoute> getRoutes() {
        return getRouteCharacteristics(RouteCharacteristics.Route);
    }

    public List<BaseRoute> getTracks() {
        return getRouteCharacteristics(RouteCharacteristics.Track);
    }

    private List<Integer> getPositionCounts(List<BaseRoute> routes) {
        List<Integer> positionCounts = new ArrayList<Integer>();
        for (BaseRoute route : routes)
            positionCounts.add(route.getPositionCount());
        return positionCounts;
    }

    private FormatAndRoutes read(InputStream source, int readBufferSize, Calendar startDate) throws IOException {
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(source, readBufferSize + 1));
        try {
            buffer.mark(readBufferSize + 1);

            for (NavigationFormat<BaseRoute> format : NavigationFormats.getReadFormats()) {
                List<BaseRoute> routes = format.read(buffer, startDate);
                if (routes != null && routes.size() > 0) {
                    log.info("Detected '" + format.getName() + "' file with " + routes.size() + " route(s) and " +
                            getPositionCounts(routes) + " positions");
                    for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
                        RouteComments.commentPositions(route.getPositions());
                        RouteComments.commentRouteName(route);
                    }
                    return new FormatAndRoutes(format, routes);
                }

                try {
                    buffer.reset();
                } catch (IOException e) {
                    // Resetting to invalid mark - if the read buffer is not large enough
                    log.severe("No known format found within " + readBufferSize + " bytes; increase the read buffer");
                    return null;
                }
            }
        }
        finally {
            buffer.close();
        }
        return null;
    }

    public boolean read(File source) throws IOException {
        log.info("Reading '" + source.getAbsolutePath() + "'");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        FileInputStream fis = new FileInputStream(source);
        try {
            this.formatAndRoutes = read(fis, (int) source.length(), startDate);
            return formatAndRoutes != null;
        }
        finally {
            fis.close();
        }
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

    public boolean read(URL url) throws IOException {
        int readBufferSize;
        InputStream in;
        if (GoogleMapsFormat.isGoogleMapsUrl(url)) {
            byte[] bytes = url.toExternalForm().getBytes();
            readBufferSize = bytes.length;
            in = new ByteArrayInputStream(bytes);
        } else {
            readBufferSize = getSize(url);
            in = url.openStream();
        }
        return read(in, readBufferSize);
    }

    public boolean read(InputStream source, int readBufferSize) throws IOException {
        log.info("Reading '" + source + "' with a buffer of " + readBufferSize + " bytes");
        try {
            this.formatAndRoutes = read(source, readBufferSize, null);
            return formatAndRoutes != null;
        }
        finally {
            source.close();
        }
    }

    public boolean read(InputStream source) throws IOException {
        return read(source, READ_BUFFER_SIZE);
    }


    public int getNumberOfFilesToWriteFor(BaseRoute route, NavigationFormat format, boolean duplicateFirstPosition) {
        return Conversion.ceiling(route.getPositionCount() + (duplicateFirstPosition ? 1 : 0), format.getMaximumPositionCount(), true);
    }

    public void write(BaseRoute route, NavigationFormat format,
                      boolean duplicateFirstPosition,
                      boolean numberPositionNames,
                      boolean ignoreMaximumPositionCount,
                      File... targets) throws IOException {
        log.info("Writing '" + format.getName() + "' file(s) with 1 route and " + route.getPositionCount() + " positions");

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
                        " format may only contain " + writeInOneChunk + " positions in one file.");
        }

        int startIndex = 0;
        for (int i = 0; i < targets.length; i++) {
            File target = targets[i];
            int endIndex = Math.min(startIndex + writeInOneChunk, positionsToWrite);
            renameRoute(route, routeToWrite, startIndex, endIndex, i, targets);
            format.write(routeToWrite, target, startIndex, endIndex, numberPositionNames);
            log.info("Wrote '" + target.getAbsoluteFile() + "'");
            startIndex += writeInOneChunk;
        }

        postProcessRoute(routeToWrite, format, duplicateFirstPosition);

        this.formatAndRoutes = new FormatAndRoutes(format, Arrays.asList(routeToWrite));
    }


    private void preprocessRoute(BaseRoute routeToWrite, NavigationFormat format, boolean duplicateFirstPosition) {
        if (format instanceof NmnFormat)
            routeToWrite.removeDuplicates();
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((NmnFormat) format).getDuplicateFirstPosition(routeToWrite));
    }

    /**
     * Gives splitted ITNRoute and SimpleRoutes a more useful name for the fragment
     */
    private void renameRoute(BaseRoute route, BaseRoute routeToWrite, int startIndex, int endIndex, int j, File... targets) {
        if (route.getFormat() instanceof TomTomRouteFormat || route.getFormat() instanceof SimpleFormat ||
                route.getFormat() instanceof GpxFormat && routeToWrite.getFormat() instanceof BcrFormat) {
            String name = RouteComments.createRouteName(routeToWrite.getPositions().subList(startIndex, endIndex));
            if (targets.length > 1)
                name = "Track" + (j + 1) + ": " + name;
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

        format.write(routesToWrite, target);
        log.info("Wrote '" + target.getAbsolutePath() + "'");

        this.formatAndRoutes = new FormatAndRoutes(format, routesToWrite);
    }
}
