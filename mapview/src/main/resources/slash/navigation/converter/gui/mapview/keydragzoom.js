/**
 * @name KeyDragZoom for V2
 * @version 2.0.5 [December 8, 2010]
 * @author: Nianwei Liu [nianwei at gmail dot com] & Gary Little [gary at luxcentral dot com]
 * @fileoverview This library adds a drag zoom capability to a V2 Google map.
 *  When drag zoom is enabled, holding down a designated hot key <code>(shift | ctrl | alt)</code>
 *  while dragging a box around an area of interest will zoom the map in to that area when
 *  the mouse button is released. Optionally, a visual control can also be supplied for turning
 *  a drag zoom operation on and off.
 *  Only one line of code is needed: <code>GMap2.enableKeyDragZoom();</code>
 *  <p>
 *  Note that if the map's container has a border around it, the border widths must be specified
 *  in pixel units (or as thin, medium, or thick). This is required because of an MSIE limitation.
 */
/*!
 *
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
(function () {
  /*jslint browser:true */
  /*global window,GMap2,GEvent,GLatLng,GLatLngBounds,GPoint,GControl,GControlPosition,GSize,G_ANCHOR_TOP_LEFT */
  /* Utility functions use "var funName=function()" syntax to allow use of the */
  /* Dean Edwards Packer compression tool (with Shrink variables, without Base62 encode). */

  /**
   * Converts "thin", "medium", and "thick" to pixel widths
   * in an MSIE environment. Not called for other browsers
   * because getComputedStyle() returns pixel widths automatically.
   * @param {String} widthValue The value of the border width parameter.
   */
  var toPixels = function (widthValue) {
    var px;
    switch (widthValue) {
    case "thin":
      px = "2px";
      break;
    case "medium":
      px = "4px";
      break;
    case "thick":
      px = "6px";
      break;
    default:
      px = widthValue;
    }
    return px;
  };
 /**
  * Get the widths of the borders of an HTML element.
  *
  * @param {Node} h  The HTML element.
  * @return {Object} The width object {top, bottom left, right}.
  */
  var getBorderWidths = function (h) {
    var computedStyle;
    var bw = {};
    if (document.defaultView && document.defaultView.getComputedStyle) {
      computedStyle = h.ownerDocument.defaultView.getComputedStyle(h, "");
      if (computedStyle) {
        // The computed styles are always in pixel units (good!)
        bw.top = parseInt(computedStyle.borderTopWidth, 10) || 0;
        bw.bottom = parseInt(computedStyle.borderBottomWidth, 10) || 0;
        bw.left = parseInt(computedStyle.borderLeftWidth, 10) || 0;
        bw.right = parseInt(computedStyle.borderRightWidth, 10) || 0;
        return bw;
      }
    } else if (document.documentElement.currentStyle) { // MSIE
      if (h.currentStyle) {
        // The current styles may not be in pixel units so try to convert (bad!)
        bw.top = parseInt(toPixels(h.currentStyle.borderTopWidth), 10) || 0;
        bw.bottom = parseInt(toPixels(h.currentStyle.borderBottomWidth), 10) || 0;
        bw.left = parseInt(toPixels(h.currentStyle.borderLeftWidth), 10) || 0;
        bw.right = parseInt(toPixels(h.currentStyle.borderRightWidth), 10) || 0;
        return bw;
      }
    }
    // Shouldn't get this far for any modern browser
    bw.top = parseInt(h.style["border-top-width"], 10) || 0;
    bw.bottom = parseInt(h.style["border-bottom-width"], 10) || 0;
    bw.left = parseInt(h.style["border-left-width"], 10) || 0;
    bw.right = parseInt(h.style["border-right-width"], 10) || 0;
    return bw;
  };

  // Page scroll values for use by getMousePosition. To prevent flickering on MSIE
  // they are calculated only when the document actually scrolls, not every time the
  // mouse moves (as they would be if they were calculated inside getMousePosition).
  var scroll = {
    x: 0,
    y: 0
  };
  var getScrollValue = function (e) {
    scroll.x = (typeof document.documentElement.scrollLeft !== "undefined" ? document.documentElement.scrollLeft : document.body.scrollLeft);
    scroll.y = (typeof document.documentElement.scrollTop !== "undefined" ? document.documentElement.scrollTop : document.body.scrollTop);
  };
  getScrollValue();

  /**
   * Get the position of the mouse relative to the document.
   * @param {Event} e  The mouse event.
   * @return {Object} The position object {left, top}.
   */
  var getMousePosition = function (e) {
    var posX = 0, posY = 0;
    e = e || window.event;
    if (typeof e.pageX !== "undefined") {
      posX = e.pageX;
      posY = e.pageY;
    } else if (typeof e.clientX !== "undefined") { // MSIE
      posX = e.clientX + scroll.x;
      posY = e.clientY + scroll.y;
    }
    return {
      left: posX,
      top: posY
    };
  };
  /**
   * Get the position of an HTML element relative to the document.
   * @param {Node} h  The HTML element.
   * @return {Object} The position object {left, top}.
   */
  var getElementPosition = function (h) {
    var posX = h.offsetLeft;
    var posY = h.offsetTop;
    var parent = h.offsetParent;
    // Add offsets for all ancestors in the hierarchy
    while (parent !== null) {
      // Adjust for scrolling elements which may affect the map position.
      //
      // See http://www.howtocreate.co.uk/tutorials/javascript/browserspecific
      //
      // "...make sure that every element [on a Web page] with an overflow
      // of anything other than visible also has a position style set to
      // something other than the default static..."
      if (parent !== document.body && parent !== document.documentElement) {
        posX -= parent.scrollLeft;
        posY -= parent.scrollTop;
      }
      // See http://groups.google.com/group/google-maps-js-api-v3/browse_thread/thread/4cb86c0c1037a5e5
      // Example: http://notebook.kulchenko.com/maps/gridmove
      var m = parent;
      // This is the "normal" way to get offset information:
      var moffx = m.offsetLeft;
      var moffy = m.offsetTop;
      // This covers those cases where a transform is used:
      if (!moffx && !moffy && window.getComputedStyle) {
        var matrix = document.defaultView.getComputedStyle(m, null).MozTransform ||
        document.defaultView.getComputedStyle(m, null).WebkitTransform;
        if (matrix) {
          if (typeof matrix === "string") {
            var parms = matrix.split(",");
            moffx += parseInt(parms[4], 10) || 0;
            moffy += parseInt(parms[5], 10) || 0;
          }
        }
      }
      posX += moffx;
      posY += moffy;
      parent = parent.offsetParent;
    }
    return {
      left: posX,
      top: posY
    };
  };
  /**
   * Set the properties of an object to those from another object.
   * @param {Object} obj The target object.
   * @param {Object} vals The source object.
   */
  var setVals = function (obj, vals) {
    if (obj && vals) {
      for (var x in vals) {
        if (vals.hasOwnProperty(x)) {
          obj[x] = vals[x];
        }
      }
    }
    return obj;
  };
  /**
   * Set the opacity. If op is not passed in, this function just performs an MSIE fix.
   * @param {Node} h The HTML element.
   * @param {Number} op The opacity value (0-1).
   */
  var setOpacity = function (h, op) {
    if (typeof op !== "undefined") {
      h.style.opacity = op;
    }
    if (typeof h.style.opacity !== "undefined" && h.style.opacity !== "") {
      h.style.filter = "alpha(opacity=" + (h.style.opacity * 100) + ")";
    }
  };
  /**
   * @name KeyDragZoomOptions
   * @class This class represents the optional parameter passed into <code>GMap2.enableKeyDragZoom</code>.
   * @property {String} [key] The hot key to hold down to activate a drag zoom, <code>shift | ctrl | alt</code>.
   *  Note that the <code>alt</code hot key refers to the Option key on a Macintosh.
   *  The default is <code>shift</code>.
   * @property {Object} [boxStyle] An object literal defining the css styles of the zoom box.
   *  The default is <code>{border: "4px solid #736AFF"}</code>.
   *  Border widths must be specified in pixel units (or as thin, medium, or thick).
   * @property {Object} [veilStyle] An object literal defining the css styles of the veil pane
   *  which covers the map when a drag zoom is activated. The previous name for this property was
   *  <code>paneStyle</code> but the use of this name is now deprecated.
   *  The default is <code>{backgroundColor: "gray", opacity: 0.25, cursor: "crosshair"}</code>.
   * @property {Boolean} [visualEnabled] A flag indicating whether a visual control is to be used.
   *  The default is <code>false</code>.
   * @property {string} [visualClass] The name of the CSS class defining the styles for the visual
   *  control.
   * @property {GControlPosition} [visualPosition] The position of the visual control.
   *  The default position is (27,295) relative to the top left corner of the map.
   * @property {String} [visualSprite] The URL of the sprite image used for showing the visual control
   *  in the on, off, and hot (i.e., when the mouse is over the control) states. The three images
   *  within the sprite must be the same size and arranged in on-hot-off order in a single row
   *  with no spaces between images.
   *  The default is <code>http://maps.gstatic.com/mapfiles/ftr/controls/dragzoom_btn.png</code>.
   * @property {GSize} [visualSize] The width and height values provided by this property are
   *  the size (in pixels) of each of the images within <code>visualSprite</code>.
   *  The default is (20,20).
   * @property {Object} [visualTips] An object literal defining the help tips that appear when
   *  the mouse moves over the visual control. The <code>off</code> property is the tip to be shown
   *  when the control is off and the <code>on</code> property is the tip to be shown when the
   *  control is on.
   *  The default values are "Turn on drag zoom mode" and "Turn off drag zoom mode", respectively.
   */
  /**
   * @name DragZoom
   * @class This class represents a drag zoom object for a map. The object is activated by holding down the hot key
   * or by turning on the visual control.
   * This object is created when <code>GMap2.enableKeyDragZoom</code> is called; it cannot be created directly.
   * Use <code>GMap2.getDragZoomObject</code> to gain access to this object in order to attach event listeners.
   * @param {GMap2} map The map to which the DragZoom object is to be attached.
   * @param {KeyDragZoomOptions} opt_zoomOpts The optional parameters.
   */
  function DragZoom(map, opt_zoomOpts) {
    var i;
    var me = this;
    this.map_ = map;
    opt_zoomOpts = opt_zoomOpts || {};
    this.key_ = opt_zoomOpts.key || "shift";
    this.key_ = this.key_.toLowerCase();
    this.borderWidths_ = getBorderWidths(this.map_.getContainer());

    this.veilDiv_ = [];
    for (i = 0; i < 4; i++) {
      this.veilDiv_[i] = document.createElement("div");
      // Prevents selection of other elements on the webpage
      // when a drag zoom operation is in progress:
      this.veilDiv_[i].onselectstart = function () {
        return false;
      };
      // Apply default style values for the veil:
      setVals(this.veilDiv_[i].style, {
        backgroundColor: "gray",
        opacity: 0.25,
        cursor: "crosshair"
      });
      // Apply style values specified in veilStyle parameter:
      setVals(this.veilDiv_[i].style, opt_zoomOpts.paneStyle); // Old option name was "paneStyle"
      setVals(this.veilDiv_[i].style, opt_zoomOpts.veilStyle); // New name is "veilStyle"
      // Apply mandatory style values:
      setVals(this.veilDiv_[i].style, {
        position: "absolute",
        overflow: "hidden",
        zIndex: 10001,
        display: "none"
      });
      // Workaround for Firefox Shift-Click problem:
      if (this.key_ === "shift") {
        this.veilDiv_[i].style.MozUserSelect = "none";
      }
      setOpacity(this.veilDiv_[i]);
      // An IE fix: If the background is transparent it cannot capture mousedown
      // events, so if it is, change the background to white with 0 opacity.
      if (this.veilDiv_[i].style.backgroundColor === "transparent") {
        this.veilDiv_[i].style.backgroundColor = "white";
        setOpacity(this.veilDiv_[i], 0);
      }
      this.map_.getContainer().appendChild(this.veilDiv_[i]);
    }

    this.visualEnabled_ = opt_zoomOpts.visualEnabled || false;
    this.visualClass_ = opt_zoomOpts.visualClass || "";
    this.visualPosition_ = opt_zoomOpts.visualPosition || this.getDefaultPosition();
    this.visualSprite_ = opt_zoomOpts.visualSprite || "http://maps.gstatic.com/mapfiles/ftr/controls/dragzoom_btn.png";
    this.visualSize_ = opt_zoomOpts.visualSize || new GSize(20, 20);
    this.visualTips_ = opt_zoomOpts.visualTips || {};
    this.visualTips_.off =  this.visualTips_.off || "Turn on drag zoom mode";
    this.visualTips_.on =  this.visualTips_.on || "Turn off drag zoom mode";

    this.boxDiv_ = document.createElement("div");
    // Apply default style values for the zoom box:
    setVals(this.boxDiv_.style, {
      border: "4px solid #736AFF"
    });
    // Apply style values specified in boxStyle parameter:
    setVals(this.boxDiv_.style, opt_zoomOpts.boxStyle);
    // Apply mandatory style values:
    setVals(this.boxDiv_.style, {
      position: "absolute",
      display: "none"
    });
    setOpacity(this.boxDiv_);
    this.map_.getContainer().appendChild(this.boxDiv_);
    this.boxBorderWidths_ = getBorderWidths(this.boxDiv_);

    this.listeners_ = [
      GEvent.bindDom(document, "keydown", this, function (e) {
        me.onKeyDown_(e);
      }),
      GEvent.bindDom(document, "keyup", this, function (e) {
        me.onKeyUp_(e);
      }),
      GEvent.bindDom(this.veilDiv_[0], "mousedown", this, function (e) {
        me.onMouseDown_(e);
      }),
      GEvent.bindDom(document, "mousedown", this, function (e) {
        me.onMouseDownDocument_(e);
      }),
      GEvent.bindDom(document, "mousemove", this, function (e) {
        me.onMouseMove_(e);
      }),
      GEvent.bindDom(document, "mouseup", this, function (e) {
        me.onMouseUp_(e);
      }),
      GEvent.bindDom(window, "scroll", this, getScrollValue)
    ];

    this.hotKeyDown_ = false;
    this.mouseDown_ = false;
    this.dragging_ = false;
    this.startPt_ = null;
    this.endPt_ = null;
    this.mapWidth_ = null;
    this.mapHeight_ = null;
    this.mousePosn_ = null;
    this.mapPosn_ = null;

    if (this.visualEnabled_) {
      this.map_.addControl(this, this.visualPosition_);
    }
  }
  /**
   * DragZoom is a subclass of <code>GControl</code>.
   */
  DragZoom.prototype = new GControl();
  /**
   * Returns the default position for the visual control.
   * @return {GControlPosition} The default position.
   * @private
   */
  DragZoom.prototype.getDefaultPosition = function () {
    return new GControlPosition(G_ANCHOR_TOP_LEFT, new GSize(27, 295));
  };
  /**
   * Initializes the visual control and returns its DOM element.
   * @param {GMap2} map The map to which the control is to be added.
   * @return {Node} The DOM element containing the visual control.
   * @private
   */
  DragZoom.prototype.initialize = function (map) {
    var image;
    var me = this;
    this.buttonDiv_ = document.createElement("div");
    this.buttonDiv_.className = this.visualClass_;
    this.buttonDiv_.style.position = "relative";
    this.buttonDiv_.style.overflow = "hidden";
    this.buttonDiv_.style.height = this.visualSize_.height + "px";
    this.buttonDiv_.style.width = this.visualSize_.width + "px";
    this.buttonDiv_.title = this.visualTips_.off;
    image = document.createElement("img");
    image.src = this.visualSprite_;
    image.style.position = "absolute";
    image.style.left = -(this.visualSize_.width * 2) + "px";
    image.style.top = 0 + "px";
    this.buttonDiv_.appendChild(image);
    this.buttonDiv_.onclick = function (e) {
      me.hotKeyDown_ = !me.hotKeyDown_;
      if (me.hotKeyDown_) {
        me.buttonDiv_.firstChild.style.left = -(me.visualSize_.width * 0) + "px";
        me.buttonDiv_.title = me.visualTips_.on;
        GEvent.trigger(me, "activate");
      } else {
        me.buttonDiv_.firstChild.style.left = -(me.visualSize_.width * 2) + "px";
        me.buttonDiv_.title = me.visualTips_.off;
        GEvent.trigger(me, "deactivate");
      }
      me.onMouseMove_(e); // Updates the veil
    };
    this.buttonDiv_.onmouseover = function () {
      me.buttonDiv_.firstChild.style.left = -(me.visualSize_.width * 1) + "px";
    };
    this.buttonDiv_.onmouseout = function () {
      if (me.hotKeyDown_) {
        me.buttonDiv_.firstChild.style.left = -(me.visualSize_.width * 0) + "px";
        me.buttonDiv_.title = me.visualTips_.on;
      } else {
        me.buttonDiv_.firstChild.style.left = -(me.visualSize_.width * 2) + "px";
        me.buttonDiv_.title = me.visualTips_.off;
      }
    };
    this.buttonDiv_.ondragstart = function () {
      return false;
    };
    setVals(this.buttonDiv_.style, {
      zIndex: 10002,
      cursor: "pointer"
    });
    map.getContainer().appendChild(this.buttonDiv_);
    return this.buttonDiv_;
  };
  /**
   * Returns <code>true</code> if the hot key is being pressed when an event occurs.
   * @param {Event} e The keyboard event.
   * @return {Boolean} Flag indicating whether the hot key is down.
   */
  DragZoom.prototype.isHotKeyDown_ = function (e) {
    var isHot;
    e = e || window.event;
    isHot = (e.shiftKey && this.key_ === "shift") || (e.altKey && this.key_ === "alt") || (e.ctrlKey && this.key_ === "ctrl");
    if (!isHot) {
      // Need to look at keyCode for Opera because it
      // doesn't set the shiftKey, altKey, ctrlKey properties
      // unless a non-modifier event is being reported.
      //
      // See http://cross-browser.com/x/examples/shift_mode.php
      // Also see http://unixpapa.com/js/key.html
      switch (e.keyCode) {
      case 16:
        if (this.key_ === "shift") {
          isHot = true;
        }
        break;
      case 17:
        if (this.key_ === "ctrl") {
          isHot = true;
        }
        break;
      case 18:
        if (this.key_ === "alt") {
          isHot = true;
        }
        break;
      }
    }
    return isHot;
  };
  /**
   * Returns <code>true</code> if the mouse is on top of the map div.
   * The position is captured in onMouseMove_.
   * @return {boolean}
   */
  DragZoom.prototype.isMouseOnMap_ = function () {
    var mousePosn = this.mousePosn_;
    if (mousePosn) {
      var mapPosn = this.mapPosn_;
      var mapSize = this.map_.getSize();
      return mousePosn.left > mapPosn.left && mousePosn.left < (mapPosn.left + mapSize.width) &&
      mousePosn.top > mapPosn.top && mousePosn.top < (mapPosn.top + mapSize.height);
    } else {
      // if user never moved mouse
      return false;
    }
  };
  /**
   * Show the veil if the hot key is down and the mouse is over the map,
   * otherwise hide the veil.
   */
  DragZoom.prototype.setVeilVisibility_ = function () {
    var i;
    if (this.map_ && this.hotKeyDown_ && this.isMouseOnMap_()) {
      var mapSize = this.map_.getSize();
      this.mapWidth_ = mapSize.width - (this.borderWidths_.left + this.borderWidths_.right);
      this.mapHeight_ = mapSize.height - (this.borderWidths_.top + this.borderWidths_.bottom);
      this.veilDiv_[0].style.left = "0px";
      this.veilDiv_[0].style.top = "0px";
      this.veilDiv_[0].style.width = this.mapWidth_ + "px";
      this.veilDiv_[0].style.height = this.mapHeight_ + "px";
      for (i = 1; i < this.veilDiv_.length; i++) {
        this.veilDiv_[i].style.width = "0px";
        this.veilDiv_[i].style.height = "0px";
      }
      for (i = 0; i < this.veilDiv_.length; i++) {
        this.veilDiv_[i].style.display = "block";
      }
    } else {
      for (i = 0; i < this.veilDiv_.length; i++) {
        this.veilDiv_[i].style.display = "none";
      }
    }
  };
  /**
   * Handle key down. Show the veil if the hot key has been pressed.
   * @param {Event} e The keyboard event.
   */
  DragZoom.prototype.onKeyDown_ = function (e) {
    if (this.map_ && !this.hotKeyDown_ && this.isHotKeyDown_(e)) {
      this.mapPosn_ = getElementPosition(this.map_.getContainer());
      this.hotKeyDown_ = true;
      this.setVeilVisibility_();
     /**
       * This event is fired when the hot key is pressed.
       * @name DragZoom#activate
       * @event
       */
      GEvent.trigger(this, "activate");
    }
    if (this.visualEnabled_ && this.isHotKeyDown_(e)) {
      this.buttonDiv_.style.display = "none";
    }
  };
  /**
   * Get the <code>GPoint</code> of the mouse position.
   * @param {Event} e The mouse event.
   * @return {GPoint} The mouse position.
   */
  DragZoom.prototype.getMousePoint_ = function (e) {
    var mousePosn = getMousePosition(e);
    var p = new GPoint();
    p.x = mousePosn.left - this.mapPosn_.left - this.borderWidths_.left;
    p.y = mousePosn.top - this.mapPosn_.top - this.borderWidths_.top;
    p.x = Math.min(p.x, this.mapWidth_);
    p.y = Math.min(p.y, this.mapHeight_);
    p.x = Math.max(p.x, 0);
    p.y = Math.max(p.y, 0);
    return p;
  };
  /**
   * Handle mouse down.
   * @param {Event} e The mouse event.
   */
  DragZoom.prototype.onMouseDown_ = function (e) {
    if (this.map_ && this.hotKeyDown_) {
      this.mapPosn_ = getElementPosition(this.map_.getContainer());
      this.dragging_ = true;
      this.startPt_ = this.endPt_ = this.getMousePoint_(e);
      this.boxDiv_.style.width = this.boxDiv_.style.height = "0px";
      var latlng = this.map_.fromContainerPixelToLatLng(this.startPt_);
      if (this.visualEnabled_) {
        this.buttonDiv_.style.display = "none";
      }
      /**
       * This event is fired when the drag operation begins.
       * The parameter passed is the geographic position of the starting point.
       * @name DragZoom#dragstart
       * @param {GLatLng} latlng The geographic position of the starting point. 
       * @event
       */
      GEvent.trigger(this, "dragstart", latlng);
    }
  };
  /**
   * Handle mouse down at the document level.
   * @param {Event} e The mouse event.
   */
  DragZoom.prototype.onMouseDownDocument_ = function (e) {
    this.mouseDown_ = true;
  };
  /**
   * Handle mouse move.
   * @param {Event} e The mouse event.
   */
  DragZoom.prototype.onMouseMove_ = function (e) {
    this.mousePosn_ = getMousePosition(e);
    if (this.dragging_) {
      this.endPt_ = this.getMousePoint_(e);
      var left = Math.min(this.startPt_.x, this.endPt_.x);
      var top = Math.min(this.startPt_.y, this.endPt_.y);
      var width = Math.abs(this.startPt_.x - this.endPt_.x);
      var height = Math.abs(this.startPt_.y - this.endPt_.y);
      // For benefit of MSIE 7/8 ensure following values are not negative:
      var boxWidth = Math.max(0, width - (this.boxBorderWidths_.left + this.boxBorderWidths_.right));
      var boxHeight = Math.max(0, height - (this.boxBorderWidths_.top + this.boxBorderWidths_.bottom));
      // Left veil rectangle:
      this.veilDiv_[0].style.top = "0px";
      this.veilDiv_[0].style.left = "0px";
      this.veilDiv_[0].style.width = left + "px";
      this.veilDiv_[0].style.height = this.mapHeight_ + "px";
      // Right veil rectangle:
      this.veilDiv_[1].style.top = "0px";
      this.veilDiv_[1].style.left = (left + width) + "px";
      this.veilDiv_[1].style.width = (this.mapWidth_ - (left + width)) + "px";
      this.veilDiv_[1].style.height = this.mapHeight_ + "px";
      // Top veil rectangle:
      this.veilDiv_[2].style.top = "0px";
      this.veilDiv_[2].style.left = left + "px";
      this.veilDiv_[2].style.width = width + "px";
      this.veilDiv_[2].style.height = top + "px";
      // Bottom veil rectangle:
      this.veilDiv_[3].style.top = (top + height) + "px";
      this.veilDiv_[3].style.left = left + "px";
      this.veilDiv_[3].style.width = width + "px";
      this.veilDiv_[3].style.height = (this.mapHeight_ - (top + height)) + "px";
      // Selection rectangle:
      this.boxDiv_.style.top = top + "px";
      this.boxDiv_.style.left = left + "px";
      this.boxDiv_.style.width = boxWidth + "px";
      this.boxDiv_.style.height = boxHeight + "px";
      this.boxDiv_.style.display = "block";
      /**
       * This event is fired repeatedly while the user drags a box across the area of interest.
       * The southwest and northeast point are passed as parameters of type <code>GPoint</code>
       * (for performance reasons), relative to the map container. Note: the event listener is 
       * responsible for converting pixel position to geographic coordinates, if necessary, using
       * <code>GMap2.fromContainerPixelToLatLng</code>.
       * @name DragZoom#drag
       * @param {GPoint} southwestPixel The southwest point of the selection area.
       * @param {GPoint} northeastPixel The northeast point of the selection area.
       * @event
       */
      GEvent.trigger(this, "drag", new GPoint(left, top + height), new GPoint(left + width, top));
    } else if (!this.mouseDown_) {
      this.mapPosn_ = getElementPosition(this.map_.getContainer());
      this.setVeilVisibility_();
    }
  };
  /**
   * Handle mouse up.
   * @param {Event} e The mouse event.
   */
  DragZoom.prototype.onMouseUp_ = function (e) {
    var me = this;
    this.mouseDown_ = false;
    if (this.dragging_) {
      if ((this.getMousePoint_(e).x === this.startPt_.x) && (this.getMousePoint_(e).y === this.startPt_.y)) {
        this.onKeyUp_(e); // Cancel event
        return;
      }
      var left = Math.min(this.startPt_.x, this.endPt_.x);
      var top = Math.min(this.startPt_.y, this.endPt_.y);
      var width = Math.abs(this.startPt_.x - this.endPt_.x);
      var height = Math.abs(this.startPt_.y - this.endPt_.y);
      // Google Maps API bug: setCenter() doesn't work as expected if the map has a
      // border on the left or top. The code here includes a workaround for this problem.
      var kGoogleCenteringBug = true;
      if (kGoogleCenteringBug) {
        left += this.borderWidths_.left;
        top += this.borderWidths_.top;
      }
      var sw = this.map_.fromContainerPixelToLatLng(new GPoint(left, top + height));
      var ne = this.map_.fromContainerPixelToLatLng(new GPoint(left + width, top));
      var bnds = new GLatLngBounds(sw, ne);
      var level = this.map_.getBoundsZoomLevel(bnds);
      this.map_.setCenter(bnds.getCenter(), level);
      // Redraw box after zoom:
      var swPt = this.map_.fromLatLngToContainerPixel(sw);
      var nePt = this.map_.fromLatLngToContainerPixel(ne);
      if (kGoogleCenteringBug) {
        swPt.x -= this.borderWidths_.left;
        swPt.y -= this.borderWidths_.top;
        nePt.x -= this.borderWidths_.left;
        nePt.y -= this.borderWidths_.top;
      }
      this.boxDiv_.style.left = swPt.x + "px";
      this.boxDiv_.style.top = nePt.y + "px";
      this.boxDiv_.style.width = (Math.abs(nePt.x - swPt.x) - (this.boxBorderWidths_.left + this.boxBorderWidths_.right)) + "px";
      this.boxDiv_.style.height = (Math.abs(nePt.y - swPt.y) - (this.boxBorderWidths_.top + this.boxBorderWidths_.bottom)) + "px";
      // Hide box asynchronously after 1 second:
      setTimeout(function () {
        me.boxDiv_.style.display = "none";
      }, 1000);
      this.dragging_ = false;
      this.onMouseMove_(e); // Updates the veil
      /**
       * This event is fired when the drag operation ends.
       * The parameter passed is the geographic bounds of the selected area.
       * Note that this event is <i>not</i> fired if the hot key is released before the drag operation ends.
       * @name DragZoom#dragend
       * @param {GLatLngBounds} bnds The geographic bounds of the selected area.
       * @event
       */
      GEvent.trigger(this, "dragend", bnds);
      // if the hot key isn't down, the drag zoom must have been activated by turning
      // on the visual control. In this case, finish up by simulating a key up event.
      if (!this.isHotKeyDown_(e)) {
        this.onKeyUp_(e);
      }
    }
  };
  /**
   * Handle key up.
   * @param {Event} e The keyboard event.
   */
  DragZoom.prototype.onKeyUp_ = function (e) {
    var i;
    if (this.map_ && this.hotKeyDown_) {
      this.hotKeyDown_ = false;
      if (this.dragging_) {
        this.boxDiv_.style.display = "none";
        this.dragging_ = false;
      }
      for (i = 0; i < this.veilDiv_.length; i++) {
        this.veilDiv_[i].style.display = "none";
      }
      if (this.visualEnabled_) {
        this.buttonDiv_.firstChild.style.left = -(this.visualSize_.width * 2) + "px";
        this.buttonDiv_.title = this.visualTips_.off;
        this.buttonDiv_.style.display = "";
      }
      /**
       * This event is fired when the hot key is released.
       * @name DragZoom#deactivate
       * @event
       */
      GEvent.trigger(this, "deactivate");
    }
  };
  /**
   * @name GMap2
   * @class These are new methods added to the Google Maps JavaScript API V2's
   * <a href="http://code.google.com/apis/maps/documentation/javascript/v2/reference.html#GMap2">GMap2</a>
   * class.
   */
  /**
   * Enables drag zoom. The user can zoom to an area of interest by holding down the hot key
   * <code>(shift | ctrl | alt )</code> while dragging a box around the area or by turning
   * on the visual control then dragging a box around the area.
   * @param {KeyDragZoomOptions} opt_zoomOpts The optional parameters.
   */
  GMap2.prototype.enableKeyDragZoom = function (opt_zoomOpts) {
    this.dragZoom_ = new DragZoom(this, opt_zoomOpts);
  };
  /**
   * Disables drag zoom.
   */
  GMap2.prototype.disableKeyDragZoom = function () {
    var i;
    var d = this.dragZoom_;
    if (d) {
      for (i = 0; i < d.listeners_.length; ++i) {
        GEvent.removeListener(d.listeners_[i]);
      }
      this.getContainer().removeChild(d.boxDiv_);
      for (i = 0; i < d.veilDiv_.length; i++) {
        this.getContainer().removeChild(d.veilDiv_[i]);
      }
      if (d.visualEnabled_) {
        d.map_.removeControl(d);
      }
      this.dragZoom_ = null;
    }
  };
  /**
   * Returns <code>true</code> if the drag zoom feature has been enabled.
   * @return {Boolean}
   */
  GMap2.prototype.keyDragZoomEnabled = function () {
    return this.dragZoom_ !== null;
  };
  /**
   * Returns the DragZoom object which is created when <code>GMap2.enableKeyDragZoom</code> is called.
   * With this object you can use <code>GEvent.addListener</code> to attach event listeners
   * for the "activate", "deactivate", "dragstart", "drag", and "dragend" events.
   * @return {DragZoom}
   */
  GMap2.prototype.getDragZoomObject = function () {
    return this.dragZoom_;
  };
})();