package com.example.client.module;

import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Visuals: remesh-free X-Ray with live opacity on non-selected blocks.
 * <p>
 * Avoids baking every non-selected block onto TRANSLUCENT (the prior path
 * stalled FPS via full {@code allChanged()} remesh + translucent sort).
 * <ul>
 *   <li>Active (opacity &lt; 100): skip OPAQUE terrain each frame — no remesh.</li>
 *   <li>Selected blocks: always-on-top filled gizmos from a periodic scan.</li>
 *   <li>Opacity 0: non-selected fully hidden (classic).</li>
 *   <li>Opacity 1–99: nearby non-selected solids drawn as merged translucent
 *       ghost AABBs with slider alpha — limited range/cap, no chunk remesh.</li>
 *   <li>Opacity 100: inactive.</li>
 * </ul>
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	/** Nearby ghost walls for mid-opacity; kept small for FPS. */
	private static final int GHOST_RANGE = 16;
	private static final int GHOST_MAX_CELLS = 1800;
	private static final int GHOST_SCAN_PERIOD = 12;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();
	private static final BlockEspScanner selectedScanner =
			new BlockEspScanner(XRayModule::getFullOpacityBlocks, BlockEspScanner.DEFAULT_RANGE);

	private static final List<BlockPos> ghostHits = new ArrayList<>();
	private static final List<AABB> ghostBoxes = new ArrayList<>();
	private static int ghostTick;

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
			selectedScanner.clear();
			invalidateGhost();
			ModConfig.save();
		}
	}

	/** Live opacity — no remesh; ghost alpha and terrain skip update next frame. */
	public static void setOpacity(float value) {
		float clamped = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
		if (opacity == clamped) {
			return;
		}
		float previous = opacity;
		opacity = clamped;
		if ((previous <= MIN_OPACITY) != (clamped <= MIN_OPACITY)
				|| (previous >= MAX_OPACITY) != (clamped >= MAX_OPACITY)) {
			invalidateGhost();
		}
		ModConfig.save();
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		BlockEspDefaults.loadCsv(fullOpacityBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(fullOpacityBlocks));
		selectedScanner.clear();
		invalidateGhost();
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
			selectedScanner.clear();
			invalidateGhost();
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

	public static boolean isActive() {
		return enabled && opacity < MAX_OPACITY;
	}

	/** Skip OPAQUE SOLID+CUTOUT for this frame while X-Ray is active. */
	public static boolean shouldHideOpaqueTerrain() {
		return isActive();
	}

	public static void tick(Minecraft client) {
		if (!isActive()) {
			selectedScanner.clear();
			invalidateGhost();
			return;
		}
		selectedScanner.tick(client);
		if (opacity > MIN_OPACITY) {
			tickGhost(client);
		} else {
			invalidateGhost();
		}
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void renderOverlays(LevelRenderer levelRenderer) {
		if (!isActive()) {
			return;
		}
		int stroke = MenuTheme.get().outline;
		int selectedFill = WorldBlockEspRenderer.fillWithAlpha(stroke & 0xFFFFFF, 0.85F);
		WorldBlockEspRenderer.drawFilledMerged(
				levelRenderer,
				selectedScanner.hits(),
				stroke,
				selectedFill,
				2.0F
		);

		if (opacity <= MIN_OPACITY || ghostBoxes.isEmpty()) {
			return;
		}
		float ghostAlpha = Mth.clamp(getOpacityFraction() * 0.55F, 0.05F, 0.55F);
		int ghostFill = WorldBlockEspRenderer.fillWithAlpha(0x6A6A78, ghostAlpha);
		WorldBlockEspRenderer.drawFilledAabbs(levelRenderer, ghostBoxes, ghostFill);
	}

	private static void tickGhost(Minecraft client) {
		if (client.level == null || client.player == null) {
			invalidateGhost();
			return;
		}
		ghostTick++;
		if (ghostTick < GHOST_SCAN_PERIOD && !ghostBoxes.isEmpty()) {
			return;
		}
		ghostTick = 0;
		rescanGhost(client.level, client.player.blockPosition());
	}

	private static void rescanGhost(Level level, BlockPos origin) {
		ghostHits.clear();
		int r = GHOST_RANGE;
		int minX = origin.getX() - r;
		int maxX = origin.getX() + r;
		int minY = Math.max(level.getMinY(), origin.getY() - r);
		int maxY = Math.min(level.getMaxY(), origin.getY() + r);
		int minZ = origin.getZ() - r;
		int maxZ = origin.getZ() + r;

		int minChunkX = minX >> 4;
		int maxChunkX = maxX >> 4;
		int minChunkZ = minZ >> 4;
		int maxChunkZ = maxZ >> 4;
		int minSectionY = Math.max(level.getMinSectionY(), minY >> 4);
		int maxSectionY = Math.min(level.getMaxSectionY(), maxY >> 4);

		outer:
		for (int cx = minChunkX; cx <= maxChunkX; cx++) {
			for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(cx, cz);
				for (int sy = minSectionY; sy <= maxSectionY; sy++) {
					int sectionIndex = level.getSectionIndexFromSectionY(sy);
					if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
						continue;
					}
					LevelChunkSection section = chunk.getSection(sectionIndex);
					if (section.hasOnlyAir()) {
						continue;
					}
					int baseX = cx << 4;
					int baseY = sy << 4;
					int baseZ = cz << 4;
					for (int lx = 0; lx < 16; lx++) {
						int x = baseX + lx;
						if (x < minX || x > maxX) {
							continue;
						}
						for (int lz = 0; lz < 16; lz++) {
							int z = baseZ + lz;
							if (z < minZ || z > maxZ) {
								continue;
							}
							for (int ly = 0; ly < 16; ly++) {
								int y = baseY + ly;
								if (y < minY || y > maxY) {
									continue;
								}
								BlockState state = section.getBlockState(lx, ly, lz);
								if (state.isAir()) {
									continue;
								}
								Block block = state.getBlock();
								if (isAir(block) || isFullOpacity(block)) {
									continue;
								}
								ghostHits.add(new BlockPos(x, y, z));
								if (ghostHits.size() >= GHOST_MAX_CELLS) {
									break outer;
								}
							}
						}
					}
				}
			}
		}

		ghostBoxes.clear();
		ghostBoxes.addAll(WorldBlockEspRenderer.mergeConnectedSolid(ghostHits));
	}

	private static void invalidateGhost() {
		ghostHits.clear();
		ghostBoxes.clear();
		ghostTick = 0;
	}

	private static boolean isAir(Block block) {
		return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
	}
}
