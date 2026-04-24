package com.tom.vivecraftcompat.simulated;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.api.client.data.CloseKeyboardContext;
import org.vivecraft.api.client.data.OpenKeyboardContext;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.InputSimulator;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction.KeyListener;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.tom.cpl.config.ConfigEntry;
import com.tom.vivecraftcompat.Client;
import com.tom.vivecraftcompat.VRHelper;
import com.tom.vivecraftcompat.VRMode;
import com.tom.vivecraftcompat.ViveCraftCompat;
import com.tom.vivecraftcompat.events.VRBindingsEvent;
import com.tom.vivecraftcompat.events.VRUpdateControllersEvent;

public class SimulatedVRInputBridge {
	private static final String SIMULATED_MOD_ID = "simulated";
	private static final String PHYSICS_STAFF_ITEM_CLASS = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem";
	private static final int RIGHT_CONTROLLER = 0;
	private static final int LEFT_CONTROLLER = 1;
	private static final double CONTROLLER_DELTA_SCALE = 8.0D;
	private static final double WHEEL_HAND_DELTA_SCALE = 10.0D;
	private static final double STAFF_DISTANCE_STICK_SCROLL_SCALE = 0.35D;
	private static final double STAFF_ROTATE_STICK_SCALE = 3.0D;
	private static final double STAFF_STICK_DEADZONE = 0.15D;
	private static final Vector2fc ZERO_STICK = new Vector2f();

	private static boolean initialized;
	private static boolean listenersRegistered;
	private static boolean reflectionInitialized;
	private static boolean reflectionUnavailable;
	private static boolean typewriterReflectionInitialized;
	private static boolean typewriterReflectionUnavailable;

	private static Field clickInteractionsField;
	private static Method onUseMethod;
	private static Method onAttackMethod;
	private static Method onMouseMoveMethod;
	private static Method onMouseScrollMethod;
	private static Method resultCancelledMethod;
	private static Method holdInteractionActiveMethod;
	private static Method blockHoldInteractionActiveMethod;
	private static Method getInteractionPosMethod;
	private static Method getPhysicsStaffDragSessionMethod;
	private static Method getTypewriterModeMethod;
	private static Method onTypewriterKeyPressMethod;
	private static Iterable<?> clickInteractions = java.util.List.of();
	private static Object physicsStaffClientHandler;
	private static Object physicsStaffManager;
	private static Object throttleLeverManager;
	private static Object steeringWheelManager;
	private static Object physicsAssemblerManager;

	private static Vector3f previousControllerDirection;
	private static double wheelStartHandAngle = Double.NaN;
	private static double wheelLastRelativeDegrees;
	private static boolean attackButtonHeld;
	private static long lastUsePressTick = Long.MIN_VALUE;
	private static long lastUseReleaseTick = Long.MIN_VALUE;
	private static long lastAttackPressTick = Long.MIN_VALUE;
	private static long lastAttackReleaseTick = Long.MIN_VALUE;
	private static boolean lastUsePressResult;
	private static boolean lastUseReleaseResult;
	private static boolean lastAttackPressResult;
	private static boolean lastAttackReleaseResult;
	private static boolean maskedUseKey;
	private static boolean savedUseKeyDown;
	private static boolean typewriterKeyboardShown;
	private static boolean typewriterKeyboardOverrodePhysical;
	private static boolean typewriterKeyboardDismissed;
	private static boolean savedPhysicalKeyboard;
	private static boolean staffMovementMuted;
	private static boolean staffMovementWasEnabled = true;
	private static boolean restoreStaffViewRotation;
	private static float savedStaffWorldRotation;
	private static float savedStaffSeatedRotation;
	private static int activeTypewriterKey = GLFW.GLFW_KEY_UNKNOWN;
	private static final Set<Integer> pressedTypewriterKeys = new HashSet<>();

	public static void init() {
		if (initialized || !isSimulatedLoaded())
			return;

		initialized = true;
		cleanupRemovedConfig();
		NeoForge.EVENT_BUS.register(SimulatedVRInputBridge.class);
	}

	public static void populateListeners() {
		if (listenersRegistered || !isSimulatedLoaded())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		registerListener(minecraft.options.keyUse, InputKind.USE);
		registerListener(minecraft.options.keyAttack, InputKind.ATTACK);
		if (GuiHandler.KEY_RIGHT_CLICK != minecraft.options.keyUse)
			registerListener(GuiHandler.KEY_RIGHT_CLICK, InputKind.USE);
		if (GuiHandler.KEY_LEFT_CLICK != minecraft.options.keyAttack)
			registerListener(GuiHandler.KEY_LEFT_CLICK, InputKind.ATTACK);
		listenersRegistered = true;
	}

	private static void registerListener(KeyMapping keyMapping, InputKind inputKind) {
		MCVR.get().getInputAction(keyMapping).registerListener(new KeyListener() {
			@Override
			public boolean onUnpressed(ControllerType controller) {
				if (inputKind == InputKind.ATTACK)
					attackButtonHeld = false;
				return fireButton(inputKind, GLFW.GLFW_RELEASE);
			}

			@Override
			public boolean onPressed(ControllerType controller) {
				if (inputKind == InputKind.ATTACK)
					attackButtonHeld = true;
				return fireButton(inputKind, GLFW.GLFW_PRESS);
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

		if (typewriterKeyboardShown && VivecraftVRMod.INSTANCE.keyMenuButton.consumeClick()) {
			dismissTypewriterKeyboardFromInput();
			return;
		}

		if (!isUsable()) {
			restoreStaffInputMutes();
			return;
		}

		if (!isPhysicsStaffDragging()) {
			restoreStaffInputMutes();
			return;
		}

		VivecraftVRMod.INSTANCE.keyHotbarPrev.consumeClick();
		VivecraftVRMod.INSTANCE.keyHotbarNext.consumeClick();

		boolean leftGripHeld = isLeftLowerTriggerHeld();
		setStaffMovementMuted(leftGripHeld);
		if (leftGripHeld && MCVR.get().movement != null)
			MCVR.get().movement.set(0.0F, 0.0F);
	}

	public static void beforeProcessBindings(float seatedRotation) {
		if (!isPhysicsStaffRotateMode()) {
			restoreStaffViewRotation = false;
			return;
		}

		ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
		savedStaffWorldRotation = dataHolder.vrSettings.worldRotation;
		savedStaffSeatedRotation = seatedRotation;
		restoreStaffViewRotation = true;
	}

	public static float afterProcessBindings(float seatedRotation) {
		processPhysicsStaffStickInput();

		if (!restoreStaffViewRotation)
			return seatedRotation;

		ClientDataHolderVR.getInstance().vrSettings.worldRotation = savedStaffWorldRotation;
		restoreStaffViewRotation = false;
		return savedStaffSeatedRotation;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void maskUseKeyDuringThrottleLeverTick(ClientTickEvent.Post event) {
		restoreMaskedUseKey();

		if (!isUsable() || !isInputLockActive())
			return;

		KeyMapping useKey = Minecraft.getInstance().options.keyUse;
		if (!useKey.isDown())
			return;

		savedUseKeyDown = true;
		maskedUseKey = true;
		useKey.setDown(false);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void restoreUseKeyAfterThrottleLeverTick(ClientTickEvent.Post event) {
		restoreMaskedUseKey();
	}

	@SubscribeEvent
	public static void showKeyboardForLinkedTypewriter(ClientTickEvent.Post event) {
		if (!VRMode.isVR() || Minecraft.getInstance().player == null) {
			typewriterKeyboardDismissed = false;
			releaseAllTypewriterKeys();
			hideTypewriterKeyboard();
			return;
		}

		if (!isTypewriterModeActive()) {
			typewriterKeyboardDismissed = false;
			releaseAllTypewriterKeys();
			hideTypewriterKeyboard();
			return;
		}

		if (typewriterKeyboardDismissed) {
			releaseAllTypewriterKeys();
			hideTypewriterKeyboard();
			return;
		}

		boolean keyboardModeChanged = usePhysicalTypewriterKeyboard();
		if (keyboardModeChanged && KeyboardHandler.SHOWING)
			KeyboardHandler.hideOverlay(CloseKeyboardContext.FORCE);

		if (!KeyboardHandler.SHOWING)
			KeyboardHandler.showOverlay(OpenKeyboardContext.FORCE);
		typewriterKeyboardShown = KeyboardHandler.SHOWING;
		if (typewriterKeyboardShown)
			KeyboardHandler.orientOverlay(false);
		if (!typewriterKeyboardShown)
			restoreTypewriterKeyboardSettings();
	}

	public static boolean pressTypewriterKey(int key) {
		if (key == GLFW.GLFW_KEY_ESCAPE)
			return dismissTypewriterKeyboardFromInput();

		if (!isTypewriterKeyboardInputMode() || key == GLFW.GLFW_KEY_UNKNOWN)
			return false;

		if (!pressedTypewriterKeys.add(key))
			return true;

		activeTypewriterKey = key;
		fireTypewriterKey(key, 0, GLFW.GLFW_PRESS, 0);
		return true;
	}

	public static boolean releaseTypewriterKey(int key) {
		if (key == GLFW.GLFW_KEY_UNKNOWN)
			return false;

		if (!pressedTypewriterKeys.remove(key))
			return isTypewriterKeyboardInputMode();

		if (isTypewriterKeyboardInputMode())
			fireTypewriterKey(key, 0, GLFW.GLFW_RELEASE, 0);
		if (activeTypewriterKey == key)
			activeTypewriterKey = GLFW.GLFW_KEY_UNKNOWN;
		return true;
	}

	public static boolean typeTypewriterCharacter(char character) {
		if (!isTypewriterKeyboardInputMode())
			return false;

		int key = getTypewriterKeyForCharacter(character);
		if (key == GLFW.GLFW_KEY_UNKNOWN)
			return false;

		if (pressedTypewriterKeys.contains(key))
			return true;

		fireTypewriterKey(key, 0, GLFW.GLFW_PRESS, 0);
		fireTypewriterKey(key, 0, GLFW.GLFW_RELEASE, 0);
		return true;
	}

	@SubscribeEvent
	public static void updateControllerDeltas(VRUpdateControllersEvent event) {
		if (!isUsable()) {
			previousControllerDirection = null;
			resetWheelHandAngle();
			restoreStaffInputMutes();
			return;
		}

		double yawDelta = 0.0D;
		double pitchDelta = 0.0D;
		if (VRMode.isVRStanding()) {
			Vector3f direction = new Vector3f(VRHelper.getRayDirection());
			if (previousControllerDirection != null) {
				yawDelta = wrapDegrees(Math.toDegrees(yaw(direction) - yaw(previousControllerDirection)));
				pitchDelta = Math.toDegrees(pitch(direction) - pitch(previousControllerDirection));
			}
			previousControllerDirection = direction;
		} else {
			previousControllerDirection = null;
		}

		double wheelYawDelta = getWheelHandYawDelta();
		if (Math.abs(yawDelta) < 0.001D && Math.abs(pitchDelta) < 0.001D && Math.abs(wheelYawDelta) < 0.001D)
			return;

		fireMouseMove(yawDelta * CONTROLLER_DELTA_SCALE, pitchDelta * CONTROLLER_DELTA_SCALE, wheelYawDelta, 0.0D, 0.0D);
	}

	private static boolean fireButton(InputKind inputKind, int action) {
		if (!isUsable())
			return false;
		Boolean duplicateResult = getDuplicateButtonResult(inputKind, action);
		if (duplicateResult != null)
			return duplicateResult;

		Minecraft minecraft = Minecraft.getInstance();
		KeyMapping keyMapping = inputKind == InputKind.USE ? minecraft.options.keyUse : minecraft.options.keyAttack;
		Method method = inputKind == InputKind.USE ? onUseMethod : onAttackMethod;

		boolean cancelled = false;
		for (Object callback : getClickInteractions()) {
			cancelled |= invokeBooleanResult(method, callback, 0, action, keyMapping);
		}
		recordButtonResult(inputKind, action, cancelled);
		return cancelled;
	}

	private static void fireMouseMove(double yaw, double pitch, double wheelYaw, double staffYaw, double staffPitch) {
		if (!isReflectionReady())
			return;

		for (Object callback : getClickInteractions()) {
			double callbackYaw = yaw;
			double callbackPitch = pitch;

			if (callback == throttleLeverManager || callback == physicsAssemblerManager) {
				callbackPitch = -callbackPitch;
			} else if (callback == steeringWheelManager) {
				callbackYaw = wheelYaw;
				callbackPitch = 0.0D;
			} else if (callback == physicsStaffManager) {
				callbackYaw = staffYaw;
				callbackPitch = staffPitch;
			}

			invokeBooleanResult(onMouseMoveMethod, callback, callbackYaw, callbackPitch);
		}
	}

	private static void fireMouseScroll(double amount) {
		if (!isReflectionReady() || physicsStaffManager == null)
			return;

		invokeBooleanResult(onMouseScrollMethod, physicsStaffManager, 0.0D, amount);
	}

	private static boolean invokeBooleanResult(Method method, Object target, Object... args) {
		try {
			Object result = method.invoke(target, args);
			return result != null && (boolean) resultCancelledMethod.invoke(result);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to forward a Simulated VR input", e);
			return false;
		}
	}

	private static Iterable<?> getClickInteractions() {
		return clickInteractions;
	}

	public static HitResult pickFromStaffHand(Player player, double range, float partialTick, boolean hitFluids) {
		if (!VRMode.isVRHandAiming())
			return player.pick(range, partialTick, hitFluids);

		Vec3 origin = getStaffHandRayOrigin(player, partialTick);
		Vec3 direction = getStaffHandRayDirection(player);
		Vec3 target = origin.add(direction.scale(range));
		ClipContext.Fluid fluidMode = hitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
		HitResult hit = SableVRBridge.clipWithRenderPose(player.level(),
				new ClipContext(origin, target, ClipContext.Block.OUTLINE, fluidMode, player));
		return hit != null ? hit : player.level().clip(new ClipContext(origin, target, ClipContext.Block.OUTLINE, fluidMode, player));
	}

	public static HitResult pickFromHand(Player player, double range, float partialTick, boolean hitFluids) {
		return pickFromStaffHand(player, range, partialTick, hitFluids);
	}

	public static HitResult pickFromRightHand(Player player, double range, float partialTick, boolean hitFluids) {
		if (!VRMode.isVRHandAiming())
			return player.pick(range, partialTick, hitFluids);

		Vec3 origin = SableVRBridge.getSeatedHandRayOrigin(player, VRHelper.getControllerRayOrigin(RIGHT_CONTROLLER), partialTick);
		Vec3 direction = SableVRBridge.getSeatedHandRayDirection(player, getRightHandRayDirection());
		Vec3 target = origin.add(direction.scale(range));
		ClipContext.Fluid fluidMode = hitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
		HitResult hit = SableVRBridge.clipWithRenderPose(player.level(),
				new ClipContext(origin, target, ClipContext.Block.OUTLINE, fluidMode, player));
		return hit != null ? hit : player.level().clip(new ClipContext(origin, target, ClipContext.Block.OUTLINE, fluidMode, player));
	}

	public static Vec3 getRightHandRayDirection() {
		Vector3f direction = VRHelper.getControllerRayDirection(RIGHT_CONTROLLER);
		return new Vec3(direction.x, direction.y, direction.z).normalize();
	}

	public static Vec3 getStaffHandRayOrigin(Player player) {
		return getStaffHandRayOrigin(player, getPartialTick());
	}

	private static Vec3 getStaffHandRayOrigin(Player player, float partialTick) {
		return SableVRBridge.getSeatedHandRayOrigin(player, getStaffHandPickRayOrigin(player), partialTick);
	}

	public static Vec3 getStaffHandRayDirection(Player player) {
		return SableVRBridge.getSeatedHandRayDirection(player, getStaffHandPickRayDirection(player));
	}

	private static Vec3 getStaffHandPickRayOrigin(Player player) {
		return VRHelper.getControllerRayOrigin(getStaffController(player));
	}

	private static Vec3 getStaffHandPickRayDirection(Player player) {
		Vector3f direction = VRHelper.getControllerRayDirection(getStaffController(player));
		return new Vec3(direction.x, direction.y, direction.z).normalize();
	}

	private static float getPartialTick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return 1.0F;
		boolean advanceGameTime = minecraft.player == null || !minecraft.level.tickRateManager().isEntityFrozen(minecraft.player);
		return minecraft.getTimer().getGameTimeDeltaPartialTick(advanceGameTime);
	}

	private static boolean isUsable() {
		Minecraft minecraft = Minecraft.getInstance();
		return VRMode.isVR() && minecraft.screen == null && minecraft.player != null && !minecraft.player.isSpectator() && isReflectionReady();
	}

	private static boolean isReflectionReady() {
		if (reflectionInitialized)
			return !reflectionUnavailable;
		if (reflectionUnavailable || !isSimulatedLoaded())
			return false;

		try {
			Class<?> clickInteractionsClass = Class.forName("dev.simulated_team.simulated.index.SimClickInteractions");
			Class<?> callbackClass = Class.forName("dev.simulated_team.simulated.util.click_interactions.InteractCallback");
			Class<?> resultClass = Class.forName("dev.simulated_team.simulated.util.click_interactions.InteractCallback$Result");
			Class<?> holdInteractionManagerClass = Class.forName("dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager");
			Class<?> blockHoldInteractionClass = Class.forName("dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction");
			Class<?> simulatedClientClass = Class.forName("dev.simulated_team.simulated.SimulatedClient");

			clickInteractionsField = clickInteractionsClass.getField("CLICK_INTERACTION_ENTRIES");
			Object interactionEntries = clickInteractionsField.get(null);
			if (interactionEntries instanceof Iterable<?> iterable) {
				clickInteractions = iterable;
			} else {
				throw new IllegalStateException("CLICK_INTERACTION_ENTRIES is not iterable");
			}
			physicsStaffClientHandler = simulatedClientClass.getField("PHYSICS_STAFF_CLIENT_HANDLER").get(null);
			physicsStaffManager = clickInteractionsClass.getField("PHYSICS_STAFF_MANAGER").get(null);
			throttleLeverManager = clickInteractionsClass.getField("THROTTLE_LEVER_MANAGER").get(null);
			steeringWheelManager = clickInteractionsClass.getField("STEERING_WHEEL_MANAGER").get(null);
			physicsAssemblerManager = clickInteractionsClass.getField("PHYSICS_ASSEMBLER_MANAGER").get(null);
			onUseMethod = callbackClass.getMethod("onUse", int.class, int.class, KeyMapping.class);
			onAttackMethod = callbackClass.getMethod("onAttack", int.class, int.class, KeyMapping.class);
			onMouseMoveMethod = callbackClass.getMethod("onMouseMove", double.class, double.class);
			onMouseScrollMethod = callbackClass.getMethod("onScroll", double.class, double.class);
			resultCancelledMethod = resultClass.getMethod("cancelled");
			holdInteractionActiveMethod = holdInteractionManagerClass.getMethod("isActive");
			blockHoldInteractionActiveMethod = blockHoldInteractionClass.getMethod("isActive");
			getInteractionPosMethod = blockHoldInteractionClass.getMethod("getInteractionPos");
			getPhysicsStaffDragSessionMethod = physicsStaffClientHandler.getClass().getMethod("getDragSession");
			reflectionInitialized = true;
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to initialize Simulated VR input reflection", e);
			return false;
		}
	}

	public static boolean isHoldInteractionActive() {
		if (!isReflectionReady())
			return false;
		try {
			return (boolean) holdInteractionActiveMethod.invoke(null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Simulated hold interaction state", e);
			return false;
		}
	}

	private static boolean isThrottleLeverActive() {
		return isBlockHoldInteractionActive(throttleLeverManager, "throttle lever");
	}

	private static boolean isSteeringWheelActive() {
		return isBlockHoldInteractionActive(steeringWheelManager, "steering wheel");
	}

	private static boolean isPhysicsAssemblerActive() {
		return isBlockHoldInteractionActive(physicsAssemblerManager, "physics assembler");
	}

	private static boolean isInputLockActive() {
		return isThrottleLeverActive() || isSteeringWheelActive() || isPhysicsAssemblerActive();
	}

	private static void restoreMaskedUseKey() {
		if (!maskedUseKey)
			return;

		Minecraft.getInstance().options.keyUse.setDown(savedUseKeyDown && isInputLockActive());
		maskedUseKey = false;
		savedUseKeyDown = false;
	}

	private static double getWheelHandYawDelta() {
		if (!isBlockHoldInteractionActive(steeringWheelManager, "steering wheel")) {
			resetWheelHandAngle();
			return 0.0D;
		}

		double handAngle = getWheelHandAngle();
		if (Double.isNaN(handAngle)) {
			resetWheelHandAngle();
			return 0.0D;
		}

		if (Double.isNaN(wheelStartHandAngle)) {
			wheelStartHandAngle = handAngle;
			wheelLastRelativeDegrees = 0.0D;
			return 0.0D;
		}

		double rawRelativeDegrees = Math.toDegrees(handAngle - wheelStartHandAngle);
		double relativeDegrees = wheelLastRelativeDegrees + wrapDegrees(rawRelativeDegrees - wheelLastRelativeDegrees);

		if (isWheelStepModifierDown()) {
			relativeDegrees = Math.round(relativeDegrees / 45.0D) * 45.0D;
		}

		double delta = relativeDegrees - wheelLastRelativeDegrees;
		if (Math.abs(delta) < 0.001D)
			return 0.0D;

		wheelLastRelativeDegrees = relativeDegrees;
		return -delta * WHEEL_HAND_DELTA_SCALE;
	}

	private static void resetWheelHandAngle() {
		wheelStartHandAngle = Double.NaN;
		wheelLastRelativeDegrees = 0.0D;
	}

	private static double getPhysicsStaffDistanceScrollDelta() {
		if (!isPhysicsStaffDragging() || !isLeftLowerTriggerHeld())
			return 0.0D;

		Vector2fc axis = getRawAxis(VivecraftVRMod.INSTANCE.keyFreeMoveStrafe);
		double delta = applyStickDeadzone(axis.y());

		if (delta == 0.0D)
			return 0.0D;

		return delta * STAFF_DISTANCE_STICK_SCROLL_SCALE;
	}

	private static Vector2fc getPhysicsStaffRotateDelta() {
		if (!isPhysicsStaffRotateMode())
			return ZERO_STICK;

		return getPhysicsStaffRotateStickDelta();
	}

	private static Vector2f getPhysicsStaffRotateStickDelta() {
		Vector2fc axis = getRawAxis(VivecraftVRMod.INSTANCE.keyRotateAxis);
		double x = applyStickDeadzone(axis.x());
		double y = applyStickDeadzone(axis.y());

		if (x == 0.0D && y == 0.0D) {
			Vector2fc fallbackAxis = getRawAxis(VivecraftVRMod.INSTANCE.keyFreeMoveRotate);
			x = applyStickDeadzone(fallbackAxis.x());
			y = applyStickDeadzone(fallbackAxis.y());
		}

		return new Vector2f((float) (x * STAFF_ROTATE_STICK_SCALE), (float) (y * STAFF_ROTATE_STICK_SCALE));
	}

	private static void processPhysicsStaffStickInput() {
		if (!isUsable() || !isPhysicsStaffDragging())
			return;

		double staffScrollDelta = getPhysicsStaffDistanceScrollDelta();
		Vector2fc staffRotateDelta = getPhysicsStaffRotateDelta();
		if (Math.abs(staffRotateDelta.x()) >= 0.001D || Math.abs(staffRotateDelta.y()) >= 0.001D)
			fireMouseMove(0.0D, 0.0D, 0.0D, staffRotateDelta.x(), staffRotateDelta.y());
		if (Math.abs(staffScrollDelta) >= 0.001D)
			fireMouseScroll(staffScrollDelta);
	}

	private static double getWheelHandAngle() {
		BlockPos pos = getInteractionPos(steeringWheelManager);
		Minecraft minecraft = Minecraft.getInstance();
		if (pos == null || minecraft.level == null)
			return Double.NaN;

		BlockState state = minecraft.level.getBlockState(pos);
		Direction facing = state.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH);
		boolean onFloor = getBooleanProperty(state, "on_floor", true);
		Vec3 handOrigin = SableVRBridge.getSeatedHandRayOrigin(minecraft.player, VRHelper.getRayOrigin(), getPartialTick());
		handOrigin = SableVRBridge.transformPositionIntoContainingSubLevel(minecraft.level, Vec3.atCenterOf(pos), handOrigin);
		Vec3 fromCenter = handOrigin.subtract(Vec3.atCenterOf(pos));
		Vec3 right = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
		Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal());
		double x = fromCenter.dot(right);
		double y = fromCenter.dot(forward);

		if (x * x + y * y < 0.01D) {
			Vec3 direction = SableVRBridge.getSeatedHandRayDirection(minecraft.player, getRightHandRayDirection());
			direction = SableVRBridge.transformDirectionIntoContainingSubLevel(minecraft.level, Vec3.atCenterOf(pos), direction);
			x = direction.dot(right);
			y = direction.dot(forward);
			if (x * x + y * y < 0.01D)
				return Double.NaN;
		}

		double angle = Math.atan2(x, y);
		return onFloor ? angle : -angle;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean getBooleanProperty(BlockState state, String name, boolean fallback) {
		for (Property<?> property : state.getProperties()) {
			if (property.getName().equals(name)) {
				Object value = state.getValue((Property) property);
				if (value instanceof Boolean b)
					return b;
			}
		}
		return fallback;
	}

	private static boolean isWheelStepModifierDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return attackButtonHeld || minecraft.options.keyAttack.isDown();
	}

	private static void restoreStaffInputMutes() {
		setStaffMovementMuted(false);
	}

	private static void setStaffMovementMuted(boolean muted) {
		if (staffMovementMuted == muted)
			return;

		VRInputAction moveAction = getInputAction(VivecraftVRMod.INSTANCE.keyFreeMoveStrafe);
		if (muted) {
			staffMovementWasEnabled = isActionEnabled(moveAction);
			setActionEnabled(moveAction, false);
		} else {
			setActionEnabled(moveAction, staffMovementWasEnabled);
		}
		staffMovementMuted = muted;
	}

	private static Vector2fc getRawAxis(KeyMapping keyMapping) {
		VRInputAction action = getInputAction(keyMapping);
		return action != null ? action.getAxis2D(false) : ZERO_STICK;
	}

	private static VRInputAction getInputAction(KeyMapping keyMapping) {
		return MCVR.get().getInputAction(keyMapping);
	}

	private static boolean isActionEnabled(VRInputAction action) {
		return action == null || action.isEnabledRaw();
	}

	private static void setActionEnabled(VRInputAction action, boolean enabled) {
		if (action != null)
			action.setEnabled(enabled);
	}

	public static boolean isPhysicsStaffRotateMode() {
		return isUsable() && isPhysicsStaffDragging() && isRightLowerTriggerHeld();
	}

	private static boolean isPhysicsStaffDragging() {
		if (!isReflectionReady() || physicsStaffClientHandler == null || getPhysicsStaffDragSessionMethod == null)
			return false;
		try {
			return getPhysicsStaffDragSessionMethod.invoke(physicsStaffClientHandler) != null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Simulated physics staff drag state", e);
			return false;
		}
	}

	private static Boolean getDuplicateButtonResult(InputKind inputKind, int action) {
		long inputTick = getInputTick();
		if (inputKind == InputKind.USE) {
			if (action == GLFW.GLFW_PRESS && lastUsePressTick == inputTick)
				return lastUsePressResult;
			if (action == GLFW.GLFW_RELEASE && lastUseReleaseTick == inputTick)
				return lastUseReleaseResult;
		} else if (inputKind == InputKind.ATTACK) {
			if (action == GLFW.GLFW_PRESS && lastAttackPressTick == inputTick)
				return lastAttackPressResult;
			if (action == GLFW.GLFW_RELEASE && lastAttackReleaseTick == inputTick)
				return lastAttackReleaseResult;
		}
		return null;
	}

	private static boolean isTypewriterModeActive() {
		String modeName = getTypewriterModeName();
		return "ACTIVE".equals(modeName) || "SCREEN_BINDING".equals(modeName) || "BINDING_FROM_ITEM".equals(modeName);
	}

	private static boolean isTypewriterKeyboardInputMode() {
		String modeName = getTypewriterModeName();
		return "ACTIVE".equals(modeName) || "BINDING_FROM_ITEM".equals(modeName);
	}

	private static String getTypewriterModeName() {
		if (!isTypewriterReflectionReady())
			return "";
		try {
			Object mode = getTypewriterModeMethod.invoke(null);
			return mode != null ? mode.toString() : "";
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableTypewriterReflection("Failed to query Simulated linked typewriter state", e);
			return "";
		}
	}

	public static boolean dismissTypewriterKeyboardFromInput() {
		if (!typewriterKeyboardShown && !typewriterKeyboardOverrodePhysical)
			return false;

		typewriterKeyboardDismissed = true;
		releaseAllTypewriterKeys();
		hideTypewriterKeyboard();
		return true;
	}

	private static void hideTypewriterKeyboard() {
		disconnectTypewriterIfActive();
		if (typewriterKeyboardShown) {
			KeyboardHandler.hideOverlay(CloseKeyboardContext.FORCE);
			typewriterKeyboardShown = false;
		}
		restoreTypewriterKeyboardSettings();
	}

	private static void disconnectTypewriterIfActive() {
		if (!"ACTIVE".equals(getTypewriterModeName()))
			return;

		fireTypewriterKey(GLFW.GLFW_KEY_ESCAPE, 0, GLFW.GLFW_PRESS, 0);
		fireTypewriterKey(GLFW.GLFW_KEY_ESCAPE, 0, GLFW.GLFW_RELEASE, 0);
	}

	private static boolean usePhysicalTypewriterKeyboard() {
		ClientDataHolderVR dataHolder = ClientDataHolderVR.getInstance();
		if (!typewriterKeyboardOverrodePhysical) {
			savedPhysicalKeyboard = dataHolder.vrSettings.physicalKeyboard;
			typewriterKeyboardOverrodePhysical = true;
		}

		boolean changed = !dataHolder.vrSettings.physicalKeyboard;
		dataHolder.vrSettings.physicalKeyboard = true;
		return changed;
	}

	private static void restoreTypewriterKeyboardSettings() {
		if (!typewriterKeyboardOverrodePhysical)
			return;

		ClientDataHolderVR.getInstance().vrSettings.physicalKeyboard = savedPhysicalKeyboard;
		typewriterKeyboardOverrodePhysical = false;
	}

	private static void releaseAllTypewriterKeys() {
		if (pressedTypewriterKeys.isEmpty())
			return;

		if (isTypewriterKeyboardInputMode()) {
			for (int key : Set.copyOf(pressedTypewriterKeys)) {
				fireTypewriterKey(key, 0, GLFW.GLFW_RELEASE, 0);
			}
		}
		pressedTypewriterKeys.clear();
		activeTypewriterKey = GLFW.GLFW_KEY_UNKNOWN;
	}

	private static void fireTypewriterKey(int key, int scanCode, int action, int modifiers) {
		if (!isTypewriterReflectionReady())
			return;
		try {
			onTypewriterKeyPressMethod.invoke(null, key, scanCode, action, modifiers);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableTypewriterReflection("Failed to forward Simulated linked typewriter key input", e);
		}
	}

	private static int getTypewriterKeyForCharacter(char character) {
		if (character >= 'a' && character <= 'z')
			return GLFW.GLFW_KEY_A + character - 'a';
		if (character >= 'A' && character <= 'Z')
			return GLFW.GLFW_KEY_A + character - 'A';
		if (character >= '0' && character <= '9')
			return GLFW.GLFW_KEY_0 + character - '0';

		return switch (character) {
		case ' ' -> GLFW.GLFW_KEY_SPACE;
		case '!' -> GLFW.GLFW_KEY_1;
		case '"' -> GLFW.GLFW_KEY_APOSTROPHE;
		case '#' -> GLFW.GLFW_KEY_3;
		case '$' -> GLFW.GLFW_KEY_4;
		case '%' -> GLFW.GLFW_KEY_5;
		case '&' -> GLFW.GLFW_KEY_7;
		case '\'' -> GLFW.GLFW_KEY_APOSTROPHE;
		case '(' -> GLFW.GLFW_KEY_9;
		case ')' -> GLFW.GLFW_KEY_0;
		case '*' -> GLFW.GLFW_KEY_8;
		case '+' -> GLFW.GLFW_KEY_EQUAL;
		case ',' -> GLFW.GLFW_KEY_COMMA;
		case '-' -> GLFW.GLFW_KEY_MINUS;
		case '.' -> GLFW.GLFW_KEY_PERIOD;
		case ';' -> GLFW.GLFW_KEY_SEMICOLON;
		case '<' -> GLFW.GLFW_KEY_COMMA;
		case '=' -> GLFW.GLFW_KEY_EQUAL;
		case '>' -> GLFW.GLFW_KEY_PERIOD;
		case '?' -> GLFW.GLFW_KEY_SLASH;
		case '@' -> GLFW.GLFW_KEY_2;
		case '[' -> GLFW.GLFW_KEY_LEFT_BRACKET;
		case '\\' -> GLFW.GLFW_KEY_BACKSLASH;
		case ']' -> GLFW.GLFW_KEY_RIGHT_BRACKET;
		case '^' -> GLFW.GLFW_KEY_6;
		case '_' -> GLFW.GLFW_KEY_MINUS;
		case '`' -> GLFW.GLFW_KEY_GRAVE_ACCENT;
		case '{' -> GLFW.GLFW_KEY_LEFT_BRACKET;
		case '|' -> GLFW.GLFW_KEY_BACKSLASH;
		case '}' -> GLFW.GLFW_KEY_RIGHT_BRACKET;
		case '~' -> GLFW.GLFW_KEY_GRAVE_ACCENT;
		default -> GLFW.GLFW_KEY_UNKNOWN;
		};
	}

	private static boolean isTypewriterReflectionReady() {
		if (typewriterReflectionInitialized)
			return !typewriterReflectionUnavailable;
		if (typewriterReflectionUnavailable || !isSimulatedLoaded())
			return false;

		try {
			Class<?> linkedTypewriterInteractionHandlerClass = Class
					.forName("dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler");
			getTypewriterModeMethod = linkedTypewriterInteractionHandlerClass.getMethod("getMode");
			onTypewriterKeyPressMethod = linkedTypewriterInteractionHandlerClass.getMethod("onKeyPress", int.class, int.class, int.class,
					int.class);
			typewriterReflectionInitialized = true;
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableTypewriterReflection("Failed to initialize Simulated linked typewriter reflection", e);
			return false;
		}
	}

	private static void recordButtonResult(InputKind inputKind, int action, boolean result) {
		long inputTick = getInputTick();
		if (inputKind == InputKind.USE) {
			if (action == GLFW.GLFW_PRESS) {
				lastUsePressTick = inputTick;
				lastUsePressResult = result;
			} else if (action == GLFW.GLFW_RELEASE) {
				lastUseReleaseTick = inputTick;
				lastUseReleaseResult = result;
			}
		} else if (inputKind == InputKind.ATTACK) {
			if (action == GLFW.GLFW_PRESS) {
				lastAttackPressTick = inputTick;
				lastAttackPressResult = result;
			} else if (action == GLFW.GLFW_RELEASE) {
				lastAttackReleaseTick = inputTick;
				lastAttackReleaseResult = result;
			}
		}
	}

	private static long getInputTick() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null ? minecraft.level.getGameTime() : Long.MIN_VALUE;
	}

	private static boolean isLeftLowerTriggerHeld() {
		return VivecraftVRMod.INSTANCE.keyHotbarPrev.isDown();
	}

	private static boolean isRightLowerTriggerHeld() {
		return VivecraftVRMod.INSTANCE.keyHotbarNext.isDown();
	}

	private static int getStaffController(Player player) {
		if (player != null && isPhysicsStaffItem(player.getOffhandItem().getItem())
				&& !isPhysicsStaffItem(player.getMainHandItem().getItem()))
			return LEFT_CONTROLLER;
		return RIGHT_CONTROLLER;
	}

	private static boolean isPhysicsStaffItem(Item item) {
		return item != null && item.getClass().getName().equals(PHYSICS_STAFF_ITEM_CLASS);
	}

	private static boolean isBlockHoldInteractionActive(Object manager, String name) {
		if (!isReflectionReady() || manager == null)
			return false;
		try {
			return (boolean) blockHoldInteractionActiveMethod.invoke(manager);
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Simulated " + name + " state", e);
			return false;
		}
	}

	private static BlockPos getInteractionPos(Object manager) {
		if (!isReflectionReady() || manager == null)
			return null;
		try {
			Object pos = getInteractionPosMethod.invoke(manager);
			if (pos instanceof BlockPos blockPos)
				return blockPos;
		} catch (ReflectiveOperationException | RuntimeException e) {
			disableReflection("Failed to query Simulated steering wheel position", e);
		}
		return null;
	}

	private static void cleanupRemovedConfig() {
		if (Client.config == null || !Client.config.hasEntry("simulated"))
			return;

		ConfigEntry config = Client.config.getEntry("simulated");
		config.clearValue("throttleLeverReverseMotion");
		config.clearValue("wheelMountReverseMotion");
		if (config.keySet().isEmpty())
			Client.config.clearValue("simulated");
		Client.config.save();
	}

	private static boolean isSimulatedLoaded() {
		return ModList.get().isLoaded(SIMULATED_MOD_ID);
	}

	private static void disableReflection(String message, Exception exception) {
		if (!reflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		reflectionUnavailable = true;
		clickInteractions = java.util.List.of();
	}

	private static void disableTypewriterReflection(String message, Exception exception) {
		if (!typewriterReflectionUnavailable)
			ViveCraftCompat.LOGGER.warn(message, exception);
		typewriterReflectionUnavailable = true;
	}

	private static double yaw(Vector3f direction) {
		return Math.atan2(direction.x, direction.z);
	}

	private static double pitch(Vector3f direction) {
		return Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z));
	}

	private static double wrapDegrees(double degrees) {
		degrees %= 360.0D;
		if (degrees >= 180.0D)
			degrees -= 360.0D;
		if (degrees < -180.0D)
			degrees += 360.0D;
		return degrees;
	}

	private static double wrapRadians(double radians) {
		return Math.toRadians(wrapDegrees(Math.toDegrees(radians)));
	}

	private static double applyStickDeadzone(double value) {
		return Math.abs(value) < STAFF_STICK_DEADZONE ? 0.0D : value;
	}

	private enum InputKind {
		USE,
		ATTACK
	}
}
