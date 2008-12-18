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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides file and file name functionality.
 *
 * @author Christian Pesch
 */

public class Files {

    /**
     * @param file the file to find the extension
     * @return the extension of the file, which are the characters
     * starting with the last dot in the file name.
     */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
     * @param name the file name to find the extension
     * @return the extension of a file name, which are the characters
     * starting with the last dot in the file name.
     */
    public static String getExtension(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1)
            return "";
        return name.substring(index, name.length());
    }

    /**
     * Remove the extension of the given file name, if there is any.
     *
     * @param name the file name to remove the extension
     * @return the file name without an extension
     */
    public static String removeExtension(String name) {
        String extension = getExtension(name);
        return name.substring(0, name.length() - extension.length());
    }

    /**
     * Replace the extension of the given file name with the
     * given extension.
     *
     * @param name      the file name to replace the extension
     * @param extension the new extension for the file name
     * @return the file name with the given extension
     */
    public static String setExtension(String name, String extension) {
        name = removeExtension(name);
        name += extension;
        return name;
    }

    public static String createReadablePath(File file) {
        String path = file.getAbsolutePath();
        try {
            path = file.getCanonicalPath();
        }
        catch(IOException e) {
            // intentionally left empty
        }
        return path;
    }

    public static String createGoPalFileName(String fileName) {
        fileName = fileName.toUpperCase();
        fileName = fileName.replaceAll("[^\\w]", " ");
        return fileName;
    }

    public static String calculateConvertFileName(File file, String extension, int fileNameLength) {
        String name = file.getName();
        name = removeExtension(name);
        name = name.substring(0, Math.min(name.length(), fileNameLength));
        name = setExtension(name, extension);
        String path = file.getParentFile() != null ? file.getParentFile().getPath() : "";
        return new File(path, name).getAbsolutePath();
    }

    private static int calculateNumberLength(int number) {
        return number > 999 ? 4 : (number > 99 ? 3 : (number > 9 ? 2 : (number > 0 ? 1 : 0)));
    }

    public static String numberToString(int number, int maximum) {
        if (number > 9999)
            throw new IllegalArgumentException("Number " + number + " is too large.");
        if (maximum > 9999)
            throw new IllegalArgumentException("Maximum " + maximum + " is too large.");
        if (number > maximum)
            throw new IllegalArgumentException("Index " + number + " larger than maximum " + maximum);

        int numberLength = calculateNumberLength(maximum);
        StringBuffer result = new StringBuffer(Integer.toString(number));
        while(result.length() < numberLength) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    public static String calculateConvertFileName(File file, int index, int maximum, String extension, int fileNameLength) {
        String name = file.getName();
        name = removeExtension(name);
        name = name.substring(0, Math.min(name.length(), fileNameLength));

        if(calculateNumberLength(maximum) > 0) {
            String number = numberToString(index, maximum);
            name = name.substring(0, Math.min(name.length(), fileNameLength - number.length()));
            name += number;
        }

        name = setExtension(name, extension);
        String path = file.getParentFile().getPath();
        return new File(path, name).getAbsolutePath();
    }

    /**
     * Collects files/directories with the given extension in the given
     * list. If path is a directory, it recursively descends the directory
     * tree. If no extension is given, all files are collected.
     *
     * @param path               the path to collect files below
     * @param collectDirectories decides whether directories are collected
     * @param collectFiles       decides whether file are collected
     * @param extension          the extension in lower case
     * @param list               the list to add hits to
     */
    private static void recursiveCollect(File path,
                                         final boolean collectDirectories,
                                         final boolean collectFiles,
                                         final String extension,
                                         final List<File> list) {
        if (path.isFile()) {
            if (collectFiles &&
                    (extension == null || getExtension(path).toLowerCase().equals(extension)))
                list.add(path);

        } else {
            if (collectDirectories)
                list.add(path);

            path.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    recursiveCollect(file, collectDirectories, collectFiles, extension, list);
                    return true;
                }
            });
        }
    }

    /**
     * Collects files below the given path with the given extension.
     * If path is a directory, the collection recursively descends the
     * directory tree. The extension comparison is case insensitive
     *
     * @param path      the path to collect files below
     * @param extension the case insensitively compare extension
     * @return the list of files found below the given path and
     *         with the given extension
     */
    public static List<File> collectFiles(File path, String extension) {
        List<File> list = new ArrayList<File>(1);
        extension = extension != null ? extension.toLowerCase() : null;
        recursiveCollect(path, false, true, extension, list);
        return list;
    }

    public static File findExistingPath(File path) {
        while (path != null && !path.exists()) {
            path = path.getParentFile();
        }
        return path != null && path.exists() ? path : null;
    }

    public static String printArrayToString(Object[] array) {
        if (array == null)
            return "null";

        StringBuffer buffer = new StringBuffer();
        buffer.append('{');
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                buffer.append(',');
            buffer.append(array[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    public static String printArrayToDialogString(Object[] array) {
        if (array == null)
            return "null";

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                if (i == array.length - 1)
                    buffer.append(" and\n");
                else
                    buffer.append(",\n");
            buffer.append("'").append(array[i]).append("'");
        }
        return buffer.toString();
    }
}
