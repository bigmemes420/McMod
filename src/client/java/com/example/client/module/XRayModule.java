package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
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
 * Visuals module: classic Fabric X-Ray for MC 26.3 (Meteor / hibiscus style).
 * <p>
 * Proven approach:
 * <ul>
 *   <li>Non-selected at opacity 0 → {@link RenderShape#INVISIBLE} (hidden).</li>
 *   <li>Non-selected at 1–99% → meshed on TRANSLUCENT with vertex alpha.</li>
 *   <li>Non-selected never occlude ({@code canOcclude=false}, empty face shapes)
 *       so buried selected ores still mesh every face.</li>
 *   <li>{@link Block#shouldRenderFace} forced true for selected blocks while
 *       active (Meteor {@code modifyDrawSide} equivalent).</li>
 *   <li>Section remesh via {@code LevelExtractor.allChanged()}; smartCull off
 *       so solid sections do not hide buried ores.</li>
 * </ul>
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static final long OPACITY_REMESH_DEBOUNCE_MS = 350L;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	private static final ThreadLocal<Boolean> TRANSLUCENT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private static volatile boolean sectionsDirty;
	private static volatile boolean remeshImmediate;
	private static volatile long opacityDirtyAtMs;

	/** Restored when X-Ray turns off so the user's smartCull preference returns. */
	private static boolean savedSmartCull = true;
	private static boolean smartCullOverridden;

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
			markSectionsDirty(true);
			ModConfig.save();
		}
	}

	public static void setOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
		if (opacity == clamped) {
			return;
		}
		float previous = opacity;
		opacity = clamped;
		if (enabled) {
			boolean prevHidden = previous <= MIN_OPACITY;
			boolean nowHidden = clamped <= MIN_OPACITY;
			boolean prevOff = previous >= MAX_OPACITY;
			boolean nowOff = clamped >= MAX_OPACITY;
			// Threshold changes need an immediate remesh; in-band fade changes
			// debounce so the slider stays responsive without thrashing.
			if (prevHidden != nowHidden || prevOff != nowOff) {
				markSectionsDirty(true);
			} else if (!nowHidden && !nowOff) {
				markSectionsDirty(false);
			} else if (nowHidden || nowOff) {
				markSectionsDirty(true);
			}
		}
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		BlockEspDefaults.loadCsv(fullOpacityBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(fullOpacityBlocks));
		if (enabled) {
			markSectionsDirty(true);
		}
	}

	public static String fullOpacityBlocksCsv() {
		return BlockEspDefaults.toCsv(fullOpacityBlocks);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		applySmartCull(Minecraft.getInstance());
		markSectionsDirty(true);
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.xray.enabled"
						: "screen.modid.menu.visuals.xray.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** X-Ray is on and opacity is below 100% (hiding or fading non-selected). */
	public static boolean isActive() {
		return enabled && opacity < MAX_OPACITY;
	}

	private static boolean isAir(Block block) {
		return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
	}

	/**
	 * Non-selected solid blocks at opacity 0 become {@link RenderShape#INVISIBLE}.
	 * At 1–99% they keep their model so alpha can be applied during meshing.
	 */
	public static RenderShape modifyRenderShape(BlockState state, RenderShape original) {
		if (!isActive() || original == RenderShape.INVISIBLE) {
			return original;
		}
		Block block = state.getBlock();
		if (isAir(block) || isFullOpacity(block)) {
			return original;
		}
		if (opacity <= MIN_OPACITY) {
			return RenderShape.INVISIBLE;
		}
		return original;
	}

	/**
	 * Non-selected blocks must not occlude faces while X-Ray is active
	 * ({@code canOcclude} + empty face occlusion shapes).
	 */
	public static boolean shouldDisableOcclusion(BlockState state) {
		if (!isActive()) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && !isFullOpacity(block);
	}

	/**
	 * Meteor-style force-draw: selected blocks always render every face while
	 * X-Ray is active (buried ores visible through stone).
	 */
	public static boolean shouldForceRenderFace(BlockState state) {
		if (!isActive()) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && isFullOpacity(block);
	}

	/** Kept for callers that still pass a neighbor / direction. */
	public static boolean shouldForceRenderFace(BlockState state, BlockState neighbor, Direction direction) {
		return shouldForceRenderFace(state);
	}

	public static boolean shouldFade(BlockState state) {
		if (!isActive() || opacity <= MIN_OPACITY) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && !isFullOpacity(block);
	}

	public static void beginTranslucentPass() {
		TRANSLUCENT_PASS.set(Boolean.TRUE);
	}

	public static void endTranslucentPass() {
		TRANSLUCENT_PASS.set(Boolean.FALSE);
	}

	public static boolean isTranslucentPass() {
		return Boolean.TRUE.equals(TRANSLUCENT_PASS.get());
	}

	/** Call from client tick so rebuilds land once (debounced for opacity). */
	public static void tick(Minecraft client) {
		applySmartCull(client);
		if (!sectionsDirty) {
			return;
		}
		if (client.level == null || client.levelExtractor == null) {
			return;
		}
		if (!remeshImmediate) {
			long wait = System.currentTimeMillis() - opacityDirtyAtMs;
			if (wait < OPACITY_REMESH_DEBOUNCE_MS) {
				return;
			}
		}
		sectionsDirty = false;
		remeshImmediate = false;
		opacityDirtyAtMs = 0L;
		// Proven remesh path used by Meteor Client (levelExtractor.allChanged).
		client.levelExtractor.allChanged();
	}

	private static void markSectionsDirty(boolean immediate) {
		sectionsDirty = true;
		if (immediate) {
			remeshImmediate = true;
			opacityDirtyAtMs = 0L;
		} else if (opacityDirtyAtMs == 0L) {
			opacityDirtyAtMs = System.currentTimeMillis();
		}
	}

	/**
	 * Disable advanced section occlusion while X-Ray is active so buried ores
	 * inside otherwise-opaque sections stay visible (Meteor chunk-occlusion cancel).
	 */
	private static void applySmartCull(Minecraft client) {
		if (client == null) {
			return;
		}
		if (isActive()) {
			if (!smartCullOverridden) {
				savedSmartCull = client.smartCull;
				smartCullOverridden = true;
			}
			client.smartCull = false;
		} else if (smartCullOverridden) {
			client.smartCull = savedSmartCull;
			smartCullOverridden = false;
		}
	}
}
