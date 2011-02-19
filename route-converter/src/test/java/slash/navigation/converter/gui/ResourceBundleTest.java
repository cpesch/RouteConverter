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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.Assert.assertTrue;

public class ResourceBundleTest {
    private List<Locale> LOCALES = Arrays.asList(Constants.ARABIA, Locale.CHINA, Constants.CROATIA, Locale.FRANCE,
            Locale.GERMANY, Constants.NEDERLANDS, Constants.SERBIA, Constants.SPAIN, Locale.US);

    @Test
    public void testEnglishAgainstOtherBundles() {
        compareEnglishAgainstOtherBundles(true);
    }

    private void compareEnglishAgainstOtherBundles(boolean throwException) {
        ResourceBundle.Control noFallbackControl = new ResourceBundle.Control() {
            public List<Locale> getCandidateLocales(String baseName, Locale locale) {
                return Arrays.asList(new Locale(locale.getLanguage()));
            }

            public Locale getFallbackLocale(String baseName, Locale locale) {
                return null;
            }
        };
        ResourceBundle root = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", Locale.ROOT);
        Enumeration<String> keys = root.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            // skip keys which are only present in the default bundle
            if (key.startsWith("locale-") ||
                    key.endsWith("-icon") || key.endsWith("-keystroke") || key.endsWith("-value") ||
                    key.equals("help-set")
                    )
                continue;

            for (Locale locale : LOCALES) {
                if (locale.equals(Locale.US))
                    continue;

                ResourceBundle bundle = ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter", locale, noFallbackControl);
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
}
