package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Capsule mode control that opens a dropdown list of labeled options (not a cycle button).
 */
public class ModeDropdownButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnSelect {
		void onSelect(int index);
	}

	private static ModeDropdownButton openInstance;

	private final Component[] labels;
	private final OnSelect onSelect;
	private final int closedHeight;
	private final int optionHeight;
	private int index;
	private boolean open;
	private boolean pressed;

	public ModeDropdownButton(
			int x,
			int y,
			int width,
			int height,
			Component[] labels,
			int index,
			OnSelect onSelect
	) {
		super(x, y, width, height, labels[clampIndex(index, labels.length)]);
		this.labels = labels;
		this.closedHeight = height;
		this.optionHeight = height;
		this.index = clampIndex(index, labels.length);
		this.onSelect = onSelect;
		setMessage(this.labels[this.index]);
	}

	private static int clampIndex(int index, int length) {
		if (length <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(index, length - 1));
	}

	public static ModeDropdownButton getOpen() {
		return openInstance;
	}

	public static void closeOpen() {
		if (openInstance != null) {
			openInstance.setOpen(false);
		}
	}

	public int getIndex() {
		return this.index;
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
		MenuTheme theme = MenuTheme.get();
		boolean headerHovered = mouseX >= this.getX()
				&& mouseY >= this.getY()
				&& mouseX < this.getX() + this.width
				&& mouseY < this.getY() + this.closedHeight;
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
		} else if (headerHovered || this.open) {
			fill = theme.capsuleHoverFill;
			outline = theme.capsuleHoverOutline;
			textColor = theme.capsuleText;
		} else {
			fill = theme.capsuleFill;
			outline = theme.capsuleOutline;
			textColor = theme.capsuleText;
		}

		MenuShapes.drawCapsule(graphics, this.getX(), this.getY(), this.width, this.closedHeight, fill, outline);

		var font = Minecraft.getInstance().font;
		int textX = this.getX() + 10;
		int textY = this.getY() + (this.closedHeight - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), textX, textY, textColor, false);

		// Chevron
		String chevron = this.open ? "▲" : "▼";
		int chevronX = this.getX() + this.width - 14;
		graphics.text(font, Component.literal(chevron), chevronX, textY, textColor, false);

		if (!this.open) {
			return;
		}

		int panelTop = this.getY() + this.closedHeight;
		for (int i = 0; i < this.labels.length; i++) {
			int oy = panelTop + i * this.optionHeight;
			boolean optHovered = mouseX >= this.getX()
					&& mouseY >= oy
					&& mouseX < this.getX() + this.width
					&& mouseY < oy + this.optionHeight;
			boolean selected = i == this.index;
			int optFill;
			int optOutline;
			if (selected) {
				optFill = theme.tabSelectedFill;
				optOutline = theme.tabSelectedOutline;
			} else if (optHovered) {
				optFill = theme.capsuleHoverFill;
				optOutline = theme.capsuleHoverOutline;
			} else {
				optFill = theme.capsuleFill;
				optOutline = theme.capsuleOutline;
			}
			MenuShapes.drawFlatRect(graphics, this.getX(), oy, this.width, this.optionHeight, optFill, optOutline);
			int optTextY = oy + (this.optionHeight - font.lineHeight) / 2;
			graphics.text(font, this.labels[i], textX, optTextY, theme.capsuleText, false);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		double mx = event.x();
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
			setOpen(false);
			return;
		}

		this.index = i;
		setMessage(this.labels[this.index]);
		setOpen(false);
		this.onSelect.onSelect(this.index);
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
