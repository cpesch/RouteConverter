package slash.navigation.converter.gui.panels;

import slash.navigation.base.NavigationFormat;
import slash.navigation.converter.gui.helper.NavigationFormatFileFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas
 * Date: 28.12.11
 * Time: 10:27
 * <p/>
 */
final class UpdateFileNameListener implements PropertyChangeListener {

    private static final Logger log = Logger.getLogger(UpdateFileNameListener.class.getName());

    private interface FileChooserSelectionAccessor {
        String getSelection();

        void setSelection(String name);
    }

    private final JFileChooser chooser;
    private NavigationFormat lastFormat;

    UpdateFileNameListener(final JFileChooser fileChooser) {
        this.chooser = fileChooser;

        lastFormat = getSelectedFormat(chooser.getFileFilter());
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {

            final NavigationFormat selectedFormat = getSelectedFormat(chooser.getFileFilter());

            // save dialog returns wrong values by calling getSelectedFile()
            // --> read it by UI
            final FileChooserSelectionAccessor accessor = getAccessor(chooser);
            if (accessor != null) {

                updateFileName(accessor, selectedFormat);
            }

            lastFormat = selectedFormat;
        }
    }

    private void updateFileName(final FileChooserSelectionAccessor accessor,
                                final NavigationFormat selectedFormat) {

        final String fileName = accessor.getSelection();
        if (fileName != null && selectedFormat != null) {
            final String fileExtension = getExtension(fileName);
            final String newExtension = selectedFormat.getExtension();
            final String lastExtension = lastFormat != null ? lastFormat.getExtension() : "";

            // don't modify user extensions or user entered filenames without extension
            if (fileExtension.equalsIgnoreCase(lastExtension)) {

                if (!lastExtension.equalsIgnoreCase(newExtension)) {
                    final String newFileName
                            = fileName.substring(0, fileName.length() - fileExtension.length())
                            + newExtension;
                    accessor.setSelection(newFileName);
                }
            }
        }
    }

    private NavigationFormat getSelectedFormat(final FileFilter fileFilter) {

        if (fileFilter instanceof NavigationFormatFileFilter)
            return ((NavigationFormatFileFilter) fileFilter).getFormat();

        return null;
    }

    private String getExtension(final String name) {
        final int index = name.lastIndexOf(".");
        if (index == -1)
            return "";
        return name.substring(index, name.length());
    }

    private FileChooserSelectionAccessor getAccessor(final JFileChooser chooser) {

        final FileChooserUI ui = chooser.getUI();

        if (ui instanceof BasicFileChooserUI) {

            // works on Windows
            return new BasicFileChooserSelectionAccessor(ui, chooser);
        }
        else if ("com.apple.laf.AquaFileChooserUI".equals(ui.getClass().getName())) {
            // MAC-UI
            return new AquaFileChooserSelectionAccessor (ui, chooser);
        }
	    else {
            log.log(Level.SEVERE, "Unsupported FileChooserUI: "+ui);
            // maybe need other variants for Linux/Mac
        }

        return null;
    }

    private static final class BasicFileChooserSelectionAccessor implements FileChooserSelectionAccessor {

        private final FileChooserUI ui;
        private final JFileChooser chooser;

        public BasicFileChooserSelectionAccessor(final FileChooserUI ui,
                                                 final JFileChooser chooser) {

            this.ui = ui;
            this.chooser = chooser;
        }

        @Override
        public String getSelection() {
            // don't work: chooser.getSelectedFile()
            // --> returns wrong values (on Windows)

            return ((BasicFileChooserUI) ui).getFileName();
        }

        @Override
        public void setSelection(final String name) {
            // getSelection don't has Folder informations
            // --> setSelectedFile can't use
//            chooser.setSelectedFile(new File(name));

            ((BasicFileChooserUI) ui).setFileName(name);
        }
    }

    private static final class AquaFileChooserSelectionAccessor implements FileChooserSelectionAccessor {

        private final FileChooserUI ui;
        private final JFileChooser chooser;

        public AquaFileChooserSelectionAccessor(final FileChooserUI ui,
                                                final JFileChooser chooser) {

            this.ui = ui;
            this.chooser = chooser;
        }

        @Override
        public String getSelection() {
            try {
                final Method getFileNameMethod = ui.getClass().getDeclaredMethod("getFileName");
                return (String) getFileNameMethod.invoke(ui);
            } catch (InvocationTargetException e) {
                log.log(Level.SEVERE, "Could not call getFileName on ui: "+ui, e);
            } catch (NoSuchMethodException e) {
                log.log(Level.SEVERE, "Could not call getFileName on ui: "+ui, e);
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE, "Could not call getFileName on ui: "+ui, e);
            }
            return "";
        }

        @Override
        public void setSelection(final String name) {

            try {
                final Method setFileNameMethod = ui.getClass().getDeclaredMethod("setFileName", String.class);
                setFileNameMethod.invoke(ui, name);
            } catch (InvocationTargetException e) {
                log.log(Level.SEVERE, "Could not call setFileName on ui: "+ui, e);
            } catch (NoSuchMethodException e) {
                log.log(Level.SEVERE, "Could not call setFileName on ui: "+ui, e);
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE, "Could not call setFileName on ui: "+ui, e);
            }
        }
    }
}
