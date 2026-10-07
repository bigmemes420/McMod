# Rooty Menu

Fabric **client-side** mod for **Minecraft 26.3** (Java 25, Fabric Loader 0.19.5, Fabric API). Custom “Rooty Menu” UI with toggles, sliders, mode dropdowns, theme colors, and HUD widgets.

> Client-only cheat / QoL style modules. Use only where allowed.

## Requirements

| Piece | Version |
|--------|---------|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5+ |
| Fabric API | matching 26.3 |
| Java | 25 |

Build: `./gradlew build` → jar under `build/libs/`.

## Keybinds (defaults)

| Key | Action |
|-----|--------|
| **Insert** | Open / close Rooty Menu (`key.rootymenu.open_menu`; rebindable in Controls) |
| **Delete** | Toggle **HUD Edit** mode (`key.rootymenu.hud_edit`) — drag/resize HUD widgets |
| Esc | Close menu / exit HUD Edit |
| Right-click module capsule | Bind / clear a **per-module** toggle key (`ModuleKeybinds`) |

## Menu overview

Custom-drawn UI (no stock button textures): top tabs, capsule toggles (left circle + flat body), cogs for settings, mode dropdowns, color picker.

**Tabs:** General · Player · Visuals · Combat · World · Movement · Misc · (Menu colors panel)

**Menu colors:** Capsule theme editor; persisted separately from module state.

## Modules (by tab)

### Visuals
- **Nametags** — through-walls style tags + scale  
- **Player ESP** / **Mob ESP** — modes, colors, mob picker  
- **Fullbright**  
- **Finder** — block ESP (outline / filled / combined), distance, opacity, per-block colors  
- **Radar** — players/mobs HUD; shapes Square / Circle / Triangle / Star; range; height arrows; drag/resize in HUD Edit  
- **Custom Crosshair** — replaces vanilla crosshair  

### Combat
AutoClicker · Velocity · Reach · Criticals · TriggerBot · Aim Assist · Hitboxes · AutoTotem  

### Movement
Flight (Vanilla / Velocity / Hover / Jetpack) · Elytra Control · Speed · No Fall · AutoSprint · Step · Spider · SafeWalk · Jesus  

### World
Scaffold · FastPlace · Tower · AirPlace  

### Player
Inventory Move · No Slow · Sneak  

### Misc
- **Notifications** — chat messages when modules toggle  
- **Viewer Retention** — HUD media viewers (see below)  

## Custom Crosshair

- **Sources:** pixel grid editor **or** image file from `<gameDir>/crosshairs/`  
- **Formats:** PNG, animated **GIF** (frame delays + disposal)  
- **Resolution:** 16×16 … 512×512 (default 64); editor + baked HUD texture match size; PNG/GIF blit scales to resolution  
- **Color:** Tint Color (multiply by in-game ARGB) or **PNG Colors** (direct / no tint)  
- **Rotate / spin** with adjustable speed  
- Dropdown + Refresh; long names ellipsized  

Create the folder if missing; drop files into `crosshairs/`.

## Viewer Retention

- Files from `<gameDir>/Media/` (created if missing)  
- **Compatible:** PNG, JPEG (`.jpg`/`.jpeg`), animated GIF  
- **Not supported:** MP4 / WebM (no video decoder)  
- Misc tab: dropdown → **Refresh** → **Add** places an instance on the HUD  
- HUD Edit (**Delete**): move / corner-resize; **right-click** a viewer deletes it  
- Instances persist (file, x, y, size)  

## HUD Edit

Press **Delete** in-game (no other screen open):

- Drag panels to move; bottom-right handle to resize  
- Radar + Viewer Retention instances  
- Right-click viewer → remove  
- Delete / Esc exits and saves layout  

## Config & folders

| Path | Purpose |
|------|---------|
| `<gameDir>/config/rootymenu-modules.properties` | Module toggles, sliders, keybinds, last tab, crosshair pixels/source/color/resolution, radar HUD layout, viewer instances, etc. |
| `<gameDir>/config/rootymenu-menu-theme.properties` | Menu theme colors |
| `<gameDir>/crosshairs/` | Custom Crosshair PNG/GIF files |
| `<gameDir>/Media/` | Viewer Retention stills/GIFs |

Fabric mod id / asset namespace: **`rootymenu`**.

## Development

```bash
./gradlew build
./gradlew runClient
```

Loom split source sets: `src/main` + `src/client`. Author commits on this line use `bigmemes420`.

## License

CC0 (see `LICENSE`) — template / project license as shipped.
