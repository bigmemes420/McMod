package com.example.client.module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Shared default ore/chest block ids for X-Ray and Finder. */
public final class BlockEspDefaults {
	private BlockEspDefaults() {
	}

	public static void seedOresAndChests(Set<Identifier> into) {
		add(into, Blocks.COAL_ORE);
		add(into, Blocks.DEEPSLATE_COAL_ORE);
		add(into, Blocks.IRON_ORE);
		add(into, Blocks.DEEPSLATE_IRON_ORE);
		add(into, Blocks.GOLD_ORE);
		add(into, Blocks.DEEPSLATE_GOLD_ORE);
		add(into, Blocks.COPPER_ORE);
		add(into, Blocks.DEEPSLATE_COPPER_ORE);
		add(into, Blocks.LAPIS_ORE);
		add(into, Blocks.DEEPSLATE_LAPIS_ORE);
		add(into, Blocks.REDSTONE_ORE);
		add(into, Blocks.DEEPSLATE_REDSTONE_ORE);
		add(into, Blocks.DIAMOND_ORE);
		add(into, Blocks.DEEPSLATE_DIAMOND_ORE);
		add(into, Blocks.EMERALD_ORE);
		add(into, Blocks.DEEPSLATE_EMERALD_ORE);
		add(into, Blocks.NETHER_GOLD_ORE);
		add(into, Blocks.NETHER_QUARTZ_ORE);
		add(into, Blocks.ANCIENT_DEBRIS);
		add(into, Blocks.SPAWNER);
		add(into, Blocks.CHEST);
		add(into, Blocks.TRAPPED_CHEST);
		add(into, Blocks.ENDER_CHEST);
	}

	public static void add(Set<Identifier> into, Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		if (id != null) {
			into.add(id);
		}
	}

	public static void loadCsv(Set<Identifier> into, String csv, Runnable seedIfEmpty) {
		into.clear();
		if (csv == null || csv.isBlank()) {
			seedIfEmpty.run();
			return;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			Identifier id = Identifier.tryParse(trimmed);
			if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				into.add(id);
			}
		}
		if (into.isEmpty()) {
			seedIfEmpty.run();
		}
	}

	public static String toCsv(Set<Identifier> ids) {
		StringBuilder sb = new StringBuilder();
		for (Identifier id : ids) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(id);
		}
		return sb.toString();
	}
}
