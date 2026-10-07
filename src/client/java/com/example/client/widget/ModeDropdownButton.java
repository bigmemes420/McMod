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
 * Mode control that opens a dropdown list of labeled options.
 * Closed header uses a flat fill + optional {@code dropdown_button} texture tint
 * (no left-circle capsule — that caused stray corner pixels outside the rect).
 * The open panel is drawn via {@link #extractOverlay} so it stacks above later rows.
 */
public class ModeDropdownButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnSelect {
		void onSelect(int index);
	}

	private static final Identifier DROPDOWN_BUTTON_TEXTURE = ExampleMod.id("textures/gui/dropdown_button.png");
	private static final int TEXTURE_WIDTH = 510;
	private static final int TEXTURE_HEIGHT = 111;

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

	/** Header only — panel is drawn later via {@link #extractOverlay}. */
	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		drawHeader(graphics, mouseX, mouseY);
	}

	/** Draw the open option list above overlapping widgets (call after other UI). */
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
		} else if (this.pressed) {
			fill = theme.dropdownHoverFill;
			textColor = theme.dropdownText;
			tint = theme.dropdownHoverOutline;
		} else if (headerHovered || this.open) {
			fill = theme.dropdownHoverFill;
			textColor = theme.dropdownText;
			tint = theme.dropdownHoverOutline;
		} else {
			fill = theme.dropdownFill;
			textColor = theme.dropdownText;
			tint = theme.dropdownOutline;
		}

		// Flat rect only — capsule left-circle overflow was the stray TL/BL pixels.
		MenuShapes.drawFlatRect(graphics, this.getX(), this.getY(), this.width, this.closedHeight, fill, tint);
		// Texture is an opaque white mask; tint with fill so it does not overwrite the outline.
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
		// Leave room for padding + chevron so long filenames cannot spill out.
		int maxLabelW = Math.max(8, this.width - 28);
		Component clipped = TextClip.ellipsize(font, this.getMessage(), maxLabelW);
		graphics.text(font, clipped, textX, textY, textColor, false);

		String chevron = this.open ? "▲" : "▼";
		int chevronX = this.getX() + this.width - 14;
		graphics.text(font, Component.literal(chevron), chevronX, textY, textColor, false);
	}

	private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		MenuTheme theme = MenuTheme.get();
		var font = Minecraft.getInstance().font;
		int textX = this.getX() + 10;
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
			int maxOptW = Math.max(8, this.width - 20);
			Component clipped = TextClip.ellipsize(font, this.labels[i], maxOptW);
			graphics.text(font, clipped, textX, optTextY, theme.dropdownText, false);
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
