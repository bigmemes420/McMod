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
 * Settings-panel slider: left label, track, and a clickable numeric value that
 * opens an inline text field (Enter commits + clamps to min/max).
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
	private static final int VALUE_BOX_PAD = 2;
	private static final int SLIDER_FILL = 0xFF3A6A9A;
	private static final int TRACK_BG = 0xFF12121A;
	private static final int THUMB_COLOR = 0xFFE8E8E8;
	private static final int PANEL_PAD_X = 8;
	private static final int EDIT_FILL = 0xFF1A1A28;
	private static final int EDIT_OUTLINE = 0xFF6A9AD0;

	private final OnLevelChange onLevelChange;
	private final Runnable onRelease;
	private final float minLevel;
	private final float maxLevel;
	private final String valueWidthSample;
	private final int labelWidth;
	private final InlineFloatEditor editor = new InlineFloatEditor();
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
				Math.max(Math.abs(minLevel), Math.abs(maxLevel)) >= 10.0F ? 999.9F : maxLevel
		);
		this.labelWidth = Minecraft.getInstance().font.width(label);
	}

	public float getLevel() {
		return this.level;
	}

	public void setLevel(float level) {
		this.level = Mth.clamp(level, this.minLevel, this.maxLevel);
	}

	private int valueBoxWidth() {
		var font = Minecraft.getInstance().font;
		return Math.max(font.width(this.valueWidthSample), font.width("0.0")) + 8;
	}

	private int valueBoxLeft() {
		return this.getX() + this.width - PANEL_PAD_X - valueBoxWidth();
	}

	private int valueBoxRight() {
		return this.getX() + this.width - PANEL_PAD_X;
	}

	private boolean hitValue(double mouseX, double mouseY) {
		int left = valueBoxLeft() - 2;
		int right = valueBoxRight() + 2;
		return mouseX >= left && mouseX < right
				&& mouseY >= this.getY() && mouseY < this.getY() + this.height;
	}

	private int trackLeft() {
		return this.getX() + PANEL_PAD_X + this.labelWidth + LABEL_GAP;
	}

	private int trackRight() {
		return valueBoxLeft() - VALUE_GAP;
	}

	private void applyLevel(float value) {
		this.level = Mth.clamp(value, this.minLevel, this.maxLevel);
		this.onLevelChange.onLevel(this.level);
	}

	private void commitEditor() {
		this.editor.commit(this.minLevel, this.maxLevel, v -> {
			applyLevel(v);
			if (this.onRelease != null) {
				this.onRelease.run();
			}
		});
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
		this.editor.tickCursor();
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused() || this.dragging || this.editor.isActive();
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

		int boxL = valueBoxLeft();
		int boxR = valueBoxRight();
		int boxT = this.getY() + VALUE_BOX_PAD;
		int boxB = this.getY() + this.height - VALUE_BOX_PAD;
		if (this.editor.isActive()) {
			this.editor.draw(graphics, font, boxL, boxT, boxR, boxB, textColor, EDIT_FILL, EDIT_OUTLINE);
		} else {
			boolean valueHover = hitValue(mouseX, mouseY);
			if (valueHover) {
				MenuShapes.drawFlatRect(graphics, boxL, boxT, boxR - boxL, boxB - boxT, EDIT_FILL, theme.capsuleHoverOutline);
			}
			String value = InlineFloatEditor.format(this.level);
			int valueWidth = font.width(value);
			int valueX = boxR - valueWidth - 2;
			graphics.text(font, value, valueX, textY, textColor, false);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (hitValue(event.x(), event.y())) {
			this.dragging = false;
			if (this.editor.isActive()) {
				return;
			}
			this.editor.begin(this.level);
			this.setFocused(true);
			return;
		}
		if (this.editor.isActive()) {
			commitEditor();
		}
		this.dragging = true;
		updateLevelFromMouse(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging && !this.editor.isActive()) {
			updateLevelFromMouse(event.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		boolean wasDragging = this.dragging;
		this.dragging = false;
		if (wasDragging && this.onRelease != null && !this.editor.isActive()) {
			this.onRelease.run();
		}
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
		if (this.editor.keyPressed(event, this.minLevel, this.maxLevel, v -> {
			applyLevel(v);
			if (this.onRelease != null) {
				this.onRelease.run();
			}
		})) {
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
