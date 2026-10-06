package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
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
 * Visuals module: X-Ray for selected blocks (mesh opacity — no Finder outlines).
 * <p>
 * Opacity 0–100% = how opaque <b>non-selected</b> blocks remain:
 * <ul>
 *   <li>0 = fully hidden (classic X-Ray)</li>
 *   <li>1–99 = translucent fade via vertex alpha on the translucent layer</li>
 *   <li>100 = no effect (terrain draws normally)</li>
 * </ul>
 * Selected blocks always render at full opacity. While active below 100%,
 * non-selected blocks do not occlude faces so selected ores stay visible
 * through walls. Does <b>not</b> draw Finder-style outline gizmos.
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	/** Worker-thread flag: current block's quads should go into the translucent layer. */
	private static final ThreadLocal<Boolean> TRANSLUCENT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/**
	 * Set when opacity/enabled/selection changes and sections must remesh.
	 * Flushed once per client tick so slider drags do not thrash the renderer.
	 */
	private static volatile boolean chunksDirty;

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
			markChunksDirty(true);
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
			// Defer remesh to client tick so slider drag stays live without
			// recreating ViewArea on every mouse move.
			markChunksDirty(false);
		}
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		BlockEspDefaults.loadCsv(fullOpacityBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(fullOpacityBlocks));
		if (enabled) {
			markChunksDirty(true);
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
		markChunksDirty(true);
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
		// Opacity 0: classic X-Ray — fully hide non-selected.
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
		if (isAir(block) || isFullOpacity(block)) {
			return false;
		}
		return true;
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

	/** Call from client tick so opacity-slider remeshes land once per frame/tick. */
	public static void tick(Minecraft client) {
		if (!chunksDirty) {
			return;
		}
		if (client.level == null || client.levelRenderer == null) {
			return;
		}
		chunksDirty = false;
		reloadChunks();
	}

	private static void markChunksDirty(boolean immediate) {
		chunksDirty = true;
		if (immediate) {
			chunksDirty = false;
			reloadChunks();
		}
	}

	/**
	 * Light remesh: reset compiled section meshes and clear the compile queue.
	 * Does <b>not</b> call {@code invalidateCompiledGeometry}, which recreates
	 * ViewArea and stalls the client.
	 */
	public static void reloadChunks() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.levelRenderer == null) {
			return;
		}
		ViewArea viewArea = client.levelRenderer.viewArea();
		if (viewArea != null) {
			viewArea.releaseAllBuffers();
		}
		SectionRenderDispatcher dispatcher = client.levelRenderer.sectionRenderDispatcher();
		if (dispatcher != null) {
			dispatcher.clearCompileQueue();
		}
	}
}
