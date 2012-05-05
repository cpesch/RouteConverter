package slash.navigation.googlemaps;

import slash.navigation.googlemaps.elevation.ElevationResponse;
import slash.navigation.googlemaps.geocode.GeocodeResponse;
import slash.navigation.jaxb.JaxbUtils;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static slash.navigation.jaxb.JaxbUtils.newContext;

public class GoogleMapsUtil {
    private static Unmarshaller newUnmarshallerElevation() {
        return JaxbUtils.newUnmarshaller(newContext(slash.navigation.googlemaps.elevation.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshallerGeocode() {
        return JaxbUtils.newUnmarshaller(newContext(slash.navigation.googlemaps.geocode.ObjectFactory.class));
    }

    private static ElevationResponse unmarshalElevation(StringReader reader) throws JAXBException {
        ElevationResponse result = null;
        try {
            result = (ElevationResponse) newUnmarshallerElevation().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        } finally {
            reader.close();
        }
        return result;
    }

    public static ElevationResponse unmarshalElevation(String string) throws JAXBException {
        return unmarshalElevation(new StringReader(string));
    }

    private static GeocodeResponse unmarshalGeocode(StringReader reader) throws JAXBException {
        GeocodeResponse result = null;
        try {
            result = (GeocodeResponse) newUnmarshallerGeocode().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        } finally {
            reader.close();
        }
        return result;
    }

    public static GeocodeResponse unmarshalGeocode(String string) throws JAXBException {
        return unmarshalGeocode(new StringReader(string));
    }
}
