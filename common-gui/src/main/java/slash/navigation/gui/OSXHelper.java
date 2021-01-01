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

import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.system.Platform.isJava9OrLater;

/**
 * Sets the dock icon for Mac OS X for RouteConverter.
 *
 * @author Christian Pesch
 */
public class OSXHelper {
    private static final Logger log = Logger.getLogger(OSXHelper.class.getName());
    private static Object application;
    private static Class<?> applicationClass;

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void initializeApplicationObject() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (application == null) {
            applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplicationMethod = applicationClass.getMethod("getApplication");
            application = getApplicationMethod.invoke(null);
        }
    }

    public static void setDockIconImage(Image image) {
        try {
            initializeApplicationObject();
            Method setDockIconImageMethod = applicationClass.getMethod("setDockIconImage", Image.class);
            setDockIconImageMethod.invoke(application, image);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.warning("Error while setting dock icon: " + getLocalizedMessage(e));
        }
    }

    /**
     * A OSXHandler has the name of the EAWT method it intends to listen for (handleAbout, for
     * example), the Object that will ultimately perform the task, and the Method to be called on
     * that Object.
     *
     * @author Christopher Tipper, Christian Pesch (Java 7, 8 compatibility)
     * <p>
     * Based on https://gist.github.com/Manouchehri/5b403eaf5ecd6a8d0cf606b29cfecca0
     */
    public static class OSXHandler implements InvocationHandler {
        Object targetObject;
        Method targetMethod;
        private final String proxySignature;

        private OSXHandler(String proxySignature, Object target, Method handler) {
            this.proxySignature = proxySignature;
            this.targetObject = target;
            this.targetMethod = handler;
        }

        private static void createProxy(OSXHandler adapter, String handlerClassName, String setMethodName) {
            try {
                initializeApplicationObject();
                Class<?> handlerClass = Class.forName((isJava9OrLater() ? "java.awt.desktop." : "com.apple.eawt.") + handlerClassName);
                Method setHandlerMethod = application.getClass().getDeclaredMethod(setMethodName, handlerClass);
                Object osxAdapterProxy = Proxy.newProxyInstance(OSXHandler.class.getClassLoader(), new Class<?>[]{handlerClass}, adapter);
                setHandlerMethod.invoke(application, osxAdapterProxy);
            } catch (ClassNotFoundException e) {
                log.severe("This version of Mac OS X does not support the Apple EAWT. ApplicationEvent handling has been disabled: " + getLocalizedMessage(e));
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
            }
        }

        public static void setAboutHandler(Object target, Method aboutHandler) {
            OSXHandler adapter = new OSXHandler("handleAbout", target, aboutHandler) {
                public void callTarget(Object appleEvent) {
                    if (appleEvent != null) {
                        try {
                            this.targetMethod.invoke(this.targetObject, appleEvent);
                        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
                            log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
                        }
                    }
                }
            };
            createProxy(adapter, "AboutHandler", "setAboutHandler");
        }

        public static void setOpenFilesHandler(Object target, Method openFilesHandler) {
            OSXHandler adapter = new OSXHandler("openFiles", target, openFilesHandler) {
                public void callTarget(Object appleEvent) {
                    if (appleEvent != null) {
                        try {
                            this.targetMethod.invoke(this.targetObject, appleEvent);
                        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
                            log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
                        }
                    }
                }
            };
            createProxy(adapter, "OpenFilesHandler", "setOpenFileHandler");
        }

        public static void setPreferencesHandler(Object target, Method preferencesHandler) {
            OSXHandler adapter = new OSXHandler("handlePreferences", target, preferencesHandler) {
                public void callTarget(Object appleEvent) {
                    if (appleEvent != null) {
                        try {
                            this.targetMethod.invoke(this.targetObject, appleEvent);
                        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
                            log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
                        }
                    }
                }
            };
            createProxy(adapter, "PreferencesHandler", "setPreferencesHandler");
        }

        public static void setQuitHandler(Object target, Method quitHandler) {
            OSXHandler adapter = new OSXHandler("handleQuitRequestWith", target, quitHandler) {
                public void callTarget(Object appleEvent, Object response) {
                    if (appleEvent != null) {
                        try {
                            this.targetMethod.invoke(this.targetObject, appleEvent, response);
                        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
                            log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
                        }
                    }
                }
            };
            createProxy(adapter, "QuitHandler", "setQuitHandler");
        }


        private boolean isCorrectMethod(Method method, Object[] args) {
            return targetMethod != null && proxySignature.equals(method.getName()) && args.length > 0;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isCorrectMethod(method, args)) {
                if (args.length == 1) {
                    callTarget(args[0]);
                } else {
                    callTarget(args[0], args[1]);
                }
            }
            return null;
        }

        public void callTarget(Object appleEvent) throws InvocationTargetException, IllegalAccessException {
            targetMethod.invoke(targetObject, (Object[]) null);
        }

        public void callTarget(Object appleEvent, Object response) throws InvocationTargetException, IllegalAccessException {
            targetMethod.invoke(targetObject, (Object[]) response);
        }
    }
}
