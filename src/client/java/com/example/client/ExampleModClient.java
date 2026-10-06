package com.example.client;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.FlightModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.VelocityModule;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public class ExampleModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ExampleMod.id("menu"));

	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		ModConfig.loadAll();
		NoFallModule.registerHooks();

		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.modid.open_menu",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_INSERT,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				toggleMenu(client);
			}
			FlightModule.tick(client);
			SpeedModule.tick(client);
			NoFallModule.tick(client);
			AutoClickerModule.tick(client);
			VelocityModule.tick(client);
			ReachModule.tick(client);
		});
	}

	/** True when {@code event} matches the configured open/close menu keybind. */
	public static boolean matchesOpenMenuKey(KeyEvent event) {
		return openMenuKey != null && openMenuKey.matches(event);
	}

	private static void toggleMenu(Minecraft client) {
		if (client.gui.screen() instanceof ExampleMenuScreen) {
			client.gui.setScreen(null);
		} else if (client.gui.screen() == null) {
			client.gui.setScreen(new ExampleMenuScreen());
		}
	}
}
