package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Custom-drawn flat rectangular menu button (top bar tabs / close / menu).
 * Sharp corners only — no circle accents.
 */
public class FlatMenuButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnPress {
		void onPress(FlatMenuButton button);
	}

	private final OnPress onPress;
	private final boolean selected;
	private boolean pressed;

	public FlatMenuButton(int x, int y, int width, int height, Component message, boolean selected, OnPress onPress) {
		super(x, y, width, height, message);
		this.selected = selected;
		this.onPress = onPress;
	}

	public FlatMenuButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		this(x, y, width, height, message, false, onPress);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused();
		int fill;
		int outline;
		int textColor;

		if (!this.active) {
			fill = theme.tabDisabledFill;
			outline = theme.tabDisabledOutline;
			textColor = theme.tabDisabledText;
		} else if (this.selected) {
			fill = theme.tabSelectedFill;
			outline = theme.tabSelectedOutline;
			textColor = theme.tabSelectedText;
		} else if (this.pressed) {
			fill = theme.tabPressedFill;
			outline = theme.tabPressedOutline;
			textColor = theme.tabText;
		} else if (hovered) {
			fill = theme.tabHoverFill;
			outline = theme.tabHoverOutline;
			textColor = theme.tabText;
		} else {
			fill = theme.tabFill;
			outline = theme.tabOutline;
			textColor = theme.tabText;
		}

		MenuShapes.drawFlatRect(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline);

		var font = Minecraft.getInstance().font;
		int textX = this.getX() + this.width / 2;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;
		graphics.centeredText(font, this.getMessage(), textX, textY, textColor);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		this.onPress.onPress(this);
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
