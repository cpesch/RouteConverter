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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Locale.CHINA;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.ITALY;
import static java.util.Locale.ROOT;
import static java.util.Locale.US;
import static org.junit.Assert.assertTrue;
import static slash.navigation.gui.helpers.UIHelper.ARABIA;
import static slash.navigation.gui.helpers.UIHelper.CROATIA;
import static slash.navigation.gui.helpers.UIHelper.CZECH;
import static slash.navigation.gui.helpers.UIHelper.NEDERLANDS;
import static slash.navigation.gui.helpers.UIHelper.SERBIA;
import static slash.navigation.gui.helpers.UIHelper.SLOVAKIA;
import static slash.navigation.gui.helpers.UIHelper.SPAIN;

public class ResourceBundleTest {
    private List<Locale> LOCALES = asList(ARABIA, CHINA, CROATIA, CZECH, FRANCE, GERMANY, ITALY, NEDERLANDS,
            SERBIA, SLOVAKIA, SPAIN, US);
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL = new ResourceBundle.Control() {
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return asList(new Locale(locale.getLanguage()));
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    @Test
    public void englishAgainstOtherBundles() {
        compareEnglishAgainstOtherBundles(true);
    }

    private void compareEnglishAgainstOtherBundles(boolean throwException) {
        ResourceBundle root = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", ROOT);
        Enumeration<String> keys = root.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            // skip keys which are only present in the default bundle
            if (key.startsWith("locale-") || key.endsWith("-icon") ||
                    key.endsWith("-mnemonic") || key.endsWith("-keystroke") || key.endsWith("-keystroke-mac") ||
                    key.equals("help-set") || key.equals("translator-missing") ||
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
                    System.out.println("key " + key + " does not exist in " + locale);
                    if (throwException)
                        assertTrue("key " + key + " does not exist in " + locale, false);
                }

                String rootValue = root.getString(key);
                if (rootValue.equals(value))
                    System.out.println("key " + key + " is not translated in " + locale);
            }
        }
    }

    @Test
    public void mnemonicsAreUnique() {
        for (Locale locale : LOCALES) {
            if (locale.equals(US))
                continue;
            checkMnemonicsAreUnique(locale);
        }
        checkMnemonicsAreUnique(ROOT);
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
