package com.example.client.widget;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/**
 * Clickable numeric field for sliders: type a value; Enter/focus-loss clamps to
 * [min, max]; Esc cancels.
 */
final class InlineFloatEditor {
	private boolean active;
	private String buffer = "";
	private int cursorBlink;

	boolean isActive() {
		return this.active;
	}

	void begin(float current) {
		this.active = true;
		this.buffer = format(current);
		this.cursorBlink = 0;
	}

	void cancel() {
		this.active = false;
		this.buffer = "";
	}

	/** Parse + clamp; returns true if a value was applied. */
	boolean commit(float min, float max, Consumer<Float> apply) {
		if (!this.active) {
			return false;
		}
		this.active = false;
		String raw = this.buffer.trim();
		this.buffer = "";
		if (raw.isEmpty() || "-".equals(raw) || ".".equals(raw) || "-.".equals(raw)) {
			return false;
		}
		try {
			float parsed = Float.parseFloat(raw);
			if (!Float.isFinite(parsed)) {
				return false;
			}
			float clamped = Mth.clamp(Math.round(parsed * 10.0F) / 10.0F, min, max);
			apply.accept(clamped);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	boolean keyPressed(KeyEvent event, float min, float max, Consumer<Float> apply) {
		if (!this.active) {
			return false;
		}
		int key = event.key();
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			commit(min, max, apply);
			return true;
		}
		if (key == InputConstants.KEY_ESCAPE) {
			cancel();
			return true;
		}
		if (key == InputConstants.KEY_BACKSPACE) {
			if (!this.buffer.isEmpty()) {
				this.buffer = this.buffer.substring(0, this.buffer.length() - 1);
			}
			return true;
		}
		return true; // consume other keys while editing
	}

	boolean charTyped(CharacterEvent event) {
		if (!this.active) {
			return false;
		}
		int cp = event.codepoint();
		if (cp >= '0' && cp <= '9') {
			if (this.buffer.length() < 12) {
				this.buffer += (char) cp;
			}
			return true;
		}
		if (cp == '.' && this.buffer.indexOf('.') < 0 && this.buffer.length() < 12) {
			this.buffer += '.';
			return true;
		}
		if (cp == '-' && this.buffer.isEmpty()) {
			this.buffer += '-';
			return true;
		}
		return true;
	}

	void tickCursor() {
		if (this.active) {
			this.cursorBlink++;
		}
	}

	void draw(
			GuiGraphicsExtractor graphics,
			Font font,
			int boxLeft,
			int boxTop,
			int boxRight,
			int boxBottom,
			int textColor,
			int boxFill,
			int boxOutline
	) {
		MenuShapes.drawFlatRect(
				graphics,
				boxLeft,
				boxTop,
				Math.max(1, boxRight - boxLeft),
				Math.max(1, boxBottom - boxTop),
				boxFill,
				boxOutline
		);
		int textY = boxTop + (boxBottom - boxTop - font.lineHeight) / 2;
		String shown = this.buffer;
		if ((this.cursorBlink / 6) % 2 == 0) {
			shown = shown + "|";
		}
		String clipped = font.plainSubstrByWidth(shown, Math.max(4, boxRight - boxLeft - 6));
		graphics.text(font, clipped, boxLeft + 3, textY, textColor, false);
	}

	static String format(float value) {
		return String.format("%.1f", value);
	}
}
