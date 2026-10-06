package com.example.client.config;

import com.example.ExampleMod;
import com.example.client.ExampleMenuScreen;
import com.example.client.module.FlightModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.PlayerOutlinesModule;
import com.example.client.module.SpeedModule;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists module toggles, sliders, last menu tab, and notifications to
 * {@code <gameDir>/config/modid-modules.properties}. Theme colors live in
 * {@link MenuTheme}'s own file.
 */
public final class ModConfig {
	private static boolean loaded;
	private static boolean saving;

	private ModConfig() {
	}

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(ExampleMod.MOD_ID + "-modules.properties");
	}

	/** Load theme + modules on client start. Safe to call once. */
	public static void loadAll() {
		MenuTheme.get();
		loadModules();
		loaded = true;
	}

	public static void loadModules() {
		Path path = configPath();
		if (!Files.isRegularFile(path)) {
			return;
		}
		Properties props = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			props.load(reader);
		} catch (IOException e) {
			ExampleMod.LOGGER.warn("Failed to load module config: {}", e.toString());
			return;
		}

		FlightModule.loadEnabled(bool(props, "flight", false));
		SpeedModule.loadEnabled(bool(props, "speed", false));
		SpeedModule.loadSpeedLevel(floatVal(props, "speedLevel", SpeedModule.DEFAULT_LEVEL));
		NoFallModule.loadEnabled(bool(props, "nofall", false));
		NametagsModule.loadEnabled(bool(props, "nametags", false));
		NametagsModule.loadScale(floatVal(props, "nametagsScale", NametagsModule.DEFAULT_SCALE));
		PlayerOutlinesModule.loadEnabled(bool(props, "playerOutlines", false));
		FullbrightModule.loadEnabled(bool(props, "fullbright", false));
		NotificationsModule.loadEnabled(bool(props, "notifications", true));
		ExampleMenuScreen.loadLastTab(props.getProperty("lastTab", "GENERAL"));
	}

	/** Snapshot current module / UI state to disk. No-op during load. */
	public static void save() {
		if (!loaded || saving) {
			return;
		}
		saving = true;
		try {
			Properties props = new Properties();
			props.setProperty("flight", String.valueOf(FlightModule.isEnabled()));
			props.setProperty("speed", String.valueOf(SpeedModule.isEnabled()));
			props.setProperty("speedLevel", Float.toString(SpeedModule.getSpeedLevel()));
			props.setProperty("nofall", String.valueOf(NoFallModule.isEnabled()));
			props.setProperty("nametags", String.valueOf(NametagsModule.isEnabled()));
			props.setProperty("nametagsScale", Float.toString(NametagsModule.getScale()));
			props.setProperty("playerOutlines", String.valueOf(PlayerOutlinesModule.isEnabled()));
			props.setProperty("fullbright", String.valueOf(FullbrightModule.isEnabled()));
			props.setProperty("notifications", String.valueOf(NotificationsModule.isEnabled()));
			props.setProperty("lastTab", ExampleMenuScreen.getLastTabName());

			Path path = configPath();
			try {
				Files.createDirectories(path.getParent());
				try (BufferedWriter writer = Files.newBufferedWriter(path)) {
					props.store(writer, "Rooty Menu module state (toggles, sliders, last tab)");
				}
			} catch (IOException e) {
				ExampleMod.LOGGER.warn("Failed to save module config: {}", e.toString());
			}
		} finally {
			saving = false;
		}
	}

	private static boolean bool(Properties props, String key, boolean def) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return def;
		}
		return Boolean.parseBoolean(raw.trim());
	}

	private static float floatVal(Properties props, String key, float def) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return def;
		}
		try {
			return Float.parseFloat(raw.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
