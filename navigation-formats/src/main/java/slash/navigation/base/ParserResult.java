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

import java.util.List;

/**
 * The result of the parsing of the {@link NavigationFormatParser}.
 *
 * @author Christian Pesch
 */

public class ParserResult {
    private final FormatAndRoutes<?, ?, ?> formatAndRoutes;
    private final UnreadableFileException failure;

    public ParserResult(FormatAndRoutes<?, ?, ?> formatAndRoutes) {
        this(formatAndRoutes, null);
    }

    private ParserResult(FormatAndRoutes<?, ?, ?> formatAndRoutes, UnreadableFileException failure) {
        this.formatAndRoutes = formatAndRoutes;
        this.failure = failure;
    }

    /**
     * @param failure why a format that recognized the source could not read it
     * @return an unsuccessful result that carries the reason
     */
    public static ParserResult failed(UnreadableFileException failure) {
        return new ParserResult(null, failure);
    }

    public boolean isSuccessful() {
        return formatAndRoutes != null;
    }

    /**
     * @return for an unsuccessful result: why a format that recognized the source could not
     * read it, or {@code null} if no format recognized the source
     */
    public UnreadableFileException getFailure() {
        return failure;
    }

    // Callers (NavigationFormatParser.write, getNumberOfFilesToWriteFor) need this format
    // correlated with getTheRoute()'s BaseRoute<?, ?> ceiling, not an independently-captured
    // wildcard - both accessors read the same formatAndRoutes field, so the widening is sound.
    @SuppressWarnings("unchecked")
    public NavigationFormat<BaseRoute<?, ?>> getFormat() {
        return (NavigationFormat<BaseRoute<?, ?>>) formatAndRoutes.getFormat();
    }

    public BaseRoute<?, ?> getTheRoute() {
        return formatAndRoutes.getRoute();
    }

    // formatAndRoutes' P/F are captured independently per access; a fresh capture of the same
    // wildcarded field is not provably the P/F already captured by getRoutes()'s declared
    // List<BaseRoute<P,F>> - both instantiate the same field, so the widening is sound.
    @SuppressWarnings("unchecked")
    public List<BaseRoute<?, ?>> getAllRoutes() {
        return (List<BaseRoute<?, ?>>) (List<?>) formatAndRoutes.getRoutes();
    }

}
