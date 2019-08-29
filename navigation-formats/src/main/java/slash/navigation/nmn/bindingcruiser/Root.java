package slash.navigation.nmn.bindingcruiser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Root {
    private Route route;

    public Root(Route route) {
        this.route = route;
    }

    Root() {
        // for deserialization
    }

    public Route getRoute() {
        return route;
    }
}
