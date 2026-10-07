package com.example.client.module;

import com.example.client.config.ModConfig;
import com.example.ExampleMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Movement: raise step height so the player walks up blocks. */
public final class StepModule {
	private static final Identifier STEP_ID = ExampleMod.id("rooty_step");

	public static final float MIN_HEIGHT = 1.0F;
	public static final float MAX_HEIGHT = 2.5F;
	public static final float DEFAULT_HEIGHT = 1.0F;

	private static boolean enabled;
	private static float height = DEFAULT_HEIGHT;

	private StepModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getHeight() {
		return height;
	}

	public static void setHeight(float value) {
		float clamped = Mth.clamp(value, MIN_HEIGHT, MAX_HEIGHT);
		if (height == clamped) {
			return;
		}
		height = clamped;
		ModConfig.save();
	}

	public static void loadHeight(float value) {
		height = Mth.clamp(value, MIN_HEIGHT, MAX_HEIGHT);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			apply(client.player, enabled);
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.movement.step.enabled"
						: "screen.rootymenu.menu.movement.step.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		apply(player, enabled);
	}

	private static void apply(LocalPlayer player, boolean on) {
		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (step == null) {
			return;
		}
		if (on) {
			// Vanilla step is 0.6; additive bump to reach desired height.
			double bump = Math.max(0.0D, height - 0.6D);
			step.addOrUpdateTransientModifier(new AttributeModifier(
					STEP_ID, bump, AttributeModifier.Operation.ADD_VALUE
			));
		} else {
			step.removeModifier(STEP_ID);
		}
	}
}
