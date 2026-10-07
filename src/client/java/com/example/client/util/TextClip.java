package com.example.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Truncate strings to a pixel width with an ellipsis so labels cannot paint
 * outside dropdowns / HUD boxes.
 */
public final class TextClip {
	private static final String ELLIPSIS = "...";

	private TextClip() {
	}

	public static String ellipsize(Font font, String text, int maxWidth) {
		if (text == null || text.isEmpty() || font == null || maxWidth <= 0) {
			return text == null ? "" : text;
		}
		if (font.width(text) <= maxWidth) {
			return text;
		}
		int ellipsisW = font.width(ELLIPSIS);
		if (maxWidth <= ellipsisW) {
			return font.plainSubstrByWidth(text, maxWidth);
		}
		String cut = font.plainSubstrByWidth(text, maxWidth - ellipsisW);
		while (!cut.isEmpty() && font.width(cut) + ellipsisW > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + ELLIPSIS;
	}

	public static Component ellipsize(Font font, Component text, int maxWidth) {
		if (text == null) {
			return Component.empty();
		}
		return Component.literal(ellipsize(font, text.getString(), maxWidth));
	}
}
