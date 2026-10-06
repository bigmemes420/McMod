package com.example.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Custom-drawn flat rectangular menu button (top bar tabs / close). No circle
 * accents — sharp corners only, not the stock Minecraft button texture.
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
		boolean hovered = this.isHoveredOrFocused();
		int fill;
		int outline;
		int textColor;

		if (!this.active) {
			fill = 0xFF2A2A2A;
			outline = 0xFF555555;
			textColor = 0xFF888888;
		} else if (this.selected) {
			fill = 0xFF2E5A8A;
			outline = 0xFF8EC8FF;
			textColor = 0xFFFFFFFF;
		} else if (this.pressed) {
			fill = 0xFF1A3A5A;
			outline = 0xFF7EC8FF;
			textColor = 0xFFFFFFFF;
		} else if (hovered) {
			fill = 0xFF3A3A48;
			outline = 0xFFB0B0C0;
			textColor = 0xFFFFFFFF;
		} else {
			fill = 0xFF252530;
			outline = 0xFF707080;
			textColor = 0xFFDDDDDD;
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
