package com.example.client.module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Shared default ore/chest block ids for Finder. */
public final class BlockEspDefaults {
	/** Written when the user clears every block so load does not re-seed defaults. */
	public static final String EMPTY_SENTINEL = "-";

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

	/**
	 * Stable per-block default color derived from the id hash (looks random,
	 * stays the same across launches until the user picks a custom color).
	 */
	public static int colorFor(Identifier id) {
		if (id == null) {
			return 0xFFFFD54A;
		}
		int h = id.hashCode();
		int mixed = h ^ (h >>> 16) * 0x45D9F3B;
		float hue = (mixed & 0xFFFF) / 65535.0F;
		float sat = 0.55F + ((mixed >>> 16) & 0xFF) / 255.0F * 0.40F;
		float val = 0.70F + ((mixed >>> 24) & 0xFF) / 255.0F * 0.30F;
		return hsvToArgb(hue, sat, val);
	}

	private static int hsvToArgb(float hue, float sat, float val) {
		float h = ((hue % 1.0F) + 1.0F) % 1.0F;
		float s = Mth.clamp(sat, 0.0F, 1.0F);
		float v = Mth.clamp(val, 0.0F, 1.0F);
		int i = (int) (h * 6.0F);
		float f = h * 6.0F - i;
		float p = v * (1.0F - s);
		float q = v * (1.0F - f * s);
		float t = v * (1.0F - (1.0F - f) * s);
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ARGB.color(
				255,
				Math.round(r * 255.0F),
				Math.round(g * 255.0F),
				Math.round(b * 255.0F)
		);
	}

	/**
	 * Load a CSV block list into {@code into}.
	 * <ul>
	 *   <li>null/blank → seed defaults</li>
	 *   <li>{@link #EMPTY_SENTINEL} → leave empty (user cleared the list)</li>
	 *   <li>otherwise parse ids; registry membership is NOT required at load
	 *       time so selections persist even if load runs before registries are
	 *       queried, and we never wipe a non-empty saved list by re-seeding</li>
	 * </ul>
	 */
	public static void loadCsv(Set<Identifier> into, String csv, Runnable seedIfEmpty) {
		into.clear();
		if (csv == null || csv.isBlank()) {
			seedIfEmpty.run();
			return;
		}
		if (EMPTY_SENTINEL.equals(csv.trim())) {
			return;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty() || EMPTY_SENTINEL.equals(trimmed)) {
				continue;
			}
			Identifier id = Identifier.tryParse(trimmed);
			if (id != null) {
				into.add(id);
			}
		}
		// Do not re-seed when every token failed to parse: keep empty and let
		// the next successful save rewrite the file. Re-seeding used to wipe
		// user selections whenever registry checks failed at client init.
	}

	public static String toCsv(Set<Identifier> ids) {
		if (ids == null || ids.isEmpty()) {
			return EMPTY_SENTINEL;
		}
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
