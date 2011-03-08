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

import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserCommandEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserListener;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserNavigationEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowOpeningEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowWillOpenEvent;
import slash.common.io.Externalization;
import slash.common.io.Platform;
import slash.common.io.TokenResolver;
import slash.common.io.Transfer;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.util.Positions;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Displays the positions of a route.
 *
 * @author Christian Pesch
 */

public class EclipseSWTMapView extends BaseMapView {
    private static final Logger log = Logger.getLogger(EclipseSWTMapView.class.getName());
    private static final String MAP_SERVER_PREFERENCE = "mapServer";
    private static final String DEBUG_PREFERENCE = "debug";

    private JWebBrowser webBrowser;
    private boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, false);

    public boolean isSupportedPlatform() {
        return Platform.isLinux() || Platform.isMac() || Platform.isWindows();
    }

    public Component getComponent() {
        return webBrowser;
    }

    // initialization

    private JWebBrowser createWebBrowser() {
        try {
            if (!NativeInterface.isOpen())
                throw new Exception("Native Interface is not initialized");
            JWebBrowser browser;
            if (Platform.isLinux()) {
                try {
                    browser = new JWebBrowser(JWebBrowser.useWebkitRuntime());
                    log.info("Using WebKit runtime to create WebBrowser");
                }
                catch (IllegalStateException e) {
                    browser = new JWebBrowser(JWebBrowser.useXULRunnerRuntime());
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
            final String language = Locale.getDefault().getLanguage();
            File html = Externalization.extractFile("slash/navigation/converter/gui/mapview/routeconverter.html", language, new TokenResolver() {
                public String resolveToken(String tokenName) {
                    if (tokenName.equals("locale"))
                        return language;
                    if (tokenName.equals("percent"))
                        return Platform.isWindows() ? "99" : "100";
                    if (tokenName.equals("mapserver"))
                        return preferences.get(MAP_SERVER_PREFERENCE, "maps.google.com");
                    return tokenName;
                }
            });
            if (html == null)
                throw new IllegalArgumentException("Cannot extract routeconverter.html");
            Externalization.extractFile("slash/navigation/converter/gui/mapview/contextmenucontrol.js");

            final String url = html.toURI().toURL().toExternalForm();
            webBrowser.runInSequence(new Runnable() {
                public void run() {
                    webBrowser.navigate(url);
                }
            });
            log.fine(System.currentTimeMillis() + " loadWebPage thread " + Thread.currentThread());
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
                log.fine(System.currentTimeMillis() + " windowWillOpen " + e.isConsumed() + " thread " + Thread.currentThread());
            }

            public void windowOpening(WebBrowserWindowOpeningEvent e) {
                log.fine(System.currentTimeMillis() + " windowOpening " + e.getLocation() + "/" + e.getSize() + " thread " + Thread.currentThread());
            }

            public void windowClosing(WebBrowserEvent e) {
                log.fine(System.currentTimeMillis() + " windowClosing " + e + " thread " + Thread.currentThread());
            }

            public void locationChanging(WebBrowserNavigationEvent e) {
                log.fine(System.currentTimeMillis() + " locationChanging " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChanged(WebBrowserNavigationEvent e) {
                log.fine(System.currentTimeMillis() + " locationChanged " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChangeCanceled(WebBrowserNavigationEvent e) {
                log.fine(System.currentTimeMillis() + " locationChangeCanceled " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            private int startCount = 0;

            public void loadingProgressChanged(WebBrowserEvent e) {
                log.fine(System.currentTimeMillis() + " loadingProgressChanged " + e.getWebBrowser().getLoadingProgress() + " thread " + Thread.currentThread());

                if (e.getWebBrowser().getLoadingProgress() == 100 && startCount == 0) {
                    // get out of the listener callback
                    new Thread(new Runnable() {
                        public void run() {
                            tryToInitialize(startCount++);
                        }
                    }, "MapViewInitializer").start();
                }
            }

            public void titleChanged(WebBrowserEvent e) {
                log.fine(System.currentTimeMillis() + " titleChanged " + e.getWebBrowser().getPageTitle() + " thread " + Thread.currentThread());
            }

            public void statusChanged(WebBrowserEvent e) {
                log.fine(System.currentTimeMillis() + " statusChanged " + e.getWebBrowser().getStatusText() + " thread " + Thread.currentThread());
            }

            public void commandReceived(WebBrowserCommandEvent e) {
                // log.fine(System.currentTimeMillis() + " commandReceived " + e.getCommand() + " thread " + Thread.currentThread());
            }
        });

        if (!loadWebPage(webBrowser))
            dispose();
    }

    private void tryToInitialize(int counter) {
        boolean existsCompatibleBrowser = getComponent() != null && isCompatible();
        synchronized (this) {
            initialized = existsCompatibleBrowser;
        }
        log.info(System.currentTimeMillis() + " initialized map: " + initialized);

        if (isInitialized()) {
            log.fine(System.currentTimeMillis() + " compatible, further initializing map");
            initializeAfterLoading();
            initializeBrowserInteraction();
            initializeCallbackListener();
            checkLocalhostResolution();
            checkCallback();
        } else {
            if (counter++ < 50) {
                log.info(System.currentTimeMillis() + " WAITING " + counter * 100 + " milliseconds");
                try {
                    Thread.sleep(counter * 100);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }

                tryToInitialize(counter);
            }
        }
    }

    private boolean isCompatible() {
        String result = executeScriptWithResult("return window.isCompatible && isCompatible();");
        return Boolean.parseBoolean(result);
    }

    private void initializeAfterLoading() {
        resize();
        update(true);
    }

    protected void disposeBrowser() {
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
            int width = Math.max(getComponent().getWidth() - 17, 0);
            int height = Math.max(getComponent().getHeight() - 2, 0);
            if (width != lastWidth || height != lastHeight) {
                executeScript("resize(" + width + "," + height + ");");
            }
            lastWidth = width;
            lastHeight = height;
        }
    }

    // zoom level and bounds

    protected int getBoundsZoomLevel(List<BaseNavigationPosition> positions) {
        if ((positions == null) || (positions.size() < 1))
            return 0;

        Wgs84Position northEast = Positions.northEast(positions);
        Wgs84Position southWest = Positions.southWest(positions);

        StringBuffer buffer = new StringBuffer();
        buffer.append("return map.getBoundsZoomLevel(new GLatLngBounds(").
                append("new GLatLng(").append(northEast.getLatitude()).append(",").
                append(northEast.getLongitude()).append("),").
                append("new GLatLng(").append(southWest.getLatitude()).append(",").
                append(southWest.getLongitude()).append(")").append("));");

        String zoomLevel = executeScriptWithResult(buffer.toString());
        return zoomLevel != null ? Transfer.parseDouble(zoomLevel).intValue() : 1;
    }

    protected int getCurrentZoomLevel() {
        String zoomLevel = executeScriptWithResult("return map.getZoom();");
        return zoomLevel != null ? Transfer.parseDouble(zoomLevel).intValue() : 1;
    }

    protected BaseNavigationPosition getNorthEastBounds() {
        return getBounds("return getNorthEastBounds();");
    }

    protected BaseNavigationPosition getSouthWestBounds() {
        return getBounds("return getSouthWestBounds();");
    }

    // script execution

    protected void executeScript(final String script) {
        if (webBrowser == null || script.length() == 0)
            return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
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
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
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
