package com.example.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Vector-style helpers for Rooty Menu custom buttons.
 * Capsule = circle on the left seamlessly joined to a rounded bar extending right.
 * Right-corner rounding uses outer quarter-arcs only (no internal circle outlines).
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
		plotCirclePoints(graphics, cx, cy, x, y, color, true, true, true, true);
		while (x < y) {
			if (d < 0) {
				d += 2 * x + 3;
			} else {
				d += 2 * (x - y) + 5;
				y--;
			}
			x++;
			plotCirclePoints(graphics, cx, cy, x, y, color, true, true, true, true);
		}
	}

	/** Outline only the top-right quarter of a circle (outer free corner). */
	public static void outlineTopRightArc(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		outlineQuarter(graphics, cx, cy, radius, color, true, false, false, true);
	}

	/** Outline only the bottom-right quarter of a circle (outer free corner). */
	public static void outlineBottomRightArc(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		outlineQuarter(graphics, cx, cy, radius, color, false, true, false, true);
	}

	private static void outlineQuarter(
			GuiGraphicsExtractor graphics,
			int cx,
			int cy,
			int radius,
			int color,
			boolean top,
			boolean bottom,
			boolean left,
			boolean right
	) {
		int x = 0;
		int y = radius;
		int d = 1 - radius;
		plotCirclePoints(graphics, cx, cy, x, y, color, top, bottom, left, right);
		while (x < y) {
			if (d < 0) {
				d += 2 * x + 3;
			} else {
				d += 2 * (x - y) + 5;
				y--;
			}
			x++;
			plotCirclePoints(graphics, cx, cy, x, y, color, top, bottom, left, right);
		}
	}

	private static void plotCirclePoints(
			GuiGraphicsExtractor graphics,
			int cx,
			int cy,
			int x,
			int y,
			int color,
			boolean top,
			boolean bottom,
			boolean left,
			boolean right
	) {
		if (right && bottom) {
			plotPixel(graphics, cx + x, cy + y, color);
			plotPixel(graphics, cx + y, cy + x, color);
		}
		if (left && bottom) {
			plotPixel(graphics, cx - x, cy + y, color);
			plotPixel(graphics, cx - y, cy + x, color);
		}
		if (right && top) {
			plotPixel(graphics, cx + x, cy - y, color);
			plotPixel(graphics, cx + y, cy - x, color);
		}
		if (left && top) {
			plotPixel(graphics, cx - x, cy - y, color);
			plotPixel(graphics, cx - y, cy - x, color);
		}
	}

	private static void plotPixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 1, y + 1, color);
	}

	/**
	 * Draws a capsule: filled left circle joined to a bar with rounded free corners.
	 * Right corners use outer quarter-arcs only — no internal circle outlines.
	 *
	 * @param leftCircleColor fill for the left circle (use same as fillColor for plain capsules;
	 *                        green when a toggle is ON)
	 */
	public static void drawCapsule(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			int fillColor,
			int outlineColor,
			int leftCircleColor
	) {
		int radius = height / 2;
		int cx = x + radius;
		int cy = y + radius;
		int right = x + width;
		int bottom = y + height;
		int cornerR = Math.min(radius, Math.max(3, height / 4));
		int rightCx = right - cornerR - 1;
		int topCy = y + cornerR;
		int botCy = bottom - cornerR - 1;

		// Bar body from the circle center to the right, with rounded free corners (fill only).
		graphics.fill(cx, y, right - cornerR, bottom, fillColor);
		graphics.fill(right - cornerR, y + cornerR, right, bottom - cornerR, fillColor);
		fillCircle(graphics, rightCx, topCy, cornerR, fillColor);
		fillCircle(graphics, rightCx, botCy, cornerR, fillColor);

		// Left circle (indicator / design feature)
		fillCircle(graphics, cx, cy, radius, leftCircleColor);

		// Left circle outline (full — this is the intentional indicator ring)
		outlineCircle(graphics, cx, cy, radius, outlineColor);

		// Bar top/bottom edges from circle center to before the right corner arcs
		graphics.horizontalLine(cx, rightCx, y, outlineColor);
		graphics.horizontalLine(cx, rightCx, bottom - 1, outlineColor);

		// Right vertical edge between corner arcs
		graphics.verticalLine(right - 1, topCy, botCy, outlineColor);

		// Outer quarter-arcs only for rounded free corners (no internal circle lines)
		outlineTopRightArc(graphics, rightCx, topCy, cornerR, outlineColor);
		outlineBottomRightArc(graphics, rightCx, botCy, cornerR, outlineColor);
	}

	public static void drawCapsule(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			int fillColor,
			int outlineColor
	) {
		drawCapsule(graphics, x, y, width, height, fillColor, outlineColor, fillColor);
	}

	/**
	 * Flat rectangle with sharp corners — used by top-bar tabs/close so there are
	 * no circle accents inside the button.
	 */
	public static void drawFlatRect(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			int fillColor,
			int outlineColor
	) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, bottom, fillColor);
		graphics.horizontalLine(x, right - 1, y, outlineColor);
		graphics.horizontalLine(x, right - 1, bottom - 1, outlineColor);
		graphics.verticalLine(x, y, bottom - 1, outlineColor);
		graphics.verticalLine(right - 1, y, bottom - 1, outlineColor);
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
		outlineTopRightArc(graphics, right - r - 1, y + r, r, outlineColor);
		outlineBottomRightArc(graphics, right - r - 1, bottom - r - 1, r, outlineColor);
		outlineQuarter(graphics, x + r, y + r, r, outlineColor, true, false, true, false);
		outlineQuarter(graphics, x + r, bottom - r - 1, r, outlineColor, false, true, true, false);
	}
}
