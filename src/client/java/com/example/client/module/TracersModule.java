package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Visuals (Meteor Tracers): always-on-top lines from unbobbed screen center
 * to nearby players and/or selected mobs, colored from Player/Mob ESP. Rebuilt every frame so they stay
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
	private static final LinkedHashSet<Identifier> selectedMobs = new LinkedHashSet<>();

	static {
		seedDefaultMobs(selectedMobs);
	}

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

	public static Set<Identifier> getSelectedMobs() {
		return Collections.unmodifiableSet(selectedMobs);
	}

	public static boolean isSelected(Identifier id) {
		return id != null && selectedMobs.contains(id);
	}

	public static boolean isSelected(EntityType<?> type) {
		if (type == null) {
			return false;
		}
		return isSelected(BuiltInRegistries.ENTITY_TYPE.getKey(type));
	}

	public static void setSelected(Identifier id, boolean selected) {
		if (id == null) {
			return;
		}
		boolean changed = selected ? selectedMobs.add(id) : selectedMobs.remove(id);
		if (changed) {
			ModConfig.save();
		}
	}

	public static void loadSelectedMobs(String csv) {
		selectedMobs.clear();
		if (csv == null || csv.isBlank()) {
			seedDefaultMobs(selectedMobs);
			return;
		}
		if (BlockEspDefaults.EMPTY_SENTINEL.equals(csv.trim())) {
			return;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty() || BlockEspDefaults.EMPTY_SENTINEL.equals(trimmed)) {
				continue;
			}
			Identifier id = Identifier.tryParse(trimmed);
			if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) && MobEspModule.isListableMob(id)) {
				selectedMobs.add(id);
			}
		}
		if (selectedMobs.isEmpty()) {
			seedDefaultMobs(selectedMobs);
		}
	}

	public static String selectedMobsCsv() {
		return BlockEspDefaults.toCsv(selectedMobs);
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
					Gizmos.line(
							from,
							interpolatedCenter(player, partialTick),
							ARGB.opaque(PlayerEspModule.getColor()),
							1.5F
					).setAlwaysOnTop();
				}
			}
			if (mode == Mode.HOSTILES || mode == Mode.BOTH) {
				for (Mob mob : client.level.getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(96.0D))) {
					if (mob.isRemoved() || !isSelected(mob.getType())) {
						continue;
					}
					Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
					Gizmos.line(
							from,
							interpolatedCenter(mob, partialTick),
							ARGB.opaque(MobEspModule.getMobColor(id)),
							1.5F
					).setAlwaysOnTop();
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

	private static void seedDefaultMobs(Set<Identifier> into) {
		addMob(into, EntityTypes.ZOMBIE);
		addMob(into, EntityTypes.SKELETON);
		addMob(into, EntityTypes.CREEPER);
		addMob(into, EntityTypes.SPIDER);
		addMob(into, EntityTypes.ENDERMAN);
		addMob(into, EntityTypes.WITCH);
		addMob(into, EntityTypes.BLAZE);
		addMob(into, EntityTypes.SLIME);
		addMob(into, EntityTypes.PHANTOM);
		addMob(into, EntityTypes.DROWNED);
		addMob(into, EntityTypes.PILLAGER);
		addMob(into, EntityTypes.VINDICATOR);
		addMob(into, EntityTypes.WARDEN);
	}

	private static void addMob(Set<Identifier> into, EntityType<?> type) {
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id != null) {
			into.add(id);
		}
	}
}
