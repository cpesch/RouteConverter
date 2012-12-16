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

package slash.navigation.converter.gui.mapview;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserCommandEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserListener;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserNavigationEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowOpeningEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowWillOpenEvent;
import slash.common.io.TokenResolver;
import slash.navigation.base.NavigationPosition;

import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.logging.Logger;

import static chrriis.dj.nativeswing.swtimpl.NativeInterface.isOpen;
import static chrriis.dj.nativeswing.swtimpl.components.JWebBrowser.useWebkitRuntime;
import static chrriis.dj.nativeswing.swtimpl.components.JWebBrowser.useXULRunnerRuntime;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isEventDispatchThread;
import static slash.common.io.Externalization.extractFile;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.system.Platform.isLinux;
import static slash.common.system.Platform.isMac;
import static slash.common.system.Platform.isWindows;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the Eclipse SWT Webbrowser embedding in the DJNative Swing JWebBrowser.
 *
 * @author Christian Pesch
 */

public class EclipseSWTMapView extends BaseMapView {
    private static final Logger log = Logger.getLogger(EclipseSWTMapView.class.getName());
    private static final String GOOGLE_MAPS_SERVER_PREFERENCE = "mapServer";
    private static final String DEBUG_PREFERENCE = "debug";

    private JWebBrowser webBrowser;
    private boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);

    public boolean isSupportedPlatform() {
        return isLinux() || isMac() || isWindows();
    }

    public Component getComponent() {
        return webBrowser;
    }

    // initialization

    private JWebBrowser createWebBrowser() {
        try {
            if (!isOpen())
                throw new Exception("Native Interface is not initialized");
            JWebBrowser browser;
            if (isLinux()) {
                try {
                    System.setProperty("org.eclipse.swt.browser.UseWebKitGTK", "true");
                    // System.setProperty("nativeswing.webbrowser.runtime", "webkit");
                    browser = new JWebBrowser(useWebkitRuntime());
                    log.info("Using WebKit runtime to create WebBrowser");
                } catch (IllegalStateException e) {
                    System.clearProperty("org.eclipse.swt.browser.UseWebKitGTK");
                    browser = new JWebBrowser(useXULRunnerRuntime());
                    log.info("Using XULRunner runtime to create WebBrowser: " + e.getMessage());
                }
            } else {
                browser = new JWebBrowser();
            }
            browser.setBarsVisible(false);
            browser.setJavascriptEnabled(true);
            return browser;
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            setInitializationCause(t);
            return null;
        }
    }

    private boolean loadWebPage(final JWebBrowser webBrowser) {
        try {
            final String language = Locale.getDefault().getLanguage().toLowerCase();
            final String country = Locale.getDefault().getCountry().toLowerCase();
            File html = extractFile("slash/navigation/converter/gui/mapview/routeconverter.html", country, new TokenResolver() {
                public String resolveToken(String tokenName) {
                    if (tokenName.equals("language"))
                        return language;
                    if (tokenName.equals("country"))
                        return country;
                    if (tokenName.equals("mapserver"))
                        return preferences.get(GOOGLE_MAPS_SERVER_PREFERENCE, "maps.google.com");
                    if (tokenName.equals("maptype"))
                        return preferences.get(MAP_TYPE_PREFERENCE, "roadmap");
                    return tokenName;
                }
            });
            if (html == null)
                throw new IllegalArgumentException("Cannot extract routeconverter.html");
            extractFile("slash/navigation/converter/gui/mapview/contextmenu.js");
            extractFile("slash/navigation/converter/gui/mapview/keydragzoom.js");
            extractFile("slash/navigation/converter/gui/mapview/label.js");
            extractFile("slash/navigation/converter/gui/mapview/latlngcontrol.js");

            final String url = html.toURI().toURL().toExternalForm();
            webBrowser.runInSequence(new Runnable() {
                public void run() {
                    webBrowser.navigate(url);
                }
            });
            log.fine(currentTimeMillis() + " loadWebPage thread " + Thread.currentThread());
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            setInitializationCause(t);
            return false;
        }
        return true;
    }

    protected void initializeBrowser() {
        webBrowser = createWebBrowser();
        if (webBrowser == null)
            return;

        webBrowser.addWebBrowserListener(new WebBrowserListener() {
            public void windowWillOpen(WebBrowserWindowWillOpenEvent e) {
                log.fine("WebBrowser windowWillOpen " + e.isConsumed() + " thread " + Thread.currentThread());
            }

            public void windowOpening(WebBrowserWindowOpeningEvent e) {
                log.fine("WebBrowser windowOpening " + e.getLocation() + "/" + e.getSize() + " thread " + Thread.currentThread());
            }

            public void windowClosing(WebBrowserEvent e) {
                log.fine("WebBrowser windowClosing " + e + " thread " + Thread.currentThread());
            }

            public void locationChanging(WebBrowserNavigationEvent e) {
                log.fine("WebBrowser locationChanging " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChanged(WebBrowserNavigationEvent e) {
                log.fine("WebBrowser locationChanged " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChangeCanceled(WebBrowserNavigationEvent e) {
                log.fine("WebBrowser locationChangeCanceled " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            private int startCount = 0;

            public void loadingProgressChanged(WebBrowserEvent e) {
                log.fine("WebBrowser loadingProgressChanged " + e.getWebBrowser().getLoadingProgress() + " thread " + Thread.currentThread());

                if (e.getWebBrowser().getLoadingProgress() == 100 && startCount == 0) {
                    // get out of the listener callback
                    new Thread(new Runnable() {
                        public void run() {
                            tryToInitialize(startCount++, currentTimeMillis());
                        }
                    }, "MapViewInitializer").start();
                }
            }

            public void titleChanged(WebBrowserEvent e) {
                log.fine("WebBrowser titleChanged " + e.getWebBrowser().getPageTitle() + " thread " + Thread.currentThread());
            }

            public void statusChanged(WebBrowserEvent e) {
                log.fine("WebBrowser statusChanged " + e.getWebBrowser().getStatusText() + " thread " + Thread.currentThread());
            }

            public void commandReceived(WebBrowserCommandEvent e) {
                // log.fine("WebBrowser commandReceived " + e.getCommand() + " thread " + Thread.currentThread());
            }
        });

        if (!loadWebPage(webBrowser))
            dispose();
    }

    private void tryToInitialize(int count, long start) {
        boolean initialized = getComponent() != null && isMapInitialized();
        synchronized (this) {
            this.initialized = initialized;
        }
        log.fine("Initialized map: " + initialized);

        if (isInitialized()) {
            runBrowserInteractionCallbacksAndTests(start);
        } else {
            long end = currentTimeMillis();
            int timeout = count++ * 100;
            if (timeout > 3000)
                timeout = 3000;
            log.info("Failed to initialize map since " + (end - start) + " ms, sleeping for " + timeout + " ms");

            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            tryToInitialize(count, start);
        }
    }

    private void runBrowserInteractionCallbacksAndTests(long start) {
        long end = currentTimeMillis();
        log.fine("Starting browser interaction, callbacks and tests after " + (end - start) + " ms");
        initializeAfterLoading();
        initializeBrowserInteraction();
        initializeCallbackListener();
        checkLocalhostResolution();
        checkCallback();
        end = currentTimeMillis();
        log.fine("Browser interaction is running after " + (end - start) + " ms");
    }

    private boolean isMapInitialized() {
        String result = executeScriptWithResult("return isInitialized();");
        return parseBoolean(result);
    }

    private void initializeAfterLoading() {
        resize();
        update(true);
    }

    // resizing

    private boolean hasBeenResizedToInvisible = false;

    public void resize() {
        new Thread(new Runnable() {
            public void run() {
                if (!isInitialized() || !getComponent().isShowing())
                    return;

                synchronized (notificationMutex) {
                    // if map is not visible remember to update and resize it again
                    // once the map becomes visible again
                    if (!isVisible()) {
                        hasBeenResizedToInvisible = true;
                    } else if (hasBeenResizedToInvisible) {
                        hasBeenResizedToInvisible = false;
                        update(true);
                    }
                    resizeMap();
                }
            }
        }, "BrowserResizer").start();
    }

    private int lastWidth = -1, lastHeight = -1;

    private void resizeMap() {
        synchronized (notificationMutex) {
            int width = max(getComponent().getWidth(), 0);
            int height = max(getComponent().getHeight(), 0);
            if (width != lastWidth || height != lastHeight) {
                executeScript("resize(" + width + "," + height + ");");
            }
            lastWidth = width;
            lastHeight = height;
        }
    }

    // bounds and center

    protected NavigationPosition getNorthEastBounds() {
        return extractLatLng("return getNorthEastBounds();");
    }

    protected NavigationPosition getSouthWestBounds() {
        return extractLatLng("return getSouthWestBounds();");
    }

    protected NavigationPosition getCurrentMapCenter() {
        return extractLatLng("return getCenter();");
    }

    protected Integer getCurrentZoom() {
        Double zoom = parseDouble(executeScriptWithResult("return getZoom();"));
        return zoom != null ? zoom.intValue() : null;
    }

    protected String getCallbacks() {
        return executeScriptWithResult("return getCallbacks();");
    }

    // script execution

    protected void executeScript(final String script) {
        if (webBrowser == null || script.length() == 0)
            return;

        if (!isEventDispatchThread()) {
            invokeLater(new Runnable() {
                public void run() {
                    webBrowser.runInSequence(new Runnable() {
                        public void run() {
                            webBrowser.executeJavascript(script);
                        }
                    });
                    logJavaScript(script, null);
                }
            });
        } else {
            webBrowser.runInSequence(new Runnable() {
                public void run() {
                    webBrowser.executeJavascript(script);
                }
            });
            logJavaScript(script, null);
        }
    }

    protected String executeScriptWithResult(final String script) {
        if (script.length() == 0)
            return null;

        final boolean pollingCallback = !script.contains("getCallbacks");
        final Object[] result = new Object[1];
        if (!isEventDispatchThread()) {
            try {
                invokeAndWait(new Runnable() {
                    public void run() {
                        webBrowser.runInSequence(new Runnable() {
                            public void run() {
                                result[0] = webBrowser.executeJavascriptWithResult(script);
                                if (debug && pollingCallback) {
                                    log.info("After invokeLater, executeJavascriptWithResult " + result[0]);
                                }
                            }
                        });
                    }
                });
            } catch (InterruptedException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            } catch (InvocationTargetException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            }
        } else {
            webBrowser.runInSequence(new Runnable() {
                public void run() {
                    result[0] = webBrowser.executeJavascriptWithResult(script);
                    if (debug && pollingCallback) {
                        log.info("After executeJavascriptWithResult " + result[0]);
                    }
                }
            });
        }

        if (pollingCallback) {
            logJavaScript(script, result[0]);
        }
        return result[0] != null ? result[0].toString() : null;
    }
}
