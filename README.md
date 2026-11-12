## ViveCraft Compat

This is a ViveCraft Compat fork for Neoforge 1.21.1, Create & Create Aeronautics

Improves compatibility between vivecraft and various other mods.
Currently supported mods:
- [Create](https://www.curseforge.com/minecraft/mc-mods/create)  
  - Fixed Driving trains  
  - Fixed track hitboxes breaking  
  - Fixed hold to set menus not working  
  - Added a gui to elevator controls
  - Schematics Construction Compat.
  - Scroll support in VR: blueprint, schematic, train HUD and elevator scrolls are
    driven from the controller sticks. Every VR scroll goes through the game's own
    wheel path (`MouseHandler.onScroll`), so anything that works with a mouse wheel
    works with a stick, including the vanilla hotbar and mod scroll handlers.
  - Goggles readout anchored to the block you are looking at instead of floating on
    the HUD, for Create goggles and Aeronautics aviators alike (they share the same
    renderer).
- [First Person Model](https://www.curseforge.com/minecraft/mc-mods/first-person-model)  
  - ~~Render the first person model in the correct place~~ (Fixed in ViveCraft 1.1.0)  
  - Hides the VR hands or allows turning on the Vanilla Hands mode where only the VR hands show, use Double Hands to see both.
- [Jade](https://www.curseforge.com/minecraft/mc-mods/jade)  
  - Register the overlay into Forge's overlay registry.
- [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap)  
  - Register the overlay into Forge's overlay registry.
- [The One Probe](https://www.curseforge.com/minecraft/mc-mods/the-one-probe)  
  - Register the overlay into Forge's overlay registry.
- [Tom's Simple Storage Mod](https://www.curseforge.com/minecraft/mc-mods/toms-storage)  
  - Fix the Wireless Terminal not working correctly.
- [Create Aeronautics / Simulated](https://github.com/Creators-of-Aeronautics/Simulated-Project) / [Sable](https://github.com/ryanhcode/sable)
  - Sable-backed VR carry support for moving physics structures (for both standing and seating on).
    - For Seating on the physics structures, u can have a Free Look Sync.
  - VR interaction fixes for Simulated blocks on moving structures.
    - VR hand ray origin / direction are projected correctly for hand-aimed interactions inside Sable sublevels.
    - Placement direction / yaw for blocks used while carried by a physics structure now derives from VR view direction in sublevel space.
  - Simulated throttle lever compatibility.
    - Hand-driven analog lever: the value follows the angle of your hand off the
      lever's axis, with easing, instead of accumulating controller pitch.
    - The lever's own readout (brass frame, value bar, signal number) is drawn in
      world space above the lever while you hold it, not just on the HUD.
  - World-space hold-interaction panels: lever, steering wheel, physics assembler
    and sublevel handle readouts are attached to the block being operated instead
    of the middle of the screen.
  - Sublevel handle: seated interaction range and stick-driven extendo range.
  - Desktop input fix: typewriter keystrokes are no longer swallowed when VR is not
    running.
  - Teleport towards physics structure problem.

 
### The overlay system (Forge Only):
You can create custom overlays in VR for various vanilla and modded HUD elements.  
These overlays can display a list of HUD elements that can be locked to your hands or float in the world.  
![Overlays image](https://cdn.modrinth.com/data/xnSuzkaS/images/07c118592f3088d15940456f8253572e0421f407.jpeg)
This uses Forge's overlay registry so this mod most likely won't be ported to Fabric.  

- U can copy the overlay now (1.6.0+)

Required mods:
- [ViveCraft](https://www.curseforge.com/minecraft/mc-mods/vivecraft)
- [Customizable Player Models](https://www.curseforge.com/minecraft/mc-mods/custom-player-models), used for the modded GUIs.

### VR compatibility todo

HUD elements that still expect a mouse, and interactions that have no VR mapping
at all. The pattern for the overlay ones is established: cancel the HUD layer on
a shared predicate and re-pose the same draw call in world space, so the mod's own
assets, layout and values are reused.

- [ ] **Toolbox radial menu.** `ToolboxHandlerClient` is a radial menu driven by
      mouse angle from screen centre, so it cannot be bounced to world space the
      way the readouts were. It needs controller-driven selection, or the mod's
      existing `FloatingGui` treatment.
- [ ] **Toolbox refill/restock.** The toolbox's refill interactions look unmapped
      in VR. Audit `ToolboxBlockEntity`'s distribute/refill path and the key it
      hangs off, then decide whether it wants a controller binding or a radial
      action.
- [ ] **Value settings overlay.** `ValueSettingsClient` draws the brass value panel
      while cranking a valve, speed controller or gearshift. It already stores the
      block it is editing (`interactHeldPos`, `interactHeldFace`), so the anchor is
      free.
- [ ] **Blueprint overlay.** `BlueprintOverlayRenderer` draws placement size and
      mode around the cursor; anchor it to the aim point.
- [ ] **Linked controller overlay.** `LinkedControllerClientHandler` draws live
      frequency and key state while holding the linked controller; anchor it to the
      held item.
- [ ] **Typewriter bind overlay.** Simulated's `LinkedTypewriterItemBindHandler`
      still draws its key-binding panel on the HUD layer.
- [ ] **In-game verification** of the hand-driven lever (sign of the angle, the
      0.6 / 0.15–2.5 release thresholds), the billboard placement for the wheel and
      assembler, and the goggles panel scale (`GoggleOverlayBillboard.PANEL_SCALE`,
      0.008).
- [ ] Leave alone on purpose: `RemainingAirOverlay`, `TrainHUD`,
      `TrackPlacementOverlay` and `SchematicHandler`. Those are status indicators
      rather than info about the thing being pointed at, and they read fine on the
      world-space GUI layer.
