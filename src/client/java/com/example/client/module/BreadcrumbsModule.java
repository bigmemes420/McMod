package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/**
 * Visuals (Meteor Breadcrumbs): trail of short line segments behind the player.
 */
public final class BreadcrumbsModule {
	public static final float MIN_MAX_POINTS = 20.0F;
	public static final float MAX_MAX_POINTS = 500.0F;
	public static final float DEFAULT_MAX_POINTS = 100.0F;

	private static boolean enabled;
	private static float maxPoints = DEFAULT_MAX_POINTS;
	private static final ArrayDeque<Vec3> points = new ArrayDeque<>();
	private static final int COLOR = 0xFFFFAA00;

	private BreadcrumbsModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getMaxPoints() {
		return maxPoints;
	}

	public static void setMaxPoints(float value) {
		float clamped = Mth.clamp(value, MIN_MAX_POINTS, MAX_MAX_POINTS);
		if (maxPoints == clamped) {
			return;
		}
		maxPoints = clamped;
		trim();
		ModConfig.save();
	}

	public static void loadMaxPoints(float value) {
		maxPoints = Mth.clamp(value, MIN_MAX_POINTS, MAX_MAX_POINTS);
		trim();
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!enabled) {
			points.clear();
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.breadcrumbs.enabled"
						: "screen.rootymenu.menu.visuals.breadcrumbs.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	private static void trim() {
		int max = Math.round(maxPoints);
		while (points.size() > max) {
			points.removeFirst();
		}
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		Vec3 pos = player.position();
		if (points.isEmpty() || points.peekLast().distanceToSqr(pos) > 0.04D) {
			points.addLast(pos);
			trim();
		}
	}

	public static void render(LevelRenderer levelRenderer) {
		if (!enabled || points.size() < 2) {
			return;
		}
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			Vec3 prev = null;
			for (Vec3 p : points) {
				if (prev != null) {
					Gizmos.line(prev, p, ARGB.opaque(COLOR), 2.0F).setAlwaysOnTop();
				}
				prev = p;
			}
		}
	}
}
