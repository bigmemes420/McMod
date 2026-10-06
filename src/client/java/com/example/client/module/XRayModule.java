package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Visuals: X-Ray ported from Meteor Client's approach for Fabric MC 26.3.
 * <p>
 * Selected (whitelist) blocks render as normal full-opaque meshes with every
 * face forced. Non-selected blocks respect the opacity slider:
 * <ul>
 *   <li>0 → tessellation cancelled (hidden)</li>
 *   <li>1–99 → absolute vertex alpha on TRANSLUCENT</li>
 *   <li>100 → X-Ray fade inactive</li>
 * </ul>
 * No overlay boxes. Chunk occlusion is cancelled (VisGraph) and ambient
 * occlusion light is forced full while active. Faded quads are forced onto
 * TRANSLUCENT via SectionCompiler translucent-pass + BakedQuad layer rewrite
 * (MC 26.3 force-opaque SOLID path otherwise ignores vertex alpha). Remesh via
 * {@code levelExtractor.allChanged()} (debounced once per tick).
 *
 * @see <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 */
public final class XRayModule {
	public static final float MIN_OPACITY = 0.0F;
	public static final float MAX_OPACITY = 100.0F;
	public static final float DEFAULT_OPACITY = 0.0F;

	private static boolean enabled;
	private static float opacity = DEFAULT_OPACITY;
	private static final LinkedHashSet<Identifier> fullOpacityBlocks = new LinkedHashSet<>();

	/** Structural / committed opacity rebuilds — flushed once per client tick. */
	private static volatile boolean sectionsDirty;

	/** SectionCompiler routes quads to TRANSLUCENT while a faded block is meshing. */
	private static final ThreadLocal<Boolean> TRANSLUCENT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

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

	/** Absolute 0–255 alpha for non-selected faded blocks (Meteor opacity byte). */
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

	/**
	 * Meteor {@code isBlocked}: true when the block should be faded/hidden
	 * (not on the whitelist). Air is never blocked.
	 */
	public static boolean isBlocked(Block block) {
		return !isAir(block) && !isFullOpacity(block);
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

	/**
	 * Meteor {@code Xray.getAlpha}: {@code -1} = leave alone (selected / inactive),
	 * {@code 0} = hide, {@code 1–255} = fade non-selected.
	 */
	public static int getAlpha(BlockState state) {
		if (!isActive()) {
			return -1;
		}
		Block block = state.getBlock();
		if (isAir(block) || isFullOpacity(block)) {
			return -1;
		}
		return getOpacityAlpha();
	}

	public static int getAlpha(BlockState state, BlockPos pos) {
		return getAlpha(state);
	}

	/**
	 * Meteor {@code modifyDrawSide}: when a selected block's face would be culled,
	 * still draw it if the neighbor does not fully occlude (so ores show through
	 * stone that is faded/hidden).
	 */
	public static boolean modifyDrawSide(BlockState state, BlockGetter view, BlockPos pos, Direction facing, boolean returns) {
		if (!isActive()) {
			return returns;
		}
		if (!returns && !isBlocked(state.getBlock())) {
			BlockPos adjPos = pos.relative(facing);
			BlockState adjState = view.getBlockState(adjPos);
			return adjState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block()
					|| adjState.getBlock() != state.getBlock()
					|| !adjState.isSolidRender()
					|| isBlocked(adjState.getBlock());
		}
		return returns;
	}

	/** Force every face of selected blocks (Meteor {@code BlockMixin}). */
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

	/** Absolute-alpha rewrite for a baked vertex color (Sodium/Meteor path). */
	public static int applyFadeAlpha(int argb, int alpha) {
		if (alpha <= 0) {
			return argb & 0x00FFFFFF;
		}
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	/** True while meshing a non-selected faded block (SectionCompilerMixin). */
	public static boolean isTranslucentPass() {
		return Boolean.TRUE.equals(TRANSLUCENT_PASS.get());
	}

	public static void beginTranslucentPass() {
		TRANSLUCENT_PASS.set(Boolean.TRUE);
	}

	public static void endTranslucentPass() {
		TRANSLUCENT_PASS.set(Boolean.FALSE);
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
