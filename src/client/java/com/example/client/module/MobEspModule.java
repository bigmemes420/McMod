package com.example.client.module;

import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Visuals: Mob ESP with Outline / 2D / 3D. Per-mob selection + colors (Finder-style).
 * Randomized defaults; only selected mobs are drawn.
 */
public final class MobEspModule {
	public enum Mode {
		OUTLINE,
		BOX_2D,
		BOX_3D
	}

	public static final int DEFAULT_COLOR = 0xFFFF5555;

	private static boolean enabled;
	private static Mode mode = Mode.OUTLINE;
	/** Legacy global color fallback when a mob has no custom entry. */
	private static int color = DEFAULT_COLOR;
	private static final LinkedHashSet<Identifier> selectedMobs = new LinkedHashSet<>();
	private static final LinkedHashMap<Identifier, Integer> mobColors = new LinkedHashMap<>();

	static {
		seedDefaultMobs(selectedMobs);
	}

	private MobEspModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static int getColor() {
		return color;
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
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		return isSelected(id);
	}

	public static void setSelected(Identifier id, boolean selected) {
		if (id == null) {
			return;
		}
		boolean changed = selected ? selectedMobs.add(id) : selectedMobs.remove(id);
		if (!selected) {
			mobColors.remove(id);
		} else if (!mobColors.containsKey(id)) {
			mobColors.put(id, BlockEspDefaults.colorFor(id));
		}
		if (changed) {
			ModConfig.save();
		}
	}

	public static int getMobColor(Identifier id) {
		if (id == null) {
			return color;
		}
		Integer custom = mobColors.get(id);
		if (custom != null) {
			return custom;
		}
		if (selectedMobs.contains(id)) {
			return BlockEspDefaults.colorFor(id);
		}
		return color;
	}

	public static void setMobColor(Identifier id, int argb) {
		if (id == null) {
			return;
		}
		mobColors.put(id, ARGB.opaque(argb));
		if (!selectedMobs.contains(id)) {
			selectedMobs.add(id);
		}
		ModConfig.save();
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
			mode = Mode.OUTLINE;
		}
	}

	public static void setColor(int argb) {
		int opaque = ARGB.opaque(argb);
		if (color == opaque) {
			return;
		}
		color = opaque;
		ModConfig.save();
	}

	public static void loadColor(int argb) {
		color = ARGB.opaque(argb);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.mob_esp.enabled"
						: "screen.rootymenu.menu.visuals.mob_esp.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
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
			if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) && isListableMob(id)) {
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

	public static String mobColorsCsv() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Identifier, Integer> e : mobColors.entrySet()) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(e.getKey()).append('=').append('#').append(String.format("%08X", e.getValue()));
		}
		return sb.toString();
	}

	public static void loadMobColors(String csv) {
		mobColors.clear();
		if (csv == null || csv.isBlank()) {
			return;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int eq = trimmed.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			Identifier id = Identifier.tryParse(trimmed.substring(0, eq).trim());
			Integer parsed = MenuTheme.parseHex(trimmed.substring(eq + 1).trim());
			if (id != null && parsed != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
				mobColors.put(id, ARGB.opaque(parsed));
			}
		}
	}

	/** True when outline mode should force through-walls outline on this mob. */
	public static boolean shouldOutline(LivingEntity entity) {
		return enabled
				&& mode == Mode.OUTLINE
				&& entity instanceof Mob
				&& !(entity instanceof Player)
				&& isSelected(entity.getType());
	}

	public static int outlineColor(LivingEntity entity) {
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return ARGB.opaque(getMobColor(id));
	}

	/** @deprecated use {@link #outlineColor(LivingEntity)} */
	@Deprecated
	public static int outlineColor() {
		return ARGB.opaque(color);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void render(LevelRenderer levelRenderer) {
		if (!enabled || mode == Mode.OUTLINE) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}
		EntityEspRenderer.Mode drawMode = mode == Mode.BOX_2D
				? EntityEspRenderer.Mode.BOX_2D
				: EntityEspRenderer.Mode.BOX_3D;
		for (Mob mob : client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(128.0D))) {
			if (mob.isRemoved() || !isSelected(mob.getType())) {
				continue;
			}
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
			EntityEspRenderer.draw(levelRenderer, mob, drawMode, ARGB.opaque(getMobColor(id)));
		}
	}

	public static boolean isListableMob(Identifier id) {
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			return false;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
		if (type == EntityTypes.PLAYER) {
			return false;
		}
		MobCategory cat = type.getCategory();
		return cat != MobCategory.MISC && type.canSummon();
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
