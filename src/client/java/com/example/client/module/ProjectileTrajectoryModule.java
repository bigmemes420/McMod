package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Visuals (Meteor Trajectories): predicts projectile path from the held
 * shooter's muzzle (not camera/screen center). Ignores view bobbing. Supports
 * solid/dashed lines, line + gradient colors, face-aligned landing region, and optional in-flight trails that decay
 * after landing or 5 seconds.
 */
public final class ProjectileTrajectoryModule {
	public enum PathStyle {
		SOLID,
		DASHED
	}

	public static final float MIN_FILL_OPACITY = 0.0F;
	public static final float MAX_FILL_OPACITY = 100.0F;
	public static final float DEFAULT_FILL_OPACITY = 35.0F;
	public static final int DEFAULT_LINE_COLOR = 0xFFFF9600;
	public static final int DEFAULT_GRADIENT_COLOR = 0xFFFF3300;

	private static final float LANDING_RADIUS = 0.35F;
	private static final int MAX_STEPS = 300;
	private static final double DASH_LEN = 0.35D;
	private static final double GAP_LEN = 0.25D;
	private static final long TRAIL_MAX_AGE_MS = 5000L;
	private static final long TRAIL_DECAY_MS = 1000L;

	private static boolean enabled;
	private static PathStyle pathStyle = PathStyle.SOLID;
	private static boolean landingCircle = true;
	private static boolean renderInFlight = true;
	private static float fillOpacity = DEFAULT_FILL_OPACITY;
	private static int lineColor = DEFAULT_LINE_COLOR;
	private static int gradientColor = DEFAULT_GRADIENT_COLOR;
	private static final Map<Integer, FlightTrail> flightTrails = new HashMap<>();

	private ProjectileTrajectoryModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	/** Skip bobView while this module is on so the muzzle path stays stable. */
	public static boolean shouldIgnoreViewBobbing() {
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

	public static boolean isRenderInFlight() {
		return renderInFlight;
	}

	public static void setRenderInFlight(boolean value) {
		if (renderInFlight == value) {
			return;
		}
		renderInFlight = value;
		ModConfig.save();
	}

	public static void loadRenderInFlight(boolean value) {
		renderInFlight = value;
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

	public static int getLineColor() {
		return lineColor;
	}

	public static void setLineColor(int argb) {
		int opaque = ARGB.opaque(argb);
		if (lineColor == opaque) {
			return;
		}
		lineColor = opaque;
		ModConfig.save();
	}

	public static void loadLineColor(int argb) {
		lineColor = ARGB.opaque(argb);
	}

	public static int getGradientColor() {
		return gradientColor;
	}

	public static void setGradientColor(int argb) {
		int opaque = ARGB.opaque(argb);
		if (gradientColor == opaque) {
			return;
		}
		gradientColor = opaque;
		ModConfig.save();
	}

	public static void loadGradientColor(int argb) {
		gradientColor = ARGB.opaque(argb);
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
		ClientLevel level = client.level;
		if (player == null || level == null) {
			return;
		}
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int fillA = Mth.clamp(Math.round(fillOpacity / 100.0F * 255.0F), 0, 255);
		long now = System.currentTimeMillis();

		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			HeldAim held = heldAim(player);
			if (held != null) {
				SimResult sim = simulateFromMuzzle(player, level, held.motion(), held.mainHand(), partialTick);
				drawSim(sim, fillA, 1.0F);
			}
			if (renderInFlight) {
				updateAndDrawFlightTrails(level, player, partialTick, fillA, now);
			} else {
				flightTrails.clear();
			}
		}
	}

	private static void updateAndDrawFlightTrails(
			ClientLevel level,
			LocalPlayer player,
			float partialTick,
			int fillA,
			long now
	) {
		Set<Integer> seen = new HashSet<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof Projectile projectile) || projectile.isRemoved()) {
				continue;
			}
			if (projectile.getOwner() != player) {
				continue;
			}
			Motion motion = motionForEntity(projectile);
			if (motion == null) {
				continue;
			}
			int id = projectile.getId();
			seen.add(id);
			FlightTrail trail = flightTrails.get(id);
			if (trail == null) {
				trail = new FlightTrail(id, now);
				flightTrails.put(id, trail);
			}
			// Freeze path once decay has started (land or 5s age).
			if (trail.decayStartMs != 0L) {
				continue;
			}
			SimResult sim = simulateFromProjectile(level, projectile, motion, partialTick, player);
			trail.points = new ArrayList<>(sim.points());
			trail.landing = sim.landing();
			trail.landingFace = sim.landingFace();
			if (now - trail.startMs >= TRAIL_MAX_AGE_MS) {
				trail.beginDecay(now);
			}
		}

		Iterator<Map.Entry<Integer, FlightTrail>> it = flightTrails.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, FlightTrail> entry = it.next();
			FlightTrail trail = entry.getValue();
			if (!seen.contains(trail.entityId)) {
				trail.beginDecay(now);
			}
			if (trail.decayStartMs != 0L) {
				long elapsed = now - trail.decayStartMs;
				if (elapsed >= TRAIL_DECAY_MS) {
					// Keep entry until the projectile is gone so we do not respawn the trail.
					if (!seen.contains(trail.entityId)) {
						it.remove();
					}
					continue;
				}
				float alpha = 1.0F - (elapsed / (float) TRAIL_DECAY_MS);
				drawSim(new SimResult(trail.points, trail.landing, trail.landingFace), fillA, alpha);
			} else {
				drawSim(new SimResult(trail.points, trail.landing, trail.landingFace), fillA, 1.0F);
			}
		}
	}

	private static void drawSim(SimResult sim, int fillA, float alphaMul) {
		if (sim.points == null || sim.points.size() < 2) {
			return;
		}
		float a = Mth.clamp(alphaMul, 0.0F, 1.0F);
		drawPath(sim.points, a);
		if (landingCircle && sim.landing != null && sim.landingFace != null) {
			int strokeA = Math.round(255 * a);
			int fillCompA = Math.round(fillA * a);
			int stroke = ARGB.color(strokeA, ARGB.red(lineColor), ARGB.green(lineColor), ARGB.blue(lineColor));
			int fill = ARGB.color(fillCompA, ARGB.red(lineColor), ARGB.green(lineColor), ARGB.blue(lineColor));
			GizmoStyle style = fillCompA > 0
					? GizmoStyle.strokeAndFill(stroke, 2.0F, fill)
					: GizmoStyle.stroke(stroke, 2.0F);
			drawLandingFace(sim.landing, sim.landingFace, style);
		}
	}

	/** Square region on the hit face (UP/DOWN/sides), not a world-flat circle. */
	private static void drawLandingFace(Vec3 center, Direction face, GizmoStyle style) {
		double e = LANDING_RADIUS;
		Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
		Vec3 c = center.add(normal.scale(0.01D));
		Vec3 u;
		Vec3 v;
		switch (face) {
			case UP, DOWN -> {
				u = new Vec3(e, 0.0D, 0.0D);
				v = new Vec3(0.0D, 0.0D, e);
			}
			case NORTH, SOUTH -> {
				u = new Vec3(e, 0.0D, 0.0D);
				v = new Vec3(0.0D, e, 0.0D);
			}
			default -> {
				u = new Vec3(0.0D, 0.0D, e);
				v = new Vec3(0.0D, e, 0.0D);
			}
		}
		Vec3 p0 = c.add(u.scale(-1.0D)).add(v.scale(-1.0D));
		Vec3 p1 = c.add(u).add(v.scale(-1.0D));
		Vec3 p2 = c.add(u).add(v);
		Vec3 p3 = c.add(u.scale(-1.0D)).add(v);
		Gizmos.rect(p0, p1, p2, p3, style).setAlwaysOnTop();
	}

	private static void drawPath(List<Vec3> points, float alphaMul) {
		int n = points.size() - 1;
		if (pathStyle == PathStyle.SOLID) {
			for (int i = 1; i < points.size(); i++) {
				float t = n <= 1 ? 0.0F : (i - 1) / (float) n;
				Gizmos.line(points.get(i - 1), points.get(i), withAlpha(lerpColor(t), alphaMul), 2.0F)
						.setAlwaysOnTop();
			}
			return;
		}
		double dashLeft = DASH_LEN;
		boolean drawing = true;
		Vec3 cursor = points.get(0);
		double traveled = 0.0;
		double total = pathLength(points);
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
					float t = total <= 1.0E-6 ? 0.0F : (float) (traveled / total);
					Gizmos.line(cursor, end, withAlpha(lerpColor(t), alphaMul), 2.0F).setAlwaysOnTop();
				}
				traveled += step;
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

	private static int withAlpha(int argb, float mul) {
		int a = Math.round(ARGB.alpha(argb) * Mth.clamp(mul, 0.0F, 1.0F));
		return ARGB.color(a, ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb));
	}

	private static double pathLength(List<Vec3> points) {
		double len = 0.0;
		for (int i = 1; i < points.size(); i++) {
			len += points.get(i).distanceTo(points.get(i - 1));
		}
		return len;
	}

	private static int lerpColor(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		int a1 = ARGB.alpha(lineColor);
		int r1 = ARGB.red(lineColor);
		int g1 = ARGB.green(lineColor);
		int b1 = ARGB.blue(lineColor);
		int a2 = ARGB.alpha(gradientColor);
		int r2 = ARGB.red(gradientColor);
		int g2 = ARGB.green(gradientColor);
		int b2 = ARGB.blue(gradientColor);
		return ARGB.color(
				Math.round(Mth.lerp(t, a1, a2)),
				Math.round(Mth.lerp(t, r1, r2)),
				Math.round(Mth.lerp(t, g1, g2)),
				Math.round(Mth.lerp(t, b1, b2))
		);
	}

	private record Motion(float power, float pitchOffsetDeg, double gravity, float drag) {
	}

	private record HeldAim(Motion motion, boolean mainHand) {
	}

	private record SimResult(List<Vec3> points, Vec3 landing, Direction landingFace) {
	}

	private static final class FlightTrail {
		final int entityId;
		final long startMs;
		long decayStartMs;
		List<Vec3> points = List.of();
		Vec3 landing;
		Direction landingFace;

		FlightTrail(int entityId, long startMs) {
			this.entityId = entityId;
			this.startMs = startMs;
		}

		void beginDecay(long now) {
			if (this.decayStartMs == 0L) {
				this.decayStartMs = now;
			}
		}
	}

	private static HeldAim heldAim(LocalPlayer player) {
		ItemStack main = player.getMainHandItem();
		Motion m = motionForStack(player, main);
		if (m != null) {
			return new HeldAim(m, true);
		}
		Motion off = motionForStack(player, player.getOffhandItem());
		if (off != null) {
			return new HeldAim(off, false);
		}
		return null;
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

	private static Motion motionForEntity(Projectile projectile) {
		if (projectile instanceof ThrownEnderpearl || projectile instanceof Snowball || projectile instanceof ThrownEgg) {
			return new Motion(0.0F, 0.0F, 0.03D, 0.99F);
		}
		if (projectile instanceof ThrownExperienceBottle) {
			return new Motion(0.0F, 0.0F, 0.07D, 0.99F);
		}
		if (projectile instanceof ThrownTrident || projectile instanceof AbstractArrow) {
			return new Motion(0.0F, 0.0F, 0.05D, 0.99F);
		}
		return null;
	}

	private static Vec3 muzzleOrigin(LocalPlayer player, boolean mainHand, float partialTick, float pitch, float yaw) {
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 look = Vec3.directionFromRotation(pitch, yaw);
		Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = look.cross(up);
		if (right.lengthSqr() < 1.0E-8) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}
		HumanoidArm arm = player.getMainArm();
		if (!mainHand) {
			arm = arm.getOpposite();
		}
		double side = arm == HumanoidArm.RIGHT ? 1.0D : -1.0D;
		return eye
				.add(right.scale(0.22D * side))
				.add(0.0D, -0.12D, 0.0D)
				.add(look.scale(0.35D));
	}

	private static SimResult simulateFromMuzzle(
			LocalPlayer player,
			Level level,
			Motion motion,
			boolean mainHand,
			float partialTick
	) {
		float pitch = player.getXRot() + motion.pitchOffsetDeg();
		float yaw = player.getYRot();
		Vec3 pos = muzzleOrigin(player, mainHand, partialTick, pitch, yaw);
		Vec3 look = Vec3.directionFromRotation(pitch, yaw);
		Vec3 vel = look.normalize().scale(motion.power());
		return simulate(level, player, pos, vel, motion);
	}

	private static SimResult simulateFromProjectile(
			Level level,
			Projectile projectile,
			Motion motion,
			float partialTick,
			LocalPlayer player
	) {
		double x = Mth.lerp(partialTick, projectile.xo, projectile.getX());
		double y = Mth.lerp(partialTick, projectile.yo, projectile.getY());
		double z = Mth.lerp(partialTick, projectile.zo, projectile.getZ());
		Vec3 pos = new Vec3(x, y, z);
		Vec3 vel = projectile.getDeltaMovement();
		return simulate(level, player, pos, vel, motion);
	}

	private static SimResult simulate(Level level, Entity clipEntity, Vec3 start, Vec3 startVel, Motion motion) {
		List<Vec3> points = new ArrayList<>();
		points.add(start);
		Vec3 landing = null;
		Direction landingFace = null;
		Vec3 prev = start;
		Vec3 vel = startVel;

		for (int i = 0; i < MAX_STEPS; i++) {
			Vec3 next = prev.add(vel);
			BlockHitResult hit = level.clip(new ClipContext(
					prev,
					next,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					clipEntity
			));
			if (hit.getType() != HitResult.Type.MISS) {
				points.add(hit.getLocation());
				landing = hit.getLocation();
				landingFace = hit.getDirection();
				break;
			}
			points.add(next);
			prev = next;
			vel = vel.scale(motion.drag()).add(0.0D, -motion.gravity(), 0.0D);
			if (prev.y < level.getMinY() - 64) {
				break;
			}
		}
		return new SimResult(points, landing, landingFace);
	}
}
