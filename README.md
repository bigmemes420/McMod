# Rooty Menu

Fabric **client-side** mod for **Minecraft 26.3** (Java 25, Fabric Loader 0.19.5, Fabric API). Custom “Rooty Menu” UI with toggles, editable sliders, single- and multi-select dropdowns, theme colors, and HUD widgets.

> Client-only cheat / QoL style modules. Use only where allowed.

Mod id / asset namespace: **`rootymenu`** (renamed from the older `modid` template id).

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
| Esc | Close menu / exit HUD Edit (while typing a slider value, Esc cancels the field first) |
| Right-click module capsule | Bind / clear a **per-module** toggle key (`ModuleKeybinds`) |

## Menu overview

Custom-drawn UI (no stock button textures): top tabs, capsule toggles (left circle + flat body), cogs for settings, mode dropdowns, **multi-select** dropdowns, color picker.

**Tabs:** General · Player · Visuals · Combat · World · Movement · Misc · (Menu colors panel)

**Menu colors:** Capsule theme editor; persisted separately from module state.

**Sliders:** Numeric value boxes are editable — click the number, type digits / decimal / backspace; **Enter** commits and clamps to min/max; **Esc** cancels. (Uses Minecraft 26 SDL text input so typing works after focus.)

**Dropdowns:** Long labels are ellipsized to the widget width. Open panels draw above later rows.

## Modules (by tab)

### Visuals
- **Nametags** — through-walls style tags + scale  
- **Player ESP** — Outline / 2D / 3D; thematic default color **cyan** (`#55E5FF`) when unset; optional outline boxes  
- **Mob ESP** — per-mob selection + colors; **thematic** defaults (zombie green, creeper green, warden teal, etc.); hash fallback only for unknown ids  
- **Fullbright**  
- **Finder** — through-world block ESP  
  - Modes: Outline / Filled / **Combined Fill** (fill + silhouette outlines)  
  - **Optimized** exposed-face meshes; Combined Fill meshes are **cached**  
  - Range capped at **128**; opacity + outline thickness  
  - Per-block colors with **thematic ore defaults** (diamond cyan, redstone red, gold, lapis blue, chests, spawner, ancient debris, …); deepslate shares the mineral tint; custom colors persist and are never wiped when unset  
- **Radar** — players/mobs HUD; shapes Square / Circle / Triangle / Star; range; height arrows; drag/resize in HUD Edit  
- **Custom Crosshair** — replaces vanilla crosshair (see below)  

### Combat
AutoClicker · Velocity · Reach · Criticals · TriggerBot · Aim Assist · **Hitboxes** · AutoTotem  

**Hitboxes:** inflates other living entities’ client hitboxes for easier targeting. **Never affects the local player** (own bounding box / movement stay vanilla).

**Targeting** (Combat tab, right side): reusable multi-select — **Players** / **Hostile mobs** / **Passive mobs**. Persisted as `combatTargeting`. Filters **Aim Assist**, **TriggerBot**, **AutoClicker**, and **Reach** (extended reach withheld while looking at a disallowed target).

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

**Persistence (all settings save/load):** source (pixels vs PNG/GIF), selected filename, color mode, color (hex ARGB), rotate, spin speed, resolution, and pixel grid (base64). Config load order applies resolution before pixels; PNG/GIF textures **reload on first HUD** so relaunch restores image mode even when config ran before the texture manager was ready.

Create the folder if missing; drop files into `crosshairs/`.

## Viewer Retention

- Files from `<gameDir>/Media/` (created if missing)  
- **Compatible:** PNG, JPEG (`.jpg`/`.jpeg`), animated GIF  
- **Not supported:** MP4 / WebM (no video decoder)  
- Misc tab: dropdown → **Refresh** → **Add** places an instance on the HUD  
- HUD Edit (**Delete**): move / corner-resize; **right-click** a viewer deletes it  
- Instances persist (file, x, y, size)  
- Textures **reload on first HUD** after launch (config load can run before the texture manager is ready — relaunch no longer leaves blank panels)  

## HUD Edit

Press **Delete** in-game (no other screen open):

- Drag panels to move; bottom-right handle to resize  
- Radar + Viewer Retention instances  
- Right-click viewer → remove  
- Delete / Esc exits and saves layout  

## Config & folders

| Path | Purpose |
|------|---------|
| `<gameDir>/config/rootymenu-modules.properties` | Module toggles, sliders, keybinds, last tab, `combatTargeting`, crosshair, radar HUD, viewer instances, Finder block colors, etc. |
| `<gameDir>/config/rootymenu-menu-theme.properties` | Menu theme colors |
| `<gameDir>/crosshairs/` | Custom Crosshair PNG/GIF files |
| `<gameDir>/Media/` | Viewer Retention stills/GIFs |

## Development

```bash
./gradlew build
./gradlew runClient
```

Loom split source sets: `src/main` + `src/client`. Author commits on this line use `bigmemes420`.

## License

CC0 (see `LICENSE`) — template / project license as shipped.
