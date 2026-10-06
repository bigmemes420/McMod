package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Compact capsule that cycles through labeled modes on click (dropdown stand-in).
 */
public class ModeCycleButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnCycle {
		void onCycle(int index);
	}

	private final Component[] labels;
	private final OnCycle onCycle;
	private int index;
	private boolean pressed;

	public ModeCycleButton(
			int x,
			int y,
			int width,
			int height,
			Component[] labels,
			int index,
			OnCycle onCycle
	) {
		super(x, y, width, height, labels[Math.max(0, Math.min(index, labels.length - 1))]);
		this.labels = labels;
		this.index = Math.max(0, Math.min(index, labels.length - 1));
		this.onCycle = onCycle;
		setMessage(this.labels[this.index]);
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused();
		int fill;
		int outline;
		int textColor;

		if (!this.active) {
			fill = theme.capsuleDisabledFill;
			outline = theme.capsuleDisabledOutline;
			textColor = theme.capsuleDisabledText;
		} else if (this.pressed) {
			fill = theme.capsulePressedFill;
			outline = theme.capsulePressedOutline;
			textColor = theme.capsuleText;
		} else if (hovered) {
			fill = theme.capsuleHoverFill;
			outline = theme.capsuleHoverOutline;
			textColor = theme.capsuleText;
		} else {
			fill = theme.capsuleFill;
			outline = theme.capsuleOutline;
			textColor = theme.capsuleText;
		}

		MenuShapes.drawCapsule(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline);

		var font = Minecraft.getInstance().font;
		int textX = this.getX() + this.width / 2;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;
		graphics.centeredText(font, this.getMessage(), textX, textY, textColor);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		this.index = (this.index + 1) % this.labels.length;
		setMessage(this.labels[this.index]);
		this.onCycle.onCycle(this.index);
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
