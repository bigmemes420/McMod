package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Visuals (Meteor Tracers): always-on-top lines from unbobbed screen center
 * to nearby players and/or hostile mobs. Rebuilt every frame so they stay
 * visible while standing still.
 */
public final class TracersModule {
	public enum Mode {
		PLAYERS,
		HOSTILES,
		BOTH
	}

	/** Push origin along look so the line clears the near plane when idle. */
	private static final double SCREEN_CENTER_PUSH = 0.2D;

	private static boolean enabled;
	private static Mode mode = Mode.BOTH;
	private static final int PLAYER_COLOR = 0xFF55E5FF;
	private static final int MOB_COLOR = 0xFFFF5555;

	private TracersModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static void setMode(Mode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			mode = Mode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			mode = Mode.BOTH;
		}
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.tracers.enabled"
						: "screen.rootymenu.menu.visuals.tracers.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/**
	 * When tracers are on, skip {@code bobView} so the camera pose matches
	 * {@link #screenCenterOrigin} (true screen-center, no view-bob wobble).
	 */
	public static boolean shouldIgnoreViewBobbing() {
		return enabled;
	}

	/** Unbobbed screen-center world point, slightly in front of the near plane. */
	public static Vec3 screenCenterOrigin(Camera camera) {
		Vec3 pos = camera.position();
		Vec3 look = Vec3.directionFromRotation(camera.xRot(), camera.yRot());
		return pos.add(look.scale(SCREEN_CENTER_PUSH));
	}

	public static void render(LevelRenderer levelRenderer) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer self = client.player;
		if (self == null || client.level == null) {
			return;
		}
		Camera camera = client.gameRenderer.mainCamera();
		if (!camera.isInitialized()) {
			return;
		}
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		// Recompute every frame so standing-still frames still redraw.
		Vec3 from = screenCenterOrigin(camera);

		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			if (mode == Mode.PLAYERS || mode == Mode.BOTH) {
				for (Player player : client.level.players()) {
					if (player == self || player.isRemoved()) {
						continue;
					}
					Gizmos.line(from, interpolatedCenter(player, partialTick), ARGB.opaque(PLAYER_COLOR), 1.5F)
							.setAlwaysOnTop();
				}
			}
			if (mode == Mode.HOSTILES || mode == Mode.BOTH) {
				for (Mob mob : client.level.getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(96.0D))) {
					if (mob.isRemoved() || mob.getType().getCategory() != MobCategory.MONSTER) {
						continue;
					}
					Gizmos.line(from, interpolatedCenter(mob, partialTick), ARGB.opaque(MOB_COLOR), 1.5F)
							.setAlwaysOnTop();
				}
			}
		}
	}

	private static Vec3 interpolatedCenter(Entity entity, float partialTick) {
		double x = Mth.lerp(partialTick, entity.xo, entity.getX());
		double y = Mth.lerp(partialTick, entity.yo, entity.getY());
		double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
		return new Vec3(x, y + entity.getBbHeight() * 0.5D, z);
	}
}
