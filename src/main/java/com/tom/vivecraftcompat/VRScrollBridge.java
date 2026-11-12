package com.tom.vivecraftcompat;

import org.vivecraft.client_vr.provider.InputSimulator;

import net.minecraft.client.Minecraft;

/**
 * The single scroll entry point for VR input.
 * <p>
 * On the desktop every scroll-driven feature arrives the same way: the wheel
 * becomes a call to {@code MouseHandler.onScroll}, which is where NeoForge posts
 * {@code InputEvent.MouseScrollingEvent} and where mod-level mixins hook in.
 * Vivecraft has no wheel, so anything scroll-driven has to be fed by hand.
 * <p>
 * Feeding {@code MouseHandler} rather than the individual handlers means a
 * feature works in VR the moment it works on the desktop: Create's schematic,
 * blueprint and train HUD scrolls, Simulated's honey glue sizing, extendo
 * handle and physics staff distance all resolve through that one call, and
 * whichever of them consumes the input also cancels the vanilla hotbar swap the
 * way it does with a real mouse.
 */
public final class VRScrollBridge {
	private VRScrollBridge() {
	}

	/**
	 * Sends wheel notches into the game's normal scroll path.
	 *
	 * @param notches notches to scroll, positive is "up". Fractional values are
	 *                passed through untouched, matching how continuous input is
	 *                accumulated by the handlers themselves.
	 * @return {@code true} when the input was delivered
	 */
	public static boolean scroll(double notches) {
		if (!VRMode.isVR() || Math.abs(notches) < 1.0E-4D)
			return false;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null || minecraft.player == null)
			return false;

		InputSimulator.scrollMouse(0.0D, notches);
		return true;
	}
}
