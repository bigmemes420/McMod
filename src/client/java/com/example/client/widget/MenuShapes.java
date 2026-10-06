package com.example.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Vector-style helpers for Rooty Menu custom buttons.
 * Capsule = circle on the left seamlessly joined to a rounded bar extending right,
 * with the full circle outline visible inside the body.
 */
public final class MenuShapes {
	private MenuShapes() {
	}

	public static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		int r2 = radius * radius;
		for (int dy = -radius; dy <= radius; dy++) {
			int dx = (int) Math.floor(Math.sqrt(r2 - dy * dy));
			graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	public static void outlineCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		int x = 0;
		int y = radius;
		int d = 1 - radius;
		plotCirclePoints(graphics, cx, cy, x, y, color);
		while (x < y) {
			if (d < 0) {
				d += 2 * x + 3;
			} else {
				d += 2 * (x - y) + 5;
				y--;
			}
			x++;
			plotCirclePoints(graphics, cx, cy, x, y, color);
		}
	}

	private static void plotCirclePoints(GuiGraphicsExtractor graphics, int cx, int cy, int x, int y, int color) {
		plotPixel(graphics, cx + x, cy + y, color);
		plotPixel(graphics, cx - x, cy + y, color);
		plotPixel(graphics, cx + x, cy - y, color);
		plotPixel(graphics, cx - x, cy - y, color);
		plotPixel(graphics, cx + y, cy + x, color);
		plotPixel(graphics, cx - y, cy + x, color);
		plotPixel(graphics, cx + y, cy - x, color);
		plotPixel(graphics, cx - y, cy - x, color);
	}

	private static void plotPixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 1, y + 1, color);
	}

	/**
	 * Draws a capsule button: filled circle on the left joined to a bar extending right
	 * with rounded free corners (top-right / bottom-right). The full circle outline
	 * remains visible over the rectangular body.
	 */
	public static void drawCapsule(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			int fillColor,
			int outlineColor
	) {
		int radius = height / 2;
		int cx = x + radius;
		int cy = y + radius;
		int right = x + width;
		int bottom = y + height;
		// Modest rounding on the bar's free corners; keep the left circle as the main round feature.
		int cornerR = Math.min(radius, Math.max(3, height / 4));

		// Bar body from the circle's vertical diameter to the right, with rounded free corners.
		graphics.fill(cx, y, right - cornerR, bottom, fillColor);
		graphics.fill(right - cornerR, y + cornerR, right, bottom - cornerR, fillColor);
		fillCircle(graphics, right - cornerR - 1, y + cornerR, cornerR, fillColor);
		fillCircle(graphics, right - cornerR - 1, bottom - cornerR - 1, cornerR, fillColor);
		fillCircle(graphics, cx, cy, radius, fillColor);

		// Full circle outline (including the arc inside the body), then bar outlines with rounded right.
		outlineCircle(graphics, cx, cy, radius, outlineColor);
		graphics.horizontalLine(cx, right - cornerR - 1, y, outlineColor);
		graphics.horizontalLine(cx, right - cornerR - 1, bottom - 1, outlineColor);
		graphics.verticalLine(right - 1, y + cornerR, bottom - cornerR - 1, outlineColor);
		outlineCircle(graphics, right - cornerR - 1, y + cornerR, cornerR, outlineColor);
		outlineCircle(graphics, right - cornerR - 1, bottom - cornerR - 1, cornerR, outlineColor);
	}

	public static void drawRoundedRect(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			int radius,
			int fillColor,
			int outlineColor
	) {
		int r = Math.min(radius, Math.min(width, height) / 2);
		int right = x + width;
		int bottom = y + height;

		graphics.fill(x + r, y, right - r, bottom, fillColor);
		graphics.fill(x, y + r, x + r, bottom - r, fillColor);
		graphics.fill(right - r, y + r, right, bottom - r, fillColor);
		fillCircle(graphics, x + r, y + r, r, fillColor);
		fillCircle(graphics, right - r - 1, y + r, r, fillColor);
		fillCircle(graphics, x + r, bottom - r - 1, r, fillColor);
		fillCircle(graphics, right - r - 1, bottom - r - 1, r, fillColor);

		graphics.horizontalLine(x + r, right - r - 1, y, outlineColor);
		graphics.horizontalLine(x + r, right - r - 1, bottom - 1, outlineColor);
		graphics.verticalLine(x, y + r, bottom - r - 1, outlineColor);
		graphics.verticalLine(right - 1, y + r, bottom - r - 1, outlineColor);
		outlineCircle(graphics, x + r, y + r, r, outlineColor);
		outlineCircle(graphics, right - r - 1, y + r, r, outlineColor);
		outlineCircle(graphics, x + r, bottom - r - 1, r, outlineColor);
		outlineCircle(graphics, right - r - 1, bottom - r - 1, r, outlineColor);
	}
}
