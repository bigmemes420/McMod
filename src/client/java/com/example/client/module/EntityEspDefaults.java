package com.example.client.module;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Thematic default colors for Mob ESP (and related entity tints).
 * Applied only when the user has not saved a custom color for that entity.
 */
public final class EntityEspDefaults {
	/** Legacy / unknown-mob fallback (soft hostile red). */
	public static final int FALLBACK_COLOR = 0xFFFF5555;

	public static final int ZOMBIE = 0xFF5D8A3E;
	public static final int SKELETON = 0xFFE8E0D0;
	public static final int CREEPER = 0xFF50C878;
	public static final int SPIDER = 0xFF8B1A1A;
	public static final int ENDERMAN = 0xFF6B2D8B;
	public static final int WITCH = 0xFF8B5CF6;
	public static final int BLAZE = 0xFFFFAA00;
	public static final int SLIME = 0xFF6BCB3C;
	public static final int PHANTOM = 0xFF5A6A9A;
	public static final int DROWNED = 0xFF2E8B8B;
	public static final int PILLAGER = 0xFF8B7355;
	public static final int VINDICATOR = 0xFF6B6B6B;
	public static final int WARDEN = 0xFF00E5C8;
	public static final int GHAST = 0xFFF0F0F0;
	public static final int WITHER = 0xFF2A2A2A;
	public static final int WITHER_SKELETON = 0xFF3D3D3D;
	public static final int PIGLIN = 0xFFE8A070;
	public static final int HOGLIN = 0xFFC4785A;
	public static final int GUARDIAN = 0xFF4A90A4;
	public static final int SHULKER = 0xFFB57EDC;
	public static final int SILVERFISH = 0xFFA0A0A8;
	public static final int ENDERMITE = 0xFF5C2D6B;
	public static final int RAVAGER = 0xFF5A4632;
	public static final int VEX = 0xFF7EC8E3;
	public static final int MAGMA_CUBE = 0xFFFF4500;
	public static final int BREEZE = 0xFFA8C8E8;

	private EntityEspDefaults() {
	}

	public static int colorFor(Identifier id) {
		if (id == null) {
			return FALLBACK_COLOR;
		}
		return thematicMobColor(id.getPath());
	}

	public static int thematicMobColor(String path) {
		if (path == null || path.isEmpty()) {
			return FALLBACK_COLOR;
		}
		// More specific paths first.
		if (path.contains("wither_skeleton")) {
			return WITHER_SKELETON;
		}
		if (path.equals("wither") || path.startsWith("wither_")) {
			return WITHER;
		}
		if (path.contains("zombie_villager") || path.contains("zombie_horse") || path.contains("zombie_nautilus")) {
			return ZOMBIE;
		}
		if (path.contains("drowned")) {
			return DROWNED;
		}
		if (path.contains("husk")) {
			return ZOMBIE;
		}
		if (path.contains("zombie")) {
			return ZOMBIE;
		}
		if (path.contains("stray") || path.contains("bogged") || path.contains("skeleton")) {
			return SKELETON;
		}
		if (path.contains("creeper")) {
			return CREEPER;
		}
		if (path.contains("cave_spider") || path.contains("spider")) {
			return SPIDER;
		}
		if (path.contains("enderman")) {
			return ENDERMAN;
		}
		if (path.contains("witch")) {
			return WITCH;
		}
		if (path.contains("blaze")) {
			return BLAZE;
		}
		if (path.contains("magma_cube")) {
			return MAGMA_CUBE;
		}
		if (path.contains("slime")) {
			return SLIME;
		}
		if (path.contains("phantom")) {
			return PHANTOM;
		}
		if (path.contains("pillager")) {
			return PILLAGER;
		}
		if (path.contains("vindicator")) {
			return VINDICATOR;
		}
		if (path.contains("evoker") || path.contains("illusioner")) {
			return WITCH;
		}
		if (path.contains("ravager")) {
			return RAVAGER;
		}
		if (path.contains("vex")) {
			return VEX;
		}
		if (path.contains("warden")) {
			return WARDEN;
		}
		if (path.contains("ghast")) {
			return GHAST;
		}
		if (path.contains("piglin") || path.contains("zombified_piglin")) {
			return PIGLIN;
		}
		if (path.contains("hoglin") || path.contains("zoglin")) {
			return HOGLIN;
		}
		if (path.contains("guardian") || path.contains("elder_guardian")) {
			return GUARDIAN;
		}
		if (path.contains("shulker")) {
			return SHULKER;
		}
		if (path.contains("silverfish")) {
			return SILVERFISH;
		}
		if (path.contains("endermite")) {
			return ENDERMITE;
		}
		if (path.contains("breeze")) {
			return BREEZE;
		}
		return hashFallback(path);
	}

	private static int hashFallback(String path) {
		int h = path.hashCode();
		int mixed = h ^ (h >>> 16) * 0x45D9F3B;
		float hue = (mixed & 0xFFFF) / 65535.0F;
		float sat = 0.55F + ((mixed >>> 16) & 0xFF) / 255.0F * 0.40F;
		float val = 0.70F + ((mixed >>> 24) & 0xFF) / 255.0F * 0.30F;
		float hh = ((hue % 1.0F) + 1.0F) % 1.0F;
		float s = Mth.clamp(sat, 0.0F, 1.0F);
		float v = Mth.clamp(val, 0.0F, 1.0F);
		int i = (int) (hh * 6.0F);
		float f = hh * 6.0F - i;
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
}
