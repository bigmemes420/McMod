package com.example.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Periodically scans loaded chunk sections around the camera for blocks whose
 * ids are in a supplied selection set. Empty / palette-mismatched sections are
 * skipped. Results are reused every frame for overlay draw.
 */
public final class BlockEspScanner {
	public static final int DEFAULT_RANGE = 48;

	private final Supplier<Set<Identifier>> selection;
	private int range;
	private final List<BlockPos> hits = new ArrayList<>();
	private int tickCounter;
	private int scanPeriodTicks = 8;

	public BlockEspScanner(Supplier<Set<Identifier>> selection, int range) {
		this.selection = selection;
		this.range = Math.max(0, range);
	}

	public int getRange() {
		return this.range;
	}

	public void setRange(int range) {
		int clamped = Math.max(0, range);
		if (this.range == clamped) {
			return;
		}
		this.range = clamped;
		clear();
	}

	public void setScanPeriodTicks(int ticks) {
		this.scanPeriodTicks = Math.max(1, ticks);
	}

	public List<BlockPos> hits() {
		return this.hits;
	}

	public void clear() {
		this.hits.clear();
		this.tickCounter = 0;
	}

	/** Call once per client tick while the owning module is enabled. */
	public void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			this.hits.clear();
			return;
		}
		this.tickCounter++;
		if (this.tickCounter < this.scanPeriodTicks && !this.hits.isEmpty()) {
			return;
		}
		this.tickCounter = 0;
		rescan(client.level, client.player.blockPosition());
	}

	private void rescan(Level level, BlockPos origin) {
		Set<Identifier> wanted = this.selection.get();
		this.hits.clear();
		if (wanted.isEmpty() || this.range <= 0) {
			return;
		}

		int r = this.range;
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
					if (!section.maybeHas(state -> {
						Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
						return id != null && wanted.contains(id);
					})) {
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
								Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
								if (id != null && wanted.contains(id)) {
									this.hits.add(new BlockPos(x, y, z));
								}
							}
						}
					}
				}
			}
		}
	}
}
