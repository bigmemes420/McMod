package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Capsule-style module row. The left circle turns green when ON; no separate
 * ON/OFF knob widget. Optional item/block icon is drawn after the circle.
 */
public class ToggleCapsuleButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnToggle {
		void onToggle(ToggleCapsuleButton button, boolean enabled);
	}

	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 4;

	private final OnToggle onToggle;
	private final ItemStack icon;
	private boolean enabled;
	private boolean pressed;

	public ToggleCapsuleButton(int x, int y, int width, int height, Component message, boolean enabled, OnToggle onToggle) {
		this(x, y, width, height, message, enabled, ItemStack.EMPTY, onToggle);
	}

	public ToggleCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			boolean enabled,
			ItemStack icon,
			OnToggle onToggle
	) {
		super(x, y, width, height, message);
		this.enabled = enabled;
		this.icon = icon == null || icon.isEmpty() ? ItemStack.EMPTY : icon.copy();
		this.onToggle = onToggle;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused();
		int fill;
		int outline;
		int textColor;
		int leftCircle;

		if (!this.active) {
			fill = theme.capsuleDisabledFill;
			outline = theme.capsuleDisabledOutline;
			textColor = theme.capsuleDisabledText;
			leftCircle = fill;
		} else if (this.pressed) {
			fill = theme.capsulePressedFill;
			outline = theme.capsulePressedOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		} else if (hovered) {
			fill = theme.capsuleHoverFill;
			outline = theme.capsuleHoverOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		} else {
			fill = theme.capsuleFill;
			outline = theme.capsuleOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		}

		MenuShapes.drawCapsule(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline, leftCircle);

		var font = Minecraft.getInstance().font;
		int radius = this.height / 2;
		int contentX = this.getX() + radius * 2 + 4;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;

		if (!this.icon.isEmpty()) {
			int iconY = this.getY() + (this.height - ICON_SIZE) / 2;
			graphics.fakeItem(this.icon, contentX, iconY);
			contentX += ICON_SIZE + ICON_GAP;
		}

		graphics.text(font, this.getMessage(), contentX, textY, textColor, false);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		this.enabled = !this.enabled;
		this.onToggle.onToggle(this, this.enabled);
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
