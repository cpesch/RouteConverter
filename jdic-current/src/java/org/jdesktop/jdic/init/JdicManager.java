/*
 * Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.jdesktop.jdic.init;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jdesktop.jdic.browser.internal.WebBrowserUtil;
import slash.navigation.util.Externalization;

/**
 * Initialization manager for JDIC to set the environment variables or
 * initialize the set up for native libraries and executable files.
 * <p>
 * There are 3 modes of operation: WebStart, file system, and .jar file.
 * <p>
 * When using WebStart, please specify a .jar file(jdic-native.jar) with the
 * native libraries for your platform to be loaded by WebStart in your JNPL.
 * This class will find the unjared native libraries and executables, and use
 * them directly.
 * <p>
 * If not in WebStart, the system will expect the native libraries to be located
 * in directory at the root of the classpath or .jar containing this class.
 * 
 * @author Michael Samblanet
 * @author Paul Huang
 * @author George Zhang
 * @author Michael Shan
 * @since July 29, 2004
 */
public class JdicManager {
	private boolean isShareNativeInitialized = false;

	/** The path for the JDIC native files (jdic.dll/libjdic.so, etc) */
	String jdicNativeLibPath = null;

	/** Singleton instance of this class */
	private static JdicManager sSingleton = null;

	/**
	 * Private constructor to prevent public construction.
	 */
	private JdicManager() {
	}

	/**
	 * Returns a singleton instance of <code>JdicManager</code>.
	 */
	public static synchronized JdicManager getManager() {
		if (sSingleton == null) {
			sSingleton = new JdicManager();
		}
		return sSingleton;
	}

	/**
	 * Initializes the shared native file settings for all the JDIC components/
	 * packages. Set necessary environment variables for the shared native
	 * library and executable files, including *.dll files on Windows, and *.so
	 * files on Unix.
	 * 
	 * @exception JdicInitException
	 *                Generic initialization exception
	 */
	public void initShareNative() throws JdicInitException {
		WebBrowserUtil.trace("init share native..... ");
		// If the shared native file setting was already initialized,
		// just return.
		if (isShareNativeInitialized) {
			return;
		}
		try {
			// Find the root path of this class.
			String jwsVersion = System.getProperty("javawebstart.version");
			if (jwsVersion != null) {
				//loaded by JWS
				WebBrowserUtil.trace("Loaded by JavaWebStart,version is "
						+ jwsVersion);
				//native libs will be loaded by webstart automatically
				jdicNativeLibPath = caculateNativeLibPathBySunJWS();
				return;
			} else {
                /* TODO modified to work out of the temp directory
				String runningURL = (new URL(JdicManager.class
						.getProtectionDomain().getCodeSource().getLocation(),
						".")).openConnection().getPermission().getName();
				WebBrowserUtil.trace("current runnning path " + runningURL);
				String runningPath = (new File(runningURL)).getCanonicalPath();
				// check if jdic is installed by applet installer,if it's,return according file path
				runningPath = dealExtensionMode(runningPath);//different file structure,bad name of runningPath
                */
                String runningPath = Externalization.getTempDirectory().getAbsolutePath();
                // check if it's cross platoform version, if it's,deal with jdic_stub.jar
				jdicNativeLibPath = dealCrossPlatformVersion(runningPath);
				
				// Add the binary path (including jdic.dll or libjdic.so) to
				// "java.library.path", since we need to use the native methods
				// in class InitUtility.				
				String JAVA_LIBRARY_PATH = jdicNativeLibPath + File.pathSeparator
						+ System.getProperty("java.library.path");
				System.setProperty("java.library.path", JAVA_LIBRARY_PATH);//deal with native resources
				
				//disable the sys_paths,which has higher priority than java.library.pah if not null 
				try {
					Field fieldSysPath = ClassLoader.class
							.getDeclaredField("sys_paths");
					fieldSysPath.setAccessible(true);
					if (fieldSysPath != null) {
						fieldSysPath.set(System.class.getClassLoader(), null);
					}
				} catch (NoSuchFieldException nfe) {
					// ignore this exception for some classloaders
					WebBrowserUtil
							.trace("Current classloader doesn't have sys_paths field.\n"
									+ nfe.getMessage());
				}
			}
		} catch (Throwable e) {
			throw new JdicInitException(e);
		}
		isShareNativeInitialized = true;
	}

	/**
	 * If jdic is installed by extention mode,folder structure like below:
	 * - for os specific version:
	 * 	jdic.jar
	 * 	jdic_native.jar
	 *  
	 * - for cross platform version:
	 *  jdic.jar
	 *  jdic_crossplatform.jar
	 *    which includes all native stuffs and jdic_stub.jar   
	 * @param jdicJarPath path of jdic.jar
	 * @return native stuff path or 
	 */
	private String dealExtensionMode(String jdicJarPath) {
		WebBrowserUtil.trace("check if loaded in extension mode");
		String jdic_native_jar = jdicJarPath + "/" + "jdic_native.jar";
		String jdic_cross_platform_jar = jdicJarPath + "/"
				+ "jdic_crossplatform.jar";
		if (extractJarFile(jdic_cross_platform_jar, jdicJarPath+"/jdic_crossplatform")) {
			WebBrowserUtil.trace("loaded in extension mode with cross platform format");
			return jdicJarPath + "/jdic_crossplatform";
		}
		if (extractJarFile(jdic_native_jar, jdicJarPath+"/jdic_native")) {
			WebBrowserUtil.trace("loaded in extension mode with os specific format");
			return jdicJarPath + "/jdic_native";
		}
		WebBrowserUtil.trace("Not loaded in extension mode");
		return jdicJarPath;
	}
	
	/**
	 * Check if jar file exists, if does,extract it. 
	 * @param jarPath
	 * @param jdicJarPath
	 * @return if extract succeeds 
	 */
	private boolean extractJarFile(String jarPath, String jdicJarPath) {
		WebBrowserUtil.trace(" jar path to verify" + jarPath);

		if (new File(jarPath).exists()) {
			try {
				JarFile jarfile = new JarFile(jarPath);
				Enumeration jarEntries = jarfile.entries();
				while (jarEntries.hasMoreElements()) {
					JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
					if (jarEntry.isDirectory()) {
						continue;
					}
					String jarFileName = jarEntry.getName();
					WebBrowserUtil.trace(" dealing with " + jarFileName);
					// jdicPath as the parent path
					File copyFile = new File(jdicJarPath, jarFileName);
					if (copyFile.exists()) {
						continue;
					}
					if (!copyFile.getParentFile().exists()) {
						copyFile.getParentFile().mkdirs();
					}
					InputStream is = jarfile.getInputStream(jarEntry);
					OutputStream os = new FileOutputStream(copyFile);
					WebBrowserUtil.copyIsToOs(is, os);
				}
			} catch (Exception e) {
				WebBrowserUtil.error(e.getMessage());
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
	/**
	 * For crossplatform version,native libs locate under platform/arch/; set jdic_stub.jar
	 * to the classpath for urlclassloader.Folder structure like:
	 *  jdic.jar
	 *  platform/arch/
	 *    includes native stuffs and jdic_stub.jar
	 * 
	 * For no-crossplatform version,native libs locate under the same folder of jdic.jar.Folder structure like:
	 * jdic.jar
	 * ....
	 * 
	 * @throws MalformedURLException
	 * @throws JdicInitException
	 *  
	 */
	private String dealCrossPlatformVersion(String jdicJarPath)
			throws MalformedURLException, JdicInitException {

		String platformPath = jdicJarPath + File.separator + getPlatform();
		File jdicStubJarFile = new File(platformPath + File.separator
				+ "jdic_stub.jar");
		if (!jdicStubJarFile.exists()) {
			//not cross platform version
			return jdicJarPath;
		} else {
			//cross platform version
			//add jdic_stub.jar into classpath
			String architecturePath = platformPath + File.separator
					+ getArchitecture();
			ClassLoader cl = getClass().getClassLoader();
			if (!(cl instanceof URLClassLoader)) {
				//not URLClassLoader,omit it,in case the stub jar has been
				// set to claspath
				String exceptionInfo = "We detect that you are not using java.net.URLClassLoader for cross platform versoin,you have to set jdic_stub.jar manually if JDIC can't work!";
				WebBrowserUtil.error(exceptionInfo);
				return architecturePath;//return the native lib path
			}
			//set stub jars to classpath
			URLClassLoader urlCl = (URLClassLoader) cl;
			try {
				Method addURLMethod = URLClassLoader.class.getDeclaredMethod(
						"addURL", new Class[] { URL.class });
				addURLMethod.setAccessible(true);
				addURLMethod.invoke(urlCl, new Object[] { jdicStubJarFile
						.toURL() });
				return architecturePath;//return the native lib path
			} catch (Throwable t) {
				t.printStackTrace();
				throw new JdicInitException(
						"Error, could not add URL to system classloader");
			}
		}
	}

	public String getBinaryPath() {
		return jdicNativeLibPath;
	}

	/**
	 * Initialize native libs' running path if loaded by webstart.This method
	 * only works for sun webstart implementaion,for other webstart
	 * implementations, you have to rewrite this method.
	 * 
	 * @throws IOException
	 * @throws JdicInitException
	 */
	private String caculateNativeLibPathBySunJWS() throws IOException,
			JdicInitException {
		String jdicLibFolder = null;
		ClassLoader cl = this.getClass().getClassLoader();
		WebBrowserUtil.trace("current class loader " + cl.getClass().getName());
		try {
			Class jnlpClassLoaderClass = Class
					.forName("com.sun.jnlp.JNLPClassLoader");
			if (cl.getClass().isAssignableFrom(jnlpClassLoaderClass)) {
				Method findLibMethod = jnlpClassLoaderClass.getDeclaredMethod(
						"findLibrary", new Class[] { String.class });
				String jdicLibURL = (String) findLibMethod.invoke(cl,
						new Object[] { "jdic" });
				jdicLibFolder = (new File(jdicLibURL)).getParentFile()
						.getCanonicalPath();
				WebBrowserUtil.trace("running path " + jdicNativeLibPath);
				isShareNativeInitialized = true;
			} else {
				throw new JdicInitException(
						"Unexpected ClassLoader for webstart, only Sun's com.sun.jnlp.JNLPClassLoader is supported,while current classloader is "
								+ cl.getClass().getName());
			}
		} catch (Exception e) {
			WebBrowserUtil.error(e.getMessage());
			e.printStackTrace();
			throw new JdicInitException(e.getMessage());
		}
		return jdicLibFolder;
	} 
	

	/**
	 * Return the canonical name of the platform. This value is derived from the
	 * System property os.name.
	 * 
	 * @return The platform string.
	 */
	private static String getPlatform() {
		// See list of os names at: http://lopica.sourceforge.net/os.html
		// or at: http://www.tolstoy.com/samizdat/sysprops.html
		String osname = System.getProperty("os.name");
		if (WebBrowserUtil.IS_OS_WINDOWS) {
			return "windows";
		}
		if (WebBrowserUtil.IS_OS_MAC) {
			return "mac";
		}
		return canonical(osname);
	}

	/**
	 * Return the name of the architecture. This value is determined by the
	 * System property os.arch.
	 * 
	 * @return The architecture string.
	 */
	private static String getArchitecture() {
		String arch = System.getProperty("os.arch");
		if (WebBrowserUtil.IS_OS_MAC) {
			if (arch.endsWith("86")) {
				// for mac, use universal binaries of ppc
				return "ppc";
			}
		}
		if (arch.endsWith("86")) {
			return "x86";
		}
		return canonical(arch);
	}

	/**
	 * @param value
	 *            The value to be canonicalized.
	 * @return The value with all '/', '\' and ' ' replaced with '_', and all
	 *         uppercase characters replaced with lower case equivalents.
	 */
	private static String canonical(String value) {
		WebBrowserUtil.trace("value:" + value);
		WebBrowserUtil.trace("canonical:"
				+ value.toLowerCase().replaceAll("[\\\\/ ]", "_"));
		return value.toLowerCase().replaceAll("[\\\\/ ]", "_");
	}

}
