/**
 * Context Menu for Google Maps
 *
 * based on http://gmaps-samples-v3.googlecode.com/svn/trunk/latlng-to-coord-control/latlng-to-coord-control.html
 */
function LatLngControl(map) {
    /**
     * Offset the control container from the mouse by this amount.
     */
    this.ANCHOR_OFFSET_ = new google.maps.Point(16, 16);

    /**
     * Pointer to the HTML container.
     */
    this.node_ = this.createHtmlNode_();

    // Add control to the map. Position is irrelevant.
    map.controls[google.maps.ControlPosition.TOP].push(this.node_);

    // Bind this OverlayView to the map so we can access MapCanvasProjection
    // to convert LatLng to Point coordinates.
    this.setMap(map);

    // Register an MVC property to indicate whether this custom control
    // is visible or hidden. Initially hide control until mouse is over map.
    this.set('visible', false);

    this.degreeFormat = 'Degrees';
}

// Extend OverlayView so we can access MapCanvasProjection.
LatLngControl.prototype = new google.maps.OverlayView();
LatLngControl.prototype.draw = function () {
};

/**
 * @private
 * Helper function creates the HTML node which is the control container.
 * @return {HTMLDivElement}
 */
LatLngControl.prototype.createHtmlNode_ = function () {
    var divNode = document.createElement('div');
    divNode.id = 'LatLngControl';
    divNode.index = 100;
    return divNode;
};

/**
 * MVC property's state change handler function to show/hide the
 * control container.
 */
LatLngControl.prototype.visible_changed = function () {
    this.node_.style.display = this.get('visible') ? '' : 'none';
};

LatLngControl.prototype.setDegreeFormat = function (degreeFormat) {
    this.degreeFormat = degreeFormat;
};

function coordinate2ddmm(coordinate, positive, negative) {
    var absolute = Math.abs(coordinate);
    var dd = Math.floor(absolute);
    var mm = (absolute - dd) * 60;
    return (coordinate >= 0 ? positive : negative) + " " + dd.toFixed(0) + "&deg; " + mm.toFixed(3) + "'";
}

function coordinate2ddmmss(coordinate, positive, negative) {
    var absolute = Math.abs(coordinate);
    var dd = Math.floor(absolute);
    var minutes = (absolute - dd) * 60;
    var mm = Math.floor(minutes);
    var sss = (minutes - mm) * 60;
    if (Math.round(sss) == 60.0) {
        mm++;
        sss = 0;
    }
    if (Math.round(mm) == 60.0) {
        dd++;
        mm = 0;
    }
    return (coordinate >= 0 ? positive : negative) + " " + dd.toFixed(0) + "&deg; " + mm.toFixed(0) + "' " + sss.toFixed(3) + "&quot;";
}

/**
 * Specified LatLng value is used to calculate pixel coordinates and
 * update the control display. Container is also repositioned.
 * @param {google.maps.LatLng} latLng Position to display
 */
LatLngControl.prototype.updatePosition = function (latLng) {
    var projection = this.getProjection();
    var point = projection.fromLatLngToContainerPixel(latLng);

    // Update control position to be anchored next to mouse position.
    this.node_.style.left = point.x + this.ANCHOR_OFFSET_.x + 'px';
    this.node_.style.top = point.y + this.ANCHOR_OFFSET_.y + 'px';

    switch(this.degreeFormat) {
        case 'Degrees':
            longitude = latLng.lng().toFixed(7);
            latitude = latLng.lat().toFixed(7);
            break;
        case 'Degrees_Minutes':
            longitude = coordinate2ddmm(latLng.lng(), 'E', 'W');
            latitude = coordinate2ddmm(latLng.lat(), 'N', 'S');
            break;
        case 'Degrees_Minutes_Seconds':
            longitude = coordinate2ddmmss(latLng.lng(), 'E', 'W');
            latitude = coordinate2ddmmss(latLng.lat(), 'N', 'S');
            break;
    }

    // Update control to display latlng and coordinates.
    this.node_.innerHTML = longitude + ',' + latitude + '<br/>';
};

