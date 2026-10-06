package com.example.client;

import com.example.ExampleMod;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ExampleModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ExampleMod.id("menu"));

	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
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
		});
	}

	private static void toggleMenu(Minecraft client) {
		if (client.gui.screen() instanceof ExampleMenuScreen) {
			client.gui.setScreen(null);
		} else if (client.gui.screen() == null) {
			client.gui.setScreen(new ExampleMenuScreen());
		}
	}
}
