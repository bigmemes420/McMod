package com.example.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 2D saturation (X) / value (Y, top=1) picker for a fixed hue.
 */
public class SaturationValueWidget extends AbstractWidget {
	@FunctionalInterface
	public interface ChangeListener {
		void onChange(float saturation, float value);
	}

	private float hue;
	private float saturation;
	private float value;
	private final ChangeListener onChange;

	public SaturationValueWidget(
			int x,
			int y,
			int size,
			float hue,
			float saturation,
			float value,
			ChangeListener onChange
	) {
		super(x, y, size, size, Component.empty());
		this.hue = hue;
		this.saturation = Mth.clamp(saturation, 0f, 1f);
		this.value = Mth.clamp(value, 0f, 1f);
		this.onChange = onChange;
	}

	public void setHue(float hue) {
		this.hue = hue;
	}

	public float getSaturation() {
		return this.saturation;
	}

	public float getValue() {
		return this.value;
	}

	public void setSaturationValue(float saturation, float value) {
		this.saturation = Mth.clamp(saturation, 0f, 1f);
		this.value = Mth.clamp(value, 0f, 1f);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = this.getX();
		int y = this.getY();
		int size = this.width;

		for (int i = 0; i < size; i++) {
			float s = size <= 1 ? 0f : (float) i / (size - 1);
			int top = HueBarWidget.hsvToRgb(this.hue, s, 1f) | 0xFF000000;
			int bottom = HueBarWidget.hsvToRgb(this.hue, s, 0f) | 0xFF000000;
			graphics.fillGradient(x + i, y, x + i + 1, y + size, top, bottom);
		}
		graphics.outline(x - 1, y - 1, size + 2, size + 2, 0xFFFFFFFF);

		int cx = x + Math.round(this.saturation * (size - 1));
		int cy = y + Math.round((1f - this.value) * (size - 1));
		graphics.fill(cx - 3, cy - 1, cx + 4, cy + 2, 0xFF000000);
		graphics.fill(cx - 1, cy - 3, cx + 2, cy + 4, 0xFF000000);
		graphics.fill(cx - 2, cy, cx + 3, cy + 1, 0xFFFFFFFF);
		graphics.fill(cx, cy - 2, cx + 1, cy + 3, 0xFFFFFFFF);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		updateFromMouse(event.x(), event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		updateFromMouse(event.x(), event.y());
	}

	private void updateFromMouse(double mouseX, double mouseY) {
		float s = (float) ((mouseX - this.getX()) / (double) Math.max(1, this.width - 1));
		float v = 1f - (float) ((mouseY - this.getY()) / (double) Math.max(1, this.height - 1));
		this.saturation = Mth.clamp(s, 0f, 1f);
		this.value = Mth.clamp(v, 0f, 1f);
		this.onChange.onChange(this.saturation, this.value);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
