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

import chrriis.dj.nativeswing.swtimpl.components.*;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.Wgs84Position;
import slash.navigation.util.Calculation;
import slash.navigation.util.Conversion;
import slash.navigation.util.Externalization;
import slash.navigation.util.Platform;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Displays the positions of a route.
 *
 * @author Christian Pesch
 */

public class EclipseSWTMapView extends BaseMapView {
    private static final Logger log = Logger.getLogger(EclipseSWTMapView.class.getName());

    private JWebBrowser webBrowser;

    public boolean isSupportedPlatform() {
        return Platform.isLinux() || Platform.isMac() || Platform.isWindows();
    }

    public Component getComponent() {
        return webBrowser;
    }

    // initialization

    private JWebBrowser createWebBrowser() {
        try {
            JWebBrowser browser = new JWebBrowser();
            browser.setBarsVisible(false);
            return browser;
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            setInitializationCause(t);
            return null;
        }
    }

    private boolean loadWebPage(JWebBrowser webBrowser) {
        try {
            File html = Externalization.extractFile("slash/navigation/converter/gui/mapview/routeconverter.html");
            if (html == null)
                throw new IllegalArgumentException("Cannot extract routeconverter.html");
            webBrowser.navigate(html.toURI().toURL().toExternalForm());
            if (debug)
                log.info(System.currentTimeMillis() + " loadWebPage thread " + Thread.currentThread());
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
                if (debug)
                    log.info(System.currentTimeMillis() + " windowWillOpen " + e.isConsumed() + " thread " + Thread.currentThread());
            }

            public void windowOpening(WebBrowserWindowOpeningEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " windowOpening " + e.getLocation() + "/" + e.getSize() + " thread " + Thread.currentThread());
            }

            public void windowClosing(WebBrowserEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " windowClosing " + e + " thread " + Thread.currentThread());
            }

            public void locationChanging(WebBrowserNavigationEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " locationChanging " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChanged(WebBrowserNavigationEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " locationChanged " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChangeCanceled(WebBrowserNavigationEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " locationChangeCanceled " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            private int startCount = 0;

            public void loadingProgressChanged(WebBrowserEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " loadingProgressChanged " + e.getWebBrowser().getLoadingProgress() + " thread " + Thread.currentThread());

                if (e.getWebBrowser().getLoadingProgress() == 100 && startCount == 0) {
                    // get out of the listener callback
                    new Thread(new Runnable() {
                        public void run() {
                            if (Platform.isLinux()) {
                                log.info(System.currentTimeMillis() + " started sleeping for 2s on Linux");
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e1) {
                                    // intentionally left empty
                                }
                                log.info(System.currentTimeMillis() + " stopped sleeping for 2s on Linux");
                            }
                            tryToInitialize(startCount++);
                        }
                    }, "MapViewInitializer").start();
                }
            }

            public void titleChanged(WebBrowserEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " titleChanged " + e.getWebBrowser().getPageTitle() + " thread " + Thread.currentThread());
            }

            public void statusChanged(WebBrowserEvent e) {
                if (debug)
                    log.info(System.currentTimeMillis() + " statusChanged " + e.getWebBrowser().getStatusText() + " thread " + Thread.currentThread());
            }

            public void commandReceived(WebBrowserEvent e, String command, String[] args) {
                if (debug)
                    log.info(System.currentTimeMillis() + " commandReceived " + command + " thread " + Thread.currentThread());
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
            if (debug)
                log.info(System.currentTimeMillis() + " compatible, further initializing map");
            initializeAfterLoading();
            initializeBrowserInteraction();
            initializeDragListener();
            checkLocalhostResolution();
            checkCallback();
        } else {
            if (counter++ < 2) {
                log.info(System.currentTimeMillis() + " WAITING " + counter * 2000 + " seconds");
                try {
                    Thread.sleep(counter * 2000);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }

                log.info(System.currentTimeMillis() + " LOADING page again");
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        loadWebPage(webBrowser);
                    }
                });
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
        if (preferences.getBoolean(SCALE_CONTROL_PREFERENCE, false))
            executeScript("map.addControl(new GScaleControl());");
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

        Wgs84Position northEast = Calculation.getNorthEast(positions);
        Wgs84Position southWest = Calculation.getSouthWest(positions);

        StringBuffer buffer = new StringBuffer();
        buffer.append("return map.getBoundsZoomLevel(new GLatLngBounds(").
                append("new GLatLng(").append(northEast.getLatitude()).append(",").
                append(northEast.getLongitude()).append("),").
                append("new GLatLng(").append(southWest.getLatitude()).append(",").
                append(southWest.getLongitude()).append(")").append("));");

        String zoomLevel = executeScriptWithResult(buffer.toString());
        return zoomLevel != null ? Conversion.parseInt(zoomLevel) : 1;
    }

    protected int getCurrentZoomLevel() {
        String zoomLevel = executeScriptWithResult("return map.getZoom();");
        return zoomLevel != null ? Conversion.parseInt(zoomLevel) : 1;
    }

    protected BaseNavigationPosition getNorthEastBounds() {
        return getBounds("return getNorthEastBounds();");
    }

    protected BaseNavigationPosition getSouthWestBounds() {
        return getBounds("return getSouthWestBounds();");
    }

    // script execution

    protected void executeScript(final String script) {
        if (script.length() == 0)
            return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    webBrowser.executeJavascript(script);
                    logExecuteScript(script, null);
                }
            });
        } else {
            webBrowser.executeJavascript(script);
            logExecuteScript(script, null);
        }
    }

    protected String executeScriptWithResult(final String script) {
        if (script.length() == 0)
            return null;

        final Object[] result = new Object[1];
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        result[0] = webBrowser.executeJavascriptWithResult(script);
                    }
                });
            } catch (InterruptedException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            } catch (InvocationTargetException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            }
        } else {
            result[0] = webBrowser.executeJavascriptWithResult(script);
        }

        logExecuteScript(script, result[0]);
        return result[0] != null ? result[0].toString() : null;
    }
}
