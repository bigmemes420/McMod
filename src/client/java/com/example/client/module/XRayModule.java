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
 * Visuals: X-Ray with a Meteor-style absolute vertex-alpha opacity path.
 * <p>
 * Selected blocks render opaque with every face forced. Non-selected blocks:
 * <ul>
 *   <li>opacity 0 → {@link RenderShape#INVISIBLE} (classic hide)</li>
 *   <li>opacity 1–99 → absolute vertex alpha (0–255) baked onto TRANSLUCENT</li>
 *   <li>opacity 100 → X-Ray fade inactive</li>
 * </ul>
 * Remesh uses {@code levelExtractor.allChanged()} (debounced once per tick).
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	private static final ThreadLocal<Boolean> TRANSLUCENT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/** Structural / committed opacity rebuilds — flushed once per client tick. */
	private static volatile boolean sectionsDirty;

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

	/** Absolute 0–255 alpha for non-selected faded blocks. */
	public static int getOpacityAlpha() {
		return Mth.clamp(Math.round(getOpacityFraction() * 255.0F), 0, 255);
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
			markSectionsDirty();
			ModConfig.save();
		}
	}

	/**
	 * Live opacity value. Remeshes when crossing hidden/fade/off thresholds.
	 * In-band fade changes wait for {@link #commitOpacity()} (slider release).
	 */
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
			if (prevHidden != nowHidden || prevOff != nowOff) {
				markSectionsDirty();
			}
		}
		ModConfig.save();
	}

	/** Remesh once after the opacity slider is released (in-band alpha bake). */
	public static void commitOpacity() {
		if (enabled && opacity > MIN_OPACITY && opacity < MAX_OPACITY) {
			markSectionsDirty();
		}
	}

	public static void loadOpacity(float value) {
		opacity = Mth.clamp(value, MIN_OPACITY, MAX_OPACITY);
	}

	public static void loadFullOpacityBlocks(String csv) {
		BlockEspDefaults.loadCsv(fullOpacityBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(fullOpacityBlocks));
		if (enabled) {
			markSectionsDirty();
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
		markSectionsDirty();
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

	private static boolean isAir(Block block) {
		return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
	}

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

	public static boolean shouldDisableOcclusion(BlockState state) {
		if (!isActive()) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && !isFullOpacity(block);
	}

	public static boolean shouldForceRenderFace(BlockState state) {
		if (!isActive()) {
			return false;
		}
		Block block = state.getBlock();
		return !isAir(block) && isFullOpacity(block);
	}

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

	/**
	 * Absolute-alpha rewrite for a baked vertex color (Meteor-style).
	 * Keeps RGB, replaces alpha with {@link #getOpacityAlpha()}.
	 */
	public static int applyFadeAlpha(int argb) {
		int alpha = getOpacityAlpha();
		if (alpha <= 0) {
			return argb & 0x00FFFFFF;
		}
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	public static void tick(Minecraft client) {
		applySmartCull(client);
		if (!sectionsDirty) {
			return;
		}
		if (client.level == null || client.levelExtractor == null) {
			return;
		}
		sectionsDirty = false;
		// Proven remesh path (Meteor / Wurst): full extractor rebuild.
		client.levelExtractor.allChanged();
	}

	private static void markSectionsDirty() {
		sectionsDirty = true;
	}

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
