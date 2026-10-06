package com.example.client.config;

import com.example.ExampleMod;
import com.example.client.ExampleMenuScreen;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.FinderModule;
import com.example.client.module.FlightModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.PlayerOutlinesModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.VelocityModule;
import com.example.client.module.XRayModule;

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
		SpeedModule.loadMode(props.getProperty("speedMode", "NORMAL"));
		NoFallModule.loadEnabled(bool(props, "nofall", false));
		NametagsModule.loadEnabled(bool(props, "nametags", false));
		NametagsModule.loadScale(floatVal(props, "nametagsScale", NametagsModule.DEFAULT_SCALE));
		PlayerOutlinesModule.loadEnabled(bool(props, "playerOutlines", false));
		FullbrightModule.loadEnabled(bool(props, "fullbright", false));
		XRayModule.loadEnabled(bool(props, "xray", false));
		XRayModule.loadOpacity(floatVal(props, "xrayOpacity", XRayModule.DEFAULT_OPACITY));
		XRayModule.loadFullOpacityBlocks(props.getProperty("xrayBlocks", ""));
		FinderModule.loadEnabled(bool(props, "finder", false));
		FinderModule.loadMode(props.getProperty("finderMode", "OUTLINE"));
		FinderModule.loadOpacity(floatVal(props, "finderOpacity", FinderModule.DEFAULT_OPACITY));
		FinderModule.loadSelectedBlocks(props.getProperty("finderBlocks", ""));
		FinderModule.loadBlockColors(props.getProperty("finderBlockColors", ""));
		AutoClickerModule.loadEnabled(bool(props, "autoclicker", false));
		AutoClickerModule.loadCps(floatVal(props, "autoclickerCps", AutoClickerModule.DEFAULT_CPS));
		VelocityModule.loadEnabled(bool(props, "velocity", false));
		VelocityModule.loadPercent(floatVal(props, "velocityPercent", VelocityModule.DEFAULT_PERCENT));
		ReachModule.loadEnabled(bool(props, "reach", false));
		ReachModule.loadBonus(floatVal(props, "reachBonus", ReachModule.DEFAULT_BONUS));
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
			props.setProperty("speedMode", SpeedModule.getMode().name());
			props.setProperty("nofall", String.valueOf(NoFallModule.isEnabled()));
			props.setProperty("nametags", String.valueOf(NametagsModule.isEnabled()));
			props.setProperty("nametagsScale", Float.toString(NametagsModule.getScale()));
			props.setProperty("playerOutlines", String.valueOf(PlayerOutlinesModule.isEnabled()));
			props.setProperty("fullbright", String.valueOf(FullbrightModule.isEnabled()));
			props.setProperty("xray", String.valueOf(XRayModule.isEnabled()));
			props.setProperty("xrayOpacity", Float.toString(XRayModule.getOpacity()));
			props.setProperty("xrayBlocks", XRayModule.fullOpacityBlocksCsv());
			props.setProperty("finder", String.valueOf(FinderModule.isEnabled()));
			props.setProperty("finderMode", FinderModule.getMode().name());
			props.setProperty("finderOpacity", Float.toString(FinderModule.getOpacity()));
			props.setProperty("finderBlocks", FinderModule.selectedBlocksCsv());
			props.setProperty("finderBlockColors", FinderModule.blockColorsCsv());
			props.setProperty("autoclicker", String.valueOf(AutoClickerModule.isEnabled()));
			props.setProperty("autoclickerCps", Float.toString(AutoClickerModule.getCps()));
			props.setProperty("velocity", String.valueOf(VelocityModule.isEnabled()));
			props.setProperty("velocityPercent", Float.toString(VelocityModule.getPercent()));
			props.setProperty("reach", String.valueOf(ReachModule.isEnabled()));
			props.setProperty("reachBonus", Float.toString(ReachModule.getBonus()));
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
