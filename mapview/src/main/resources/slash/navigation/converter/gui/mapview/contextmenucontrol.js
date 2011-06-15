/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Based on ContextMenuControl 1.0 from Wolfgang Pichler
 *
 * @name ContextMenuControl for RouteConverter
 * @version 1.1
 * @copyright 2009 Wolfgang Pichler & 2010 Christian Pesch
 * @author Wolfgang Pichler, Christian Pesch
 * @fileoverview
 * <p>This class lets you add a control to the map which mimics
 * the context menu of Google Maps. This control extends the
 * <a href="http://www.google.com/apis/maps/documentation/reference.html#GControl">
 * GControl</a> interface.</p>
 * <p>Note: ContextMenuControl doesn't work in Opera because Opera doesn't
 * support the oncontextmenu event and doesn't give access to right mouse clicks.
 * </p>
 */

/**
 * @desc Creates a control.
 * @constructor
 */
function ContextMenuControl() {
}
/**
 * Extends GOverlay class from the Google Maps API.
 *  Second param (selectable) should be set to true
 * @private
 */
ContextMenuControl.prototype = new GControl(false, true);

/**
 * @desc Initialize the control on the map.
 * @param {GMap2} map The map that has had this control added to
 * @return {Element} mapdiv Div that holds the map
 * @private
 */
ContextMenuControl.prototype.initialize = function (map) {
  var me = this;
  me.map_ = map;
  var mapdiv = map.getContainer();

  // Prevents the browser's own context menu to appear.
  if (mapdiv.addEventListener) {
    mapdiv.addEventListener("contextmenu", function (e) {
      e.stopPropagation();
      e.preventDefault();
      }, false);
  } else if (mapdiv.attachEvent) {
    mapdiv.attachEvent("oncontextmenu", function () {
      var e = window.event;
      e.cancelBubble = true;
      e.returnValue = false;
    });
  }
  me.createContextMenu_();

  // Displays our context menu on single right mouse click
  GEvent.addListener(map, "singlerightclick", function (pixelPoint, src, ov) {
    if (ov instanceof GMarker) {
        me.clickedIndex_ = ov.index_;
    } else {
        me.clickedIndex_ = -1;
    }
    me.clickedPoint_ = map.fromContainerPixelToLatLng(pixelPoint);

    // Correction of IE bug
    var posX = document.all ? (pixelPoint.x - 40) : pixelPoint.x;
    var posY = document.all ? (pixelPoint.y + 10) : pixelPoint.y;

    var mapwidth = map.getSize().width;
    var mapheight = map.getSize().height;
    var menuwidth = me.menuList.offsetWidth;
    var menuheight = me.menuList.offsetHeight;

    // Adjusts the position of the context menu
    if (mapwidth - menuwidth < posX) {
      posX = posX - menuwidth;
    }
    if (mapheight - menuheight < posY) {
      posY = posY - menuheight - 20;
    }
    me.menuList.style.visibility = "visible";
    me.menuList.visible = true;
    var pos = new GControlPosition(G_ANCHOR_TOP_LEFT, new GSize(posX, posY));
    pos.apply(me.menuList);
  });

  // Closes context menu when the cursor is being moved out of the map.
  // This DomListener is a workaround for Internet Explorer because
  //  the 'normal' GEvent Listener doesn't work correctly in IE.
  GEvent.addDomListener(mapdiv, "mouseout", function (e) {
    if (me.menuList.visible) {
      if (!e) {
        var e = window.event;
      }
      if (me.checkMouseLeave_(mapdiv, e)) {
        me.hideMenu_();
      }
    }
  });

  // Closes context menu in case of a left click on the map
  GEvent.addListener(map, "click", function () {
    me.hideMenu_();
  });

  // Closes context menu after dragging the map
  GEvent.addListener(map, "dragend", function () {
    me.hideMenu_();
  });
  // Return a dummy element to keep the API happy
  return document.createElement("b");
};

/**
 * Creates an initially hidden unordered menu list.
 * @return {Element} ul that holds the list entries of the context menu.
 * @private
 */
ContextMenuControl.prototype.createContextMenu_ = function () {
  var me = this;
  me.menuList = document.createElement("ul");
  me.menuList.style.font = "small Arial";
  me.menuList.style.whiteSpace = "nowrap";
  me.menuList.style.color = "#0000cd";
  me.menuList.style.backgroundColor = "#fff";
  me.menuList.style.listStyle = "none";
  me.menuList.style.padding = "0px";
  me.menuList.style.width = "21ex";
  me.menuList.style.border = "1px solid #666";
  me.menuList.style.position = "absolute";

    me.menuList.appendChild(me.createListItem_("Insert", "insert"));
    me.menuList.appendChild(me.createListItem_("Remove", "remove"));
    me.menuList.appendChild(me.createRuler_());
    me.menuList.appendChild(me.createListItem_("Center map", "center"));
    me.menuList.appendChild(me.createListItem_("Zoom in", "in"));
    me.menuList.appendChild(me.createListItem_("Zoom out", "out"));
    me.hideMenu_();
    // Adds context menu to the map container
  me.map_.getContainer().appendChild(me.menuList);
  return me.menuList;
};

/**
 * Avoids firing a mouseout event when the mouse moves over a child element.
 * This will be caused by event bubbling.
 * Borrowed from: http://www.faqts.com/knowledge_base/view.phtml/aid/1606/fid/145
 * @param {Element} element Parent div
 * @param {Event} evt The passed mouse event
 * @return {Boolean}
 * @private
 */
ContextMenuControl.prototype.checkMouseLeave_ = function (element, evt) {
  if (element.contains && evt.toElement) {
    return !element.contains(evt.toElement);
  } else if (evt.relatedTarget) {
    return !this.containsDOM_(element, evt.relatedTarget);
  }
};

/**
 * Checks if the mouse leaves the parent element.
 * @param {Element} container Parent div
 * @param {Event} containee Event of node that the mouse entered when leaving the target
 * @return {Boolean}
 * @private
 */
ContextMenuControl.prototype.containsDOM_ = function (container, containee) {
  var isParent = false;
  do {
    if ((isParent = container == containee)) {
      break;
    }
    containee = containee.parentNode;
  } while(containee != null);
  return isParent;
};

/**
 * Creates clickable context menu list items.
 * @param {String} text Text to display in list item.
 * @param {String} arg Used to identify the clicked entry.
 * @return {Element} List item that holds the entry.
 * @private
 */
ContextMenuControl.prototype.createListItem_ = function (text, arg) {
  var me = this;
  var entry = document.createElement("li");
  entry.style.padding = "0px 6px";
  entry.style.lineHeight = "1.6em";
  entry.appendChild(document.createTextNode(text));

  GEvent.addDomListener(entry, "mouseover", function () {
    entry.style.cursor = "pointer";
    entry.style.backgroundColor = "#00ddff";
  });

  GEvent.addDomListener(entry, "mouseout", function () {
    entry.style.cursor = "default";
    entry.style.backgroundColor = "#fff";
  });

  GEvent.addDomListener(entry, "click", function () {
    if (arg == "insert") {
      me.insertPosition_();
    } else if (arg == "remove") {
      me.removePosition_();
    } else if (arg == "in") {
      me.map_.zoomIn();
    } else if (arg == "out") {
      me.map_.zoomOut();
    } else if (arg == "center") {
      var point = me.clickedPoint_;
      me.map_.panTo(point);
    }
    // Hides the menu after it's been used
    me.hideMenu_();
  });
  return entry;
};

/**
 * Adds position.
 * @private
 */
ContextMenuControl.prototype.insertPosition_ = function () {
    var me = this;
    var point = me.clickedPoint_;
    callJava("insert-position/" + point.lat() + "/" + point.lng());
};

/**
 * Removes position.
 * @private
 */
ContextMenuControl.prototype.removePosition_ = function () {
  var me = this;
  var index = me.clickedIndex_;
  if(index >= 0) {
    callJava("remove-position/" + index);
  }    
};

/**
 * Creates a styled horizontal ruler between the list entries.
 * @return {Element} hr as separator.
 * @private
 */
ContextMenuControl.prototype.createRuler_ = function () {
  var hr = document.createElement("hr");
  hr.style.height = "1px";
  hr.style.border = "1px";
  hr.style.color = "#e2e2e2";
  hr.style.backgroundColor = "#e2e2e2";
  // Further IE bug
  if (document.all) {
    hr.style.display = "block";
    hr.style.margin = "-6px";
  } else {
    hr.style.margin = "0px";
  }
  return hr;
};

/**
 * Hides the context menu and sets its property visible to false.
 * @private
 */
ContextMenuControl.prototype.hideMenu_ = function () {
  this.menuList.style.visibility = "hidden";
  this.menuList.visible = false;
};

/**
 * Creates required properties for icons.
 * @param {GIcon} icon
 * @private
 */
ContextMenuControl.prototype.createIcon_ = function (image) {
  var icon = new GIcon();
  var url = "http://maps.google.com/mapfiles/";
  if (image == "arrow") {
    icon.image = url + "arrow.png";
    icon.shadow = url + "arrowshadow.png";
    icon.iconSize = new GSize(39, 34);
    icon.shadowSize = new GSize(39, 34);
    icon.iconAnchor = new GPoint(20, 34);
    icon.infoWindowAnchor = new GPoint(20, 0);
  } else {
    icon.image = url + image;
    icon.shadow = url + "shadow50.png";
    icon.iconSize = new GSize(20, 34);
    icon.shadowSize = new GSize(37, 34);
    icon.iconAnchor = new GPoint(9, 34);
    icon.infoWindowAnchor = new GPoint(19, 2);
  }
  return icon;
};
