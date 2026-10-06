package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.module.MobEspModule;
import com.example.client.widget.ColorSwatchButton;
import com.example.client.widget.FlatMenuButton;
import com.example.client.widget.ToggleCapsuleButton;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Submenu to pick which mobs get Mob ESP, with a per-mob custom color swatch.
 */
public class MobEspMobsScreen extends Screen {
	private static final int TOP_PAD = 28;
	private static final int SEARCH_HEIGHT = 20;
	private static final int ROW_HEIGHT = 24;
	private static final int ROW_GAP = 4;
	private static final int CAPSULE_WIDTH = 360;
	private static final int COLOR_WIDTH = 28;
	private static final int VISIBLE_ROWS = 12;

	private final Screen parent;
	private EditBox searchBox;
	private String filter = "";
	private int scrollOffset;
	private List<Identifier> filtered = List.of();

	public MobEspMobsScreen(Screen parent) {
		super(Component.translatable("screen.modid.menu.visuals.mob_esp.edit.title"));
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
				Component.translatable("screen.modid.menu.visuals.mob_esp.search")
		);
		this.searchBox.setMaxLength(64);
		this.searchBox.setValue(this.filter);
		this.searchBox.setHint(Component.translatable("screen.modid.menu.visuals.mob_esp.search.hint"));
		this.searchBox.setResponder(value -> {
			this.filter = value == null ? "" : value;
			this.scrollOffset = 0;
			rebuildListOnly();
		});
		this.addRenderableWidget(this.searchBox);

		rebuildListOnly();
	}

	private void rebuildListOnly() {
		List<?> children = List.copyOf(this.children());
		for (Object child : children) {
			if (child == this.searchBox) {
				continue;
			}
			if (child instanceof FlatMenuButton) {
				continue;
			}
			if (child instanceof AbstractWidget widget) {
				this.removeWidget(widget);
			}
		}

		this.filtered = buildFiltered();
		int maxScroll = Math.max(0, this.filtered.size() - VISIBLE_ROWS);
		this.scrollOffset = Math.min(this.scrollOffset, maxScroll);

		int rowWidth = Math.min(CAPSULE_WIDTH, this.width - 48 - COLOR_WIDTH - 8);
		int y = TOP_PAD + SEARCH_HEIGHT + 12;
		int end = Math.min(this.filtered.size(), this.scrollOffset + VISIBLE_ROWS);
		for (int i = this.scrollOffset; i < end; i++) {
			Identifier id = this.filtered.get(i);
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
			String label = type.getDescription().getString() + " (" + id + ")";
			boolean selected = MobEspModule.isSelected(id);
			this.addRenderableWidget(new ToggleCapsuleButton(
					24,
					y,
					rowWidth,
					ROW_HEIGHT,
					Component.literal(label),
					selected,
					iconFor(type),
					(button, enabled) -> MobEspModule.setSelected(id, enabled)
			));
			int colorX = 24 + rowWidth + 8;
			int color = MobEspModule.getMobColor(id);
			this.addRenderableWidget(new ColorSwatchButton(
					colorX,
					y,
					COLOR_WIDTH,
					ROW_HEIGHT,
					color,
					() -> {
						if (this.minecraft != null) {
							String title = type.getDescription().getString();
							this.minecraft.gui.setScreen(new ColorPickerScreen(
									this,
									title,
									MobEspModule.getMobColor(id),
									argb -> {
										MobEspModule.setMobColor(id, argb);
										MobEspModule.setSelected(id, true);
									}
							));
						}
					}
			));
			y += ROW_HEIGHT + ROW_GAP;
		}
	}

	private static ItemStack iconFor(EntityType<?> type) {
		Optional<net.minecraft.core.Holder<net.minecraft.world.item.Item>> egg = SpawnEggItem.byId(type);
		return egg.map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
	}

	private List<Identifier> buildFiltered() {
		String q = this.filter.trim().toLowerCase(Locale.ROOT);
		List<Identifier> out = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (!MobEspModule.isListableMob(id)) {
				continue;
			}
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
			if (!q.isEmpty()) {
				String path = id.toString().toLowerCase(Locale.ROOT);
				String name = type.getDescription().getString().toLowerCase(Locale.ROOT);
				if (!path.contains(q) && !name.contains(q)) {
					continue;
				}
			}
			out.add(id);
		}
		out.sort(Comparator
				.comparing((Identifier id) -> !MobEspModule.isSelected(id))
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
				"screen.modid.menu.visuals.mob_esp.edit.hint",
				this.filtered.size(),
				MobEspModule.getSelectedMobs().size()
		).getString();
		int hintX = 24 + this.font.width(this.title) + 16;
		graphics.text(this.font, hint, hintX, 8, theme.panelHint, false);
	}
}
