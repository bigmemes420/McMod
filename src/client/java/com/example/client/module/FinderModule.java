package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Visuals module: through-world block ESP for a dedicated selection list.
 * Modes: Outline, Filled Boxes, Combined Fill. Render path caches merged
 * AABBs per color between scans to keep FPS high.
 */
public final class FinderModule {
	public enum RenderMode {
		OUTLINE,
		FILLED,
		COMBINED_FILL;

		public static RenderMode fromString(String raw) {
			if (raw == null || raw.isBlank()) {
				return OUTLINE;
			}
			String key = raw.trim().toUpperCase(Locale.ROOT).replace('+', '_').replace('-', '_').replace(' ', '_');
			if ("FILLED_BOXES".equals(key) || "FILLED_BOX".equals(key)) {
				return FILLED;
			}
			if ("FILLED_COMBINED".equals(key) || "FILLEDCOMBINED".equals(key)
					|| "COMBINED".equals(key) || "COMBINED_FILL".equals(key)
					|| "COMBINEDFILL".equals(key)) {
				return COMBINED_FILL;
			}
			try {
				return RenderMode.valueOf(key);
			} catch (IllegalArgumentException e) {
				return OUTLINE;
			}
		}
	}

	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 55.0F;

	public static final float MIN_OUTLINE_THICKNESS = 0.5F;
	public static final float MAX_OUTLINE_THICKNESS = 8.0F;
	public static final float DEFAULT_OUTLINE_THICKNESS = 2.0F;

	public static final float MIN_DISTANCE = 0.0F;
	public static final float MAX_DISTANCE = 1000.0F;
	public static final float DEFAULT_DISTANCE = (float) BlockEspScanner.DEFAULT_RANGE;

	private static boolean enabled;
	private static RenderMode mode = RenderMode.OUTLINE;
	private static float opacity = DEFAULT_OPACITY;
	private static float outlineThickness = DEFAULT_OUTLINE_THICKNESS;
	private static float distance = DEFAULT_DISTANCE;
	private static final LinkedHashSet<Identifier> selectedBlocks = new LinkedHashSet<>();
	private static final LinkedHashMap<Identifier, Integer> blockColors = new LinkedHashMap<>();
	private static final BlockEspScanner scanner = new BlockEspScanner(FinderModule::getSelectedBlocks, BlockEspScanner.DEFAULT_RANGE);

	/** Cached per-color merged AABBs — rebuilt when scanner generation changes. */
	private static final List<ColorGroup> renderCache = new ArrayList<>();
	private static int cachedGen = -1;
	private static RenderMode cachedMode = null;

	static {
		BlockEspDefaults.seedOresAndChests(selectedBlocks);
		scanner.setScanPeriodTicks(10);
	}

	private FinderModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static RenderMode getMode() {
		return mode;
	}

	public static float getOpacity() {
		return opacity;
	}

	public static float getOpacityFraction() {
		return opacity / MAX_OPACITY;
	}

	public static float getOutlineThickness() {
		return outlineThickness;
	}

	public static Set<Identifier> getSelectedBlocks() {
		return Collections.unmodifiableSet(selectedBlocks);
	}

	public static boolean isSelected(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		return id != null && selectedBlocks.contains(id);
	}

	public static boolean isSelected(Identifier id) {
		return id != null && selectedBlocks.contains(id);
	}

	public static void setSelected(Identifier id, boolean selected) {
		if (id == null) {
			return;
		}
		boolean changed = selected ? selectedBlocks.add(id) : selectedBlocks.remove(id);
		if (!selected) {
			blockColors.remove(id);
		}
		if (changed) {
			scanner.clear();
			invalidateRenderCache();
			ModConfig.save();
		}
	}

	public static int getBlockColor(Identifier id) {
		if (id == null) {
			return BlockEspDefaults.colorFor(null);
		}
		Integer custom = blockColors.get(id);
		return custom != null ? custom : BlockEspDefaults.colorFor(id);
	}

	public static void setBlockColor(Identifier id, int argb) {
		if (id == null) {
			return;
		}
		blockColors.put(id, argb);
		if (!selectedBlocks.contains(id)) {
			selectedBlocks.add(id);
			scanner.clear();
		}
		invalidateRenderCache();
		ModConfig.save();
	}

	public static void loadSelectedBlocks(String csv) {
		BlockEspDefaults.loadCsv(selectedBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(selectedBlocks));
		scanner.clear();
		invalidateRenderCache();
	}

	public static String selectedBlocksCsv() {
		return BlockEspDefaults.toCsv(selectedBlocks);
	}

	public static String blockColorsCsv() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Identifier, Integer> e : blockColors.entrySet()) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(e.getKey()).append('=').append('#').append(String.format("%08X", e.getValue()));
		}
		return sb.toString();
	}

	public static void loadBlockColors(String csv) {
		blockColors.clear();
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
			Integer color = com.example.client.config.MenuTheme.parseHex(trimmed.substring(eq + 1).trim());
			if (id != null && color != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				blockColors.put(id, color);
			}
		}
		invalidateRenderCache();
	}

	public static void setMode(RenderMode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		invalidateRenderCache();
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		mode = RenderMode.fromString(raw);
	}

	public static void setOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
		if (opacity == clamped) {
			return;
		}
		opacity = clamped;
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void setOutlineThickness(float value) {
		float clamped = Mth.clamp(value, MIN_OUTLINE_THICKNESS, MAX_OUTLINE_THICKNESS);
		if (outlineThickness == clamped) {
			return;
		}
		outlineThickness = clamped;
		ModConfig.save();
	}

	public static void loadOutlineThickness(float value) {
		outlineThickness = Mth.clamp(value, MIN_OUTLINE_THICKNESS, MAX_OUTLINE_THICKNESS);
	}

	public static float getDistance() {
		return distance;
	}

	public static void setDistance(float value) {
		float clamped = Mth.clamp(value, MIN_DISTANCE, MAX_DISTANCE);
		if (distance == clamped) {
			return;
		}
		distance = clamped;
		scanner.setRange(Math.round(distance));
		invalidateRenderCache();
		ModConfig.save();
	}

	public static void loadDistance(float value) {
		distance = Mth.clamp(value, MIN_DISTANCE, MAX_DISTANCE);
		scanner.setRange(Math.round(distance));
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!enabled) {
			scanner.clear();
			invalidateRenderCache();
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.finder.enabled"
						: "screen.rootymenu.menu.visuals.finder.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			scanner.clear();
			invalidateRenderCache();
			return;
		}
		scanner.tick(client);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void renderOverlays(LevelRenderer levelRenderer) {
		if (!enabled) {
			return;
		}
		var hits = scanner.hits();
		if (hits.isEmpty()) {
			return;
		}

		ensureRenderCache(Minecraft.getInstance().level, hits);
		float thickness = getOutlineThickness();
		float fillAlpha = Mth.clamp(getOpacityFraction(), 0.0F, 1.0F);

		if (mode == RenderMode.COMBINED_FILL) {
			// Combined still needs per-block ids for silhouette; reuse hit list once.
			WorldBlockEspRenderer.drawCombinedFill(
					levelRenderer,
					hits,
					pos -> idAt(Minecraft.getInstance().level, pos),
					id -> WorldBlockEspRenderer.fillWithAlpha(getBlockColor(id) & 0xFFFFFF, fillAlpha),
					FinderModule::getBlockColor,
					thickness
			);
			return;
		}

		for (ColorGroup group : renderCache) {
			if (mode == RenderMode.OUTLINE) {
				WorldBlockEspRenderer.drawOutlineAabbs(levelRenderer, group.boxes, group.stroke, thickness);
			} else {
				int fill = WorldBlockEspRenderer.fillWithAlpha(group.stroke & 0xFFFFFF, fillAlpha);
				WorldBlockEspRenderer.drawFilledAabbs(levelRenderer, group.boxes, group.stroke, fill, thickness);
			}
		}
	}

	private static void ensureRenderCache(Level level, List<BlockPos> hits) {
		int gen = scanner.generation();
		if (gen == cachedGen && cachedMode == mode && !renderCache.isEmpty()) {
			return;
		}
		cachedGen = gen;
		cachedMode = mode;
		renderCache.clear();

		Map<Integer, List<BlockPos>> byColor = new LinkedHashMap<>();
		for (BlockPos pos : hits) {
			Identifier id = idAt(level, pos);
			byColor.computeIfAbsent(getBlockColor(id), k -> new ArrayList<>()).add(pos);
		}
		for (Map.Entry<Integer, List<BlockPos>> e : byColor.entrySet()) {
			List<AABB> boxes = WorldBlockEspRenderer.mergeConnectedSolid(e.getValue());
			renderCache.add(new ColorGroup(e.getKey(), boxes));
		}
	}

	private static void invalidateRenderCache() {
		cachedGen = -1;
		renderCache.clear();
		cachedMode = null;
	}

	private static Identifier idAt(Level level, BlockPos pos) {
		if (level == null) {
			return null;
		}
		return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
	}

	private record ColorGroup(int stroke, List<AABB> boxes) {
	}
}
