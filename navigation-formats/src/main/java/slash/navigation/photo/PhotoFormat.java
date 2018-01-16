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

package slash.navigation.photo;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffDirectory;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.Directory;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoRational;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import static java.io.File.createTempFile;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static java.util.Collections.singletonList;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_EXIF_IMAGE_LENGTH;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_EXIF_IMAGE_WIDTH;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_EXPOSURE_TIME;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_FLASH;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_FNUMBER;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_FOCAL_LENGTH;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_ISO;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_USER_COMMENT;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_ALTITUDE;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_DATE_STAMP;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_DOP;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_LATITUDE;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_LONGITUDE;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_MEASURE_MODE;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_MEASURE_MODE_VALUE_2_DIMENSIONAL_MEASUREMENT;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_MEASURE_MODE_VALUE_3_DIMENSIONAL_MEASUREMENT;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SATELLITES;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SPEED;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SPEED_REF;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SPEED_REF_VALUE_KMPH;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SPEED_REF_VALUE_KNOTS;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_SPEED_REF_VALUE_MPH;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_TIME_STAMP;
import static org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_VERSION_ID;
import static org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryConstants.DIRECTORY_TYPE_EXIF;
import static org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryConstants.DIRECTORY_TYPE_GPS;
import static org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryConstants.DIRECTORY_TYPE_ROOT;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.TIFF_TAG_DATE_TIME;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.TIFF_TAG_MAKE;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.TIFF_TAG_MODEL;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.InputOutput.copyAndClose;
import static slash.common.io.Transfer.parseInteger;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.common.type.ISO8601.formatDate;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.common.UnitConversion.nauticMilesToKiloMeter;
import static slash.navigation.common.UnitConversion.statuteMilesToKiloMeter;
import static slash.navigation.photo.TagState.NotTaggable;
import static slash.navigation.photo.TagState.Tagged;

/**
 * Reads a route with a position from Photo (.jpg) files with embedded EXIF and GPS metadata.
 *
 * @author Christian Pesch
 */
public class PhotoFormat extends SimpleFormat<Wgs84Route> {
    private static final Logger log = Logger.getLogger(PhotoFormat.class.getName());
    private static final String DATE_FORMAT = "yyyy:MM:dd";
    private static final String DATE_TIME_FORMAT = "yyyy:MM:dd HH:mm:ss";
    private static final DecimalFormat XX_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat XXXX_FORMAT = new DecimalFormat("0000");
    private static final int READ_BUFFER_SIZE = 64 * 1024;

    public String getName() {
        return "Photo (" + getExtension() + ")";
    }

    public String getExtension() {
        return ".jpg";
    }

    public int getMaximumPositionCount() {
        return 1;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws Exception {
        BufferedInputStream bufferedSource = new BufferedInputStream(source, READ_BUFFER_SIZE);
        bufferedSource.mark(READ_BUFFER_SIZE);

        Dimension size = Imaging.getImageSize(bufferedSource, null);
        if (size == null)
            return;

        PhotoPosition position = new PhotoPosition(NotTaggable, context.getStartDate(), "No EXIF data", null);

        bufferedSource.reset();
        ImageMetadata metadata = Imaging.getMetadata(bufferedSource, null);
        TiffImageMetadata tiffImageMetadata = extractTiffImageMetadata(metadata);
        if (tiffImageMetadata != null) {
            @SuppressWarnings("unchecked")
            List<Directory> directories = (List<Directory>) tiffImageMetadata.getDirectories();
            for (Directory directory : directories)
                log.info("Reading EXIF directory " + directory);

            extendPosition(position, tiffImageMetadata, context.getStartDate());
        }

        bufferedSource.reset();
        File image = context.getFile();
        if (image == null)
            image = extractToTempFile(bufferedSource);
        position.setOrigin(image);
        position.setWaypointType(Photo);
        context.appendRoute(new Wgs84Route(this, Waypoints, new ArrayList<Wgs84Position>(singletonList(position))));
    }

    private TiffImageMetadata extractTiffImageMetadata(ImageMetadata metadata) {
        TiffImageMetadata result = null;
        if (metadata instanceof JpegImageMetadata)
            result = ((JpegImageMetadata) metadata).getExif();
        else if (metadata instanceof TiffImageMetadata)
            result = (TiffImageMetadata) metadata;
        return result;
    }

    private File extractToTempFile(InputStream inputStream) throws IOException {
        File temp = createTempFile("photo", ".jpg", getTemporaryDirectory());
        temp.deleteOnExit();
        copyAndClose(inputStream, new FileOutputStream(temp));
        return temp;
    }

    private String parseRoot(TiffImageMetadata metadata, TagInfo tag) throws ImageReadException {
        TiffDirectory rootDirectory = metadata.findDirectory(DIRECTORY_TYPE_ROOT);
        return rootDirectory != null ? trim((String) rootDirectory.getFieldValue(tag)) : null;
    }

    private String parseMake(TiffImageMetadata metadata) throws ImageReadException {
        return parseRoot(metadata, TIFF_TAG_MAKE);
    }

    private String parseModel(TiffImageMetadata metadata) throws ImageReadException {
        return parseRoot(metadata, TIFF_TAG_MODEL);
    }

    private CompactCalendar parseDateTime(TiffImageMetadata metadata) throws ImageReadException {
        return parseDate(parseRoot(metadata, TIFF_TAG_DATE_TIME), DATE_TIME_FORMAT);
    }

    private String parseExif(TiffImageMetadata metadata, TagInfo tag) throws ImageReadException {
        TiffDirectory exifDirectory = metadata.findDirectory(DIRECTORY_TYPE_EXIF);
        return exifDirectory != null ? trim((String) exifDirectory.getFieldValue(tag)) : null;
    }

    private Integer parseExifInteger(TiffImageMetadata metadata, TagInfo tag) throws ImageReadException {
        TiffDirectory exifDirectory = metadata.findDirectory(DIRECTORY_TYPE_EXIF);
        if (exifDirectory == null)
            return null;
        Object fieldValue = exifDirectory.getFieldValue(tag);
        return fieldValue != null ? parseInt(fieldValue.toString()) : null;
    }

    private RationalNumber parseExifRationalNumber(TiffImageMetadata metadata, TagInfo tag) throws ImageReadException {
        TiffDirectory exifDirectory = metadata.findDirectory(DIRECTORY_TYPE_EXIF);
        if (exifDirectory == null)
            return null;
        return (RationalNumber) exifDirectory.getFieldValue(tag);
    }

    private String parseUserComment(TiffImageMetadata metadata) throws ImageReadException {
        TiffDirectory exifDirectory = metadata.findDirectory(DIRECTORY_TYPE_EXIF);
        if (exifDirectory != null) {
            String userComment = parseExif(metadata, EXIF_TAG_USER_COMMENT);
            if (userComment != null)
                return userComment;
        }

        TiffDirectory rootDirectory = metadata.findDirectory(DIRECTORY_TYPE_ROOT);
        if (rootDirectory != null) {
            String make = parseMake(metadata);
            String model = parseModel(metadata);
            CompactCalendar dateTime = parseDateTime(metadata);
            return trim((make != null ? make : "") + " " + (model != null ? model : "") + " Photo" + (dateTime != null ? " from " + formatDate(dateTime) : ""));
        }
        return "GPS";
    }

    private CompactCalendar parseExifTime(TiffImageMetadata metadata, CompactCalendar startDate) throws ImageReadException {
        String dateString = null;
        TiffDirectory exifDirectory = metadata.findDirectory(DIRECTORY_TYPE_EXIF);
        if (exifDirectory != null) {
            dateString = parseExif(metadata, EXIF_TAG_DATE_TIME_ORIGINAL);
            if (dateString == null)
                dateString = parseExif(metadata, EXIF_TAG_DATE_TIME_DIGITIZED);
        }
        TiffDirectory rootDirectory = metadata.findDirectory(DIRECTORY_TYPE_ROOT);
        if (rootDirectory != null && dateString == null)
            dateString = (String) rootDirectory.getFieldValue(TIFF_TAG_DATE_TIME);
        return dateString != null ? parseDate(dateString, DATE_TIME_FORMAT) : startDate;
    }

    private RationalNumber getFieldValue(TiffDirectory directory, TagInfoRational tagInfoRational) {
        try {
            return directory.getFieldValue(tagInfoRational);
        } catch (ImageReadException e) {
            log.fine("Could not parse " + tagInfoRational + ": " + getLocalizedMessage(e));
            return null;
        }
    }

    private Double parseAltitude(TiffDirectory directory) throws ImageReadException {
        RationalNumber altitudeNumber = getFieldValue(directory, GPS_TAG_GPS_ALTITUDE);
        if (altitudeNumber == null)
            return null;

        double altitude = altitudeNumber.doubleValue();
        Byte altitudeRef = directory.getFieldValue(GPS_TAG_GPS_ALTITUDE_REF);
        return altitudeRef == 1 ? -altitude : altitude;
    }

    private Double parseSpeed(TiffDirectory directory) throws ImageReadException {
        RationalNumber speedNumber = getFieldValue(directory, GPS_TAG_GPS_SPEED);
        if (speedNumber == null)
            return null;

        double speed = speedNumber.doubleValue();
        String speedRef = (String) directory.getFieldValue(GPS_TAG_GPS_SPEED_REF);
        if (speedRef != null) {
            if (GPS_TAG_GPS_SPEED_REF_VALUE_MPH.equals(speedRef))
                speed = statuteMilesToKiloMeter(speed);
            else if (GPS_TAG_GPS_SPEED_REF_VALUE_KNOTS.equals(speedRef))
                speed = nauticMilesToKiloMeter(speed);
        }
        return speed;
    }

    private CompactCalendar parseGPSTime(TiffDirectory directory, CompactCalendar startDate) throws ImageReadException {
        String dateString = (String) directory.getFieldValue(GPS_TAG_GPS_DATE_STAMP);
        CompactCalendar date = dateString != null ? parseDate(dateString, DATE_FORMAT) : startDate;
        RationalNumber[] timeStamp = (RationalNumber[]) directory.getFieldValue(GPS_TAG_GPS_TIME_STAMP);
        if (timeStamp != null) {
            Calendar calendar = date.getCalendar();
            calendar.set(HOUR, timeStamp[0].intValue());
            calendar.set(MINUTE, timeStamp[1].intValue());
            calendar.set(SECOND, timeStamp[2].intValue());
            date = fromCalendar(calendar);
        }
        return date;
    }

    private Double parseDirection(TiffDirectory directory) {
        RationalNumber direction = getFieldValue(directory, GPS_TAG_GPS_IMG_DIRECTION);
        return direction != null ? direction.doubleValue() : null;
    }

    private Integer parseSatellites(TiffDirectory directory) throws ImageReadException {
        String satellites = (String) directory.getFieldValue(GPS_TAG_GPS_SATELLITES);
        return satellites != null ? parseInteger(satellites) : null;
    }

    private boolean is2DimensionalMeasurement(TiffDirectory directory) throws ImageReadException {
        String measurementMode = (String) directory.getFieldValue(GPS_TAG_GPS_MEASURE_MODE);
        return measurementMode != null && measurementMode.equals(Integer.toString(GPS_TAG_GPS_MEASURE_MODE_VALUE_2_DIMENSIONAL_MEASUREMENT));
    }

    private Double parseDOP(TiffDirectory directory) {
        RationalNumber dop = getFieldValue(directory, GPS_TAG_GPS_DOP);
        return dop != null ? dop.doubleValue() : null;
    }

    private void extendPosition(PhotoPosition position, TiffImageMetadata metadata, CompactCalendar startDate) throws ImageReadException {
        TiffImageMetadata.GPSInfo gpsInfo = metadata.getGPS();
        if (gpsInfo != null) {
            position.setLongitude(gpsInfo.getLongitudeAsDegreesEast());
            position.setLatitude(gpsInfo.getLatitudeAsDegreesNorth());
            position.setTagState(Tagged);
        }

        CompactCalendar time = parseExifTime(metadata, startDate);
        TiffDirectory gpsDirectory = metadata.findDirectory(DIRECTORY_TYPE_GPS);
        if (gpsDirectory != null) {
            position.setElevation(parseAltitude(gpsDirectory));
            position.setSpeed(parseSpeed(gpsDirectory));
            time = parseGPSTime(gpsDirectory, time);

            position.setHeading(parseDirection(gpsDirectory));
            position.setSatellites(parseSatellites(gpsDirectory));
            Double dop = parseDOP(gpsDirectory);
            if (is2DimensionalMeasurement(gpsDirectory))
                position.setHdop(dop);
            else
                position.setPdop(dop);
        }
        position.setTime(time);

        position.setDescription(parseUserComment(metadata));
        position.setMake(parseMake(metadata));
        position.setModel(parseModel(metadata));
        position.setWidth(parseExifInteger(metadata, EXIF_TAG_EXIF_IMAGE_WIDTH));
        position.setHeight(parseExifInteger(metadata, EXIF_TAG_EXIF_IMAGE_LENGTH));
        position.setfNumber(parseExifRationalNumber(metadata, EXIF_TAG_FNUMBER));
        position.setExposure(parseExifRationalNumber(metadata, EXIF_TAG_EXPOSURE_TIME));
        position.setFlash(parseExifInteger(metadata, EXIF_TAG_FLASH));
        position.setFocal(parseExifRationalNumber(metadata, EXIF_TAG_FOCAL_LENGTH));
        position.setPhotographicSensitivity(parseExifInteger(metadata, EXIF_TAG_ISO));
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            PhotoPosition position = (PhotoPosition) positions.get(i);
            File source = position.getOrigin(File.class);
            if (source != null)
                write(position, source, target);
        }
    }

    public void write(PhotoPosition position, File source, OutputStream target) throws IOException {
        try {
            ImageMetadata metadata = Imaging.getMetadata(source);
            TiffImageMetadata tiffImageMetadata = extractTiffImageMetadata(metadata);

            TiffOutputSet outputSet = null;
            if (tiffImageMetadata != null)
                outputSet = tiffImageMetadata.getOutputSet();
            if (outputSet == null)
                outputSet = new TiffOutputSet();

            TiffOutputDirectory rootDirectory = outputSet.getOrCreateRootDirectory();
            rootDirectory.removeField(TIFF_TAG_MAKE);
            rootDirectory.removeField(TIFF_TAG_MODEL);

            rootDirectory.add(TIFF_TAG_MAKE, position.getMake());
            rootDirectory.add(TIFF_TAG_MODEL, position.getModel());

            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
            exifDirectory.removeField(EXIF_TAG_USER_COMMENT);
            exifDirectory.removeField(EXIF_TAG_EXIF_IMAGE_WIDTH);
            exifDirectory.removeField(EXIF_TAG_EXIF_IMAGE_LENGTH);
            exifDirectory.removeField(EXIF_TAG_FNUMBER);
            exifDirectory.removeField(EXIF_TAG_EXPOSURE_TIME);
            exifDirectory.removeField(EXIF_TAG_FLASH);
            exifDirectory.removeField(EXIF_TAG_FOCAL_LENGTH);
            exifDirectory.removeField(EXIF_TAG_ISO);

            exifDirectory.add(EXIF_TAG_USER_COMMENT, position.getDescription());
            if (position.getWidth() != null)
                exifDirectory.add(EXIF_TAG_EXIF_IMAGE_WIDTH, position.getWidth().shortValue());
            if (position.getHeight() != null)
                exifDirectory.add(EXIF_TAG_EXIF_IMAGE_LENGTH, position.getHeight().shortValue());
            exifDirectory.add(EXIF_TAG_FNUMBER, position.getfNumber());
            exifDirectory.add(EXIF_TAG_EXPOSURE_TIME, position.getExposure());
            if (position.getFlash() != null)
                exifDirectory.add(EXIF_TAG_FLASH, position.getFlash().shortValue());
            exifDirectory.add(EXIF_TAG_FOCAL_LENGTH, position.getFocal());
            if (position.getPhotographicSensitivity() != null)
                exifDirectory.add(EXIF_TAG_ISO, position.getPhotographicSensitivity().shortValue());

            TiffOutputDirectory gpsDirectory = outputSet.getOrCreateGPSDirectory();
            gpsDirectory.removeField(GPS_TAG_GPS_VERSION_ID);
            gpsDirectory.add(GPS_TAG_GPS_VERSION_ID, (byte) 2, (byte) 3, (byte) 0, (byte) 0);

            gpsDirectory.removeField(GPS_TAG_GPS_LONGITUDE);
            gpsDirectory.removeField(GPS_TAG_GPS_LONGITUDE_REF);
            gpsDirectory.removeField(GPS_TAG_GPS_LATITUDE);
            gpsDirectory.removeField(GPS_TAG_GPS_LATITUDE_REF);
            gpsDirectory.removeField(GPS_TAG_GPS_ALTITUDE);
            gpsDirectory.removeField(GPS_TAG_GPS_ALTITUDE_REF);
            gpsDirectory.removeField(GPS_TAG_GPS_SPEED);
            gpsDirectory.removeField(GPS_TAG_GPS_SPEED_REF);
            gpsDirectory.removeField(GPS_TAG_GPS_DATE_STAMP);
            gpsDirectory.removeField(GPS_TAG_GPS_TIME_STAMP);
            gpsDirectory.removeField(GPS_TAG_GPS_IMG_DIRECTION);
            gpsDirectory.removeField(GPS_TAG_GPS_SATELLITES);
            gpsDirectory.removeField(GPS_TAG_GPS_MEASURE_MODE);
            gpsDirectory.removeField(GPS_TAG_GPS_DOP);

            if (position.getLongitude() != null && position.getLatitude() != null)
                outputSet.setGPSInDegrees(position.getLongitude(), position.getLatitude());

            if (position.getElevation() != null) {
                gpsDirectory.add(GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(abs(position.getElevation())));
                gpsDirectory.add(GPS_TAG_GPS_ALTITUDE_REF, (byte) (position.getElevation() > 0 ? 0 : 1));
            }

            if (position.getSpeed() != null) {
                gpsDirectory.add(GPS_TAG_GPS_SPEED, RationalNumber.valueOf(position.getSpeed()));
                gpsDirectory.add(GPS_TAG_GPS_SPEED_REF, GPS_TAG_GPS_SPEED_REF_VALUE_KMPH);
            }

            if (position.getTime() != null) {
                Calendar calendar = position.getTime().getCalendar();

                gpsDirectory.add(GPS_TAG_GPS_TIME_STAMP,
                        RationalNumber.valueOf(calendar.get(HOUR_OF_DAY)),
                        RationalNumber.valueOf(calendar.get(MINUTE)),
                        RationalNumber.valueOf(calendar.get(SECOND)));
                String dateStamp = XXXX_FORMAT.format(calendar.get(YEAR)) + ":" +
                        XX_FORMAT.format(calendar.get(MONTH) + 1) + ":" +
                        XX_FORMAT.format(calendar.get(DAY_OF_MONTH));
                gpsDirectory.add(GPS_TAG_GPS_DATE_STAMP, dateStamp);
            }

            if (position.getHeading() != null)
                gpsDirectory.add(GPS_TAG_GPS_IMG_DIRECTION, RationalNumber.valueOf(position.getHeading()));

            if (position.getSatellites() != null)
                gpsDirectory.add(GPS_TAG_GPS_SATELLITES, position.getSatellites().toString());

            if (position.getPdop() != null) {
                gpsDirectory.add(GPS_TAG_GPS_MEASURE_MODE, Integer.toString(GPS_TAG_GPS_MEASURE_MODE_VALUE_3_DIMENSIONAL_MEASUREMENT));
                gpsDirectory.add(GPS_TAG_GPS_DOP, RationalNumber.valueOf(position.getPdop()));
            } else if (position.getHdop() != null) {
                gpsDirectory.add(GPS_TAG_GPS_MEASURE_MODE, Integer.toString(GPS_TAG_GPS_MEASURE_MODE_VALUE_2_DIMENSIONAL_MEASUREMENT));
                gpsDirectory.add(GPS_TAG_GPS_DOP, RationalNumber.valueOf(position.getHdop()));
            }

            new ExifRewriter().updateExifMetadataLossless(source, target, outputSet);
        } catch (ImageReadException | ImageWriteException e) {
            throw new IOException(e);
        }
    }
}
