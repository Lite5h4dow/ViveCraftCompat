package com.tom.vivecraftcompat.create;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction.KeyListener;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.ViveCraftCompat;
import com.tom.vivecraftcompat.events.VRBindingsEvent;

public class CreateSchematicVRInputBridge {
	private static final String CREATE_MOD_ID = "create";
	private static final double STICK_DEADZONE = 0.55D;
	private static final int INITIAL_SCROLL_DELAY = 10;
	private static final int REPEAT_SCROLL_DELAY = 5;
	private static final Vector2fc ZERO_STICK = new Vector2f();

	private static boolean initialized;
	private static boolean listenersRegistered;
	private static boolean reflectionInitialized;
	private static boolean reflectionUnavailable;
	private static boolean toolMenuFocused;
	private static boolean schematicScrollMode;
	private static boolean schematicActivateToolMode;
	private static boolean schematicAltMode;
	private static boolean schematicCtrlMode;
	private static boolean restoreViewRotation;
	private static boolean attackButtonHeld;
	private static float savedWorldRotation;
	private static float savedSeatedRotation;
	private static int toolMenuScrollCooldown;
	private static int toolMenuScrollDirection;
	private static int toolActionScrollCooldown;
	private static int toolActionScrollDirection;

	private static Object schematicHandler;
	private static Object schematicAndQuillHandler;
	private static Object toolMenuKey;
	private static Method isActiveMethod;
	private static Method schematicAndQuillIsActiveMethod;
	private static Method mouseScrolledMethod;
	private static Method schematicAndQuillMouseScrolledMethod;
	private static Method onKeyInputMethod;
	private static Method getKeybindMethod;

	public static void init() {
		if (initialized || !isCreateLoaded())
			return;

		initialized = true;
		NeoForge.EVENT_BUS.register(CreateSchematicVRInputBridge.class);
	}

	public static void populateListeners() {
		if (listenersRegistered || !isCreateLoaded())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		registerAttackListener(minecraft.options.keyAttack);
		if (GuiHandler.KEY_LEFT_CLICK != minecraft.options.keyAttack)
			registerAttackListener(GuiHandler.KEY_LEFT_CLICK);
		listenersRegistered = true;
	}

	private static void registerAttackListener(KeyMapping keyMapping) {
		MCVR.get().getInputAction(keyMapping).registerListener(new KeyListener() {
			@Override
			public boolean onUnpressed(ControllerType controller) {
				attackButtonHeld = false;
				return false;
			}

			@Override
			public boolean onPressed(ControllerType controller) {
				attackButtonHeld = true;
				return false;
			}

			@Override
			public int getPriority() {
				return 0;
			}
		});
	}

	@SubscribeEvent
	public static void processBindings(VRBindingsEvent event) {
		if (!listenersRegistered)
			populateListeners();

		if (!isUsable())
		{
			schematicAltMode = false;
			schematicCtrlMode = false;
			schematicScrollMode = false;
			schematicActivateToolMode = false;
			return;
		}

		schematicAltMode = isTeleportButtonHeld();
		schematicCtrlMode = isAttackButtonHeld();
		schematicScrollMode = schematicCtrlMode;
		schematicActivateToolMode = schematicCtrlMode && isDeployedSchematicActive();

		if (schematicCtrlMode) {
			KeyMapping attackKey = Minecraft.getInstance().options.keyAttack;
			attackKey.consumeClick();
			attackKey.setDown(false);
		}
	}

	public static void beforeProcessBindings(float seatedRotation) {
		if (!isSchematicActionMode()) {
			restoreViewRotation = false;
			return;
		}

		ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
		savedWorldRotation = dataHolder.vrSettings.worldRotation;
		savedSeatedRotation = seatedRotation;
		restoreViewRotation = true;
	}

	public static float afterProcessBindings(float seatedRotation) {
		boolean usable = isUsable();
		boolean altHeld = usable && schematicAltMode;
		boolean ctrlHeld = usable && schematicCtrlMode;
		schematicScrollMode = ctrlHeld;
		schematicActivateToolMode = ctrlHeld && isDeployedSchematicActive();

		if (!usable) {
			setToolMenuFocused(false);
			resetToolMenuScroll();
			resetToolActionScroll();
		} else {
			if (altHeld && isDeployedSchematicActive()) {
				setToolMenuFocused(true);
				processToolMenuScroll();
				if (MCVR.get().movement != null)
					MCVR.get().movement.set(0.0F, 0.0F);
			} else {
				setToolMenuFocused(false);
				resetToolMenuScroll();
			}

			if (ctrlHeld)
				processToolActionScroll();
			else
				resetToolActionScroll();
		}

		if (!restoreViewRotation)
			return seatedRotation;

		ClientDataHolderVR.getInstance().vrSettings.worldRotation = savedWorldRotation;
		restoreViewRotation = false;
		return savedSeatedRotation;
	}

	public static boolean isSchematicScrollMode() {
		return schematicScrollMode;
	}

	public static boolean isSchematicActivateToolMode() {
		return schematicActivateToolMode;
	}

	public static boolean isSchematicAltMode() {
		return schematicAltMode;
	}

	public static boolean isSchematicCtrlMode() {
		return schematicCtrlMode;
	}

	public static boolean shouldSuppressTeleport() {
		return isUsable() && isTeleportButtonHeld();
	}

	private static void processToolMenuScroll() {
		int direction = getStickScrollDirection(getRawAxis(VivecraftVRMod.INSTANCE.keyFreeMoveStrafe));
		if (direction == 0) {
			resetToolMenuScroll();
			return;
		}

		if (toolMenuScrollDirection != direction || toolMenuScrollCooldown <= 0) {
			boolean sameDirection = toolMenuScrollDirection == direction;
			invokeSchematicMouseScrolled(direction);
			toolMenuScrollDirection = direction;
			toolMenuScrollCooldown = sameDirection ? REPEAT_SCROLL_DELAY : INITIAL_SCROLL_DELAY;
			return;
		}

		toolMenuScrollCooldown--;
	}

	private static void processToolActionScroll() {
		int direction = getStickScrollDirection(getRightStickAxis());
		if (direction == 0) {
			resetToolActionScroll();
			return;
		}

		if (toolActionScrollDirection != direction || toolActionScrollCooldown <= 0) {
			boolean sameDirection = toolActionScrollDirection == direction;
			invokeActiveMouseScrolled(direction);
			toolActionScrollDirection = direction;
			toolActionScrollCooldown = sameDirection ? REPEAT_SCROLL_DELAY : INITIAL_SCROLL_DELAY;
			return;
		}

		toolActionScrollCooldown--;
	}

	private static void resetToolMenuScroll() {
		toolMenuScrollCooldown = 0;
		toolMenuScrollDirection = 0;
	}

	private static void resetToolActionScroll() {
		toolActionScrollCooldown = 0;
		toolActionScrollDirection = 0;
	}

	private static int getStickScrollDirection(Vector2fc axis) {
		double y = axis.y();
		if (Math.abs(y) < STICK_DEADZONE)
			return 0;
		return y > 0.0D ? 1 : -1;
	}

	private static Vector2fc getRightStickAxis() {
		Vector2fc axis = getRawAxis(VivecraftVRMod.INSTANCE.keyRotateAxis);
		if (Math.abs(axis.x()) >= STICK_DEADZONE || Math.abs(axis.y()) >= STICK_DEADZONE)
			return axis;
		return getRawAxis(VivecraftVRMod.INSTANCE.keyFreeMoveRotate);
	}

	private static Vector2fc getRawAxis(KeyMapping keyMapping) {
		VRInputAction action = MCVR.get().getInputAction(keyMapping);
		return action != null ? action.getAxis2D(false) : ZERO_STICK;
	}

	private static boolean isSchematicActionMode() {
		return isUsable() && isAttackButtonHeld();
	}

	private static boolean isUsable() {
		Minecraft minecraft = Minecraft.getInstance();
		return VRMode.isVR() && minecraft.screen == null && minecraft.player != null && isAnySchematicActive();
	}

	private static boolean isAnySchematicActive() {
		return isDeployedSchematicActive() || isSchematicAndQuillActive();
	}

	private static boolean isDeployedSchematicActive() {
		if (!isReflectionReady())
			return false;
		try {
			return (boolean) isActiveMethod.invoke(schematicHandler);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Create schematic state", e);
			return false;
		}
	}

	private static boolean isSchematicAndQuillActive() {
		if (!isReflectionReady())
			return false;
		try {
			return (boolean) schematicAndQuillIsActiveMethod.invoke(schematicAndQuillHandler);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Create schematic and quill state", e);
			return false;
		}
	}

	private static void setToolMenuFocused(boolean focused) {
		if (toolMenuFocused == focused)
			return;

		toolMenuFocused = focused;
		invokeToolMenuKey(focused);
	}

	private static void invokeToolMenuKey(boolean pressed) {
		if (!isReflectionReady())
			return;

		try {
			onKeyInputMethod.invoke(schematicHandler, getToolMenuKeyCode(), pressed);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to forward Create schematic tool-menu key input", e);
		}
	}

	private static void invokeActiveMouseScrolled(double amount) {
		if (isDeployedSchematicActive()) {
			invokeSchematicMouseScrolled(amount);
			return;
		}

		if (isSchematicAndQuillActive())
			invokeSchematicAndQuillMouseScrolled(amount);
	}

	private static void invokeSchematicMouseScrolled(double amount) {
		if (!isReflectionReady())
			return;

		try {
			mouseScrolledMethod.invoke(schematicHandler, amount);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to forward Create schematic scroll input", e);
		}
	}

	private static void invokeSchematicAndQuillMouseScrolled(double amount) {
		if (!isReflectionReady())
			return;

		try {
			schematicAndQuillMouseScrolledMethod.invoke(schematicAndQuillHandler, amount);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to forward Create schematic and quill scroll input", e);
		}
	}

	private static int getToolMenuKeyCode() throws ReflectiveOperationException {
		Object keyMapping = getKeybindMethod.invoke(toolMenuKey);
		if (keyMapping instanceof KeyMapping mapping)
			return mapping.getKey().getValue();
		return GLFW.GLFW_KEY_LEFT_ALT;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean isReflectionReady() {
		if (reflectionInitialized)
			return !reflectionUnavailable;
		if (reflectionUnavailable || !isCreateLoaded())
			return false;

		try {
			Class<?> createClientClass = Class.forName("com.simibubi.create.CreateClient");
			Class<?> schematicHandlerClass =
					Class.forName("com.simibubi.create.content.schematics.client.SchematicHandler");
			Class<?> schematicAndQuillHandlerClass =
					Class.forName("com.simibubi.create.content.schematics.client.SchematicAndQuillHandler");
			Class<?> allKeysClass = Class.forName("com.simibubi.create.AllKeys");

			Field schematicHandlerField = createClientClass.getField("SCHEMATIC_HANDLER");
			Field schematicAndQuillHandlerField = createClientClass.getField("SCHEMATIC_AND_QUILL_HANDLER");
			schematicHandler = schematicHandlerField.get(null);
			schematicAndQuillHandler = schematicAndQuillHandlerField.get(null);
			isActiveMethod = schematicHandlerClass.getMethod("isActive");
			schematicAndQuillIsActiveMethod = schematicAndQuillHandlerClass.getDeclaredMethod("isActive");
			schematicAndQuillIsActiveMethod.setAccessible(true);
			mouseScrolledMethod = schematicHandlerClass.getMethod("mouseScrolled", double.class);
			schematicAndQuillMouseScrolledMethod = schematicAndQuillHandlerClass.getMethod("mouseScrolled", double.class);
			onKeyInputMethod = schematicHandlerClass.getMethod("onKeyInput", int.class, boolean.class);
			getKeybindMethod = allKeysClass.getMethod("getKeybind");
			toolMenuKey = Enum.valueOf((Class<Enum>) allKeysClass.asSubclass(Enum.class), "TOOL_MENU");
			reflectionInitialized = true;
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Create schematic VR input reflection", e);
			return false;
		}
	}

	private static boolean isTeleportButtonHeld() {
		return VivecraftVRMod.INSTANCE.keyTeleport.isDown();
	}

	private static boolean isAttackButtonHeld() {
		return attackButtonHeld || Minecraft.getInstance().options.keyAttack.isDown();
	}

	private static boolean isCreateLoaded() {
		return ModList.get().isLoaded(CREATE_MOD_ID);
	}

	private static void disableReflection(String message, Exception exception) {
		if (!reflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		reflectionUnavailable = true;
		schematicScrollMode = false;
		schematicActivateToolMode = false;
	}
}
