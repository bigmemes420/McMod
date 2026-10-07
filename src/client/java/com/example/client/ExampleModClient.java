package com.example.client;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.TriggerBotModule;
import com.example.client.module.HitboxesModule;
import com.example.client.module.CriticalsModule;
import com.example.client.module.CustomCrosshairModule;
import com.example.client.module.AutoTotemModule;
import com.example.client.module.AirPlaceModule;
import com.example.client.module.AimAssistModule;
import com.example.client.module.ZoomModule;
import com.example.client.module.TracersModule;
import com.example.client.module.ParkourModule;
import com.example.client.module.NoHurtCamModule;
import com.example.client.module.BreadcrumbsModule;
import com.example.client.module.AntiAFKModule;
import com.example.client.module.AutoWalkModule;
import com.example.client.module.AutoToolModule;
import com.example.client.module.AutoRespawnModule;
import com.example.client.module.AutoReconnectModule;
import com.example.client.module.AutoEatModule;
import com.example.client.module.AutoMineModule;
import com.example.client.module.AutoFishModule;
import com.example.client.module.AutoArmorModule;
import com.example.client.module.AutoSprintModule;
import com.example.client.module.FastPlaceModule;
import com.example.client.module.FinderModule;
import com.example.client.module.ElytraControlModule;
import com.example.client.module.FlightModule;
import com.example.client.module.InventoryMoveModule;
import com.example.client.module.JesusModule;
import com.example.client.module.MobEspModule;
import com.example.client.module.ModuleKeybinds;
import com.example.client.module.NoFallModule;
import com.example.client.module.PlayerEspModule;
import com.example.client.module.RadarModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SafeWalkModule;
import com.example.client.module.ScaffoldModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.SpiderModule;
import com.example.client.module.StepModule;
import com.example.client.module.TowerModule;
import com.example.client.module.VelocityModule;
import com.example.client.module.ViewerRetentionModule;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public class ExampleModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ExampleMod.id("menu"));

	private static KeyMapping openMenuKey;
	private static KeyMapping hudEditKey;

	@Override
	public void onInitializeClient() {
		ModConfig.loadAll();
		NoFallModule.registerHooks();
		RadarModule.registerHud();
		CustomCrosshairModule.registerHud();
		ViewerRetentionModule.registerHud();

		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.rootymenu.open_menu",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_INSERT,
				CATEGORY
		));
		// Toggle HUD layout edit mode (move/resize Radar). Ignored while other screens are open.
		hudEditKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.rootymenu.hud_edit",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_DELETE,
				CATEGORY
		));

		LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
			FinderModule.renderOverlays(context.levelRenderer());
			PlayerEspModule.render(context.levelRenderer());
			MobEspModule.render(context.levelRenderer());
			TracersModule.render(context.levelRenderer());
			BreadcrumbsModule.render(context.levelRenderer());
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				toggleMenu(client);
			}
			while (hudEditKey.consumeClick()) {
				toggleHudEdit(client);
			}
			ModuleKeybinds.tick(client);
			FlightModule.tick(client);
			ElytraControlModule.tick(client);
			SpeedModule.tick(client);
			NoFallModule.tick(client);
			AutoSprintModule.tick(client);
			StepModule.tick(client);
			SpiderModule.tick(client);
			SafeWalkModule.tick(client);
			JesusModule.tick(client);
			AutoClickerModule.tick(client);
			VelocityModule.tick(client);
			ReachModule.tick(client);
			CriticalsModule.tick(client);
			TriggerBotModule.tick(client);
			AimAssistModule.tick(client);
			AutoTotemModule.tick(client);
			ScaffoldModule.tick(client);
			FastPlaceModule.tick(client);
			TowerModule.tick(client);
			AirPlaceModule.tick(client);
			InventoryMoveModule.tick(client);
			FinderModule.tick(client);
			AutoRespawnModule.tick(client);
			AutoWalkModule.tick(client);
			ParkourModule.tick(client);
			AntiAFKModule.tick(client);
			AutoToolModule.tick(client);
			AutoArmorModule.tick(client);
			AutoEatModule.tick(client);
			BreadcrumbsModule.tick(client);
			AutoReconnectModule.tick(client);
			AutoFishModule.tick(client);
			AutoMineModule.tick(client);
		});
	}

	/** True when {@code event} matches the configured open/close menu keybind. */
	public static boolean matchesOpenMenuKey(KeyEvent event) {
		return openMenuKey != null && openMenuKey.matches(event);
	}

	public static boolean matchesHudEditKey(KeyEvent event) {
		return hudEditKey != null && hudEditKey.matches(event);
	}

	private static void toggleMenu(Minecraft client) {
		if (client.gui.screen() instanceof ExampleMenuScreen) {
			client.gui.setScreen(null);
		} else if (client.gui.screen() == null) {
			client.gui.setScreen(new ExampleMenuScreen());
		}
	}

	/**
	 * Toggle on key press. Only enters edit mode in-game (no screen), so Delete
	 * still erases text in chat / signs / Rooty Menu search boxes.
	 */
	private static void toggleHudEdit(Minecraft client) {
		if (client.gui.screen() instanceof HudEditScreen) {
			client.gui.setScreen(null);
		} else if (client.gui.screen() == null && client.player != null) {
			client.gui.setScreen(new HudEditScreen());
		}
	}
}
