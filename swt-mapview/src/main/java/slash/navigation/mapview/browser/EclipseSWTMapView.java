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

package slash.navigation.mapview.browser;

import chrriis.dj.nativeswing.swtimpl.components.*;
import slash.navigation.common.NavigationPosition;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import static chrriis.dj.nativeswing.swtimpl.NativeInterface.isOpen;
import static chrriis.dj.nativeswing.swtimpl.components.JWebBrowser.useWebkitRuntime;
import static chrriis.dj.nativeswing.swtimpl.components.JWebBrowser.useXULRunnerRuntime;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.currentTimeMillis;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.isEventDispatchThread;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.system.Platform.isLinux;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the Eclipse SWT Webbrowser embedding in the DJNative Swing JWebBrowser.
 *
 * @author Christian Pesch
 */

public class EclipseSWTMapView extends BrowserMapView {
    private static final Logger log = Logger.getLogger(EclipseSWTMapView.class.getName());

    private JWebBrowser webBrowser;

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
                    browser = new JWebBrowser(useWebkitRuntime());
                    log.info("Using WebKit runtime to create WebBrowser");
                } catch (IllegalStateException e) {
                    System.clearProperty("org.eclipse.swt.browser.UseWebKitGTK");
                    browser = new JWebBrowser(useXULRunnerRuntime());
                    log.info("Using XULRunner runtime to create WebBrowser: " + e);
                }
            } else {
                browser = new JWebBrowser();
            }
            browser.setBarsVisible(false);
            browser.setJavascriptEnabled(true);

            browser.addWebBrowserListener(new WebBrowserListener() {
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

                private int startCount;

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

            return browser;
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t);
            setInitializationCause(t);
            return null;
        }
    }

    private boolean loadWebPage() {
        try {
            final String url = prepareWebPage();
            webBrowser.runInSequence(new Runnable() {
                public void run() {
                    webBrowser.navigate(url);
                }
            });
        } catch (Throwable t) {
            log.severe("Cannot load web page: " + t);
            setInitializationCause(t);
            return false;
        }
        return true;
    }

    protected void initializeBrowser() {
        webBrowser = createWebBrowser();
        if (webBrowser == null)
            return;

        log.info("Using Eclipse SWT Browser to create map view");
        initializeWebPage();
    }

    protected void initializeWebPage() {
        log.info("Loading Google Maps API from " + getGoogleMapsServerApiUrl());
        if (!loadWebPage())
            dispose();
    }

    protected boolean isMapInitialized() {
        String result = executeScriptWithResult("return isInitialized();");
        return parseBoolean(result);
    }

    // bounds and center

    protected NavigationPosition getNorthEastBounds() {
        return parsePosition("return getNorthEastBounds();");
    }

    protected NavigationPosition getSouthWestBounds() {
        return parsePosition("return getSouthWestBounds();");
    }

    protected NavigationPosition getCurrentMapCenter() {
        return parsePosition("return getCenter();");
    }

    protected Integer getCurrentZoom() {
        Double zoom = parseDouble(executeScriptWithResult("return getZoom();"));
        return zoom != null ? zoom.intValue() : null;
    }

    protected String getCallbacks() {
        return executeScriptWithResult("return getCallbacks();");
    }

    public boolean isSupportsPrinting() {
        return true;
    }

    public boolean isSupportsPrintingWithDirections() {
        return true;
    }

    public void print(String title, boolean withDirections) {
        executeScript("printMap(\"" + title + "\", " + withDirections + ");");
    }

    // script execution

    protected void executeScript(final String script) {
        if (webBrowser == null || script.length() == 0)
            return;

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                webBrowser.runInSequence(new Runnable() {
                    public void run() {
                        webBrowser.executeJavascript(script);
                    }
                });
                logJavaScript(script, null);
            }
        });
    }

    protected String executeScriptWithResult(final String script) {
        if (script.length() == 0)
            return null;

        final boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);
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
            } catch (InterruptedException | InvocationTargetException e) {
                log.severe("Cannot execute script with result: " + e);
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
