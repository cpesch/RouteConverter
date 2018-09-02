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

package slash.common.io;

import slash.common.type.CompactCalendar;

import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static slash.common.io.InputOutput.DEFAULT_BUFFER_SIZE;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.HexadecimalNumber.encodeBytes;

/**
 * Provides file and file name functionality.
 *
 * @author Christian Pesch
 */

public class Files {

    /**
     * @param file the file to find the extension
     * @return the extension of the file, which are the characters
     * starting with the last dot in the file name in lowercase characters
     */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
     * @param name the file name to find the extension
     * @return the extension of a file name, which are the characters
     * starting with the last dot in the file name in lowercase characters
     */
    public static String getExtension(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1)
            return "";
        return name.substring(index, name.length()).toLowerCase();
    }

    public static String getExtension(List<URL> urls) {
        String extension = "";
        for (URL url : urls) {
            extension = getExtension(url.toExternalForm());
        }
        return extension.toLowerCase();
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

    public static String extractFileName(String path) {
        int index = path.lastIndexOf('/');
        if (index != -1)
            path = path.substring(index + 1);
        return path;
    }

    public static File absolutize(File file) {
        if (!file.isAbsolute())
            file = new File(file.getAbsolutePath());
        return file;
    }

    public static String createReadablePath(File file) {
        String path = file.getAbsolutePath();
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            // intentionally left empty
        }
        return path;
    }

    public static String createReadablePath(URL url) {
        File file = toFile(url);
        if (file != null)
            return createReadablePath(file);
        return url.toExternalForm();
    }

    public static String shortenPath(String path, int maximumLength) {
        if (path.length() <= maximumLength)
            return path;
        String lastPathFragment = lastPathFragment(path, maximumLength, true);
        if (lastPathFragment.length() == maximumLength)
            return lastPathFragment;
        return path.substring(0, maximumLength - 3 - lastPathFragment.length()) + "..." + lastPathFragment;
    }

    private static String lastPathFragment(String path, int maximumLength, boolean includeSeparator) {
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        int index = path.lastIndexOf('/');
        if (index == -1)
            index = path.lastIndexOf('\\');
        if (index != -1)
            path = path.substring(index + (includeSeparator ? 0 : 1));
        if (path.length() > maximumLength - 3)
            return "..." + path.substring(max(0, path.length() - maximumLength + 3));
        else
            return path;
    }

    public static String lastPathFragment(String path, int maximumLength) {
        return lastPathFragment(path, maximumLength, false);
    }

    public static File toFile(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                // intentionally left empty
            }
        }
        return null;
    }

    public static List<URL> toUrls(String... urls) {
        List<URL> result = new ArrayList<>(urls.length);
        for (String url : urls) {
            try {
                result.add(new URL(url));
            } catch (MalformedURLException e) {

                // fallback from URL to file
                try {
                    result.add(new File(url).toURI().toURL());
                } catch (MalformedURLException e1) {
                    // intentionally left empty
                }
            }
        }
        return result;
    }

    public static List<URL> toUrls(File... files) {
        List<URL> urls = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                // intentionally left empty
            }
        }
        return urls;
    }

    public static List<URL> reverse(List<URL> urls) {
        List<URL> result = new ArrayList<>();
        for (URL url : urls)
            result.add(0, url);
        return result;
    }

    public static String createGoPalFileName(String fileName) {
        fileName = fileName.toUpperCase();
        fileName = fileName.replaceAll("[^\\w.]", " ");
        return fileName;
    }

    public static String calculateConvertFileName(File file, String extension, int fileNameLength) {
        String name = file.getName();
        name = name.substring(0, min(name.length(), fileNameLength));
        name = setExtension(name, extension);
        String path = file.getParentFile() != null ? file.getParentFile().getPath() : "";
        return new File(path, name).getAbsolutePath();
    }

    private static int calculateNumberLength(int number) {
        return number > 999 ? 4 : (number > 99 ? 3 : (number > 9 ? 2 : (number > 0 ? 1 : 0)));
    }

    static String numberToString(int number, int maximum) {
        if (number > 9999)
            throw new IllegalArgumentException("Number " + number + " is too large.");
        if (maximum > 9999)
            throw new IllegalArgumentException("Maximum " + maximum + " is too large.");
        if (number > maximum)
            throw new IllegalArgumentException("Index " + number + " larger than maximum " + maximum);

        int numberLength = calculateNumberLength(maximum);
        StringBuilder result = new StringBuilder(Integer.toString(number));
        while (result.length() < numberLength) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    static String calculateConvertFileName(File file, int index, int maximum, String extension, int fileNameLength) {
        String name = file.getName();
        name = removeExtension(name);
        name = name.substring(0, min(name.length(), fileNameLength));

        if (calculateNumberLength(maximum) > 0) {
            String number = numberToString(index, maximum);
            name = name.substring(0, min(name.length(), fileNameLength - number.length()));
            name += number;
        }

        name = setExtension(name, extension);
        File parentFile = file.getParentFile();
        String path = parentFile != null ? parentFile.getPath() : ".";
        return new File(path, name).getAbsolutePath();
    }

    public static File[] createTargetFiles(File pattern, int fileCount, String extension, int fileNameLength) {
        File[] files = new File[fileCount];
        if (fileCount == 1) {
            files[0] = new File(calculateConvertFileName(pattern, extension, fileNameLength));
        } else {
            for (int i = 0; i < fileCount; i++) {
                files[i] = new File(calculateConvertFileName(pattern, i + 1, fileCount, extension, fileNameLength));
            }
        }
        return files;
    }

    public static void writePartialFile(InputStream inputStream, long fileSize, File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        byte[] buffer = new byte[1024];
        while (true) {
            try {
                int read = inputStream.read(buffer);
                if (read == -1)
                    break;
                raf.write(buffer, 0, read);
            } catch (EOFException e) {
                break;
            }
        }

        raf.setLength(fileSize);
        raf.close();
    }

    private static final String DEFAULT_ALGORITHM = "SHA1";

    public static String generateChecksum(InputStream inputStream) throws IOException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(format("Should no happen: algorithm %s not found", DEFAULT_ALGORITHM), e);
        }

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;

        while (-1 != (read = inputStream.read(buffer))) {
            messageDigest.update(buffer, 0, read);
        }

        return encodeBytes(messageDigest.digest());
    }

    public static String generateChecksum(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return generateChecksum(inputStream);
        }
    }

    public static CompactCalendar getLastModified(File file) {
        return fromMillis(file.lastModified());
    }

    public static void setLastModified(File file, Long lastModified) throws IOException {
        if (lastModified == null)
            return;
        if (!file.setLastModified(lastModified))
            throw new IOException(format("Could not set last modified of %s to %s", file, lastModified));
    }

    public static void setLastModified(File file, CompactCalendar lastModified) throws IOException {
        if (lastModified == null)
            return;
        setLastModified(file, lastModified.getTimeInMillis());
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
                    (extension == null || getExtension(path).equals(extension)))
                list.add(path);

        } else {
            if (collectDirectories)
                list.add(path);

            //noinspection ResultOfMethodCallIgnored
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
     * with the given extension
     */
    public static List<File> collectFiles(File path, String extension) {
        extension = extension != null ? extension.toLowerCase() : null;
        List<File> list = new ArrayList<>(1);
        recursiveCollect(path, false, true, extension, list);
        return list;
    }

    public static List<File> collectFiles(List<File> files) {
        List<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.isFile())
                result.add(file);
            else if (file.isDirectory()) {
                File[] list = file.listFiles(new FileFileFilter());
                if (list != null)
                    result.addAll(asList(list));
            }
        }
        return result;
    }

    public static File findExistingPath(File path) {
        while (path != null && !path.exists()) {
            path = path.getParentFile();
        }
        return path != null && path.exists() ? path : null;
    }

    private static void delete(File file) throws IOException {
        if (file.exists() && !file.delete())
            throw new IOException(format("Cannot delete %s", file));
    }

    public static void recursiveDelete(File path) throws IOException {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    recursiveDelete(file);
                delete(file);
            }
        }
        delete(path);
    }

    public static String printArrayToDialogString(Object[] array, boolean shorten) {
        if (array == null)
            return "null";

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                if (i == array.length - 1)
                    buffer.append(" and\n");
                else
                    buffer.append(",\n");
            String string = array[i].toString();
            if(shorten)
                string = shortenPath(string, 60);
            buffer.append("'").append(string).append("'");
        }
        return buffer.toString();
    }
}
