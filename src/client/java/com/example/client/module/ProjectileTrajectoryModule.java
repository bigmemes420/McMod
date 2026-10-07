package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Visuals (Meteor Trajectories simplified): predicts held projectile path with
 * solid or dashed lines, optional landing circle + fill opacity.
 */
public final class ProjectileTrajectoryModule {
	public enum PathStyle {
		SOLID,
		DASHED
	}

	public static final float MIN_FILL_OPACITY = 0.0F;
	public static final float MAX_FILL_OPACITY = 100.0F;
	public static final float DEFAULT_FILL_OPACITY = 35.0F;

	private static final int LINE_COLOR = 0xFFFF9600;
	private static final int LANDING_STROKE = 0xFFFF9600;
	private static final float LANDING_RADIUS = 0.35F;
	private static final int MAX_STEPS = 300;
	private static final double DASH_LEN = 0.35D;
	private static final double GAP_LEN = 0.25D;

	private static boolean enabled;
	private static PathStyle pathStyle = PathStyle.SOLID;
	private static boolean landingCircle = true;
	private static float fillOpacity = DEFAULT_FILL_OPACITY;

	private ProjectileTrajectoryModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static PathStyle getPathStyle() {
		return pathStyle;
	}

	public static void setPathStyle(PathStyle value) {
		if (value == null || pathStyle == value) {
			return;
		}
		pathStyle = value;
		ModConfig.save();
	}

	public static void loadPathStyle(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			pathStyle = PathStyle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			pathStyle = PathStyle.SOLID;
		}
	}

	public static boolean isLandingCircle() {
		return landingCircle;
	}

	public static void setLandingCircle(boolean value) {
		if (landingCircle == value) {
			return;
		}
		landingCircle = value;
		ModConfig.save();
	}

	public static void loadLandingCircle(boolean value) {
		landingCircle = value;
	}

	public static float getFillOpacity() {
		return fillOpacity;
	}

	public static void setFillOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_FILL_OPACITY, MAX_FILL_OPACITY);
		if (fillOpacity == clamped) {
			return;
		}
		fillOpacity = clamped;
		ModConfig.save();
	}

	public static void loadFillOpacity(float value) {
		fillOpacity = Mth.clamp(value, MIN_FILL_OPACITY, MAX_FILL_OPACITY);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.projectile_trajectory.enabled"
						: "screen.rootymenu.menu.visuals.projectile_trajectory.disabled"
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
		LocalPlayer player = client.player;
		Level level = client.level;
		if (player == null || level == null) {
			return;
		}
		Motion motion = motionForHeld(player);
		if (motion == null) {
			return;
		}
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		SimResult sim = simulate(player, level, motion, partialTick);
		if (sim.points.size() < 2) {
			return;
		}

		int lineArgb = ARGB.opaque(LINE_COLOR);
		int fillA = Mth.clamp(Math.round(fillOpacity / 100.0F * 255.0F), 0, 255);
		int fillArgb = ARGB.color(fillA, LINE_COLOR);

		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			drawPath(sim.points, lineArgb);
			if (landingCircle && sim.landing != null) {
				GizmoStyle style = fillA > 0
						? GizmoStyle.strokeAndFill(ARGB.opaque(LANDING_STROKE), 2.0F, fillArgb)
						: GizmoStyle.stroke(ARGB.opaque(LANDING_STROKE), 2.0F);
				Gizmos.circle(sim.landing, LANDING_RADIUS, style).setAlwaysOnTop();
			}
		}
	}

	private static void drawPath(List<Vec3> points, int color) {
		if (pathStyle == PathStyle.SOLID) {
			for (int i = 1; i < points.size(); i++) {
				Gizmos.line(points.get(i - 1), points.get(i), color, 2.0F).setAlwaysOnTop();
			}
			return;
		}
		// Dashed: walk the polyline, emit DASH_LEN segments with GAP_LEN skips.
		double dashLeft = DASH_LEN;
		boolean drawing = true;
		Vec3 cursor = points.get(0);
		for (int i = 1; i < points.size(); i++) {
			Vec3 next = points.get(i);
			Vec3 seg = next.subtract(cursor);
			double segLen = seg.length();
			if (segLen < 1.0E-6) {
				cursor = next;
				continue;
			}
			Vec3 dir = seg.scale(1.0 / segLen);
			double remaining = segLen;
			while (remaining > 1.0E-6) {
				double step = Math.min(remaining, dashLeft);
				Vec3 end = cursor.add(dir.scale(step));
				if (drawing) {
					Gizmos.line(cursor, end, color, 2.0F).setAlwaysOnTop();
				}
				cursor = end;
				remaining -= step;
				dashLeft -= step;
				if (dashLeft <= 1.0E-6) {
					drawing = !drawing;
					dashLeft = drawing ? DASH_LEN : GAP_LEN;
				}
			}
		}
	}

	private record Motion(float power, float pitchOffsetDeg, double gravity, float drag) {
	}

	private record SimResult(List<Vec3> points, Vec3 landing) {
	}

	private static Motion motionForHeld(LocalPlayer player) {
		ItemStack stack = player.getMainHandItem();
		Motion m = motionForStack(player, stack);
		if (m != null) {
			return m;
		}
		return motionForStack(player, player.getOffhandItem());
	}

	private static Motion motionForStack(LocalPlayer player, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		Item item = stack.getItem();
		if (item instanceof EnderpearlItem || item instanceof SnowballItem || item instanceof EggItem) {
			return new Motion(1.5F, 0.0F, 0.03D, 0.99F);
		}
		if (item instanceof ExperienceBottleItem) {
			return new Motion(0.7F, -20.0F, 0.07D, 0.99F);
		}
		if (item instanceof TridentItem) {
			return new Motion(2.5F, 0.0F, 0.05D, 0.99F);
		}
		if (item instanceof BowItem) {
			int use = player.getTicksUsingItem();
			if (!player.isUsingItem() && stack.is(Items.BOW)) {
				// Show full-draw preview when holding a bow idle.
				use = BowItem.MAX_DRAW_DURATION;
			} else if (!player.isUsingItem()) {
				return null;
			}
			float charge = BowItem.getPowerForTime(use);
			if (charge < 0.1F) {
				charge = 1.0F;
			}
			return new Motion(charge * 3.0F, 0.0F, 0.05D, 0.99F);
		}
		if (item instanceof CrossbowItem) {
			if (!CrossbowItem.isCharged(stack)) {
				return null;
			}
			return new Motion(3.15F, 0.0F, 0.05D, 0.99F);
		}
		return null;
	}

	private static SimResult simulate(LocalPlayer player, Level level, Motion motion, float partialTick) {
		List<Vec3> points = new ArrayList<>();
		double x = Mth.lerp(partialTick, player.xo, player.getX());
		double y = Mth.lerp(partialTick, player.yo, player.getY())
				+ player.getEyeHeight()
				- 0.1D;
		double z = Mth.lerp(partialTick, player.zo, player.getZ());
		Vec3 pos = new Vec3(x, y, z);

		float pitch = player.getXRot() + motion.pitchOffsetDeg();
		float yaw = player.getYRot();
		Vec3 look = Vec3.directionFromRotation(pitch, yaw);
		Vec3 vel = look.normalize().scale(motion.power());

		points.add(pos);
		Vec3 landing = null;
		Vec3 prev = pos;

		for (int i = 0; i < MAX_STEPS; i++) {
			Vec3 next = prev.add(vel);
			BlockHitResult hit = level.clip(new ClipContext(
					prev,
					next,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					player
			));
			if (hit.getType() != HitResult.Type.MISS) {
				points.add(hit.getLocation());
				landing = hit.getLocation();
				break;
			}
			points.add(next);
			prev = next;
			vel = vel.scale(motion.drag()).add(0.0D, -motion.gravity(), 0.0D);
			if (prev.y < level.getMinY() - 64) {
				break;
			}
		}
		return new SimResult(points, landing);
	}
}
