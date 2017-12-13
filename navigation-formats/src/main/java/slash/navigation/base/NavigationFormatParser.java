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

import slash.common.io.NotClosingUnderlyingInputStream;
import slash.common.type.CompactCalendar;
import slash.navigation.babel.BabelFormat;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.rest.Get;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.url.GoogleMapsUrlFormat;
import slash.navigation.url.MotoPlanerUrlFormat;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.io.File.separatorChar;
import static java.lang.Math.min;
import static java.lang.String.format;
import static slash.common.io.Transfer.ceiling;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.NavigationFormatConverter.asFormat;
import static slash.navigation.base.NavigationFormatConverter.convertRoute;
import static slash.navigation.base.RouteComments.*;
import static slash.navigation.url.GoogleMapsUrlFormat.isGoogleMapsLinkUrl;
import static slash.navigation.url.GoogleMapsUrlFormat.isGoogleMapsProfileUrl;
import static slash.navigation.url.MotoPlanerUrlFormat.isMotoPlanerUrl;

/**
 * Parses byte streams with navigation information via {@link NavigationFormat} classes.
 *
 * @author Christian Pesch
 */

public class NavigationFormatParser {
    private static final Logger log = Logger.getLogger(NavigationFormatParser.class.getName());
    private static final int READ_BUFFER_SIZE = 1024 * 1024;
    private final NavigationFormatRegistry navigationFormatRegistry;
    private final List<NavigationFormatParserListener> listeners = new CopyOnWriteArrayList<>();

    public NavigationFormatParser(NavigationFormatRegistry navigationFormatRegistry) {
        this.navigationFormatRegistry = navigationFormatRegistry;
    }

    public NavigationFormatRegistry getNavigationFormatRegistry() {
        return navigationFormatRegistry;
    }

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
        List<Integer> positionCounts = new ArrayList<>();
        for (BaseRoute route : routes)
            positionCounts.add(route.getPositionCount());
        return positionCounts;
    }

    @SuppressWarnings("unchecked")
    private void internalRead(InputStream buffer, List<NavigationFormat> formats, ParserContext context) throws IOException {
        int routeCountBefore = context.getRoutes().size();
        NavigationFormat firstSuccessfulFormat = null;

        try {
            for (NavigationFormat<BaseRoute> format : formats) {
                notifyReading(format);

                log.fine(format("Trying to read with %s", format));
                try {
                    format.read(buffer, context);

                    // if no route has been read, take the first that didn't throw an exception
                    if (firstSuccessfulFormat == null)
                        firstSuccessfulFormat = format;
                } catch (Exception e) {
                    log.severe(format("Error reading with %s: %s, %s", format, e.getClass(), e));
                }

                if (context.getRoutes().size() > routeCountBefore) {
                    context.addFormat(format);
                    break;
                }

                try {
                    buffer.reset();
                } catch (IOException e) {
                    log.severe("Cannot reset() stream to mark()");
                    break;
                }
            }
        } finally {
            //noinspection ThrowFromFinallyBlock
            buffer.close();
        }

        if(context.getRoutes().size() == 0 && context.getFormats().size() == 0 && firstSuccessfulFormat != null)
            context.addFormat(firstSuccessfulFormat);
    }

    public ParserResult read(File source, List<NavigationFormat> formats) throws IOException {
        log.info("Reading '" + source.getAbsolutePath() + "' by " + formats.size() + " formats");
        return read(new FileInputStream(source), (int) source.length(), extractStartDate(source), source, formats);
    }

    public ParserResult read(File source) throws IOException {
        return read(source, getNavigationFormatRegistry().getReadFormats());
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
        commentRoutePositions(routes);
        for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : routes) {
            commentRouteName(route);
        }
    }

    @SuppressWarnings("unchecked")
    private void commentRoute(BaseRoute route) {
        commentPositions(route.getPositions());
        commentRouteName(route);
    }

    @SuppressWarnings("unchecked")
    private ParserResult createResult(ParserContext<BaseRoute> context) throws IOException {
        List<BaseRoute> source = context.getRoutes();
        // if (source != null && source.size() > 0) {
        if (source != null && context.getFormats().size() > 0) {
            NavigationFormat format = determineFormat(source, context.getFormats().get(0));
            List<BaseRoute> destination = convertRoute(source, format);
            log.info("Detected '" + format.getName() + "' with " + destination.size() + " route(s) and " +
                    getPositionCounts(destination) + " positions");
            if(destination.size() == 0)
                destination.add(format.createRoute(RouteCharacteristics.Route, null, new ArrayList<>()));
            commentRoutes(destination);
            return new ParserResult(new FormatAndRoutes(format, destination));
        } else
            return new ParserResult(null);
    }

    private class InternalParserContext<R extends BaseRoute> extends ParserContextImpl<R> {
        public InternalParserContext(File file, CompactCalendar startDate) {
            super(file, startDate);
        }

        public void parse(InputStream inputStream, CompactCalendar startDate, String preferredExtension) throws IOException {
            internalSetStartDate(startDate);
            internalRead(inputStream, getNavigationFormatRegistry().getReadFormatsPreferredByExtension(preferredExtension), this);
        }

        public void parse(String urlString) throws IOException {
            // replace CWD with current working directory for easier testing
            urlString = urlString.replace("CWD", new File(".").getCanonicalPath()).replace(separatorChar, '/');
            URL url = new URL(urlString);
            int readBufferSize = getSize(url);
            log.info("Reading '" + url + "' with a buffer of " + readBufferSize + " bytes");
            NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(openStream(url)));
            buffer.mark(readBufferSize + 1);
            try {
                CompactCalendar startDate = extractStartDate(url);
                internalSetStartDate(startDate);
                internalRead(buffer, getNavigationFormatRegistry().getReadFormats(), this);
            } finally {
                //noinspection ThrowFromFinallyBlock
                buffer.closeUnderlyingInputStream();
            }
        }
    }

    private ParserResult read(InputStream source, int readBufferSize, CompactCalendar startDate, File file,
                              List<NavigationFormat> formats) throws IOException {
        log.fine("Reading '" + source + "' with a buffer of " + readBufferSize + " bytes by " + formats.size() + " formats");
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(source));
        buffer.mark(readBufferSize + 1);
        try {
            ParserContext<BaseRoute> context = new InternalParserContext<>(file, startDate);
            internalRead(buffer, formats, context);
            return createResult(context);
        } finally {
            //noinspection ThrowFromFinallyBlock
            buffer.closeUnderlyingInputStream();
        }
    }

    public ParserResult read(String source) throws IOException {
        return read(new ByteArrayInputStream(source.getBytes()));
    }

    public ParserResult read(InputStream source) throws IOException {
        return read(source, getNavigationFormatRegistry().getReadFormats());
    }

    public ParserResult read(InputStream source, List<NavigationFormat> formats) throws IOException {
        return read(source, READ_BUFFER_SIZE, null, null, formats);
    }

    private int getSize(URL url) throws IOException {
        try {
            if (url.getProtocol().equals("file"))
                return (int) new File(url.toURI()).length();
            else
                return READ_BUFFER_SIZE;
        } catch (URISyntaxException e) {
            throw new IOException("Cannot determine file from URL: " + e);
        }
    }

    private CompactCalendar extractStartDate(File file) {
        Calendar startDate = Calendar.getInstance(UTC);
        startDate.setTimeInMillis(file.lastModified());
        return fromCalendar(startDate);
    }

    private File extractFile(URL url) throws IOException {
        try {
            if (url.getProtocol().equals("file")) {
                return new File(url.toURI());
            } else
                return null;
        } catch (URISyntaxException e) {
            throw new IOException("Cannot determine file from URL: " + e);
        }
    }

    private CompactCalendar extractStartDate(URL url) throws IOException {
        File file = extractFile(url);
        if (file != null) {
            return extractStartDate(file);
        } else
            return null;
    }

    public ParserResult read(URL url, List<NavigationFormat> formats) throws IOException {
        if (isGoogleMapsProfileUrl(url)) {
            url = new URL(url.toExternalForm() + "&output=kml");
            formats = new ArrayList<>(formats);
            formats.add(0, new Kml22Format());

        } else if (isGoogleMapsLinkUrl(url)) {
            byte[] bytes = url.toExternalForm().getBytes();
            List<NavigationFormat> readFormats = new ArrayList<>(formats);
            readFormats.add(0, new GoogleMapsUrlFormat());
            return read(new ByteArrayInputStream(bytes), bytes.length, null, null, readFormats);

        } else if (isMotoPlanerUrl(url)) {
            byte[] bytes = url.toExternalForm().getBytes();
            List<NavigationFormat> readFormats = new ArrayList<>(formats);
            readFormats.add(0, new MotoPlanerUrlFormat());
            return read(new ByteArrayInputStream(bytes), bytes.length, null, null, readFormats);
        }

        int readBufferSize = getSize(url);
        log.info("Reading '" + url + "' with a buffer of " + readBufferSize + " bytes");
        return read(openStream(url), readBufferSize, extractStartDate(url), extractFile(url), formats);
    }

    private InputStream openStream(URL url) throws IOException {
        String urlString = url.toExternalForm();
        // make sure HTTPS requests use HTTP Client with it's SSL tweaks
        if(urlString.contains("https://")) {
            Get get = new Get(urlString);
            return get.executeAsStream();
        }
        return url.openStream();
    }

    public ParserResult read(URL url) throws IOException {
        return read(url, getNavigationFormatRegistry().getReadFormats());
    }


    public static int getNumberOfFilesToWriteFor(BaseRoute route, NavigationFormat format, boolean duplicateFirstPosition) {
        return ceiling(route.getPositionCount() + (duplicateFirstPosition ? 1 : 0), format.getMaximumPositionCount(), true);
    }

    @SuppressWarnings("unchecked")
    private void write(BaseRoute route, NavigationFormat format,
                       boolean duplicateFirstPosition,
                       boolean ignoreMaximumPositionCount,
                       ParserCallback parserCallback,
                       OutputStream... targets) throws IOException {
        log.info("Writing '" + format.getName() + "' position lists with 1 route and " + route.getPositionCount() + " positions");

        BaseRoute routeToWrite = asFormat(route, format);
        commentRoute(routeToWrite);
        preprocessRoute(routeToWrite, format, duplicateFirstPosition, parserCallback);

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
            int endIndex = min(startIndex + writeInOneChunk, positionsToWrite);
            renameRoute(route, routeToWrite, startIndex, endIndex, i, targets);
            format.write(routeToWrite, target, startIndex, endIndex);
            log.info("Wrote position list from " + startIndex + " to " + endIndex);
            startIndex += writeInOneChunk;
        }

        postProcessRoute(routeToWrite, format, duplicateFirstPosition);
    }

    public void write(BaseRoute route, NavigationFormat format, File target) throws IOException {
        write(route, format, false, false, null, target);
    }

    public void write(BaseRoute route, NavigationFormat format,
                      boolean duplicateFirstPosition,
                      boolean ignoreMaximumPositionCount,
                      ParserCallback parserCallback,
                      File... targets) throws IOException {
        OutputStream[] targetStreams = new OutputStream[targets.length];
        for (int i = 0; i < targetStreams.length; i++)
            targetStreams[i] = new FileOutputStream(targets[i]);
        write(route, format, duplicateFirstPosition, ignoreMaximumPositionCount, parserCallback, targetStreams);
        for (File target : targets)
            log.info("Wrote '" + target.getAbsolutePath() + "'");
    }


    @SuppressWarnings("unchecked")
    private void preprocessRoute(BaseRoute routeToWrite, NavigationFormat format,
                                 boolean duplicateFirstPosition,
                                 ParserCallback parserCallback) {
        if (format instanceof NmnFormat)
            routeToWrite.removeDuplicates();
        if (format instanceof NmnFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((NmnFormat) format).getDuplicateFirstPosition(routeToWrite));
        if (format instanceof CoPilotFormat && duplicateFirstPosition)
            routeToWrite.add(0, ((CoPilotFormat) format).getDuplicateFirstPosition(routeToWrite));
        if (format instanceof TcxFormat)
            routeToWrite.ensureIncreasingTime();
        if (parserCallback != null)
            parserCallback.process(routeToWrite, format);
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
        if ((format instanceof NmnFormat || format instanceof CoPilotFormat) && duplicateFirstPosition)
            routeToWrite.remove(0);
    }


    @SuppressWarnings("unchecked")
    public void write(List<BaseRoute> routes, MultipleRoutesFormat format, File target) throws IOException {
        log.info("Writing '" + format.getName() + "' with " + routes.size() + " routes and " +
                getPositionCounts(routes) + " positions");

        List<BaseRoute> routesToWrite = new ArrayList<>(routes.size());
        for (BaseRoute route : routes) {
            BaseRoute routeToWrite = asFormat(route, format);
            commentRoute(routeToWrite);
            preprocessRoute(routeToWrite, format, false, null);
            routesToWrite.add(routeToWrite);
            postProcessRoute(routeToWrite, format, false);
        }

        try (OutputStream outputStream = new FileOutputStream(target)) {
            format.write(routesToWrite, outputStream);
            log.info("Wrote '" + target.getAbsolutePath() + "'");
        }
    }
}
