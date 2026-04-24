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
