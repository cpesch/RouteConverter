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

package slash.navigation.converter.gui.models;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.photo.PhotoPosition;
import slash.navigation.photo.TagState;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.text.MessageFormat;

import static slash.navigation.photo.TagState.NotTaggable;
import static slash.navigation.photo.TagState.Taggable;
import static slash.navigation.photo.TagState.Tagged;

/**
 * A bidirectional adapter that counts the number of photos
 * for the {@link TagState}s.
 *
 * @author Christian Pesch
 */

public class PhotoTagStateToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private final JLabel label;

    public PhotoTagStateToJLabelAdapter(PositionsModel positionsModel, JLabel label) {
        super(positionsModel);
        this.label = label;
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private Integer count(TagState tagState) {
        int count = 0;
        for (int i = 0, c = getDelegate().getRowCount(); i < c; i++) {

            PhotoPosition position = PhotoPosition.class.cast(getDelegate().getPosition(i));
            if (tagState.equals(position.getTagState()))
                count++;
        }
        return count;
    }

    protected void updateAdapterFromDelegate(TableModelEvent event) {
        String text = MessageFormat.format(RouteConverter.getBundle().getString("photos-tagstate"),
                count(Tagged),
                count(Taggable),
                count(NotTaggable)
        );
        if(text.length() == 0)
            text = "-";
        label.setText(text);
    }
}