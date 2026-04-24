package com.tom.vivecraftcompat;

import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;

import net.minecraft.world.phys.Vec3;

public class VRHelper {
	private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();

	public static boolean isVRPlayerInitialized() {
		return DATA_HOLDER.vrPlayer != null && DATA_HOLDER.vrPlayer.vrdata_world_render != null;
	}

	public static boolean isStanding() {
		return !DATA_HOLDER.vrSettings.seated;
	}

	public static Vec3 getRayOrigin() {
		return getControllerRayOrigin(0);
	}

	public static Vector3f getRayDirection() {
		return getControllerRayDirection(0);
	}

	public static Vec3 getControllerRayOrigin(int controller) {
		return DATA_HOLDER.vrPlayer.vrdata_world_render.getController(controller).getPosition();
	}

	public static Vector3f getControllerRayDirection(int controller) {
		return DATA_HOLDER.vrPlayer.vrdata_world_render.getController(controller).getDirection();
	}
}
