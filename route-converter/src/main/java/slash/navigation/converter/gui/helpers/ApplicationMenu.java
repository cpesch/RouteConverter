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

package slash.navigation.converter.gui.helpers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventObject;
import java.util.logging.Logger;

import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.system.Platform.isJava9;

/**
 * Creates an application menu for Mac OS X for RouteConverter.
 *
 * @author Christian Pesch
 */

public class ApplicationMenu {
    private static final Logger log = Logger.getLogger(ApplicationMenu.class.getName());

    public void addApplicationMenuItems() {
        try {
            OSXHandler.setAboutHandler(this, getClass().getDeclaredMethod("about", EventObject.class));
            OSXHandler.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", EventObject.class));
            OSXHandler.setQuitHandler(this, getClass().getDeclaredMethod("quit", EventObject.class, Object.class));
        } catch (NoSuchMethodException | SecurityException e) {
            log.warning("Error while adding application menu items: " + getLocalizedMessage(e));
        }

        /* Java 7,8
        Application application = Application.getApplication();
        application.setAboutHandler(new AboutHandler() {
            public void handleAbout(AppEvent.AboutEvent aboutEvent) {
                run("show-about");
            }
        });
        */

        /* Java 9
        Desktop.getDesktop().setAboutHandler(new AboutHandler() {
            public void handleAbout(AboutEvent e) {
                run("show-about");
           }
        });
        */
    }

    @SuppressWarnings("unused")
    public void about(EventObject event) {
        run("show-about");
    }

    @SuppressWarnings("unused")
    public void preferences(EventObject event) {
        run("show-options");
    }

    @SuppressWarnings("unused")
    public void quit(EventObject event, Object response) {
        run("exit");
    }

    private void run(String actionName) {
        slash.navigation.gui.Application.getInstance().getContext().getActionManager().run(actionName);
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
    private static class OSXHandler implements InvocationHandler {
        private static Object application;
        Object targetObject;
        Method targetMethod;
        private String proxySignature;

        private OSXHandler(String proxySignature, Object target, Method handler) {
            this.proxySignature = proxySignature;
            this.targetObject = target;
            this.targetMethod = handler;
        }

        @SuppressWarnings("JavaReflectionMemberAccess")
        private static void initializeApplicationObject() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            if (application == null) {
                Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                application = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
            }
        }

        private static void createProxy(OSXHandler adapter, String handlerName) {
            try {
                initializeApplicationObject();
                Class<?> handlerClass = Class.forName((isJava9() ? "java.awt.desktop." : "com.apple.eawt.") + handlerName);
                Method setHandlerMethod = application.getClass().getDeclaredMethod("set" + handlerName, handlerClass);
                Object osxAdapterProxy = Proxy.newProxyInstance(OSXHandler.class.getClassLoader(), new Class<?>[]{handlerClass}, adapter);
                setHandlerMethod.invoke(application, osxAdapterProxy);
            } catch (ClassNotFoundException e) {
                log.severe("This version of Mac OS X does not support the Apple EAWT. ApplicationEvent handling has been disabled: " + getLocalizedMessage(e));
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.severe("Mac OS X Adapter could not talk to EAWT: " + getLocalizedMessage(e));
            }
        }

        static void setAboutHandler(Object target, Method aboutHandler) {
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
            createProxy(adapter, "AboutHandler");
        }

        static void setPreferencesHandler(Object target, Method preferencesHandler) {
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
            createProxy(adapter, "PreferencesHandler");
        }

        static void setQuitHandler(Object target, Method quitHandler) {
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
            createProxy(adapter, "QuitHandler");
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
