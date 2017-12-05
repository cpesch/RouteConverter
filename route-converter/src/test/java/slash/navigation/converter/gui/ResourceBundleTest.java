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

package slash.navigation.converter.gui;

import org.junit.Test;

import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.*;
import static org.junit.Assert.assertTrue;
import static slash.common.helpers.LocaleHelper.*;

public class ResourceBundleTest {
    private static final Logger log = Logger.getLogger(ResourceBundleTest.class.getName());
    private List<Locale> LOCALES = asList(ARABIA, CHINA, CROATIA, CZECH, DENMARK, FRANCE, GERMANY, ITALY, JAPAN,
            NEDERLANDS, NORWAY_BOKMAL, POLAND, PORTUGAL, RUSSIA, SERBIA, SLOVAKIA, SPAIN, UKRAINE, US);
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL = new ResourceBundle.Control() {
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return singletonList(new Locale(locale.getLanguage()));
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    @Test
    public void englishAgainstOtherBundles() {
        compareEnglishAgainstOtherBundles(false);
    }

    @SuppressWarnings("SameParameterValue")
    private void compareEnglishAgainstOtherBundles(boolean throwException) {
        ResourceBundle root = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", ROOT);
        ResourceBundle english = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", US);

        Enumeration<String> keys = english.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            // skip keys which are only present in the default bundle
            if (key.startsWith("locale-") || key.startsWith("map-view-") || key.endsWith("-icon") ||
                    key.endsWith("-mnemonic") || key.endsWith("-keystroke") || key.endsWith("-keystroke-mac") ||
                    key.equals("help-set") || key.equals("translator-missing") || key.startsWith("waypoint-type-") ||
                    key.equals("FileChooser.acceptAllFileFilterText"))
                continue;

            for (Locale locale : LOCALES) {
                if (locale.equals(US))
                    continue;

                ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", locale, NO_FALLBACK_CONTROL);
                String value = null;
                try {
                    value = bundle.getString(key);
                } catch (MissingResourceException e) {
                    log.fine("key " + key + " does not exist in locale " + locale);
                    if (throwException)
                        assertTrue("key " + key + " does not exist in " + locale, false);
                }

                try {
                    value = root.getString(key);
                } catch (MissingResourceException e) {
                    log.fine("key " + key + " does not exist in root " + root);
                    if (throwException)
                        assertTrue("key " + key + " does not exist in root " + root, false);
                }

                String englishValue = english.getString(key);
                if (englishValue.equals(value))
                    log.fine("key " + key + " is not translated in " + locale);
            }
        }
    }

    @Test
    public void mnemonicsAreUnique() {
        for (Locale locale : LOCALES)
            checkMnemonicsAreUnique(locale);
    }

    private void checkMnemonicsAreUnique(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", locale);
        Enumeration<String> keys = bundle.getKeys();
        Map<String, Set<String>> mnemonics = new HashMap<>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!key.endsWith("mnemonic"))
                continue;

            String mnemonic = bundle.getString(key);
            Set<String> existing = mnemonics.get(mnemonic);
            if (existing == null) {
                existing = new HashSet<>();
                mnemonics.put(mnemonic, existing);
            }
            existing.add(key);
        }

        for (String mnemonic : mnemonics.keySet()) {
            Set<String> duplicates = mnemonics.get(mnemonic);
            if (duplicates.size() > 1)
                log.fine("mnemonic " + mnemonic + " is used for " + duplicates + " in " + locale);
        }
    }
}
