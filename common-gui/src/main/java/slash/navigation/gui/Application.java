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

package slash.navigation.gui;

import slash.common.jarinjar.ClassPathExtender;
import slash.navigation.gui.helpers.CombinedResourceBundle;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static java.util.prefs.Preferences.userNodeForPackage;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ProxyHelper.setUseSystemProxies;
import static slash.navigation.gui.helpers.UIHelper.setLookAndFeel;

/**
 * The base of all graphical user interfaces.
 *
 * @author Christian Pesch
 */

public abstract class Application {
    private static final Logger log = getLogger(SingleFrameApplication.class.getName());
    private static Application application;
    private final List<ExitListener> exitListeners;
    private final ApplicationContext context;
    private final Preferences preferences = userNodeForPackage(getClass());

    private static final String PREFERRED_LANGUAGE_PREFERENCE = "preferredLanguage";
    private static final String PREFERRED_COUNTRY_PREFERENCE = "preferredCountry";

    Application() {
        exitListeners = new CopyOnWriteArrayList<>();
        context = new ApplicationContext();
    }

    public static synchronized Application getInstance() {
        return application;
    }

    private static synchronized void setInstance(Application theApplication) {
        application = theApplication;
    }

    public ApplicationContext getContext() {
        return context;
    }

    public Locale getLocale() {
        String language = preferences.get(PREFERRED_LANGUAGE_PREFERENCE, "");
        String country = preferences.get(PREFERRED_COUNTRY_PREFERENCE, "");
        return new Locale(language, country);
    }

    public void setLocale(Locale locale) {
        if (!ROOT.equals(locale)) {
            preferences.put(PREFERRED_LANGUAGE_PREFERENCE, locale.getLanguage());
            preferences.put(PREFERRED_COUNTRY_PREFERENCE, locale.getCountry());
        } else {
            preferences.remove(PREFERRED_LANGUAGE_PREFERENCE);
            preferences.remove(PREFERRED_COUNTRY_PREFERENCE);
        }
    }

    private static void initializeLocale(Preferences preferences) {
        String language = preferences.get(PREFERRED_LANGUAGE_PREFERENCE, Locale.getDefault().getLanguage());
        String country = preferences.get(PREFERRED_COUNTRY_PREFERENCE, Locale.getDefault().getCountry());
        Locale.setDefault(new Locale(language, country));
    }

    private static ResourceBundle initializeBundles(List<String> bundleNames) {
        CombinedResourceBundle bundle = new CombinedResourceBundle(bundleNames);
        bundle.load();
        return bundle;
    }

    private static ClassLoader extendClassPath() {
        ClassPathExtender extender = new ClassPathExtender();

        File javaFxJar = new File(System.getProperty("java.home"), "lib/jfxrt.jar");
        if (javaFxJar.exists()) {
            try {
                extender.addExternalFile(javaFxJar);
            } catch (Exception e) {
                log.info("Cannot extend classpath with JavaFX from " + javaFxJar + ": " + e);
            }
        }

        return extender.getClassLoader();
    }

    public static <T extends Application> void launch(final Class<T> applicationClass, final List<String> bundleNames, final String[] args) {
        final ClassLoader contextClassLoader = extendClassPath();
        if (contextClassLoader != null)
            Thread.currentThread().setContextClassLoader(contextClassLoader);

        Runnable doCreateAndShowGUI = () -> {
            try {
                if (contextClassLoader != null)
                    Thread.currentThread().setContextClassLoader(contextClassLoader);

                setLookAndFeel();
                setUseSystemProxies(); 
                initializeLocale(userNodeForPackage(applicationClass));
                ResourceBundle bundle = initializeBundles(bundleNames);

                Application application = create(applicationClass);
                setInstance(application);
                application.getContext().setBundle(bundle);
                application.startup();
                application.parseInitialArgs(args);
            } catch (Exception e) {
                String msg = format("Application %s failed to launch", applicationClass);
                log.log(SEVERE, msg, e);
                throw new Error(msg, e);
            }
        };
        invokeLater(doCreateAndShowGUI);
    }

    private static <T extends Application> T create(Class<T> applicationClass) throws Exception {
        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        return ctor.newInstance();
    }

    protected abstract void startup();

    protected void parseInitialArgs(String[] args) {
    }

    public/*for ExitAction*/ void exit(EventObject event) {
        for (ExitListener listener : exitListeners) {
            if (!listener.canExit(event)) {
                return;
            }
        }
        try {
            shutdown();
        } catch (Exception e) {
            log.log(WARNING, "Unexpected error in Application.shutdown()", e);
        } finally {
            end();
        }
    }

    public interface ExitListener extends EventListener {
        boolean canExit(EventObject event);
    }

    public void addExitListener(ExitListener listener) {
        exitListeners.add(listener);
    }

    protected void shutdown() {
        getContext().getNotificationManager().dispose();
    }

    void end() {
        Runtime.getRuntime().exit(0);
    }
}
