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
 * Visuals module: X-Ray for selected blocks.
 * <p>
 * Opacity 0–100% = how opaque non-selected blocks remain:
 * 0 = fully hidden (classic X-Ray), 1–99 = translucent fade, 100 = fully opaque
 * (no hide). Selected blocks always render at full opacity and, while X-Ray is
 * active below 100%, their faces are not culled by neighboring solid blocks.
 * <p>
 * Opacity slider updates are applied live: the value is stored immediately and
 * sections are lightly reset (not full LevelRenderer reinits) on the next client
 * tick so dragging stays responsive.
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
			// recreating ViewArea on every mouse move (prior fix's failure mode).
			markChunksDirty(false);
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
	 * ViewArea and stalls the client — that was why the prior opacity fix felt
	 * non-live while dragging the slider.
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
