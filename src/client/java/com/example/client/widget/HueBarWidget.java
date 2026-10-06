package com.example.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

/**
 * Horizontal hue rainbow bar (0–1). Click/drag to pick hue.
 */
public class HueBarWidget extends AbstractWidget {
	private float hue;
	private final DoubleConsumer onChange;

	public HueBarWidget(int x, int y, int width, int height, float hue, DoubleConsumer onChange) {
		super(x, y, width, height, Component.empty());
		this.hue = Mth.clamp(hue, 0f, 1f);
		this.onChange = onChange;
	}

	public float getHue() {
		return this.hue;
	}

	public void setHue(float hue) {
		this.hue = Mth.clamp(hue, 0f, 1f);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = this.getX();
		int y = this.getY();
		int w = this.width;
		int h = this.height;
		int segments = Math.max(6, w);
		for (int i = 0; i < segments; i++) {
			float hh = (i + 0.5f) / segments;
			int color = hsvToRgb(hh, 1f, 1f) | 0xFF000000;
			int x0 = x + i * w / segments;
			int x1 = x + (i + 1) * w / segments;
			graphics.fill(x0, y, x1, y + h, color);
		}
		graphics.outline(x - 1, y - 1, w + 2, h + 2, 0xFFFFFFFF);

		int markerX = x + Math.round(this.hue * (w - 1));
		graphics.fill(markerX - 1, y - 2, markerX + 2, y + h + 2, 0xFF000000);
		graphics.fill(markerX, y - 1, markerX + 1, y + h + 1, 0xFFFFFFFF);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		updateFromMouse(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		updateFromMouse(event.x());
	}

	private void updateFromMouse(double mouseX) {
		float t = (float) ((mouseX - this.getX()) / (double) Math.max(1, this.width - 1));
		this.hue = Mth.clamp(t, 0f, 1f);
		this.onChange.accept(this.hue);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}

	/** RGB without alpha; hue in [0,1]. */
	public static int hsvToRgb(float h, float s, float v) {
		h = ((h % 1f) + 1f) % 1f;
		float c = v * s;
		float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
		float m = v - c;
		float r;
		float g;
		float b;
		float sector = h * 6f;
		if (sector < 1f) {
			r = c;
			g = x;
			b = 0f;
		} else if (sector < 2f) {
			r = x;
			g = c;
			b = 0f;
		} else if (sector < 3f) {
			r = 0f;
			g = c;
			b = x;
		} else if (sector < 4f) {
			r = 0f;
			g = x;
			b = c;
		} else if (sector < 5f) {
			r = x;
			g = 0f;
			b = c;
		} else {
			r = c;
			g = 0f;
			b = x;
		}
		int ri = Mth.clamp((int) ((r + m) * 255f + 0.5f), 0, 255);
		int gi = Mth.clamp((int) ((g + m) * 255f + 0.5f), 0, 255);
		int bi = Mth.clamp((int) ((b + m) * 255f + 0.5f), 0, 255);
		return (ri << 16) | (gi << 8) | bi;
	}
}
