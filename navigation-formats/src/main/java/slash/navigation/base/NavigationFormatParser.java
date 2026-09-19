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
import slash.navigation.photo.PhotoFormat;
import slash.navigation.tcx.TcxFormat;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.io.File.separatorChar;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static slash.common.io.Files.getExtension;
import static slash.common.io.Files.toUrl;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.ceiling;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.NavigationFormatConverter.asFormat;
import static slash.navigation.base.NavigationFormatConverter.convertRoute;
import static slash.navigation.base.RouteComments.*;
import static slash.navigation.url.GoogleMapsUrlFormat.isGoogleMapsProfileUrl;

/**
 * Parses byte streams with navigation information via {@link NavigationFormat} classes.
 *
 * @author Christian Pesch
 */

public class NavigationFormatParser {
    private static final Logger log = Logger.getLogger(NavigationFormatParser.class.getName());
    public static final int TOTAL_BUFFER_SIZE = 1024 * 1024;
    private static final int CHUNK_BUFFER_SIZE = 8 * 1024;
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

    // The probing loop below shares one ParserContext across every candidate format, so all
    // candidates need a single common route ceiling for the duration of the probe - each
    // format's own narrower R is exactly a BaseRoute<?, ?>, so the widening is sound even
    // though javac cannot verify it across independently-typed NavigationFormat<?> instances.
    @SuppressWarnings("unchecked")
    private static List<NavigationFormat<BaseRoute<?, ?>>> widen(List<NavigationFormat<?>> formats) {
        return (List<NavigationFormat<BaseRoute<?, ?>>>) (List<?>) formats;
    }

    @SuppressWarnings("unchecked")
    private static NavigationFormat<BaseRoute<?, ?>> widen(NavigationFormat<?> format) {
        return (NavigationFormat<BaseRoute<?, ?>>) format;
    }

    @SuppressWarnings("unchecked")
    private static MultipleRoutesFormat<BaseRoute<?, ?>> widen(MultipleRoutesFormat<?> format) {
        return (MultipleRoutesFormat<BaseRoute<?, ?>>) format;
    }

    private void notifyReading(NavigationFormat<BaseRoute<?, ?>> format) {
        for (NavigationFormatParserListener listener : listeners) {
            listener.reading(format);
        }
    }

    private List<Integer> getPositionCounts(List<BaseRoute<?, ?>> routes) {
        List<Integer> positionCounts = new ArrayList<>();
        for (BaseRoute<?, ?> route : routes)
            // guard against strange effects in tests only
            if (route != null)
                positionCounts.add(route.getPositionCount());
        return positionCounts;
    }

    // outcome of trying a single candidate format against the shared buffer, shared by both
    // probe loops below (internalRead and bufferedInternalRead) since only what happens after
    // a MATCHED/DECLINED-with-reset-failure result differs between them (rc/RouteConverter#194)
    private enum ProbeOutcome { MATCHED, READ, DECLINED }

    private ProbeOutcome probeFormat(NavigationFormat<BaseRoute<?, ?>> format, InputStream buffer,
                                     ParserContext<BaseRoute<?, ?>> context, int routeCountBefore) {
        notifyReading(format);

        log.fine(format("Trying to read with %s", format));
        boolean declined = false;
        try {
            format.read(buffer, context);
        } catch (Exception e) {
            // probing tries every candidate format in turn, so a format declining a file it does
            // not handle (e.g. Gpx11Format on a GPX 1.0 file, before Gpx10Format reads it) is normal
            // control flow, not an error - keep it at fine so it does not raise a false alarm
            log.fine(format("Cannot read with %s, trying next format: %s", format, e));
            declined = true;
        }

        if (context.getRoutes().size() > routeCountBefore) {
            context.addFormat(format);
            return ProbeOutcome.MATCHED;
        }
        return declined ? ProbeOutcome.DECLINED : ProbeOutcome.READ;
    }

    private void internalRead(InputStream buffer, List<NavigationFormat<BaseRoute<?, ?>>> formats, ParserContext<BaseRoute<?, ?>> context) throws IOException {
        int routeCountBefore = context.getRoutes().size();
        NavigationFormat<BaseRoute<?, ?>> firstSuccessfulFormat = null;

        try {
            for (NavigationFormat<BaseRoute<?, ?>> format : formats) {
                ProbeOutcome outcome = probeFormat(format, buffer, context, routeCountBefore);
                if (outcome == ProbeOutcome.MATCHED)
                    break;
                // if no route has been read, take the first that didn't throw an exception
                if (outcome == ProbeOutcome.READ && firstSuccessfulFormat == null)
                    firstSuccessfulFormat = format;

                try {
                    buffer.reset();
                } catch (IOException e) {
                    log.severe("Cannot reset() stream to mark() (probe cap " + TOTAL_BUFFER_SIZE + " bytes): " + e.getLocalizedMessage());
                    break;
                }
            }
        } finally {
            buffer.close();
        }

        if (context.getRoutes().isEmpty() && context.getFormats().isEmpty() && firstSuccessfulFormat != null)
            context.addFormat(firstSuccessfulFormat);
    }

    public ParserResult read(File source, List<NavigationFormat<?>> formats) throws IOException {
        log.info("Reading '" + source.getAbsolutePath() + "' by " + formats.size() + " formats");
        return read(() -> openFileInputStream(source), markSizeFor(source.length()), extractStartDate(source), source, widen(formats));
    }

    private static InputStream openFileInputStream(File source) {
        try {
            return new FileInputStream(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ParserResult read(File source) throws IOException {
        return read(source, getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(source)));
    }

    private NavigationFormat<BaseRoute<?, ?>> determineFormat(List<BaseRoute<?, ?>> routes, NavigationFormat<BaseRoute<?, ?>> preferredFormat) {
        NavigationFormat<BaseRoute<?, ?>> result = preferredFormat;
        for (BaseRoute<?, ?> route : routes) {
            // more than one route: the same result
            if (result.equals(route.getFormat()))
                continue;

            // result is capable of storing multiple routes
            if (result.isSupportsMultipleRoutes())
                continue;

            // result from GPSBabel-based format which allows only one route but is represented by GPX 1.0
            if (((NavigationFormat<?>) result) instanceof BabelFormat)
                continue;

            // default for multiple routes is GPX 1.1
            result = widen(new Gpx11Format());
        }
        return result;
    }

    private void commentRoutes(List<BaseRoute<?, ?>> routes) {
        commentRoutePositions(routes);
        for (BaseRoute<?, ?> route : routes) {
            commentRouteName(route);
        }
    }

    private void commentRoute(BaseRoute<?, ?> route) {
        commentPositions(route.getPositions());
        commentRouteName(route);
    }

    private ParserResult createResult(ParserContext<BaseRoute<?, ?>> context) throws IOException {
        List<BaseRoute<?, ?>> source = context.getRoutes();
        // if (source != null && source.size() > 0) {
        if (source != null && !context.getFormats().isEmpty()) {
            NavigationFormat<BaseRoute<?, ?>> format = determineFormat(source, context.getFormats().get(0));
            List<BaseRoute<?, ?>> destination = convertRoute(source, format);
            log.info("Detected '" + format.getName() + "' with " + destination.size() + " route(s) and " +
                    getPositionCounts(destination) + " positions");
            if (destination.isEmpty())
                destination.add(format.createRoute(RouteCharacteristics.Route, null, new ArrayList<>()));
            commentRoutes(destination);
            return new ParserResult(FormatAndRoutes.of(format, destination));
        } else
            return new ParserResult(null);
    }

    private class InternalParserContext extends ParserContextImpl<BaseRoute<?, ?>> {
        InternalParserContext(File file, CompactCalendar startDate) {
            super(file, startDate);
        }

        public void parse(InputStream inputStream, CompactCalendar startDate, String preferredExtension) throws IOException {
            internalSetStartDate(startDate);
            internalRead(inputStream, widen(getNavigationFormatRegistry().getReadFormatsPreferredByExtension(preferredExtension)), this);
        }

        public void parse(String urlString) throws IOException {
            // replace CWD with current working directory for easier testing
            urlString = urlString.replace("CWD", new File(".").getCanonicalPath()).replace(separatorChar, '/');
            URL url = toUrl(urlString);
            byte[] bytes;
            try (InputStream inputStream = url.openStream()) {
                bytes = inputStream.readAllBytes();
            }
            log.info("Reading '" + url + "' with " + bytes.length + " bytes");
            internalSetStartDate(extractStartDate(url));
            bufferedInternalRead(() -> new ByteArrayInputStream(bytes), markSizeFor(bytes.length), widen(getNavigationFormatRegistry().getReadFormats()), this);
        }
    }

    private ParserResult read(Supplier<InputStream> source, int readBufferSize, CompactCalendar startDate, File file,
                              List<NavigationFormat<BaseRoute<?, ?>>> formats) throws IOException {
        log.fine("Reading with a buffer of " + readBufferSize + " bytes by " + formats.size() + " formats");
        ParserContext<BaseRoute<?, ?>> context = new InternalParserContext(file, startDate);
        bufferedInternalRead(source, readBufferSize, formats, context);
        return createResult(context);
    }

    /**
     * Returns the mark() size for probing a source of the given length: the
     * length itself for sources up to {@link #TOTAL_BUFFER_SIZE} - so their end
     * stays inside the mark and reset() between all format attempts succeeds -
     * and the cap for larger ones, so opening a big file does not buffer it
     * completely into the heap and a length above 2 GiB cannot overflow the
     * int mark size into a negative value.
     */
    static int markSizeFor(long length) {
        return (int) min(max(length, 0L), (long) TOTAL_BUFFER_SIZE);
    }

    /**
     * Opens the source and marks past its end, capped at {@link #TOTAL_BUFFER_SIZE}, so
     * reset() between format attempts succeeds unless a format reads past the cap - in which
     * case the candidate that overran the cap is skipped and probing re-opens a fresh stream
     * from {@code source} to continue with the remaining candidates (rc/RouteConverter#188).
     * A {@code source} that cannot be re-opened (e.g. a one-shot {@link InputStream}) returns
     * {@code null} on its second call, which stops probing exactly as a failed reset() used to.
     */
    private void bufferedInternalRead(Supplier<InputStream> source, int markSize, List<NavigationFormat<BaseRoute<?, ?>>> formats,
                                      ParserContext<BaseRoute<?, ?>> context) throws IOException {
        int routeCountBefore = context.getRoutes().size();
        NavigationFormat<BaseRoute<?, ?>> firstSuccessfulFormat = null;

        NotClosingUnderlyingInputStream buffer = openAndMark(getStream(source), markSize);
        try {
            for (NavigationFormat<BaseRoute<?, ?>> format : formats) {
                ProbeOutcome outcome = probeFormat(format, buffer, context, routeCountBefore);
                if (outcome == ProbeOutcome.MATCHED)
                    break;
                // if no route has been read, take the first that didn't throw an exception
                if (outcome == ProbeOutcome.READ && firstSuccessfulFormat == null)
                    firstSuccessfulFormat = format;

                try {
                    buffer.reset();
                } catch (IOException e) {
                    // the candidate above read past the mark cap, so this buffer's mark is gone - that
                    // only invalidates the candidate that overran it, not the remaining candidates, so
                    // re-open a fresh stream and keep probing instead of aborting the whole loop
                    log.warning("Cannot reset() stream to mark() (probe cap " + TOTAL_BUFFER_SIZE +
                            " bytes), reopening for the next candidate: " + e.getLocalizedMessage());

                    InputStream reopened;
                    try {
                        reopened = source.get();
                    } catch (RuntimeException reopenFailure) {
                        log.severe("Cannot reopen stream after failed reset(): " + reopenFailure.getLocalizedMessage());
                        break;
                    }
                    if (reopened == null) {
                        log.severe("Cannot reopen stream after failed reset(): no source left to reopen from");
                        break;
                    }

                    buffer.closeUnderlyingInputStream();
                    buffer = openAndMark(reopened, markSize);
                }
            }
        } finally {
            buffer.closeUnderlyingInputStream();
        }

        if (context.getRoutes().isEmpty() && context.getFormats().isEmpty() && firstSuccessfulFormat != null)
            context.addFormat(firstSuccessfulFormat);
    }

    private static InputStream getStream(Supplier<InputStream> source) throws IOException {
        try {
            return source.get();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static NotClosingUnderlyingInputStream openAndMark(InputStream raw, int markSize) {
        NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(raw, CHUNK_BUFFER_SIZE));
        buffer.mark(markSize + CHUNK_BUFFER_SIZE * 2);
        return buffer;
    }

    /**
     * A {@link Supplier} for a source that can only be read once, e.g. a caller-supplied
     * {@link InputStream} that cannot be re-opened: returns {@code source} on the first call
     * and {@code null} on every call after, signalling "cannot recover" to the probe loop.
     */
    private static Supplier<InputStream> oneShot(InputStream source) {
        InputStream[] holder = {source};
        return () -> {
            InputStream result = holder[0];
            holder[0] = null;
            return result;
        };
    }

    public ParserResult read(String source) throws IOException {
        return read(new ByteArrayInputStream(source.getBytes(UTF8_ENCODING)));
    }

    public ParserResult read(InputStream source) throws IOException {
        return read(source, getNavigationFormatRegistry().getReadFormats());
    }

    public ParserResult read(InputStream source, List<NavigationFormat<?>> formats) throws IOException {
        return read(oneShot(source), TOTAL_BUFFER_SIZE, null, null, widen(formats));
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

    private BaseUrlParsingFormat getUrlParsingFormat(String url) {
        for(BaseUrlParsingFormat format : getNavigationFormatRegistry().getUrlParsingFormats()) {
            if(format.findURL(url) != null)
                return format;
        }
        return null;
    }

    public ParserResult read(URL url, List<NavigationFormat<?>> formats) throws IOException {
        BaseUrlParsingFormat urlParsingFormat = getUrlParsingFormat(url.toExternalForm());
        if(urlParsingFormat != null) {
            List<NavigationFormat<?>> readFormats = new ArrayList<>(formats);
            readFormats.add(0, urlParsingFormat);
            byte[] bytes = url.toExternalForm().getBytes();
            return read(() -> new ByteArrayInputStream(bytes), bytes.length, null, null, widen(readFormats));
        }

        if (isGoogleMapsProfileUrl(url)) {
            url = toUrl(url.toExternalForm() + "&output=kml");
            List<NavigationFormat<?>> withKml = new ArrayList<>(formats);
            withKml.add(0, new Kml22Format());
            formats = withKml;
        }

        // read the whole response into memory so the exact size drives the mark()
        // read limit; a fixed guess smaller than the content broke reset() between
        // format attempts for catalog files larger than the default buffer
        try (InputStream inputStream = url.openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            log.info("Reading '" + url + "' with " + bytes.length + " bytes");
            return read(() -> new ByteArrayInputStream(bytes), bytes.length, extractStartDate(url), extractFile(url), widen(formats));
        }
    }

    public ParserResult read(URL url) throws IOException {
        return read(url, getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(url)));
    }


    public static int getNumberOfFilesToWriteFor(BaseRoute<?, ?> route, NavigationFormat<?> format, boolean duplicateFirstPosition) {
        return ceiling(route.getPositionCount() + (duplicateFirstPosition ? 1 : 0), format.getMaximumPositionCount(), true);
    }

    private void write(BaseRoute<?, ?> route, NavigationFormat<BaseRoute<?, ?>> format,
                       boolean duplicateFirstPosition,
                       boolean ignoreMaximumPositionCount,
                       ParserCallback parserCallback,
                       OutputStream... targets) throws IOException {
        log.info("Writing '" + format.getName() + "' position lists with 1 route and " + route.getPositionCount() + " positions");

        BaseRoute<?, ?> routeToWrite = asFormat(route, format);
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

    public void write(BaseRoute<?, ?> route, NavigationFormat<?> format, File target) throws IOException {
        write(route, format, false, false, null, target);
    }

    public void write(BaseRoute<?, ?> route, NavigationFormat<?> format,
                      boolean duplicateFirstPosition,
                      boolean ignoreMaximumPositionCount,
                      ParserCallback parserCallback,
                      File... targets) throws IOException {
        OutputStream[] targetStreams = new OutputStream[targets.length];
        for (int i = 0; i < targets.length; i++) {
            // PhotoFormat modifies target in place since it needs the image date,
            // so we don't create a FileOutputStream to avoid zeroing the file
            if (!(format instanceof PhotoFormat))
                targetStreams[i] = new FileOutputStream(targets[i]);
        }
        write(route, widen(format), duplicateFirstPosition, ignoreMaximumPositionCount, parserCallback, targetStreams);
        for (File target : targets)
            log.info("Wrote '" + target.getAbsolutePath() + "'");
    }


    private <P extends BaseNavigationPosition> void preprocessRoute(BaseRoute<P, ?> routeToWrite, NavigationFormat<BaseRoute<?, ?>> format,
                                 boolean duplicateFirstPosition,
                                 ParserCallback parserCallback) {
        NavigationFormat<?> anyFormat = format;
        if (anyFormat instanceof NmnFormat)
            routeToWrite.removeDuplicates();
        if (anyFormat instanceof NmnFormat nmnFormat && duplicateFirstPosition) {
            P position = nmnFormat.getDuplicateFirstPosition(routeToWrite);
            if (position != null)
                routeToWrite.add(0, position);
        }
        if (anyFormat instanceof CoPilotFormat coPilotFormat && duplicateFirstPosition) {
            P position = coPilotFormat.getDuplicateFirstPosition(routeToWrite);
            if (position != null)
                routeToWrite.add(0, position);
        }
        if (anyFormat instanceof TcxFormat)
            routeToWrite.ensureIncreasingTime();
        if (parserCallback != null)
            parserCallback.process(routeToWrite, format);
    }

    private void renameRoute(BaseRoute<?, ?> route, BaseRoute<?, ?> routeToWrite, int startIndex, int endIndex, int trackIndex, OutputStream... targets) {
        // gives splitted TomTomRoute and SimpleRoute routes a more useful name for the fragment
        if (route.getFormat() instanceof TomTomRouteFormat || route.getFormat() instanceof SimpleFormat ||
                route.getFormat() instanceof GpxFormat && routeToWrite.getFormat() instanceof BcrFormat) {
            String name = createRouteName(routeToWrite.getPositions().subList(startIndex, endIndex));
            if (targets.length > 1)
                name = "Track" + (trackIndex + 1) + ": " + name;
            routeToWrite.setName(name);
        }
    }

    private void postProcessRoute(BaseRoute<?, ?> routeToWrite, NavigationFormat<BaseRoute<?, ?>> format, boolean duplicateFirstPosition) {
        NavigationFormat<?> anyFormat = format;
        if ((anyFormat instanceof NmnFormat || anyFormat instanceof CoPilotFormat) && duplicateFirstPosition)
            routeToWrite.remove(0);
    }


    public void write(List<BaseRoute<?, ?>> routes, MultipleRoutesFormat<?> format, File target) throws IOException {
        log.info("Writing '" + format.getName() + "' with " + routes.size() + " routes and " +
                getPositionCounts(routes) + " positions");

        MultipleRoutesFormat<BaseRoute<?, ?>> anyFormat = widen(format);
        List<BaseRoute<?, ?>> routesToWrite = new ArrayList<>(routes.size());
        for (BaseRoute<?, ?> route : routes) {
            BaseRoute<?, ?> routeToWrite = asFormat(route, anyFormat);
            commentRoute(routeToWrite);
            preprocessRoute(routeToWrite, anyFormat, false, null);
            routesToWrite.add(routeToWrite);
            postProcessRoute(routeToWrite, anyFormat, false);
        }

        try (OutputStream outputStream = new FileOutputStream(target)) {
            anyFormat.write(routesToWrite, outputStream);
            log.info("Wrote '" + target.getAbsolutePath() + "'");
        }
    }
}
