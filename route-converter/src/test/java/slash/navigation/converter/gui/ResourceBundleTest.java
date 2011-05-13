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
import slash.navigation.gui.Constants;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class ResourceBundleTest {
    private List<Locale> LOCALES = Arrays.asList(Constants.ARABIA, Locale.CHINA, Constants.CROATIA, Locale.FRANCE,
            Locale.GERMANY, Constants.NEDERLANDS, Constants.SERBIA, Constants.SLOVAKIA, Constants.SPAIN, Locale.US);
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL = new ResourceBundle.Control() {
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return Arrays.asList(new Locale(locale.getLanguage()));
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    @Test
    public void testEnglishAgainstOtherBundles() {
        compareEnglishAgainstOtherBundles(true);
    }

    private void compareEnglishAgainstOtherBundles(boolean throwException) {
        ResourceBundle root = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", Locale.ROOT);
        Enumeration<String> keys = root.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            // skip keys which are only present in the default bundle
            if (key.startsWith("locale-") ||
                    key.endsWith("-icon") || key.endsWith("-keystroke") || key.endsWith("-value") ||
                    key.equals("help-set"))
                continue;

            for (Locale locale : LOCALES) {
                if (locale.equals(Locale.US))
                    continue;

                ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", locale, NO_FALLBACK_CONTROL);
                try {
                    bundle.getString(key);
                } catch (MissingResourceException e) {
                    System.out.println("key " + key + " does not exist in " + locale);
                    if (throwException)
                        assertTrue("key " + key + " does not exist in " + locale, false);
                }
            }
        }
    }

    @Test
    public void testMnemonicsAreUnique() {
        for (Locale locale : LOCALES) {
            if (locale.equals(Locale.US))
                continue;
            checkMnemonicsAreUnique(locale);
        }
        checkMnemonicsAreUnique(Locale.ROOT);
    }

    private void checkMnemonicsAreUnique(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", locale, NO_FALLBACK_CONTROL);
        Enumeration<String> keys = bundle.getKeys();
        Map<String, Set<String>> mnemonics = new HashMap<String, Set<String>>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!key.endsWith("mnemonic"))
                continue;

            String mnemonic = bundle.getString(key);
            Set<String> existing = mnemonics.get(mnemonic);
            if (existing == null) {
                existing = new HashSet<String>();
                mnemonics.put(mnemonic, existing);
            }
            existing.add(key);
        }

        for (String mnemonic : mnemonics.keySet()) {
            Set<String> duplicates = mnemonics.get(mnemonic);
            if (duplicates.size() > 1)
                System.out.println("mnemonic " + mnemonic + " is used for " + duplicates + " in " + locale);
        }
    }
}
