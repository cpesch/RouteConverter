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

import java.util.List;
import java.util.Locale;

import static java.util.Locale.*;
import static slash.common.helpers.LocaleHelper.*;

/**
 * The single source of truth for the locales RouteConverter offers in the user interface.
 *
 * A locale is supported once its translation has a useful coverage; near-empty translations
 * (below roughly 5 percent) are not offered in the chooser although their bundle files stay
 * in the repository so the translation platform can revive them.
 *
 * @author Christian Pesch
 */

public class RouteConverterLocales {
    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            ARABIA, BRAZIL, CATALAN, CHINA, CROATIA, CZECH, DENMARK, FRANCE, GERMANY, HUNGARY,
            ITALY, JAPAN, KOREA, NEDERLANDS, NORWAY_BOKMAL, POLAND, PORTUGAL, RUSSIA, SERBIA,
            SLOVAKIA, SPAIN, TAMIL, UKRAINE, US
    );

    /**
     * The locales offered in the options dialog: the supported locales plus {@link Locale#ROOT}
     * for "use the system default".
     */
    public static Locale[] toChooserLocales() {
        Locale[] result = new Locale[SUPPORTED_LOCALES.size() + 1];
        SUPPORTED_LOCALES.toArray(result);
        result[result.length - 1] = ROOT;
        return result;
    }
}
