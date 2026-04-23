# LiveMap Mod

A comprehensive live map system for Wurm Unlimited servers, providing real-time interactive web-based maps with player tracking, village boundaries, and beautiful terrain rendering.

## Features

### 🗺️ **Advanced Map Rendering**
- **Improved Isometric Rendering**: Based on LiveHudMap client mod algorithm
- **Accurate Terrain Shading**: Height-based shading with proper water coloring
- **Tile-Based System**: Efficient tile generation with caching
- **Zoom Support**: Multiple zoom levels for detailed exploration

### 📍 **Live Data Tracking**
- **Player Positions**: Real-time player tracking with kingdom-colored markers
- **Village Boundaries**: Visual deed boundaries with village information
- **Auto-Refresh**: Data updates every 5 seconds

### 🌐 **Web-Based Viewer**
- **Interactive Map**: Pan, zoom, and click features using Leaflet.js
- **Layer Controls**: Toggle players and villages on/off
- **Responsive Design**: Works on desktop and mobile
- **Clean UI**: Minimalist interface with essential controls

### 🔌 **Event-Based API**
- **Decoupled Architecture**: Uses ModActionEvent/ModQueryEvent for httpserver integration
- **Extensible**: Easy to add custom endpoints or data sources
- **Client-Ready**: API designed for future client mod integration

## Installation

1. **Prerequisites**:
   - httpserver mod must be installed and configured
   - Server must be running WurmModLoader framework

2. **Deploy mod files**:
   ```bash
   cp mods/livemap/build/mods/livemap/ ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited\ Dedicated\ Server/mods/
   cp mods/livemap/build/mods/livemap.properties ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited\ Dedicated\ Server/mods/
   cp mods/livemap/build/mods/livemap.config ~/.local/share/Steam/steamapps/common/Wurm\ Unlimited\ Dedicated\ Server/mods/
   ```

3. **Restart server**

4. **Access map**: `http://<server-ip>:<httpserver-port>/livemap/`

## Configuration

Edit `mods/livemap.config`:

```properties
# Tile size for map tiles (in pixels)
# Higher values = better quality but more memory/bandwidth
tileSize=256

# Cache time for generated tiles (in minutes)
cacheTimeMinutes=30

# Enable real-time player position tracking
enablePlayerTracking=true

# Enable village/deed display
enableVillageDisplay=true
```

## API Endpoints

### Web Interface
- **GET `/livemap/`** - Interactive map viewer (HTML)
- **GET `/livemap/index.html`** - Same as above

### Map Tiles
- **GET `/livemap/tile/{z}/{x}/{y}.png`** - Map tile at zoom level `z`, coordinates `x,y`
  - Zoom levels: 0-5 (configurable via maxZoom)
  - Returns PNG image

### Live Data API
- **GET `/livemap/api/data`** - JSON with current players and villages
  ```json
  {
    "players": [
      {
        "name": "PlayerName",
        "x": 1234,
        "y": 5678,
        "surface": true,
        "kingdom": 1
      }
    ],
    "villages": [
      {
        "name": "VillageName",
        "startX": 1000,
        "startY": 1000,
        "endX": 1050,
        "endY": 1050,
        "type": "democracy",
        "mayor": "MayorName"
      }
    ]
  }
  ```

- **GET `/livemap/api/config`** - JSON with map configuration
  ```json
  {
    "mapSize": 2048,
    "tileSize": 256,
    "maxZoom": 5,
    "enablePlayers": true,
    "enableVillages": true
  }
  ```

## Architecture

### Rendering Pipeline
```
Server.surfaceMesh → LiveMapRenderer → BufferedImage → PNG → HTTP Response
                            ↓
                    Height-based shading
                    Tile coloring
                    Water effects
```

### Tile Coordinate System
- **Zoom Level 0**: Entire map in 1 tile
- **Zoom Level 1**: Map divided into 2x2 tiles
- **Zoom Level 2**: Map divided into 4x4 tiles
- **Zoom Level z**: Map divided into 2^z × 2^z tiles

### Event-Based Integration
```java
// Register HTTP endpoint
ModActionEvent endpoint = new ModActionEvent("httpserver:register_endpoint");
endpoint.set("modName", "livemap");
endpoint.set("pattern", Pattern.compile("^/tile/(?<path>.*)$"));
endpoint.set("handler", (Function<String, InputStream>) this::handleTileRequest);
EventBus.getInstance().post(endpoint);
```

## Future Client Integration

The API is designed to support future client-side mod integration:

1. **Client mod can consume same API**: `/livemap/api/data` and `/livemap/tile/` endpoints
2. **Handshake protocol**: Client can negotiate with server for map sync
3. **Custom rendering**: Client can use `LiveMapRenderer` algorithm locally
4. **Real-time updates**: Server-Sent Events (SSE) can be added for push updates

### Planned Features
- [ ] Server-Sent Events for real-time push updates
- [ ] Guard tower markers
- [ ] Resource node markers (mines, trees, etc.)
- [ ] Coordinate search and "go to" feature
- [ ] Player click for stats/details
- [ ] Customizable marker icons
- [ ] Cave layer support
- [ ] Measurement tools (distance, area)
- [ ] Screenshot/export functionality

## Technical Details

### Performance Optimizations
- **Tile Caching**: Generated tiles are cached for configurable duration
- **On-Demand Generation**: Tiles only generated when requested
- **Efficient Rendering**: Reuses server mesh data without copies

### Memory Usage
- Tile cache size: ~100KB per tile (256x256 PNG)
- Default cache: 30 minutes = ~100 tiles max = ~10MB RAM

### Bandwidth
- Initial load: ~500KB (HTML + JS libraries)
- Tile requests: ~50-100KB per tile
- Data API: ~1-5KB per request (depends on player/village count)

## Comparison with LiveHudMap

| Feature | LiveHudMap (Client) | LiveMap (Server) |
|---------|-------------------|------------------|
| Rendering Algorithm | ✅ Isometric | ✅ Same algorithm |
| Height Shading | ✅ Advanced | ✅ Same quality |
| Water Effects | ✅ Blue tint | ✅ Same effect |
| Player Marker | ✅ Local only | ✅ All players |
| Village Display | ❌ No | ✅ Yes |
| Web-Based | ❌ No | ✅ Yes |
| Real-time Updates | ✅ Instant | ✅ 5sec refresh |

## Troubleshooting

### Map tiles not loading
- Check httpserver is running: `/livemap/api/config` should return JSON
- Check server logs for errors
- Verify httpserver port is accessible

### Player positions not updating
- Check `enablePlayerTracking=true` in config
- Verify players are online and on surface
- Check browser console for API errors

### Performance issues
- Increase `cacheTimeMinutes` to reduce regeneration
- Decrease `tileSize` to reduce memory usage
- Limit zoom levels by adjusting `maxZoom` in code

## Credits

- **Rendering Algorithm**: Based on LiveHudMap by ago
- **Map Library**: Leaflet.js
- **Framework**: WurmModLoader event-based architecture

## License

This mod is part of the WurmModLoader-CommunityMods project.
