package com.example.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Small filled color swatch that opens a picker (or runs a callback) on click. */
public class ColorSwatchButton extends AbstractWidget {
	private int color;
	private final Runnable onPress;

	public ColorSwatchButton(int x, int y, int width, int height, int color, Runnable onPress) {
		super(x, y, width, height, Component.empty());
		this.color = color | 0xFF000000;
		this.onPress = onPress;
	}

	public void setColor(int argb) {
		this.color = argb | 0xFF000000;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.color);
		int outline = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF808080;
		graphics.outline(this.getX(), this.getY(), this.width, this.height, outline);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.onPress.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
