package com.example.client.module;

import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Visuals module: through-world block ESP for a dedicated selection list.
 * Modes: {@link RenderMode#OUTLINE} (stroke only) or {@link RenderMode#FILLED}
 * (filled boxes with a dedicated opacity slider). Uses theme
 * {@code finderOutline} — independent of X-Ray and Player Outlines.
 */
public final class FinderModule {
	public enum RenderMode {
		OUTLINE,
		FILLED;

		public static RenderMode fromString(String raw) {
			if (raw == null || raw.isBlank()) {
				return OUTLINE;
			}
			try {
				return RenderMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return OUTLINE;
			}
		}
	}

	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 55.0F;

	private static boolean enabled;
	private static RenderMode mode = RenderMode.OUTLINE;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> selectedBlocks = new LinkedHashSet<>();
	private static final BlockEspScanner scanner = new BlockEspScanner(FinderModule::getSelectedBlocks, BlockEspScanner.DEFAULT_RANGE);

	static {
		BlockEspDefaults.seedOresAndChests(selectedBlocks);
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
		if (changed) {
			scanner.clear();
			ModConfig.save();
		}
	}

	public static void loadSelectedBlocks(String csv) {
		BlockEspDefaults.loadCsv(selectedBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(selectedBlocks));
		scanner.clear();
	}

	public static String selectedBlocksCsv() {
		return BlockEspDefaults.toCsv(selectedBlocks);
	}

	public static void setMode(RenderMode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
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

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!enabled) {
			scanner.clear();
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.finder.enabled"
						: "screen.modid.menu.visuals.finder.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			scanner.clear();
			return;
		}
		scanner.tick(client);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void renderOverlays(LevelRenderer levelRenderer) {
		if (!enabled) {
			return;
		}
		int stroke = MenuTheme.get().finderOutline;
		if (mode == RenderMode.OUTLINE) {
			WorldBlockEspRenderer.drawOutlines(levelRenderer, scanner.hits(), stroke);
			return;
		}
		float fillAlpha = Mth.clamp(getOpacityFraction(), 0.0F, 1.0F);
		int fill = WorldBlockEspRenderer.fillWithAlpha(stroke & 0xFFFFFF, fillAlpha);
		WorldBlockEspRenderer.drawFilled(levelRenderer, scanner.hits(), stroke, fill);
	}
}
