'use strict';

(function() {

// Disable no-js text
var noJsElement = document.getElementById("nojs");
if(noJsElement !== null) {
	noJsElement.style.visibility = "hidden";
}

// Prepare arrays for loaded data
WurmMapGen.config = null;

WurmMapGen.villages = null;
WurmMapGen.guardtowers = null;
WurmMapGen.structures = null;
WurmMapGen.portals = null;

// Helper function to fetch a dataset from a JSON file
function fetchData(key, path) {
	return fetch('data/' + path)
		.then(function(response) { return response.json() })
		.then(function(responseData) {
			WurmMapGen[key] = responseData[key];
			return Promise.resolve();
		});
}

// Keep track of whether or not the window is focused and active
var windowIsFocused = true;
window.onblur = function(){ windowIsFocused = false; }
window.onfocus = function(){ windowIsFocused = true; }

// Helper function to set timeout for refreshing realtime data
function setRealtimeTimer() {
	var time = 2000;

	// Throttle when window unfocused — still alive but lower priority
	if (!windowIsFocused) {
		time = 10000;
	}

	WurmMapGen.realtimeTimer = setTimeout(function() {
		fetchData('players', ('players.json?_=' + Date.now())).then(function() {
			WurmMapGen.map.updatePlayerMarkers();
			WurmMapGen.gui.playerCount = WurmMapGen.players.length;
			setRealtimeTimer();
		});
	}, time);
}

// Prepare promises to load data
var promises = [
	fetchData('config', 'config.json'),
	fetchData('villages', 'villages.json'),
	fetchData('guardtowers', 'guardtowers.json'),
	fetchData('structures', 'structures.json'),
	fetchData('portals', 'portals.json')
];

if (document.body.getAttribute('data-realtime') === 'true') {
	promises.push(fetchData('players', ('players.json?_=' + Date.now())));
}

// Start loading
Promise.all(promises)
.catch(function(err) {
	console.error('Could not load data');
	console.error(err);
	document.write('Something went wrong, map data could not be loaded'); // TODO add better error handling
})
.then(function() {
	// Add computed config values
	WurmMapGen.config.xyMulitiplier = (WurmMapGen.config.actualMapSize / WurmMapGen.config.mapTileSize);

	// Create the map
	WurmMapGen.map.create();

	// Initialise the GUI
	WurmMapGen.gui.init();

	// Set interval to refresh realtime data
	if (document.body.getAttribute('data-realtime') === 'true') {
		setRealtimeTimer();
	}
});

// End IIFE
})();
