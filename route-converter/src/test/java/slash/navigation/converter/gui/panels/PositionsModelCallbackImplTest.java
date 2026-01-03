package slash.navigation.converter.gui.panels;

import junit.framework.TestCase;
import slash.common.type.CompactCalendar;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModelCallback;
import slash.navigation.converter.gui.models.TimeZoneModel;
import slash.navigation.converter.gui.models.UnitSystemModel;
import slash.navigation.gpx.GpxPosition;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.mockito.Mockito.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;

public class PositionsModelCallbackImplTest extends TestCase {

    private static final TimeZone ZONE_UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone ZONE_BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    private PositionsModelCallback sut;
    private TimeZoneModel timeZoneModel;
    private NavigationPosition position;
    private Locale originalLocale;

    @Override
    public void setUp() throws Exception {
        timeZoneModel = new TimeZoneModel("unittest."+getClass().getSimpleName(), ZONE_UTC);

        sut = new PositionsModelCallbackImpl(timeZoneModel);
        position = mock(NavigationPosition.class);

        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
    }

    @Override
    public void tearDown() throws Exception {
        Locale.setDefault(originalLocale);
    }

    public void testGetStringNull() {
        timeZoneModel.setTimeZone(ZONE_UTC);
        NavigationPosition position = new GpxPosition(null, null, null, null, null, null);
        assertEquals("", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, DESCRIPTION_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, LONGITUDE_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, LATITUDE_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, ELEVATION_COLUMN_INDEX));
        assertEquals("", sut.getStringAt(position, SPEED_COLUMN_INDEX));
    }

    public void testGetStringUTC() {
        timeZoneModel.setTimeZone(ZONE_UTC);
        NavigationPosition position = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);
        assertEquals("01.02.25, 21:34:56", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.02.25", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("21:34:56", sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals("test", sut.getStringAt(position, DESCRIPTION_COLUMN_INDEX));
//        assertEquals("1.2", sut.getStringAt(position, LONGITUDE_COLUMN_INDEX));
//        assertEquals("3.4", sut.getStringAt(position, LATITUDE_COLUMN_INDEX));
//        assertEquals("5.6 m", sut.getStringAt(position, ELEVATION_COLUMN_INDEX));
//        assertEquals("7.8 km/h", sut.getStringAt(position, SPEED_COLUMN_INDEX));


        position = createTestPosition(2025, 1, 1, 0, 0, 0, ZONE_UTC);
        assertEquals("01.01.25, 00:00:00", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.01.25", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("00:00:00", sut.getStringAt(position, TIME_COLUMN_INDEX));

        position = createTestPosition(1945, 1, 1, 0, 0, 0, ZONE_UTC);
        assertEquals("01.01.45, 00:00:00", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.01.45", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("00:00:00", sut.getStringAt(position, TIME_COLUMN_INDEX));
    }

    public void testGetStringBerlin() {
        timeZoneModel.setTimeZone(ZONE_BERLIN);

        NavigationPosition position = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);
        assertEquals("01.02.25, 22:34:56", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.02.25", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("22:34:56", sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals("test", sut.getStringAt(position, DESCRIPTION_COLUMN_INDEX));
//        assertEquals("1.2", sut.getStringAt(position, LONGITUDE_COLUMN_INDEX));
//        assertEquals("3.4", sut.getStringAt(position, LATITUDE_COLUMN_INDEX));
//        assertEquals("5.6 m", sut.getStringAt(position, ELEVATION_COLUMN_INDEX));
//        assertEquals("7.8 km/h", sut.getStringAt(position, SPEED_COLUMN_INDEX));

        position = createTestPosition(2025, 1, 1, 0, 0, 0, ZONE_UTC);
        assertEquals("01.01.25, 01:00:00", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.01.25", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("01:00:00", sut.getStringAt(position, TIME_COLUMN_INDEX));

        position = createTestPosition(1945, 1, 1, 0, 0, 0, ZONE_UTC);
        assertEquals("01.01.45, 01:00:00", sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals("01.01.45", sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals("01:00:00", sut.getStringAt(position, TIME_COLUMN_INDEX));
    }

    public void testSetSameValueUTC() {
        // wenn man nur in die Zelle klickt und nichts ändert, so darf sich der Wert nicht ändern
        timeZoneModel.setTimeZone(ZONE_UTC);
        NavigationPosition referencePosition = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);
        NavigationPosition position = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DATE_COLUMN_INDEX, sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, TIME_COLUMN_INDEX, sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DESCRIPTION_COLUMN_INDEX, sut.getStringAt(position, DESCRIPTION_COLUMN_INDEX));
        assertEquals(referencePosition, position);

//        sut.setValueAt(position, LONGITUDE_COLUMN_INDEX, sut.getStringAt(position, LONGITUDE_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, LATITUDE_COLUMN_INDEX, sut.getStringAt(position, LATITUDE_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, ELEVATION_COLUMN_INDEX, sut.getStringAt(position, ELEVATION_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, SPEED_COLUMN_INDEX, sut.getStringAt(position, SPEED_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
    }

    public void testSetSameValueUTC_1945() {
        // wenn man nur in die Zelle klickt und nichts ändert, so darf sich der Wert nicht ändern
        timeZoneModel.setTimeZone(ZONE_UTC);
        NavigationPosition referencePosition = createTestPosition(1945, 2, 1, 21, 34, 56, ZONE_UTC);
        NavigationPosition position = createTestPosition(1945, 2, 1, 21, 34, 56, ZONE_UTC);

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DATE_COLUMN_INDEX, sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, TIME_COLUMN_INDEX, sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);
    }

    public void testSetSameValueBerlin() {
        // wenn man nur in die Zelle klickt und nichts ändert, so darf sich der Wert nicht ändern
        timeZoneModel.setTimeZone(ZONE_BERLIN);
        NavigationPosition referencePosition = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);
        NavigationPosition position = createTestPosition(2025, 2, 1, 21, 34, 56, ZONE_UTC);

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DATE_COLUMN_INDEX, sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, TIME_COLUMN_INDEX, sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DESCRIPTION_COLUMN_INDEX, sut.getStringAt(position, DESCRIPTION_COLUMN_INDEX));
        assertEquals(referencePosition, position);

//        sut.setValueAt(position, LONGITUDE_COLUMN_INDEX, sut.getStringAt(position, LONGITUDE_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, LATITUDE_COLUMN_INDEX, sut.getStringAt(position, LATITUDE_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, ELEVATION_COLUMN_INDEX, sut.getStringAt(position, ELEVATION_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
//
//        sut.setValueAt(position, SPEED_COLUMN_INDEX, sut.getStringAt(position, SPEED_COLUMN_INDEX));
//        assertEquals(referencePosition, position);
    }

    public void testSetSameValueBERLIN_1945() {
        // wenn man nur in die Zelle klickt und nichts ändert, so darf sich der Wert nicht ändern
        timeZoneModel.setTimeZone(ZONE_BERLIN);
        NavigationPosition referencePosition = createTestPosition(1945, 2, 1, 21, 34, 56, ZONE_UTC);
        NavigationPosition position = createTestPosition(1945, 2, 1, 21, 34, 56, ZONE_UTC);

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, sut.getStringAt(position, DATE_TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, DATE_COLUMN_INDEX, sut.getStringAt(position, DATE_COLUMN_INDEX));
        assertEquals(referencePosition, position);

        sut.setValueAt(position, TIME_COLUMN_INDEX, sut.getStringAt(position, TIME_COLUMN_INDEX));
        assertEquals(referencePosition, position);
    }


    public void testSetDateUTC() {
        timeZoneModel.setTimeZone(ZONE_UTC);
        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, DATE_COLUMN_INDEX, "01.01.25");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.2025", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.2025", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.25", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "10.8.23", cal(2023, 8, 10, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1998", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.98", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.98", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1945", cal(1945, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.45", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.45", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, null, null);
        runSetTimeTestStep(DATE_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist ==> dann wird 00:00:00 in der Eingabe-Zeitzone angenommen
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.25", cal(2025, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.2025", cal(2025, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.2025", cal(2025, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.25", cal(2025, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "10.8.23", cal(2023, 8, 10, 0, 0, 0, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1998", cal(1998, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.98", cal(1998, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.98", cal(1998, 1, 1, 0, 0, 0, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1945", cal(1945, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.45", cal(2045, 1, 1, 0, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.45", cal(2045, 1, 1, 0, 0, 0, ZONE_UTC));
    }

    public void testSetDateBerlin() {
        timeZoneModel.setTimeZone(ZONE_BERLIN);
        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, DATE_COLUMN_INDEX, "01.01.25");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.2025", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.2025", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.25", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        // Wert in der Sommerzeit
        runSetTimeTestStep(DATE_COLUMN_INDEX, "10.8.23", cal(2023, 8, 10, 0, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1998", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.98", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.98", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.1945", cal(1945, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "01.01.45", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "1.1.45", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, null, null);
        runSetTimeTestStep(DATE_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist ==> dann wird 00:00:00 in der Eingabe-Zeitzone angenommen
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(DATE_COLUMN_INDEX, "02.01.25", cal(2025, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "02.01.2025", cal(2025, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.2025", cal(2025, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.25", cal(2025, 1, 1, 23, 0, 0, ZONE_UTC));

        // Sommerzeit
        runSetTimeTestStep(DATE_COLUMN_INDEX, "10.8.23", cal(2023, 8, 9, 22, 0, 0, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.1998", cal(1998, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "02.01.98", cal(1998, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.98", cal(1998, 1, 1, 23, 0, 0, ZONE_UTC));

        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.1945", cal(1945, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "02.01.45", cal(2045, 1, 1, 23, 0, 0, ZONE_UTC));
        runSetTimeTestStep(DATE_COLUMN_INDEX, "2.1.45", cal(2045, 1, 1, 23, 0, 0, ZONE_UTC));
    }


    public void testSetTimeUtc() {
        timeZoneModel.setTimeZone(ZONE_UTC);
        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, TIME_COLUMN_INDEX, "01:02:03");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(2025, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(2025, 1, 1, 16, 06, 18, ZONE_UTC));

        // in der Sommerzeit (auch wenn UTC das nicht kennt)
        when(position.getTime()).thenReturn(cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(2025, 8, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(2025, 8, 1, 16, 06, 18, ZONE_UTC));


        runSetTimeTestStep(TIME_COLUMN_INDEX, null, null);
        runSetTimeTestStep(TIME_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist ==> dann wird 1.1.1970 als Datum genommen
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(1970, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(1970, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(1970, 1, 1, 16, 06, 18, ZONE_UTC));
    }

    public void testSetTimeBerlin() {
        timeZoneModel.setTimeZone(ZONE_BERLIN);
        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, TIME_COLUMN_INDEX, "02:02:03");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(2025, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(2025, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(2025, 1, 1, 15, 06, 18, ZONE_UTC));

        // in der Sommerzeit
        when(position.getTime()).thenReturn(cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, TIME_COLUMN_INDEX, "03:02:03");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(2025, 7, 31, 23, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(2025, 8, 1, 13, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(2025, 8, 1, 14, 06, 18, ZONE_UTC));

        // TODO - Zeiten, die es genau zur Zeitumstellung nicht oder mehrfach gibt


        runSetTimeTestStep(TIME_COLUMN_INDEX, null, null);
        runSetTimeTestStep(TIME_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist ==> dann wird 1.1.1970 als Datum genommen
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(TIME_COLUMN_INDEX, "1:2:3", cal(1970, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:25:35", cal(1970, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(TIME_COLUMN_INDEX, "15:65:78", cal(1970, 1, 1, 15, 06, 18, ZONE_UTC));
    }

    public void testSetDateTimeUtc() {
        timeZoneModel.setTimeZone(ZONE_UTC);

        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, "01.01.25, 01:02:03");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:25:35", cal(2025, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:65:78", cal(2025, 1, 1, 16, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:25:35", cal(2025, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:65:78", cal(2025, 1, 1, 16, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.45, 1:2:3", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.1945, 1:2:3", cal(1945, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.98, 1:2:3", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));

        // in der Sommerzeit
        when(position.getTime()).thenReturn(cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 1:2:3", cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:25:35", cal(2025, 8, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:65:78", cal(2025, 8, 1, 16, 06, 18, ZONE_UTC));

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, null, null);
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:25:35", cal(2025, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:65:78", cal(2025, 1, 1, 16, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.25, 1:2:3", cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:25:35", cal(2025, 1, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:65:78", cal(2025, 1, 1, 16, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.45, 1:2:3", cal(2045, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.1945, 1:2:3", cal(1945, 1, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.98, 1:2:3", cal(1998, 1, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 1:2:3", cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:25:35", cal(2025, 8, 1, 15, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:65:78", cal(2025, 8, 1, 16, 06, 18, ZONE_UTC));
    }

    public void testSetDateTimeBerlin() {
        timeZoneModel.setTimeZone(ZONE_BERLIN);

        when(position.getTime()).thenReturn(cal(2025, 1, 1, 1, 2, 3, ZONE_UTC));

        sut.setValueAt(position, DATE_TIME_COLUMN_INDEX, "01.01.25, 02:02:03");
        verify(position, atLeast(1)).getTime();
        verifyNoMoreInteractions(position);
        clearInvocations(position);

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:25:35", cal(2025, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:65:78", cal(2025, 1, 1, 15, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:25:35", cal(2025, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:65:78", cal(2025, 1, 1, 15, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.45, 1:2:3", cal(2045, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.1945, 1:2:3", cal(1945, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.98, 1:2:3", cal(1998, 1, 1, 0, 2, 3, ZONE_UTC));

        // in der Sommerzeit (auch wenn UTC das nicht kennt)
        when(position.getTime()).thenReturn(cal(2025, 8, 1, 1, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 1:2:3", cal(2025, 7, 31, 23, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:25:35", cal(2025, 8, 1, 13, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:65:78", cal(2025, 8, 1, 14, 06, 18, ZONE_UTC));

        // TODO - Zeiten, die es genau zur Zeitumstellung nicht oder mehrfach gibt

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, null, null);
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "", null);

        // Variante, wenn noch kein Wert gesetzt ist
        when(position.getTime()).thenReturn(null);

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:25:35", cal(2025, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "01.01.25, 15:65:78", cal(2025, 1, 1, 15, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.25, 1:2:3", cal(2025, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:25:35", cal(2025, 1, 1, 14, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.2025, 15:65:78", cal(2025, 1, 1, 15, 06, 18, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.45, 1:2:3", cal(2045, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.1945, 1:2:3", cal(1945, 1, 1, 0, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.1.98, 1:2:3", cal(1998, 1, 1, 0, 2, 3, ZONE_UTC));

        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 1:2:3", cal(2025, 7, 31, 23, 2, 3, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:25:35", cal(2025, 8, 1, 13, 25, 35, ZONE_UTC));
        runSetTimeTestStep(DATE_TIME_COLUMN_INDEX, "1.8.2025, 15:65:78", cal(2025, 8, 1, 14, 06, 18, ZONE_UTC));
    }

    private void runSetTimeTestStep(int column, String toSet, CompactCalendar expectedSet) {
        sut.setValueAt(position, column, toSet);
        verify(position, atLeast(1)).getTime();
        verify(position).setTime(expectedSet);
        verifyNoMoreInteractions(position);
        clearInvocations(position);
    }

    private static NavigationPosition createTestPosition(int year, int month, int day,
                                                         int hour, int minute, int second,
                                                         TimeZone timeZone) {

        CompactCalendar calendar = cal(year, month, day, hour, minute, second, timeZone);
        return new GpxPosition(1.2, 3.4, 5.6, 7.8, calendar, "test");
    }

    private static CompactCalendar cal(int year, int month, int day, int hour, int minute, int second, TimeZone timeZone) {
        Calendar c = Calendar.getInstance(timeZone);
        c.clear(); // prevents "remnants" such as milliseconds
        c.set(year, month - 1, day, hour, minute, second);
        return CompactCalendar.fromCalendar(c);
    }
}