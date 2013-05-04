/**
 * Context Menu for Google Maps
 */
(function(window, undefined){

	// Use the correct document accordingly with window argument (sandbox)
	var document = window.document,

		// shorthand some stuff
		$ = jQuery,
		g = google.maps;

	/**
	 * Create the context menu
	 * Note: Menu id needs to be unique, in case of multiple maps and context menus
	 */
	function ContextMenu(opts)
	{
		// A way to access 'this' object from inside functions
		var self = this;

		if ( opts.map !== undefined )
		{
			// Put the map onto the object
			this.theMap = opts.map;

			// Keep track of where you clicked, for the callback functions.
			this.clickedLatLng = null;

			// Create the context menu element
			this.theMenu = $(document.createElement('div'))
				.attr('class', 'ContextMenu')

				// .. disable the browser context menu on our context menu
				.bind('ContextMenu', function() { return false; })

				// .. append a ul list element
				.append($(document.createElement('ul')))

				// .. then append it to the map object
				.appendTo(this.theMap.getDiv());

			// Display and position the menu
			g.event.addListener(this.theMap, 'rightclick', function(e)
			{
				// Shorthand some stuff
				var mapDiv = $(self.theMap.getDiv()),
					menu = self.theMenu,
					x = e.pixel.x,
					y = e.pixel.y;

				// Hide the context menu if its open
				menu.hide();

				// Save the clicked location
				self.clickedLatLng = e.latLng;

				// Adjust the menu if clicked to close to the edge of the map
				if ( x > mapDiv.width() - menu.width() )
					x -= menu.width();

				if ( y > mapDiv.height() - menu.height() )
					y -= menu.height();

				// Set the location and fade in the context menu
				menu.css({ top: y, left: x }).fadeIn(200);
			});

			// Hide context menu on several events
			$.each('click dragstart zoom_changed maptypeid_changed center_changed'.split(' '), function(i,name){
				g.event.addListener(self.theMap, name, function(){ self.theMenu.hide(); });
			});
		}
	}

	/**
	 * Add new items to the context menu
	 */
	ContextMenu.prototype.addItem = function(name, loc, callback)
	{
		// If no loc was provided
		if ( typeof loc === 'function')
		{
			callback = loc;
			loc = undefined;
		}

		// A way to access 'this' object from inside functions
		var self = this,

			// The name turned into camelCase for use in the li id, and anchor href
			idName = name.toCamel(),

			// The li element
			li = $(document.createElement('li'))
				.attr('id', idName);

		// the anchor element
		$(document.createElement('a'))
			.attr('href', '#'+idName).html(name)
			.appendTo(li)

			// Add some nice hover effects
			.hover(function() {
				$(this).parent().toggleClass('hover');
			})

			// Set the click event
			.click(function(){

				// fade out the menu
				self.theMenu.hide();

				// call the callback function - 'this' would refer back to the jQuery object of the item element
				callback.call(this, self.theMap, self.clickedLatLng);

				// make sure the click doesnt take us anywhere
				return false;
			});

		// If `loc` is a number put it at that location
		if ( typeof loc === 'number' && loc < this.theMenu.find('li').length)
			this.theMenu.find('li').eq(loc).before(li);

		// .. else appened it to the end of the menu
		else
			this.theMenu.find('ul').append(li);

		// Return the whole list item
		return li;
	};

	/**
	 * Add a seperators
	 *
	 * @return jQuery The list item that is created.
	 */
	ContextMenu.prototype.addSep = function(loc)
	{
		// Create the li element
		var li = $(document.createElement('li'))
			.addClass('separator')

			// .. add a div child
			.append($(document.createElement('div')));

		// If loc is a number put the li at that location
		if ( typeof loc === 'number' )
			this.theMenu.find('li').eq(loc).before(li);

		// .. else appened it to the end
		else
			this.theMenu.find('ul').append(li);

		// Return the li element
		return li
	};

	/**
	 * Remove a menu list item.
	 */
	ContextMenu.prototype.remove = function(item)
	{
		// No need to search for name if its a jquery object
		if ( item instanceof $ )
			item.remove();

		// Find all the elements and remove the one at the specified index
		else if ( typeof item === 'number' )
			this.theMenu.find('li').eq(item).remove();

		// Find all the items by the id name and remove them
		else if ( typeof item === 'string' )
		{
			// Find and remove the element
			this.theMenu.find('#'+item.toCamel()).remove();
		}
	};

	// Expose this to the global object
	window.ContextMenu = ContextMenu;

	/**
	 * Convert a string into a 'camelCase' string
	 *
	 * @example 'Camel case string'.toCamel() -> 'camelCaseString'
	 */
	String.prototype.toCamel = function() {
		return this.toLowerCase().replace(/(\s)([a-z])/gi, function(match, group1, group2){
			return group2.toUpperCase().replace(group1,'');
		});
	}

})(window);