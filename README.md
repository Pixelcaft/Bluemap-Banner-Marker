# 🗺️ PixelCaft's BM Banner Marker

**PixelCaft's BM Banner Marker** is a lightweight, server-side Minecraft mod for NeoForge that automatically adds **banner markers to [BlueMap](https://bluemap.bluecolored.de/)** based on in-game banners.

> ✅ **Client not required** – this mod runs entirely server-side.

---

## 🧭 How to Use

1. **Rename a banner** in an anvil to `#type`, where `type` is a valid marker type (see below).
2. **Place the banner** in the world.
3. A marker will appear automatically on your BlueMap!

Run `/bmbm markertypes` to see which types are available.

---

## 🔧 Features

- 📍 Place a banner → it shows up as a marker on BlueMap.
- 🗑️ Remove the banner → the marker disappears.
- 🎨 Uses banner color as the marker color.
- ⚙️ Custom marker icons supported (16 colors per type).

---

## 💻 Commands

### `/bmbm info`
Shows basic info and help.

### `/bmbm markertypes`
Lists available marker types.

#### 🛠️ Admin Commands
Requires OP (level 2+):

- `/bmbm removemarker <X> <Y> <Z>`  
  Removes a marker on the map

- `/bmbm reload`  
  Reloads the mod config and marker types from file.

---

## 🧪 Technical Details

- 🧩 **Server-side only mod** (`displayTest = IGNORE_SERVER_ONLY`)
- 🛠️ Built for **Minecraft 1.21.1** using **NeoForge**
- 🔵 Designed to integrate with **BlueMap API**
- 📐 Default marker icons are **64x64 pixels**

### Custom Marker Icons
By default, the mod uses built-in marker icons.

To add **custom icons**:

1. Create **16 color variants** (one per dye color) for each marker type.
2. Place them in: `./bluemap/web/maps/<world>/assets/<type>_<color>.png`
3. For example: `./bluemap/web/maps/world_the_nether/assets/house_red.png`
