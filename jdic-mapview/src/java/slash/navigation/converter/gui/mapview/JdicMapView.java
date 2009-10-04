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

import org.jdesktop.jdic.browser.WebBrowser;
import org.jdesktop.jdic.browser.WebBrowserEvent;
import org.jdesktop.jdic.browser.WebBrowserListener;
import org.jdesktop.jdic.browser.internal.WebBrowserUtil;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.Wgs84Position;
import slash.navigation.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Displays the positions of a route.
 *
 * @author Christian Pesch
 */

public class JdicMapView extends BaseMapView {
    private static final Logger log = Logger.getLogger(JdicMapView.class.getName());

    private WebBrowser webBrowser;
    private int scrollBarSize = 0;

    public boolean isSupportedPlatform() {
        return Platform.isLinux() || Platform.isWindows();
    }

    public Component getComponent() {
        return webBrowser;
    }

    // initialization

    private WebBrowser createWebBrowser() {
        try {
            String path = "bin/" + Platform.getOsName() + "/" + Platform.getOsArchitecture() + "/";
            if (Platform.isLinux()) {
                Externalization.extractFile(path + "libmozembed-linux-gtk1.2.so");
                Externalization.extractFile(path + "libmozembed-linux-gtk2.so");
                Externalization.extractFile(path + "mozembed-linux-gtk1.2");
                Externalization.extractFile(path + "mozembed-linux-gtk2");
            }
            if (Platform.isMac())
                Externalization.extractFile(path + "libjdic.jnilib");
            if (Platform.isWindows()) {
                Externalization.extractFile(path + "IeEmbed.exe");
                scrollBarSize = 20;
            }

            WebBrowserUtil.enableDebugMessages(!Platform.isWindows());

            return new WebBrowser(false);
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            setInitializationCause(t);
            return null;
        }
    }

    private boolean loadWebPage(WebBrowser webBrowser) {
        try {
            File html = Externalization.extractFile("slash/navigation/converter/gui/mapview/routeconverter.html");
            if (html == null)
                throw new IllegalArgumentException("Cannot extract routeconverter.html");
            webBrowser.setURL(html.toURI().toURL());
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
            public void downloadStarted(WebBrowserEvent event) {
                log.fine(System.currentTimeMillis() + " downloadStarted " + event + " thread " + Thread.currentThread());
            }

            public void downloadCompleted(WebBrowserEvent event) {
                log.fine(System.currentTimeMillis() + " downloadCompleted " + event + " thread " + Thread.currentThread());
                if (Platform.isMac())
                    documentCompleted(event);
            }

            public void downloadProgress(WebBrowserEvent event) {
                log.fine(System.currentTimeMillis() + " downloadProgress " + event + " thread " + Thread.currentThread());
           }

            public void downloadError(WebBrowserEvent event) {
                log.fine(System.currentTimeMillis() + " downloadError " + event + " thread " + Thread.currentThread());
            }

            public void documentCompleted(WebBrowserEvent event) {
                log.fine(System.currentTimeMillis() + " documentCompleted " + event + " thread " + Thread.currentThread());
                synchronized (notificationMutex) {
                    initialized = getComponent() != null && isCompatible();
                }

                if (isInitialized()) {
                    new Thread(new Runnable() {
                        public void run() {
                            initializeDragListener();
                            initializeAfterLoading();
                            checkLocalhostResolution();
                            checkCallback();
                        }
                    }, "MapViewInitializer").start();
                }
            }

            public void titleChange(WebBrowserEvent event) {
            }

            public void statusTextChange(WebBrowserEvent event) {
            }
        });

        if (!loadWebPage(webBrowser)) {
            dispose();
            return;
        }

        initializeBrowserInteraction();
    }

    private boolean isCompatible() {
        String result;
        synchronized (notificationMutex) {
            result = executeScriptWithResult("window.isCompatible && isCompatible()");
        }
        return Boolean.parseBoolean(result);
    }

    private void initializeAfterLoading() {
        // workaround for Linux and Mac versions where the window has to be resized to show a complete map
        if (Platform.isLinux() || Platform.isMac()) {
            forceResize();
        } else
            resize();
        update(true);
        if(preferences.getBoolean(SCALE_CONTROL_PREFERENCE, false))
            executeScript("map.addControl(new GScaleControl());");
    }

    protected void disposeBrowser() {
        if (webBrowser != null) {
            webBrowser.dispose();

            long start = System.currentTimeMillis();
            while (true) {
                long end = System.currentTimeMillis();
                if (end - start > 5000 || !webBrowser.isInitialized()) {
                    log.info("MapView dispose stopped after " + (end - start) + " ms");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            }

            webBrowser = null;
        }
    }

    // resizing

    private boolean hasBeenResizedToInvisible = false;

    public void resize() {
        if (!isInitialized() || !getComponent().isShowing())
            return;

        synchronized (notificationMutex) {
            // if map is not visible remember to update and resize it again
            // once the map becomes visible again
            if (!isVisible()) {
                hasBeenResizedToInvisible = true;
                resizeMap();
            } else if (hasBeenResizedToInvisible) {
                hasBeenResizedToInvisible = false;
                update(true);
                forceResize();
            } else {
                resizeMap();
            }
        }
    }

    private int lastWidth = -1, lastHeight = -1;

    private void resizeMap() {
        new Thread(new Runnable() {
            public void run() {
                synchronized (notificationMutex) {
                    int width = Math.max(getComponent().getWidth() - scrollBarSize, 0);
                    int height = Math.max(getComponent().getHeight() - scrollBarSize, 0);
                    if (width != lastWidth || height != lastHeight) {
                        executeScript("resize(" + width + "," + height + ")");
                    }
                    lastWidth = width;
                    lastHeight = height;
                }
            }
        }, "BrowserResizer").start();
    }

    private void forceResize() {
        final Dimension dimension = getComponent().getSize();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getComponent().setSize(dimension.width - 1, dimension.height - 1);
                resize();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getComponent().setSize(dimension);
                        resize();
                    }
                });
            }
        });
    }

    // zoom level and bounds

    protected int getBoundsZoomLevel(List<BaseNavigationPosition> positions) {
        if ((positions == null) || (positions.size() < 1))
            return 0;

        Wgs84Position northEast = Positions.northEast(positions);
        Wgs84Position southWest = Positions.southWest(positions);

        StringBuffer buffer = new StringBuffer();
        buffer.append("map.getBoundsZoomLevel(new GLatLngBounds(").
                append("new GLatLng(").append(northEast.getLatitude()).append(",").
                append(northEast.getLongitude()).append("),").
                append("new GLatLng(").append(southWest.getLatitude()).append(",").
                append(southWest.getLongitude()).append(")").append("));\n");

        String zoomLevel = executeScript(buffer);
        return zoomLevel != null ? Transfer.parseInt(zoomLevel) : 1;
    }

    protected int getCurrentZoomLevel() {
        String zoomLevel = executeScriptWithResult("map.getZoom();");
        return zoomLevel != null ? Transfer.parseInt(zoomLevel) : 1;
    }

    protected BaseNavigationPosition getNorthEastBounds() {
        return getBounds("getNorthEastBounds();");
    }

    protected BaseNavigationPosition getSouthWestBounds() {
        return getBounds("getSouthWestBounds();");
    }

    // script execution

    private String executeScript(StringBuffer script) {
        if (script.length() == 0)
            return null;

        if ((1000 <= script.length() % 1024) && (script.length() % 1024) <= 1018) {
            for (int i = 0; i < 20; i++) {
                script.append(';');
            }
        }
        return executeScriptWithResult(script.toString());
    }

    protected synchronized void executeScript(String script) {
        webBrowser.executeScript(script);
        logExecuteScript(script, null);
    }

    protected synchronized String executeScriptWithResult(String script) {
        String result = webBrowser.executeScript(script);
        logExecuteScript(script, result);
        return result;
    }
}
