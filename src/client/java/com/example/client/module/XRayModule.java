package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
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
 * Visuals module: mesh-split X-Ray (MC 26.3 Fabric) — no Finder boxes.
 * <p>
 * Approach (rewritten):
 * <ul>
 *   <li><b>Selected</b> blocks keep normal SOLID meshes at full opacity.</li>
 *   <li><b>Non-selected</b> at opacity 0 become {@link RenderShape#INVISIBLE}
 *       (classic hide). At 1–99% they stay meshed but are forced onto the
 *       translucent chunk layer with vertex alpha = opacity/100.</li>
 *   <li>Non-selected never occlude faces while active, so selected ores still
 *       mesh buried faces without drawing ESP boxes.</li>
 *   <li>Section rebuilds are structural (enable / selection / opacity mode) and
 *       opacity-slider updates are debounced — never a remesh loop, never
 *       {@code clearCompileQueue} / {@code invalidateCompiledGeometry}.</li>
 * </ul>
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	/** Minimum gap between opacity-driven section rebuilds while dragging. */
	private static final long OPACITY_REMESH_DEBOUNCE_MS = 350L;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	/** Worker-thread flag: current block's quads should go into the translucent layer. */
	private static final ThreadLocal<Boolean> TRANSLUCENT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/**
	 * Set when enable/selection/opacity requires a section rebuild.
	 * Flushed from client tick with debounce for opacity-only changes.
	 */
	private static volatile boolean sectionsDirty;
	/** When true, the next tick remesh ignores the opacity debounce. */
	private static volatile boolean remeshImmediate;
	private static volatile long opacityDirtyAtMs;

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

	/** Opacity as 0–1 fraction for vertex alpha. */
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
			// Mode boundary (hidden ↔ fade ↔ off) remeshes immediately; in-between
			// fade values debounce so slider drags do not thrash the renderer.
			boolean prevHidden = previous <= MIN_OPACITY;
			boolean nowHidden = clamped <= MIN_OPACITY;
			boolean prevOff = previous >= MAX_OPACITY;
			boolean nowOff = clamped >= MAX_OPACITY;
			if (prevHidden != nowHidden || prevOff != nowOff) {
				markSectionsDirty(true);
			} else if (!nowHidden && !nowOff) {
				markSectionsDirty(false);
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
	 * Non-selected blocks must not occlude faces while X-Ray is active, so
	 * selected ores/chests still mesh every face even when buried in stone.
	 */
	public static boolean shouldDisableOcclusion(BlockState state) {
		if (!isActive()) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && !isFullOpacity(block);
	}

	/**
	 * Non-selected blocks at opacity 1–99% get vertex alpha = opacity/100 and
	 * are forced onto the translucent chunk layer.
	 */
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
		if (!sectionsDirty) {
			return;
		}
		if (client.level == null || client.levelRenderer == null) {
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
		rebuildVisibleSections(client);
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
	 * Soft-reset compiled section meshes near the camera so they recompile with
	 * the current opacity / selection. Prefer per-section {@code reset()} over
	 * recreating the view area. Must <b>not</b> call {@code clearCompileQueue()}
	 * (cancels rebuilds and blanks the world).
	 */
	private static void rebuildVisibleSections(Minecraft client) {
		ViewArea viewArea = client.levelRenderer.viewArea();
		if (viewArea == null) {
			return;
		}
		if (client.player == null) {
			viewArea.releaseAllBuffers();
			return;
		}

		BlockPos origin = client.player.blockPosition();
		int sectionRange = Math.max(4, client.options.getEffectiveRenderDistance() + 2);
		int minY = viewArea.minY();
		int maxY = viewArea.maxY();
		boolean any = false;

		for (int sx = -sectionRange; sx <= sectionRange; sx++) {
			for (int sz = -sectionRange; sz <= sectionRange; sz++) {
				for (int y = minY; y <= maxY; y += 16) {
					BlockPos sample = new BlockPos(
							(origin.getX() >> 4 << 4) + (sx << 4) + 8,
							y + 8,
							(origin.getZ() >> 4 << 4) + (sz << 4) + 8
					);
					SectionRenderDispatcher.RenderSection section = viewArea.getRenderSectionAt(sample);
					if (section != null) {
						section.reset();
						any = true;
					}
				}
			}
		}

		if (!any) {
			viewArea.releaseAllBuffers();
		}
	}
}
