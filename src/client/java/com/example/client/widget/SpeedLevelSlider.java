package com.example.client.widget;

import com.example.client.module.SpeedModule;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Speed-level slider (min–max from {@link SpeedModule}) backed by {@link AbstractSliderButton}.
 */
public class SpeedLevelSlider extends AbstractSliderButton {
	@FunctionalInterface
	public interface OnLevelChange {
		void onLevel(float level);
	}

	private final OnLevelChange onChange;
	private float level;

	public SpeedLevelSlider(int x, int y, int width, int height, float level, OnLevelChange onChange) {
		super(x, y, width, height, Component.empty(), toSliderValue(level));
		this.level = Mth.clamp(level, SpeedModule.MIN_LEVEL, SpeedModule.MAX_LEVEL);
		this.onChange = onChange;
		updateMessage();
	}

	private static double toSliderValue(float level) {
		float min = SpeedModule.MIN_LEVEL;
		float max = SpeedModule.MAX_LEVEL;
		return Mth.clamp((level - min) / (max - min), 0.0, 1.0);
	}

	public float getLevel() {
		return this.level;
	}

	public void setLevel(float level) {
		this.level = Mth.clamp(level, SpeedModule.MIN_LEVEL, SpeedModule.MAX_LEVEL);
		this.value = toSliderValue(this.level);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		this.setMessage(Component.translatable("screen.modid.menu.movement.speed.level", String.format("%.1f", this.level)));
	}

	@Override
	protected void applyValue() {
		float min = SpeedModule.MIN_LEVEL;
		float max = SpeedModule.MAX_LEVEL;
		// Snap to one decimal place
		this.level = Mth.clamp(Math.round((min + (float) this.value * (max - min)) * 10.0F) / 10.0F, min, max);
		this.onChange.onLevel(this.level);
		updateMessage();
	}
}
