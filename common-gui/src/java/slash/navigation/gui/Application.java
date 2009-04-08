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

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.beans.Beans;
import java.lang.reflect.Constructor;

/**
 * The base of all graphical user interfaces.
 *
 * @author Christian Pesch
 */

public abstract class Application {
    private static final Logger log = Logger.getLogger(SingleFrameApplication.class.getName());
    private static Application application = null;
    private final List<ExitListener> exitListeners;
    private final ApplicationContext context;
    private Preferences preferences = Preferences.userNodeForPackage(getClass());

    private static final String PREFERRED_LANGUAGE_PREFERENCE = "preferredLanguage";
    private static final String PREFERRED_COUNTRY_PREFERENCE = "preferredCountry";

    protected Application() {
        exitListeners = new CopyOnWriteArrayList<ExitListener>();
        context = new ApplicationContext();
    }

    public static synchronized Application getInstance() {
        return application;
    }

    public final ApplicationContext getContext() {
        return context;
    }

    public void setLocale(Locale locale) {
        if (!Constants.ROOT_LOCALE.equals(locale)) {
            preferences.put(PREFERRED_LANGUAGE_PREFERENCE, locale.getLanguage());
            preferences.put(PREFERRED_COUNTRY_PREFERENCE, locale.getCountry());
        } else {
            preferences.remove(PREFERRED_LANGUAGE_PREFERENCE);
            preferences.remove(PREFERRED_COUNTRY_PREFERENCE);
        }
    }

    protected static void setDefaultLocale(Preferences preferences) {
        String language = preferences.get(PREFERRED_LANGUAGE_PREFERENCE, Locale.getDefault().getLanguage());
        String country = preferences.get(PREFERRED_COUNTRY_PREFERENCE, Locale.getDefault().getCountry());
        Locale.setDefault(new Locale(language, country));
    }


    public static synchronized <T extends Application> void launch(final Class<T> applicationClass, final String[] args) {
        Constants.setLookAndFeel();
        setDefaultLocale(Preferences.userNodeForPackage(applicationClass));

        Runnable doCreateAndShowGUI = new Runnable() {
            public void run() {
                try {
                    application = create(applicationClass);
                    application.initialize(args);
                    application.startup();
                    application.waitForReady();
                }
                catch (Exception e) {
                    String msg = String.format("Application %s failed to launch", applicationClass);
                    log.log(Level.SEVERE, msg, e);
                    throw new Error(msg, e);
                }
            }
        };
        SwingUtilities.invokeLater(doCreateAndShowGUI);
    }

    static <T extends Application> T create(Class<T> applicationClass) throws Exception {
        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        T application = ctor.newInstance();

        ApplicationContext ctx = application.getContext();
        ctx.setApplicationClass(applicationClass);
        ctx.setApplication(application);

        /* TODO Load the application resource map, notably the Application.* properties. */
        ctx.setBundle(ResourceBundle.getBundle(applicationClass.getSuperclass().getName()));

        return application;
    }

    protected void initialize(String[] args) {
    }

    protected abstract void startup();

    private void waitForReady() {
    }


    public void exit(EventObject event) {
        for (ExitListener listener : exitListeners) {
            if (!listener.canExit(event)) {
                return;
            }
        }
        try {
            for (ExitListener listener : exitListeners) {
                try {
                    listener.willExit(event);
                }
                catch (Exception e) {
                    log.log(Level.WARNING, "ExitListener.willExit() failed", e);
                }
            }
            shutdown();
        }
        catch (Exception e) {
            log.log(Level.WARNING, "unexpected error in Application.shutdown()", e);
        }
        finally {
            end();
        }
    }

    public interface ExitListener extends EventListener {
        boolean canExit(EventObject event);

        void willExit(EventObject event);
    }

    public void addExitListener(ExitListener listener) {
        exitListeners.add(listener);
    }

    public void removeExitListener(ExitListener listener) {
        exitListeners.remove(listener);
    }

    protected void shutdown() {
    }

    protected void end() {
        Runtime.getRuntime().exit(0);
    }
}
