package com.example.client.widget;

import com.example.client.config.MenuTheme;
import com.example.client.module.SpeedModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Speed capsule: left circle toggles the module (green when ON); the bar holds
 * a custom in-button slider for speed level. Clicking outside the circle does
 * not toggle.
 */
public class SpeedCapsuleButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnToggle {
		void onToggle(boolean enabled);
	}

	@FunctionalInterface
	public interface OnLevelChange {
		void onLevel(float level);
	}

	private static final int TRACK_PAD_Y = 7;
	private static final int TRACK_HEIGHT = 6;
	private static final int THUMB_WIDTH = 3;
	private static final int VALUE_GAP = 6;
	private static final int SLIDER_FILL = 0xFF3A6A9A;
	private static final int TRACK_BG = 0xFF12121A;
	private static final int THUMB_COLOR = 0xFFE8E8E8;

	private final OnToggle onToggle;
	private final OnLevelChange onLevelChange;
	private boolean enabled;
	private float level;
	private boolean draggingSlider;
	private boolean pressedCircle;

	public SpeedCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			boolean enabled,
			float level,
			OnToggle onToggle,
			OnLevelChange onLevelChange
	) {
		super(x, y, width, height, Component.translatable("screen.modid.menu.movement.speed"));
		this.enabled = enabled;
		this.level = Mth.clamp(level, SpeedModule.MIN_LEVEL, SpeedModule.MAX_LEVEL);
		this.onToggle = onToggle;
		this.onLevelChange = onLevelChange;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public float getLevel() {
		return this.level;
	}

	public void setLevel(float level) {
		this.level = Mth.clamp(level, SpeedModule.MIN_LEVEL, SpeedModule.MAX_LEVEL);
	}

	private int circleRadius() {
		return this.height / 2;
	}

	private boolean isInLeftCircle(double mouseX, double mouseY) {
		int radius = circleRadius();
		int cx = this.getX() + radius;
		int cy = this.getY() + radius;
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		return dx * dx + dy * dy <= (double) (radius + 1) * (radius + 1);
	}

	private String levelText() {
		return String.format("%.1f", this.level);
	}

	/** Slider track starts after the left circle + label. */
	private int trackLeft() {
		var font = Minecraft.getInstance().font;
		int radius = circleRadius();
		int labelWidth = font.width(this.getMessage());
		return this.getX() + radius * 2 + 4 + labelWidth + 8;
	}

	/** Track ends before the numeric level value. */
	private int trackRight() {
		var font = Minecraft.getInstance().font;
		int valueWidth = font.width("10.0");
		return this.getX() + this.width - 8 - valueWidth - VALUE_GAP;
	}

	private void updateLevelFromMouse(double mouseX) {
		int left = trackLeft();
		int right = trackRight();
		float t = (float) ((mouseX - left) / (double) Math.max(1, right - left));
		t = Mth.clamp(t, 0.0F, 1.0F);
		float min = SpeedModule.MIN_LEVEL;
		float max = SpeedModule.MAX_LEVEL;
		this.level = Mth.clamp(Math.round((min + t * (max - min)) * 10.0F) / 10.0F, min, max);
		this.onLevelChange.onLevel(this.level);
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
		} else if (this.pressedCircle) {
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
		int radius = circleRadius();
		int textX = this.getX() + radius * 2 + 4;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;
		graphics.text(font, this.getMessage(), textX, textY, textColor, false);

		int trackL = trackLeft();
		int trackR = trackRight();
		if (trackR > trackL + 4) {
			int trackY = this.getY() + TRACK_PAD_Y;
			int trackBottom = trackY + TRACK_HEIGHT;
			graphics.fill(trackL, trackY, trackR, trackBottom, TRACK_BG);

			float min = SpeedModule.MIN_LEVEL;
			float max = SpeedModule.MAX_LEVEL;
			float t = (this.level - min) / (max - min);
			int fillRight = trackL + Math.round(t * (trackR - trackL));
			if (fillRight > trackL) {
				graphics.fill(trackL, trackY, fillRight, trackBottom, SLIDER_FILL);
			}

			int thumbX = Mth.clamp(fillRight - THUMB_WIDTH / 2, trackL, trackR - THUMB_WIDTH);
			graphics.fill(thumbX, trackY - 1, thumbX + THUMB_WIDTH, trackBottom + 1, THUMB_COLOR);
		}

		String value = levelText();
		int valueWidth = font.width(value);
		int valueX = this.getX() + this.width - 8 - valueWidth;
		graphics.text(font, value, valueX, textY, textColor, false);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (isInLeftCircle(event.x(), event.y())) {
			this.pressedCircle = true;
			this.draggingSlider = false;
			this.enabled = !this.enabled;
			this.onToggle.onToggle(this.enabled);
		} else {
			this.pressedCircle = false;
			this.draggingSlider = true;
			updateLevelFromMouse(event.x());
		}
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		if (this.draggingSlider) {
			updateLevelFromMouse(event.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.pressedCircle = false;
		this.draggingSlider = false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
