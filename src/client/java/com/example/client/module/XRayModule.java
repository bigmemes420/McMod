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
import java.util.Set;

/**
 * Visuals module: real-time X-Ray for selected blocks (MC 26.3 Fabric).
 * <p>
 * <b>No chunk remesh</b> — opacity and selection updates apply the same frame.
 * <ul>
 *   <li>Opacity 0–99: skip opaque/cutout terrain each frame (classic X-Ray) and
 *       draw selected blocks as always-on-top filled boxes. Overlay fill alpha
 *       tracks the slider live — no remesh.</li>
 *   <li>Opacity 100: no X-Ray effect (terrain draws normally).</li>
 * </ul>
 * Selected blocks are found by a lightweight periodic scan and rendered with
 * {@link WorldBlockEspRenderer} gizmos — buried ores stay visible without
 * face-occlusion remeshing.
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();
	private static final BlockEspScanner scanner = new BlockEspScanner(XRayModule::getFullOpacityBlocks, BlockEspScanner.DEFAULT_RANGE);

	static {
		BlockEspDefaults.seedOresAndChests(fullOpacityBlocks);
	}

	private XRayModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getOpacity() {
		return opacity;
	}

	/** Opacity as 0–1 fraction. */
	public static float getOpacityFraction() {
		return opacity / MAX_OPACITY;
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
			scanner.clear();
			ModConfig.save();
		}
	}

	public static void setOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
		if (opacity == clamped) {
			return;
		}
		opacity = clamped;
		// Live — no remesh. Terrain skip + overlay alpha read this each frame.
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		BlockEspDefaults.loadCsv(fullOpacityBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(fullOpacityBlocks));
		scanner.clear();
	}

	public static String fullOpacityBlocksCsv() {
		return BlockEspDefaults.toCsv(fullOpacityBlocks);
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
				enabled ? "screen.modid.menu.visuals.xray.enabled"
						: "screen.modid.menu.visuals.xray.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** X-Ray is on and opacity is below 100% (effect active). */
	public static boolean isActive() {
		return enabled && opacity < MAX_OPACITY;
	}

	/**
	 * Classic terrain hide: skip OPAQUE chunk layers (SOLID+CUTOUT) for this frame
	 * whenever X-Ray is active. Reads live opacity — dragging to 100 restores
	 * terrain immediately without remesh.
	 */
	public static boolean shouldHideOpaqueTerrain() {
		return isActive();
	}

	public static void tick(Minecraft client) {
		if (!isActive()) {
			scanner.clear();
			return;
		}
		scanner.tick(client);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void renderOverlays(LevelRenderer levelRenderer) {
		if (!isActive()) {
			return;
		}
		int stroke = MenuTheme.get().outline;
		// Live opacity → overlay fill alpha (terrain skip is binary below 100).
		float fillAlpha = Mth.clamp(0.90F - getOpacityFraction() * 0.55F, 0.30F, 0.90F);
		int fill = WorldBlockEspRenderer.fillWithAlpha(stroke & 0xFFFFFF, fillAlpha);
		WorldBlockEspRenderer.drawFilled(levelRenderer, scanner.hits(), stroke, fill);
	}
}
