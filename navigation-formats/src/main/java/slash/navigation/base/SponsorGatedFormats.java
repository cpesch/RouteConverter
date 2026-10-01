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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.base;

import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.msfs.MSFSFlightPlanFormat;
import slash.navigation.rtz.RtzFormat;
import slash.navigation.simple.GoRiderGpsFormat;

import java.util.List;

/**
 * The write formats that are free to use only a few times in the desktop application and are
 * gated behind a sponsor donation afterwards. Non-interactive writers (command line, online
 * converter) cannot ask for a donation and must therefore never offer these formats.
 * <p>
 * Keep in sync with {@code ConvertPanel.SPONSOR_FEATURES}; {@code ConvertPanelSponsorFeatureTest}
 * fails if the two drift apart.
 *
 * @author Christian Pesch
 */

public final class SponsorGatedFormats {
    public static final List<Class<? extends NavigationFormat<?>>> CLASSES = List.of(
            GarminFlightPlanFormat.class,
            MSFSFlightPlanFormat.class,
            GoRiderGpsFormat.class,
            RtzFormat.class);

    private SponsorGatedFormats() {
    }

    public static boolean isSponsorGated(NavigationFormat<?> format) {
        // isInstance so a subclass of a sponsor format cannot slip past
        for (Class<? extends NavigationFormat<?>> formatClass : CLASSES)
            if (formatClass.isInstance(format))
                return true;
        return false;
    }
}
