package com.example.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Submenu button matching the design: circle on the left seamlessly joined to a
 * bar extending right with rounded free corners.
 */
public class CapsuleButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnPress {
		void onPress(CapsuleButton button);
	}

	private final OnPress onPress;
	private boolean pressed;

	public CapsuleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
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
		} else if (this.pressed) {
			fill = 0xFF1A3A5A;
			outline = 0xFF7EC8FF;
			textColor = 0xFFFFFFFF;
		} else if (hovered) {
			fill = 0xFF2A4A6A;
			outline = 0xFFAAD4FF;
			textColor = 0xFFFFFFFF;
		} else {
			fill = 0xFF1E1E28;
			outline = 0xFFE0E0E0;
			textColor = 0xFFE8E8E8;
		}

		MenuShapes.drawCapsule(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline);

		var font = Minecraft.getInstance().font;
		int radius = this.height / 2;
		int textX = this.getX() + radius * 2 + 4;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), textX, textY, textColor, false);
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
