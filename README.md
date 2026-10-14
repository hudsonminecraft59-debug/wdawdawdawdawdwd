# Verdant (Fabric, Minecraft 1.21.11)

Client-side mod with a forest-green click GUI. Press **Right Shift** in a world to open it
(rebind in Options > Controls > Key Binds > Verdant). Use GUI scale 2 or 3 so all four panels fit.

## Visuals
- **Player ESP** - boxes and tracer lines for other players
- **Storage ESP** - chests, trapped chests, ender chests, hoppers, shulker boxes (barrels optional)
- **Spawn Finder** - mob spawners
- **Sus Chunks** - chunks whose blocks add up to a base score, with chat alerts
- Every ESP module: range, through-walls, outline width/opacity, fill, tracer lines, distance fade, rainbow mode

## Combat (off by default)
- **Kill Aura** - hits the closest target in reach (players / hostile mobs / animals), optional lowest-health-first, cooldown aware
- **Auto Crit** - works with Kill Aura: waits until you are falling, optional auto jump
- **Auto Totem** - swaps a totem into the off-hand (always, or below a health you choose)

## Utility
- **Fullbright**, **Auto Sprint**
- **HUD** - watermark, coordinates, FPS, speed (blocks/second), list of enabled modules

## Colors
Per-block on/off plus a hue / saturation / brightness editor with presets and "Apply to all".

Settings save to `config/verdant.properties`.

## Needs
Fabric Loader, Fabric API for 1.21.11, Java 21. Install only on your own client (not on the server).
Many servers ban ESP and combat mods - check the rules first.

## Build
`gradle build` (Gradle 9.2+). The jar is in `build/libs/` - use the one without `-sources`.
