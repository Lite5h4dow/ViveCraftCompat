# ViveCraft Compat

ViveCraft Compat is a NeoForge 1.21.1 compatibility layer that makes Vivecraft's VR controllers and hand tracking feel native in Create, Create: Aeronautics / Simulated, Sable, and the HUD mods around them. It keeps each mod's own assets, layouts and values; it replaces the input and camera-space assumptions that do not exist in VR.

Originally created by **tom5454** and **glomzzz**; the first versions of ViveCraft Compat are theirs. The NeoForge 1.21.1 fork and the VR work here are maintained by **lotus / Lite5h4dow**.

## What it fixes

### Create

- **Trains** — driving, track hitboxes, hold-to-set menus, and a GUI for elevator controls.
- **Schematics** — construction compat and VR scroll support. Blueprint, schematic, train HUD and elevator scrolling are driven from the controller sticks; every VR scroll goes through the game's own mouse-wheel path (`MouseHandler.onScroll`), so anything that works with a wheel works with a stick.
- **Goggles and aviators** — Create's goggles readout (speed, stress impact/capacity, and `IHaveGoggleInformation` lines; Aeronautics aviators share the renderer) is available as the overlay layer `vivecraftcompat:create_goggles`. Pin it to a hand, the HMD, or the world with the overlay helper. The normal HUD copy is untouched unless you detach the layer.
- **Value settings, raycasts and outlines** — VR-aware ray and outline handling for Create's value panels and block outlines.

### Create: Aeronautics / Simulated / Sable

- **Sable-backed VR carry** for moving physics structures, both standing and seated, with optional free-look sync while seated.
- **Sublevel-aware hands** — the VR hand ray origin and direction are projected into the Sable sublevel; block placement direction and yaw derive from the VR view in sublevel space.
- **Hand-driven throttle levers and steering wheels** — floor-mounted levers use relative hand travel from the grip point, not accumulated controller pitch. East/west floor mounts travel along their facing direction like north/south; the X-axis handle swap is collision-only.
- **Hold interactions** — lever, steering wheel, physics assembler and sublevel handle readouts stay on the normal HUD. Use the overlay helper if you want them elsewhere.
- **Linked typewriter** — ESC and the VR menu button close the key-binding screen (saving entries and releasing the user). Desktop keystrokes are no longer swallowed when VR is not running.
- **Seats, elevators, teleporting** — VR seat handling, elevator controls, and fixes for teleporting toward physics structures.

### HUD and overlay helper (NeoForge)

Create overlays from any registered HUD layer and lock them to your **left hand**, **right hand**, **HMD**, or body, or float them in the world. Overlays can be copied (1.6.0+). Jade, JourneyMap and The One Probe are registered as HUD layers.

`vivecraftcompat:create_goggles` exposes Create's goggles/stress panel to the same system. It draws exactly once: the normal HUD copy is suppressed only while the layer is detached, and the overlay capture still renders it.

![Overlays image](https://cdn.modrinth.com/data/xnSuzkaS/images/07c118592f3088d15940456f8253572e0421f407.jpeg)

### Other mods

- **First Person Model** — hide the VR hands, use the Vanilla Hands mode, or show both with Double Hands.
- **Tom's Simple Storage** — fixes the Wireless Terminal in VR.

## Requirements

- [Vivecraft](https://www.curseforge.com/minecraft/mc-mods/vivecraft) (NeoForge 1.21.1)
- [Customizable Player Models](https://www.curseforge.com/minecraft/mc-mods/custom-player-models) — used for the modded GUIs.

Optional: Create, Create: Aeronautics / Simulated + Sable, Jade, JourneyMap, The One Probe, First Person Model, Tom's Simple Storage.

## Known gaps

- **Thaumaturge casters** — casting already works through vanilla item use. The focus radial, research table, and Thaumaturge's caster hand transform are not implemented. Wands, staves and sceptres still go through Thaumaturge's `WandHandRenderer`, which cancels `RenderHandEvent` and bypasses Vivecraft's controller/reverse-hands transform; expect the vanilla hand frame until that is bridged.
- **Cursor-driven Create UIs** — the toolbox radial/refill, value settings overlay, blueprint overlay, linked-controller overlay, and the typewriter bind overlay still need controller selection or world/hand anchoring.
- **Thaumaturge research and caster screens** — the research table, Thaumonomicon, focal manipulator and focus pouch are still cursor-first.

## Credits

- **tom5454** and **glomzzz** — original ViveCraft Compat and its first versions; tom5454 is the original author and MIT copyright holder (see `LICENSE`).
- **lotus / Lite5h4dow** — NeoForge 1.21.1 fork and the VR compatibility work.

## License

MIT. See [LICENSE](LICENSE). Original copyright (c) 2023 tom5454.
