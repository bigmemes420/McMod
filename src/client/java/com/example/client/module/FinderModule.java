package com.example.client.module;

import com.example.client.config.MenuTheme;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Visuals module: through-world block outlines for a dedicated selection list.
 * Own Edit screen (like X-Ray). Does not remesh or alter terrain rendering —
 * outlines are always-on-top gizmos from a periodic block scan.
 */
public final class FinderModule {
	private static boolean enabled;
	private static final LinkedHashSet<Identifier> selectedBlocks = new LinkedHashSet<>();
	private static final BlockEspScanner scanner = new BlockEspScanner(FinderModule::getSelectedBlocks, BlockEspScanner.DEFAULT_RANGE);

	static {
		BlockEspDefaults.seedOresAndChests(selectedBlocks);
	}

	private FinderModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Set<Identifier> getSelectedBlocks() {
		return Collections.unmodifiableSet(selectedBlocks);
	}

	public static boolean isSelected(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		return id != null && selectedBlocks.contains(id);
	}

	public static boolean isSelected(Identifier id) {
		return id != null && selectedBlocks.contains(id);
	}

	public static void setSelected(Identifier id, boolean selected) {
		if (id == null) {
			return;
		}
		boolean changed = selected ? selectedBlocks.add(id) : selectedBlocks.remove(id);
		if (changed) {
			scanner.clear();
			ModConfig.save();
		}
	}

	public static void loadSelectedBlocks(String csv) {
		BlockEspDefaults.loadCsv(selectedBlocks, csv, () -> BlockEspDefaults.seedOresAndChests(selectedBlocks));
		scanner.clear();
	}

	public static String selectedBlocksCsv() {
		return BlockEspDefaults.toCsv(selectedBlocks);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!enabled) {
			scanner.clear();
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.finder.enabled"
						: "screen.modid.menu.visuals.finder.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			scanner.clear();
			return;
		}
		scanner.tick(client);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void renderOverlays(LevelRenderer levelRenderer) {
		if (!enabled) {
			return;
		}
		WorldBlockEspRenderer.drawOutlines(levelRenderer, scanner.hits(), MenuTheme.get().outline);
	}
}
