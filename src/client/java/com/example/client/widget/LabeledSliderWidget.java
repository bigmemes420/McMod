package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Settings-panel slider: left label, track, numeric value. No toggle circle.
 */
public class LabeledSliderWidget extends AbstractWidget {
	@FunctionalInterface
	public interface OnLevelChange {
		void onLevel(float level);
	}

	private static final int TRACK_HEIGHT = 6;
	private static final int THUMB_WIDTH = 3;
	private static final int LABEL_GAP = 8;
	private static final int VALUE_GAP = 6;
	private static final int SLIDER_FILL = 0xFF3A6A9A;
	private static final int TRACK_BG = 0xFF12121A;
	private static final int THUMB_COLOR = 0xFFE8E8E8;
	private static final int PANEL_PAD_X = 8;

	private final OnLevelChange onLevelChange;
	private final Runnable onRelease;
	private final float minLevel;
	private final float maxLevel;
	private final String valueWidthSample;
	private final int labelWidth;
	private float level;
	private boolean dragging;

	public LabeledSliderWidget(
			int x,
			int y,
			int width,
			int height,
			Component label,
			float level,
			float minLevel,
			float maxLevel,
			OnLevelChange onLevelChange
	) {
		this(x, y, width, height, label, level, minLevel, maxLevel, onLevelChange, null);
	}

	public LabeledSliderWidget(
			int x,
			int y,
			int width,
			int height,
			Component label,
			float level,
			float minLevel,
			float maxLevel,
			OnLevelChange onLevelChange,
			Runnable onRelease
	) {
		super(x, y, width, height, label);
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		this.level = Mth.clamp(level, minLevel, maxLevel);
		this.onLevelChange = onLevelChange;
		this.onRelease = onRelease;
		this.valueWidthSample = String.format(
				"%.1f",
				Math.max(Math.abs(minLevel), Math.abs(maxLevel)) >= 10.0F ? 99.9F : maxLevel
		);
		this.labelWidth = Minecraft.getInstance().font.width(label);
	}

	public float getLevel() {
		return this.level;
	}

	public void setLevel(float level) {
		this.level = Mth.clamp(level, this.minLevel, this.maxLevel);
	}

	private int trackLeft() {
		return this.getX() + PANEL_PAD_X + this.labelWidth + LABEL_GAP;
	}

	private int trackRight() {
		var font = Minecraft.getInstance().font;
		int valueWidth = font.width(this.valueWidthSample);
		return this.getX() + this.width - PANEL_PAD_X - valueWidth - VALUE_GAP;
	}

	private void updateLevelFromMouse(double mouseX) {
		int left = trackLeft();
		int right = trackRight();
		float t = (float) ((mouseX - left) / (double) Math.max(1, right - left));
		t = Mth.clamp(t, 0.0F, 1.0F);
		this.level = Mth.clamp(
				Math.round((this.minLevel + t * (this.maxLevel - this.minLevel)) * 10.0F) / 10.0F,
				this.minLevel,
				this.maxLevel
		);
		this.onLevelChange.onLevel(this.level);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused() || this.dragging;
		int fill = hovered ? theme.capsuleHoverFill : theme.capsuleFill;
		int outline = hovered ? theme.capsuleHoverOutline : theme.capsuleOutline;
		int textColor = theme.capsuleText;

		MenuShapes.drawFlatRect(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline);

		var font = Minecraft.getInstance().font;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), this.getX() + PANEL_PAD_X, textY, textColor, false);

		int trackL = trackLeft();
		int trackR = trackRight();
		if (trackR > trackL + 4) {
			int trackY = this.getY() + (this.height - TRACK_HEIGHT) / 2;
			int trackBottom = trackY + TRACK_HEIGHT;
			graphics.fill(trackL, trackY, trackR, trackBottom, TRACK_BG);

			float t = (this.level - this.minLevel) / (this.maxLevel - this.minLevel);
			int fillRight = trackL + Math.round(t * (trackR - trackL));
			if (fillRight > trackL) {
				graphics.fill(trackL, trackY, fillRight, trackBottom, SLIDER_FILL);
			}

			int thumbX = Mth.clamp(fillRight - THUMB_WIDTH / 2, trackL, trackR - THUMB_WIDTH);
			graphics.fill(thumbX, trackY - 1, thumbX + THUMB_WIDTH, trackBottom + 1, THUMB_COLOR);
		}

		String value = String.format("%.1f", this.level);
		int valueWidth = font.width(value);
		int valueX = this.getX() + this.width - PANEL_PAD_X - valueWidth;
		graphics.text(font, value, valueX, textY, textColor, false);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.dragging = true;
		updateLevelFromMouse(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging) {
			updateLevelFromMouse(event.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		boolean wasDragging = this.dragging;
		this.dragging = false;
		if (wasDragging && this.onRelease != null) {
			this.onRelease.run();
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
