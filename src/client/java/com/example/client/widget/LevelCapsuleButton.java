package com.example.client.widget;

import com.example.client.config.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Capsule with left-circle toggle (green when ON) and an in-bar level slider.
 * Clicking the numeric value opens an inline text field (Enter clamps to min/max).
 */
public class LevelCapsuleButton extends AbstractWidget {
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
	private static final int VALUE_BOX_PAD = 2;
	private static final int SLIDER_FILL = 0xFF3A6A9A;
	private static final int TRACK_BG = 0xFF12121A;
	private static final int THUMB_COLOR = 0xFFE8E8E8;
	private static final int EDIT_FILL = 0xFF1A1A28;
	private static final int EDIT_OUTLINE = 0xFF6A9AD0;

	private final OnToggle onToggle;
	private final OnLevelChange onLevelChange;
	private final float minLevel;
	private final float maxLevel;
	private final String valueWidthSample;
	private final InlineFloatEditor editor = new InlineFloatEditor();
	private boolean enabled;
	private float level;
	private boolean draggingSlider;
	private boolean pressedCircle;

	public LevelCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			boolean enabled,
			float level,
			float minLevel,
			float maxLevel,
			OnToggle onToggle,
			OnLevelChange onLevelChange
	) {
		super(x, y, width, height, message);
		this.enabled = enabled;
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		this.level = Mth.clamp(level, minLevel, maxLevel);
		this.onToggle = onToggle;
		this.onLevelChange = onLevelChange;
		// Wide enough for values like "10.0" or "0.5"
		this.valueWidthSample = String.format("%.1f", Math.max(Math.abs(minLevel), Math.abs(maxLevel)) >= 10.0F ? 999.9F : maxLevel);
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
		this.level = Mth.clamp(level, this.minLevel, this.maxLevel);
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

	private int valueBoxWidth() {
		var font = Minecraft.getInstance().font;
		return Math.max(font.width(this.valueWidthSample), font.width("0.0")) + 8;
	}

	private int valueBoxLeft() {
		return this.getX() + this.width - 8 - valueBoxWidth();
	}

	private int valueBoxRight() {
		return this.getX() + this.width - 8;
	}

	private boolean hitValue(double mouseX, double mouseY) {
		return mouseX >= valueBoxLeft() - 2 && mouseX < valueBoxRight() + 2
				&& mouseY >= this.getY() && mouseY < this.getY() + this.height;
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
		return valueBoxLeft() - VALUE_GAP;
	}

	private void applyLevel(float value) {
		this.level = Mth.clamp(value, this.minLevel, this.maxLevel);
		this.onLevelChange.onLevel(this.level);
	}

	private void commitEditor() {
		this.editor.commit(this.minLevel, this.maxLevel, this::applyLevel);
	}

	private void updateLevelFromMouse(double mouseX) {
		int left = trackLeft();
		int right = trackRight();
		float t = (float) ((mouseX - left) / (double) Math.max(1, right - left));
		t = Mth.clamp(t, 0.0F, 1.0F);
		applyLevel(Math.round((this.minLevel + t * (this.maxLevel - this.minLevel)) * 10.0F) / 10.0F);
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

			float t = (this.level - this.minLevel) / (this.maxLevel - this.minLevel);
			int fillRight = trackL + Math.round(t * (trackR - trackL));
			if (fillRight > trackL) {
				graphics.fill(trackL, trackY, fillRight, trackBottom, SLIDER_FILL);
			}

			int thumbX = Mth.clamp(fillRight - THUMB_WIDTH / 2, trackL, trackR - THUMB_WIDTH);
			graphics.fill(thumbX, trackY - 1, thumbX + THUMB_WIDTH, trackBottom + 1, THUMB_COLOR);
		}

		int boxL = valueBoxLeft();
		int boxR = valueBoxRight();
		int boxT = this.getY() + VALUE_BOX_PAD;
		int boxB = this.getY() + this.height - VALUE_BOX_PAD;
		this.editor.tickCursor();
		if (this.editor.isActive()) {
			this.editor.draw(graphics, font, boxL, boxT, boxR, boxB, textColor, EDIT_FILL, EDIT_OUTLINE);
		} else {
			if (hitValue(mouseX, mouseY)) {
				MenuShapes.drawFlatRect(graphics, boxL, boxT, boxR - boxL, boxB - boxT, EDIT_FILL, theme.capsuleHoverOutline);
			}
			String value = InlineFloatEditor.format(this.level);
			int valueWidth = font.width(value);
			graphics.text(font, value, boxR - valueWidth - 2, textY, textColor, false);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (hitValue(event.x(), event.y())) {
			this.pressedCircle = false;
			this.draggingSlider = false;
			if (!this.editor.isActive()) {
				this.editor.begin(this.level);
				this.setFocused(true);
			}
			return;
		}
		if (this.editor.isActive()) {
			commitEditor();
		}
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
		if (this.draggingSlider && !this.editor.isActive()) {
			updateLevelFromMouse(event.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.pressedCircle = false;
		this.draggingSlider = false;
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused && this.editor.isActive()) {
			commitEditor();
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.editor.keyPressed(event, this.minLevel, this.maxLevel, this::applyLevel)) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.editor.charTyped(event)) {
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
