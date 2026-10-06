package com.example.client.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/**
 * 0–255 channel slider (R/G/B) backed by {@link AbstractSliderButton}.
 */
public class ColorChannelSlider extends AbstractSliderButton {
	private final String label;
	private final IntConsumer onChange;
	private int channel;

	public ColorChannelSlider(int x, int y, int width, int height, String label, int channel, IntConsumer onChange) {
		super(x, y, width, height, Component.empty(), channel / 255.0);
		this.label = label;
		this.channel = Mth.clamp(channel, 0, 255);
		this.onChange = onChange;
		updateMessage();
	}

	public void setChannel(int channel) {
		this.channel = Mth.clamp(channel, 0, 255);
		this.value = this.channel / 255.0;
		updateMessage();
	}

	public int getChannel() {
		return this.channel;
	}

	@Override
	protected void updateMessage() {
		this.setMessage(Component.literal(this.label + ": " + this.channel));
	}

	@Override
	protected void applyValue() {
		this.channel = Mth.clamp((int) Math.round(this.value * 255.0), 0, 255);
		this.onChange.accept(this.channel);
	}
}
