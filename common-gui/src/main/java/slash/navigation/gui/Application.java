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

import slash.navigation.gui.helpers.UIHelper;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

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

    Application() {
        exitListeners = new CopyOnWriteArrayList<ExitListener>();
        context = new ApplicationContext();
    }

    public static synchronized Application getInstance() {
        return application;
    }

    private static synchronized void setInstance(Application theApplication) {
        application = theApplication;
    }

    public final ApplicationContext getContext() {
        return context;
    }

    public Locale getLocale() {
        String language = preferences.get(PREFERRED_LANGUAGE_PREFERENCE, "");
        String country = preferences.get(PREFERRED_COUNTRY_PREFERENCE, "");
        return new Locale(language, country);
    }

    public void setLocale(Locale locale) {
        if (!Locale.ROOT.equals(locale)) {
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

    private static void invokeNativeInterfaceMethod(String name) {
        try {
            Class<?> clazz = Class.forName("chrriis.dj.nativeswing.swtimpl.NativeInterface");
            Method method = clazz.getMethod(name);
            method.invoke(null);
        } catch (Exception e) {
            log.info("Cannot invoke NativeInterface#" + name + "(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void openNativeInterface() {
        invokeNativeInterfaceMethod("open");
    }

    private static void runNativeInterfaceEventPump() {
        invokeNativeInterfaceMethod("runEventPump");
    }

    public static <T extends Application> void launch(final Class<T> applicationClass, final String[] args) {
        UIHelper.setLookAndFeel();
        openNativeInterface();
        initializeLocale(Preferences.userNodeForPackage(applicationClass));

        Runnable doCreateAndShowGUI = new Runnable() {
            public void run() {
                try {
                    Application application = create(applicationClass);
                    setInstance(application);
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
        runNativeInterfaceEventPump();
    }

    private static ResourceBundle tryToLoadBundleFor(Class<?> clazz) {
        try {
            return ResourceBundle.getBundle(clazz.getName());
        } catch (Exception e) {
            log.log(Level.FINER, "Cannot load bundle for class " + clazz, e);
            return null;
        }
    }

    private static <T extends Application> T create(Class<T> applicationClass) throws Exception {
        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        T application = ctor.newInstance();

        ApplicationContext ctx = application.getContext();
        ResourceBundle bundle = tryToLoadBundleFor(applicationClass);
        if (bundle == null)
            bundle = tryToLoadBundleFor(applicationClass.getSuperclass());
        ctx.setBundle(bundle);
        String helpSetUrl = bundle.getString("help-set");
        if (helpSetUrl != null)
            ctx.setHelpBrokerUrl(helpSetUrl);
        ctx.setHelpBrokerClassLoader(applicationClass.getClassLoader());
        return application;
    }

    protected void initialize(String[] args) {
    }

    protected abstract void startup();

    protected void waitForReady() {
    }


    public/*for ExitAction*/ void exit(EventObject event) {
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

    protected void shutdown() {
    }

    void end() {
        Runtime.getRuntime().exit(0);
    }
}
