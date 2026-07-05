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

import slash.navigation.gui.helpers.CombinedResourceBundle;

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

    private static volatile CrashHandler crashHandler;
    private static final ThreadLocal<Boolean> handlingCrash = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Registers the application-specific handler that receives every uncaught
     * exception routed to the default handler installed in {@link #launch}. Kept
     * pluggable so this application-generic module carries no report/dialog logic.
     */
    public static void setCrashHandler(CrashHandler handler) {
        crashHandler = handler;
    }

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

    public static <T extends Application> void launch(final Class<T> applicationClass, final List<String> bundleNames, final String[] args) {
        // Last-resort net: since Java 7 the EDT routes uncaught exceptions to the
        // thread's handler, which falls back to this default. Without it, a throw
        // in an invokeLater/background runnable (e.g. the async map/profile view
        // setup) died on System.err and never reached the log file or an error
        // report — a blank/half-built UI with nothing to diagnose. Now every
        // uncaught exception, on any thread, lands in the RC log.
        //
        // This also covers exceptions thrown on the EDT while a modal dialog's
        // secondary event pump is active: on Java 9+ (verified on 17/21)
        // EventDispatchThread.processException routes to
        // getUncaughtExceptionHandler().uncaughtException(...), and the nested modal
        // pump uses the same pumpOneEventForFilters -> processException path, so it
        // falls back to this default handler too. No custom EventQueue is needed.
        // (See EventDispatchThreadExceptionTest for the plain-EDT verification.)
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.log(SEVERE, format("Uncaught exception in thread %s", thread.getName()), throwable);

            // hand off to the pluggable application handler, guarded against a crash
            // loop: a throw from within the handler must not re-enter it on the same
            // thread (the concrete handler additionally shows at most one dialog per
            // session and only spools further crashes)
            CrashHandler handler = crashHandler;
            if (handler != null && !handlingCrash.get()) {
                handlingCrash.set(Boolean.TRUE);
                try {
                    handler.handleCrash(thread, throwable);
                } catch (Throwable t) {
                    log.log(SEVERE, "Crash handler failed", t);
                } finally {
                    handlingCrash.set(Boolean.FALSE);
                }
            }
        });

        Runnable doCreateAndShowGUI = () -> {
            try {
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
