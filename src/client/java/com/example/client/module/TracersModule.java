package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Visuals (Meteor Tracers): always-on-top lines from screen center (camera
 * origin, ignoring view bobbing) to nearby players and/or hostile mobs.
 */
public final class TracersModule {
	public enum Mode {
		PLAYERS,
		HOSTILES,
		BOTH
	}

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

	public static void render(LevelRenderer levelRenderer) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer self = client.player;
		if (self == null || client.level == null) {
			return;
		}
		// Camera position is the view origin (screen center). bobView is pose-only,
		// so this origin ignores view bobbing and stays locked to the crosshair ray.
		Camera camera = client.gameRenderer.mainCamera();
		Vec3 from = camera.position();
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			if (mode == Mode.PLAYERS || mode == Mode.BOTH) {
				for (Player player : client.level.players()) {
					if (player == self || player.isRemoved()) {
						continue;
					}
					Gizmos.line(from, player.getBoundingBox().getCenter(), ARGB.opaque(PLAYER_COLOR), 1.5F)
							.setAlwaysOnTop();
				}
			}
			if (mode == Mode.HOSTILES || mode == Mode.BOTH) {
				for (Mob mob : client.level.getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(96.0D))) {
					if (mob.isRemoved() || mob.getType().getCategory() != MobCategory.MONSTER) {
						continue;
					}
					Gizmos.line(from, mob.getBoundingBox().getCenter(), ARGB.opaque(MOB_COLOR), 1.5F)
							.setAlwaysOnTop();
				}
			}
		}
	}
}
