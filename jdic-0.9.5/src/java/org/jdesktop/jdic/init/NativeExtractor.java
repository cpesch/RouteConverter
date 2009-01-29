/*
 * Copyright (C) 2008 Sun Microsystems, Inc. All rights reserved. Use is
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Native DLL loader class
 * @author uta
 */
public class NativeExtractor {
    
    public static String getPlatformDLLext() 
    {
        if (getPlatform().equals("windows")) {
            return ".dll";
        }
        return ".so";
    }
    
    /**
     * Return the canonical name of the platform. This value is derived from the
     * System property os.name.
     * 
     * @return The platform string.
     */
    public static String getPlatform() {
        // See list of os names at: http://lopica.sourceforge.net/os.html
        // or at: http://www.tolstoy.com/samizdat/sysprops.html
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
                return "windows";
        }
        return canonical(osname);
    }

    /**
     * Return the name of the architecture. This value is determined by the
     * System property os.arch.
     * 
     * @return The architecture string.
     */
    public static String getArchitecture() {
        String arch = System.getProperty("os.arch");
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
        return value.toLowerCase().replaceAll("[\\\\/ ]", "_");
    }
    
    private static File tmpBaseDir = new File(
            System.getProperty("java.io.tmpdir")
            + File.separator + "jdic_0_9_5");

    public static void copyStream(
        InputStream in,
        OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read = 0;
        while((read = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void compareStreams(
        InputStream in1,
        InputStream in2) throws IOException
    {
        while( true ){
            int buf1 = in1.read();
            int buf2 = in2.read();
            if(buf1==buf2){
                if(buf1==-1)
                    break;
            }else{
                throw new IOException("wrong JDIC version in the temp holder!");
            }
        }
    }
    
    public static String getBinary() throws IOException
    {
        if( !tmpBaseDir.exists() ) {
            tmpBaseDir.mkdir();
        }   
        return tmpBaseDir.getCanonicalPath();
    }   
    
    public static String extractBinary(String libName) throws IOException
    {
        if( !tmpBaseDir.exists() ) {
            tmpBaseDir.mkdir();
        }   
        File dll = new File(tmpBaseDir.getCanonicalPath()
                + File.separator + libName);
        dll.delete();
        
        String localName = "bin/" + getPlatform() + "/" + getArchitecture() + "/" + libName;
        InputStream is = NativeExtractor.class
                .getClassLoader()
                .getResourceAsStream(localName);
        
        if(null==is){
            throw new IOException("Native resourse [" + localName + "] was not found in JAR.");
        }
        
        if( dll.exists() ){
            //share violation - already in use
            InputStream  iso = null;
            try {
                iso =  new FileInputStream(dll);
                compareStreams(is, iso);
            } finally {
                is.close();
                if(null!=iso)
                    iso.close();                
            }
        } else {
            OutputStream os = null;            
            try {
                os = new FileOutputStream(dll);
                copyStream(is, os);
            } finally {                
                is.close();
                if(null!=is)
                    os.close();
            }    
        }
        return dll.getCanonicalPath();
    }
    
    public static void loadLibruary(final String libName) 
            throws PrivilegedActionException 
    {
        AccessController.doPrivileged( new PrivilegedExceptionAction() { 
            public Object run() throws IOException {
                File dll = new File(extractBinary( libName + getPlatformDLLext() ));

                /* TODO let's hope we don't need this - 1.6 only
                dll.setExecutable(true);
                dll.setReadable(true);
                dll.setWritable(true);
                */

                System.load(dll.getCanonicalPath());
                return null;
            }
        });
    }
    
    public static Process exec( final String[] args) 
            throws PrivilegedActionException 
    {
        final Process[] res = new Process[] {null};
        AccessController.doPrivileged( new PrivilegedExceptionAction() { 
            public Object run() throws IOException {
                String exeName = args[0];
                File exe = new File( extractBinary(exeName) );
                
                /* TODO let's hope we don't need this - 1.6 only
                exe.setExecutable(true);
                exe.setReadable(true);
                exe.setWritable(true);
                */

                args[0] = exe.getCanonicalPath();
                res[0] = Runtime.getRuntime().exec( args );
                return null;
            }
        });
        return res[0];
    }
    
}