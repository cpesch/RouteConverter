package slash.navigation.converter.gui.panels;

import slash.navigation.base.NavigationFormat;
import slash.navigation.converter.gui.helper.NavigationFormatFileFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: Thomas
 * Date: 28.12.11
 * Time: 10:27
 * <p/>
 */
final class UpdateFileNameListener implements PropertyChangeListener {

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

        // works on Windows
        // maybe need own Variant for Linux/Mac
        if (ui instanceof BasicFileChooserUI) {

            return new BasicFileChooserSelectionAccessor(ui, chooser);
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
}
