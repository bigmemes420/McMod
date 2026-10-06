package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Visuals module: hides non-selected blocks so ores/chests stay visible.
 * Opacity 0–100%: how opaque non-selected blocks remain (0 = fully hidden / classic
 * X-Ray, 100 = fully opaque / no hide). Selected blocks always render full opacity.
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	static {
		seedDefaults(fullOpacityBlocks);
	}

	private XRayModule() {
	}

	private static void seedDefaults(Set<Identifier> into) {
		addDefault(into, Blocks.COAL_ORE);
		addDefault(into, Blocks.DEEPSLATE_COAL_ORE);
		addDefault(into, Blocks.IRON_ORE);
		addDefault(into, Blocks.DEEPSLATE_IRON_ORE);
		addDefault(into, Blocks.GOLD_ORE);
		addDefault(into, Blocks.DEEPSLATE_GOLD_ORE);
		addDefault(into, Blocks.COPPER_ORE);
		addDefault(into, Blocks.DEEPSLATE_COPPER_ORE);
		addDefault(into, Blocks.LAPIS_ORE);
		addDefault(into, Blocks.DEEPSLATE_LAPIS_ORE);
		addDefault(into, Blocks.REDSTONE_ORE);
		addDefault(into, Blocks.DEEPSLATE_REDSTONE_ORE);
		addDefault(into, Blocks.DIAMOND_ORE);
		addDefault(into, Blocks.DEEPSLATE_DIAMOND_ORE);
		addDefault(into, Blocks.EMERALD_ORE);
		addDefault(into, Blocks.DEEPSLATE_EMERALD_ORE);
		addDefault(into, Blocks.NETHER_GOLD_ORE);
		addDefault(into, Blocks.NETHER_QUARTZ_ORE);
		addDefault(into, Blocks.ANCIENT_DEBRIS);
		addDefault(into, Blocks.SPAWNER);
		addDefault(into, Blocks.CHEST);
		addDefault(into, Blocks.TRAPPED_CHEST);
		addDefault(into, Blocks.ENDER_CHEST);
	}

	private static void addDefault(Set<Identifier> into, Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		if (id != null) {
			into.add(id);
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getOpacity() {
		return opacity;
	}

	public static Set<Identifier> getFullOpacityBlocks() {
		return Collections.unmodifiableSet(fullOpacityBlocks);
	}

	public static boolean isFullOpacity(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		return id != null && fullOpacityBlocks.contains(id);
	}

	public static boolean isFullOpacity(Identifier id) {
		return id != null && fullOpacityBlocks.contains(id);
	}

	public static void setFullOpacity(Identifier id, boolean selected) {
		if (id == null) {
			return;
		}
		boolean changed = selected ? fullOpacityBlocks.add(id) : fullOpacityBlocks.remove(id);
		if (changed) {
			reloadChunks();
			ModConfig.save();
		}
	}

	public static void setOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
		if (opacity == clamped) {
			return;
		}
		opacity = clamped;
		if (enabled) {
			reloadChunks();
		}
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		fullOpacityBlocks.clear();
		if (csv == null || csv.isBlank()) {
			seedDefaults(fullOpacityBlocks);
			return;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			Identifier id = Identifier.tryParse(trimmed);
			if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				fullOpacityBlocks.add(id);
			}
		}
		if (fullOpacityBlocks.isEmpty()) {
			seedDefaults(fullOpacityBlocks);
		}
	}

	public static String fullOpacityBlocksCsv() {
		StringBuilder sb = new StringBuilder();
		for (Identifier id : fullOpacityBlocks) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(id);
		}
		return sb.toString();
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		reloadChunks();
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.xray.enabled"
						: "screen.modid.menu.visuals.xray.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/**
	 * When X-Ray is on and opacity &lt; 100%, non-selected blocks become invisible.
	 * Selected (full-opacity) blocks always keep their normal render shape.
	 */
	public static RenderShape modifyRenderShape(BlockState state, RenderShape original) {
		if (!enabled || opacity >= MAX_OPACITY) {
			return original;
		}
		if (original == RenderShape.INVISIBLE) {
			return original;
		}
		Block block = state.getBlock();
		if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
			return original;
		}
		if (isFullOpacity(block)) {
			return original;
		}
		// Opacity 0–99: hide non-selected so selected blocks show through.
		return RenderShape.INVISIBLE;
	}

	public static void reloadChunks() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.levelRenderer == null || client.gameRenderer == null) {
			return;
		}
		client.levelRenderer.invalidateCompiledGeometry(
				client.level,
				client.options,
				client.gameRenderer.mainCamera(),
				client.getBlockColors()
		);
	}
}
