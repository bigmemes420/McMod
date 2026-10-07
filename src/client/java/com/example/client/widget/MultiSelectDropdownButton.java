package com.example.client.widget;

import com.example.ExampleMod;
import com.example.client.config.MenuTheme;
import com.example.client.util.TextClip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Reusable multi-select dropdown: header toggles the panel; clicking an option
 * toggles that entry without closing. Draw the open panel via {@link #extractOverlay}.
 */
public class MultiSelectDropdownButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnToggle {
		void onToggle(int index, boolean selected);
	}

	private static final Identifier DROPDOWN_BUTTON_TEXTURE = ExampleMod.id("textures/gui/dropdown_button.png");
	private static final int TEXTURE_WIDTH = 510;
	private static final int TEXTURE_HEIGHT = 111;

	private static MultiSelectDropdownButton openInstance;

	private final Component title;
	private final Component[] labels;
	private final boolean[] selected;
	private final OnToggle onToggle;
	private final int closedHeight;
	private final int optionHeight;
	private boolean open;
	private boolean pressed;

	public MultiSelectDropdownButton(
			int x,
			int y,
			int width,
			int height,
			Component title,
			Component[] labels,
			boolean[] selected,
			OnToggle onToggle
	) {
		super(x, y, width, height, title);
		if (labels == null || labels.length == 0) {
			throw new IllegalArgumentException("labels required");
		}
		this.title = title;
		this.labels = labels;
		this.selected = new boolean[labels.length];
		for (int i = 0; i < labels.length; i++) {
			this.selected[i] = selected != null && i < selected.length && selected[i];
		}
		this.onToggle = onToggle;
		this.closedHeight = height;
		this.optionHeight = height;
		refreshHeaderMessage();
	}

	public static MultiSelectDropdownButton getOpen() {
		return openInstance;
	}

	public static void closeOpen() {
		if (openInstance != null) {
			openInstance.setOpen(false);
		}
	}

	public boolean isOpen() {
		return this.open;
	}

	public void setOpen(boolean open) {
		if (this.open == open) {
			return;
		}
		this.open = open;
		if (open) {
			ModeDropdownButton.closeOpen();
			if (openInstance != null && openInstance != this) {
				openInstance.setOpen(false);
			}
			openInstance = this;
			setHeight(this.closedHeight + this.labels.length * this.optionHeight);
		} else {
			if (openInstance == this) {
				openInstance = null;
			}
			setHeight(this.closedHeight);
		}
	}

	public void close() {
		setOpen(false);
	}

	public boolean[] getSelected() {
		return this.selected.clone();
	}

	private void refreshHeaderMessage() {
		int count = 0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.labels.length; i++) {
			if (!this.selected[i]) {
				continue;
			}
			if (count > 0) {
				sb.append(", ");
			}
			sb.append(this.labels[i].getString());
			count++;
		}
		if (count == 0) {
			setMessage(Component.translatable("screen.rootymenu.menu.combat.targeting.none"));
		} else if (count == this.labels.length) {
			setMessage(Component.translatable("screen.rootymenu.menu.combat.targeting.all"));
		} else {
			setMessage(Component.literal(sb.toString()));
		}
	}

	private int expandedBottom() {
		return this.getY() + this.closedHeight + this.labels.length * this.optionHeight;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (!this.active || !this.visible) {
			return false;
		}
		int bottom = this.open ? expandedBottom() : this.getY() + this.closedHeight;
		return mouseX >= this.getX()
				&& mouseY >= this.getY()
				&& mouseX < this.getX() + this.width
				&& mouseY < bottom;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		drawHeader(graphics, mouseX, mouseY);
	}

	public void extractOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!this.open) {
			return;
		}
		drawPanel(graphics, mouseX, mouseY);
	}

	private void drawHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		MenuTheme theme = MenuTheme.get();
		boolean headerHovered = mouseX >= this.getX()
				&& mouseY >= this.getY()
				&& mouseX < this.getX() + this.width
				&& mouseY < this.getY() + this.closedHeight;
		int fill;
		int textColor;
		int tint;
		if (!this.active) {
			fill = theme.capsuleDisabledFill;
			textColor = theme.capsuleDisabledText;
			tint = theme.capsuleDisabledOutline;
		} else if (this.pressed || headerHovered || this.open) {
			fill = theme.dropdownHoverFill;
			textColor = theme.dropdownText;
			tint = theme.dropdownHoverOutline;
		} else {
			fill = theme.dropdownFill;
			textColor = theme.dropdownText;
			tint = theme.dropdownOutline;
		}

		MenuShapes.drawFlatRect(graphics, this.getX(), this.getY(), this.width, this.closedHeight, fill, tint);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				DROPDOWN_BUTTON_TEXTURE,
				this.getX() + 1,
				this.getY() + 1,
				0.0F,
				0.0F,
				Math.max(1, this.width - 2),
				Math.max(1, this.closedHeight - 2),
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT,
				fill | 0xFF000000
		);

		var font = Minecraft.getInstance().font;
		int textX = this.getX() + 10;
		int textY = this.getY() + (this.closedHeight - font.lineHeight) / 2;
		// Title on the left when space allows; value summary is the message.
		String titleStr = this.title.getString();
		int titleW = font.width(titleStr);
		int maxLabelW = Math.max(8, this.width - 28);
		if (titleW + 8 < maxLabelW / 2) {
			graphics.text(font, this.title, textX, textY, textColor, false);
			int valueX = textX + titleW + 6;
			int valueMax = Math.max(8, this.width - 28 - titleW - 6);
			Component clipped = TextClip.ellipsize(font, this.getMessage(), valueMax);
			graphics.text(font, clipped, valueX, textY, textColor, false);
		} else {
			Component clipped = TextClip.ellipsize(font, this.getMessage(), maxLabelW);
			graphics.text(font, clipped, textX, textY, textColor, false);
		}
		String chevron = this.open ? "▲" : "▼";
		graphics.text(font, Component.literal(chevron), this.getX() + this.width - 14, textY, textColor, false);
	}

	private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		MenuTheme theme = MenuTheme.get();
		var font = Minecraft.getInstance().font;
		int panelTop = this.getY() + this.closedHeight;
		for (int i = 0; i < this.labels.length; i++) {
			int oy = panelTop + i * this.optionHeight;
			boolean optHovered = mouseX >= this.getX()
					&& mouseY >= oy
					&& mouseX < this.getX() + this.width
					&& mouseY < oy + this.optionHeight;
			boolean on = this.selected[i];
			int optFill;
			int optOutline;
			if (on) {
				optFill = theme.dropdownSelectedFill;
				optOutline = theme.dropdownSelectedOutline;
			} else if (optHovered) {
				optFill = theme.dropdownHoverFill;
				optOutline = theme.dropdownHoverOutline;
			} else {
				optFill = theme.dropdownFill;
				optOutline = theme.dropdownOutline;
			}
			MenuShapes.drawFlatRect(graphics, this.getX(), oy, this.width, this.optionHeight, optFill, optOutline);
			int optTextY = oy + (this.optionHeight - font.lineHeight) / 2;
			String mark = on ? "✓ " : "   ";
			Component line = Component.literal(mark).append(this.labels[i]);
			Component clipped = TextClip.ellipsize(font, line, Math.max(8, this.width - 20));
			graphics.text(font, clipped, this.getX() + 10, optTextY, theme.dropdownText, false);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		double my = event.y();
		if (my >= this.getY() && my < this.getY() + this.closedHeight) {
			setOpen(!this.open);
			return;
		}
		if (!this.open) {
			return;
		}
		int panelTop = this.getY() + this.closedHeight;
		if (my < panelTop || my >= expandedBottom()) {
			setOpen(false);
			return;
		}
		int i = (int) ((my - panelTop) / this.optionHeight);
		if (i < 0 || i >= this.labels.length) {
			return;
		}
		this.selected[i] = !this.selected[i];
		refreshHeaderMessage();
		if (this.onToggle != null) {
			this.onToggle.onToggle(i, this.selected[i]);
		}
		// Stay open for further toggles.
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.pressed = false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
