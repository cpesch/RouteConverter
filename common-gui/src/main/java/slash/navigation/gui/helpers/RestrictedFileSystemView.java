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

package slash.navigation.gui.helpers;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Vector;

import static java.io.File.createTempFile;

import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;

/**
 * Fallback implementation of a FileSystemView.
 *
 * Based on http://jython.svn.sourceforge.net/viewvc/jython/branches/Release_2_2maint/installer/src/java/org/python/util/install/RestrictedFileSystemView.java?view=markup&pathrev=4161
 *
 * @author Christian Pesch
 */

class RestrictedFileSystemView extends FileSystemView {
    private static final String newFolderString = UIManager.getString("FileChooser.other.newFolder");

    private File defaultDirectory;

    RestrictedFileSystemView() {
        this(null);
    }

    RestrictedFileSystemView(File defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
    }

    public boolean isRoot(File f) {
        if (f == null || !f.isAbsolute()) {
            return false;
        }

        File[] roots = getRoots();
        for (File root : roots) {
            if (root.equals(f)) {
                return true;
            }
        }
        return false;
    }

    public Boolean isTraversable(File f) {
        return f.isDirectory();
    }

    public String getSystemDisplayName(File f) {
        String name = null;
        if (f != null) {
            if (isRoot(f)) {
                name = f.getAbsolutePath();
            } else {
                name = f.getName();
            }
        }
        return name;
    }

    public String getSystemTypeDescription(File f) {
        return null;
    }

    public Icon getSystemIcon(File f) {
        if (f != null) {
            return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
        } else {
            return null;
        }
    }

    public boolean isParent(File folder, File file) {
        return !(folder == null || file == null) && folder.equals(file.getParentFile());
    }

    public File getChild(File parent, String fileName) {
        return createFileObject(parent, fileName);
    }

    public boolean isFileSystem(File f) {
        return true;
    }

    public boolean isHiddenFile(File f) {
        return f.isHidden();
    }

    public boolean isFileSystemRoot(File dir) {
        return isRoot(dir);
    }

    public boolean isDrive(File dir) {
        return false;
    }

    public boolean isFloppyDrive(File dir) {
        return false;
    }

    public boolean isComputerNode(File dir) {
        return false;
    }

    public File[] getRoots() {
        return File.listRoots();
    }

    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    public File getDefaultDirectory() {
        if (defaultDirectory == null) {
            try {
                File temp = createTempFile("filesystemview", "restricted", getTemporaryDirectory());
                temp.deleteOnExit();
                defaultDirectory = temp.getParentFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defaultDirectory;
    }

    public File createFileObject(File dir, String filename) {
        if (dir == null) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }

    public File createFileObject(String path) {
        File f = new File(path);
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }

    public File[] getFiles(File dir, boolean useFileHiding) {
        Vector<File> files = new Vector<>();

        File[] names;
        names = dir.listFiles();
        File f;

        int nameCount = (names == null) ? 0 : names.length;
        for (int i = 0; i < nameCount; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            f = names[i];
            if (!useFileHiding || !isHiddenFile(f)) {
                files.addElement(f);
            }
        }

        return files.toArray(new File[files.size()]);
    }

    public File getParentDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File psf = dir.getParentFile();
            if (psf != null) {
                if (isFileSystem(psf)) {
                    File f = psf;
                    if (!f.exists()) {
                        File ppsf = psf.getParentFile();
                        if (ppsf == null || !isFileSystem(ppsf)) {
                            f = createFileSystemRoot(f);
                        }
                    }
                    return f;
                } else {
                    return psf;
                }
            }
        }
        return null;
    }

    protected File createFileSystemRoot(File f) {
        return new FileSystemRoot(f);
    }

    static class FileSystemRoot extends File {
        public FileSystemRoot(File f) {
            super(f, "");
        }

        public boolean isDirectory() {
            return true;
        }

        public String getName() {
            return getPath();
        }
    }

    public File createNewFolder(File containingDir) throws IOException {
        if (containingDir == null)
            throw new IOException("Containing directory is null:");

        File newFolder = createFileObject(containingDir, newFolderString);
        int i = 2;
        while (newFolder.exists() && (i < 100)) {
            newFolder = createFileObject(containingDir, MessageFormat.format(newFolderString, i));
            i++;
        }

        if (newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            newFolder = ensureDirectory(newFolder);
        }
        return newFolder;
    }
}

