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

import slash.navigation.gui.jarinjar.ClassPathExtender;
import slash.navigation.jnlp.SingleInstance;
import slash.navigation.jnlp.SingleInstanceCallback;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import static slash.common.system.Platform.getBits;
import static slash.common.system.Platform.getOperationSystem;
import static slash.navigation.gui.helpers.UIHelper.setLookAndFeel;
import static slash.navigation.gui.helpers.UIHelper.setUseSystemProxies;

/**
 * The base of all graphical user interfaces.
 *
 * @author Christian Pesch
 */

public abstract class Application {
    private static final Logger log = getLogger(SingleFrameApplication.class.getName());
    private static Application application = null;
    private final List<ExitListener> exitListeners;
    private final ApplicationContext context;
    private Preferences preferences = userNodeForPackage(getClass());

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

    public final ApplicationContext getContext() {
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

    private static ResourceBundle initializeBundles(String[] bundleNames) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ResourceBundle lastBundle = null;
        for (String bundleName : bundleNames) {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
            if (lastBundle != null)
                setParentBundle(bundle, lastBundle);
            lastBundle = bundle;
        }

        return lastBundle;
    }

    private static void setParentBundle(ResourceBundle bundle, ResourceBundle parentBundle) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field field = ResourceBundle.class.getDeclaredField("parent");
        field.setAccessible(true);
        ResourceBundle bundlesParentOrNull = ResourceBundle.class.cast(field.get(bundle));

        Method method = ResourceBundle.class.getDeclaredMethod("setParent", ResourceBundle.class);
        method.setAccessible(true);
        method.invoke(bundlesParentOrNull != null ? bundlesParentOrNull : bundle, parentBundle);
    }

    private static ClassLoader extendClassPath() {
        ClassPathExtender extender = new ClassPathExtender();

        String swtJar = "swt-" + getOperationSystem() + "-" + getBits() + ".jar";
        try {
            extender.addJarInJar(swtJar);
        } catch (Exception e) {
            log.info("Cannot extend classpath with SWT from " + swtJar + ": " + e);
        }

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

    private static void invokeNativeInterfaceMethod(String name) {
        try {
            Class<?> clazz = Class.forName("chrriis.dj.nativeswing.swtimpl.NativeInterface");
            Method method = clazz.getMethod(name);
            method.invoke(null);
        } catch (Exception e) {
            log.info("Cannot invoke NativeInterface#" + name + "(): " + e);
        }
    }

    private static void openNativeInterface() {
        invokeNativeInterfaceMethod("open");
    }

    private static void runNativeInterfaceEventPump() {
        invokeNativeInterfaceMethod("runEventPump");
    }

    private SingleInstance singleInstance;

    private void initializeSingleInstance() {
        try {
            singleInstance = new SingleInstance(new SingleInstanceCallback() {
                public void newActivation(String[] args) {
                    Application.this.parseNewActivationArgs(args);
                }
            });
        } catch (Throwable t) {
            // intentionally left empty
        }
    }

    public static <T extends Application> void launch(final Class<T> applicationClass, final String[] bundleNames, final String[] args) {
        final ClassLoader contextClassLoader = extendClassPath();
        if (contextClassLoader != null)
            Thread.currentThread().setContextClassLoader(contextClassLoader);

        Runnable doCreateAndShowGUI = new Runnable() {
            public void run() {
                try {
                    if (contextClassLoader != null)
                        Thread.currentThread().setContextClassLoader(contextClassLoader);

                    setLookAndFeel();
                    setUseSystemProxies();
                    openNativeInterface();
                    initializeLocale(userNodeForPackage(applicationClass));
                    ResourceBundle bundle = initializeBundles(bundleNames);

                    Application application = create(applicationClass);
                    setInstance(application);
                    application.getContext().setBundle(bundle);
                    application.initializeSingleInstance();
                    application.startup();
                    application.parseInitialArgs(args);
                } catch (Exception e) {
                    String msg = format("Application %s failed to launch", applicationClass);
                    log.log(SEVERE, msg, e);
                    throw new Error(msg, e);
                }
            }
        };
        invokeLater(doCreateAndShowGUI);
        runNativeInterfaceEventPump();
    }

    private static <T extends Application> T create(Class<T> applicationClass) throws Exception {
        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        return ctor.newInstance();
    }

    protected abstract void startup();

    protected void parseInitialArgs(String[] args) {
    }

    protected void parseNewActivationArgs(String[] args) {
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
                } catch (Exception e) {
                    log.log(WARNING, "ExitListener.willExit() failed", e);
                }
            }
            shutdown();
        } catch (Exception e) {
            log.log(WARNING, "Unexpected error in Application.shutdown()", e);
        } finally {
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
        getContext().getNotificationManager().dispose();
    }

    void end() {
        if (singleInstance != null)
            singleInstance.dispose();
        Runtime.getRuntime().exit(0);
    }
}
