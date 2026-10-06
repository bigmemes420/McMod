package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.module.XRayModule;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.ToggleCapsuleButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Submenu to pick which blocks always render at full opacity while X-Ray is on.
 * Includes a search bar that filters by id or display name.
 */
public class XRayBlocksScreen extends Screen {
	private static final int TOP_PAD = 28;
	private static final int SEARCH_HEIGHT = 20;
	private static final int ROW_HEIGHT = 24;
	private static final int ROW_GAP = 4;
	private static final int CAPSULE_WIDTH = 320;
	private static final int VISIBLE_ROWS = 12;

	private final Screen parent;
	private EditBox searchBox;
	private String filter = "";
	private int scrollOffset;
	private List<Identifier> filtered = List.of();

	public XRayBlocksScreen(Screen parent) {
		super(Component.translatable("screen.modid.menu.visuals.xray.edit.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		rebuild();
	}

	private void rebuild() {
		this.clearWidgets();

		int closeW = 64;
		this.addRenderableWidget(new FlatMenuButton(
				this.width - closeW - 8,
				6,
				closeW,
				20,
				Component.translatable("screen.modid.menu.close"),
				button -> this.onClose()
		));

		this.searchBox = new EditBox(
				this.font,
				24,
				TOP_PAD,
				Math.min(CAPSULE_WIDTH, this.width - 48),
				SEARCH_HEIGHT,
				Component.translatable("screen.modid.menu.visuals.xray.search")
		);
		this.searchBox.setMaxLength(64);
		this.searchBox.setValue(this.filter);
		this.searchBox.setHint(Component.translatable("screen.modid.menu.visuals.xray.search.hint"));
		this.searchBox.setResponder(value -> {
			this.filter = value == null ? "" : value;
			this.scrollOffset = 0;
			rebuildListOnly();
		});
		this.addRenderableWidget(this.searchBox);

		rebuildListOnly();
	}

	private void rebuildListOnly() {
		// Remove previous row widgets but keep search + close (indices 0,1)
		List<?> children = List.copyOf(this.children());
		for (Object child : children) {
			if (child == this.searchBox) {
				continue;
			}
			if (child instanceof FlatMenuButton) {
				continue;
			}
			if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
				this.removeWidget(widget);
			}
		}

		this.filtered = buildFiltered();
		int maxScroll = Math.max(0, this.filtered.size() - VISIBLE_ROWS);
		this.scrollOffset = Math.min(this.scrollOffset, maxScroll);

		int y = TOP_PAD + SEARCH_HEIGHT + 12;
		int end = Math.min(this.filtered.size(), this.scrollOffset + VISIBLE_ROWS);
		for (int i = this.scrollOffset; i < end; i++) {
			Identifier id = this.filtered.get(i);
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			String label = block.getName().getString() + " (" + id + ")";
			boolean selected = XRayModule.isFullOpacity(id);
			this.addRenderableWidget(new ToggleCapsuleButton(
					24,
					y,
					Math.min(CAPSULE_WIDTH, this.width - 48),
					ROW_HEIGHT,
					Component.literal(label),
					selected,
					(button, enabled) -> XRayModule.setFullOpacity(id, enabled)
			));
			y += ROW_HEIGHT + ROW_GAP;
		}
	}

	private List<Identifier> buildFiltered() {
		String q = this.filter.trim().toLowerCase(Locale.ROOT);
		List<Identifier> out = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
				continue;
			}
			if (!q.isEmpty()) {
				String path = id.toString().toLowerCase(Locale.ROOT);
				String name = block.getName().getString().toLowerCase(Locale.ROOT);
				if (!path.contains(q) && !name.contains(q)) {
					continue;
				}
			}
			out.add(id);
		}
		out.sort(Comparator
				.comparing((Identifier id) -> !XRayModule.isFullOpacity(id))
				.thenComparing(Identifier::toString));
		return out;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			int maxScroll = Math.max(0, this.filtered.size() - VISIBLE_ROWS);
			this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
			rebuildListOnly();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.fill(0, 0, this.width, 28, theme.topBar);
		graphics.horizontalLine(0, this.width - 1, 28, theme.topBarLine);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		MenuTheme theme = MenuTheme.get();
		graphics.text(this.font, this.title, 24, 8, theme.title, true);
		String hint = Component.translatable(
				"screen.modid.menu.visuals.xray.edit.hint",
				this.filtered.size(),
				XRayModule.getFullOpacityBlocks().size()
		).getString();
		int hintX = 24 + this.font.width(this.title) + 16;
		graphics.text(this.font, hint, hintX, 8, theme.panelHint, false);
	}
}
