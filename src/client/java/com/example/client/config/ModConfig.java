package com.example.client.config;

import com.example.ExampleMod;
import com.example.client.ExampleMenuScreen;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.TriggerBotModule;
import com.example.client.module.HitboxesModule;
import com.example.client.module.CriticalsModule;
import com.example.client.module.AutoTotemModule;
import com.example.client.module.AimAssistModule;
import com.example.client.module.AutoSprintModule;
import com.example.client.module.FastPlaceModule;
import com.example.client.module.FinderModule;
import com.example.client.module.FlightModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.JesusModule;
import com.example.client.module.ModuleKeybinds;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.PlayerOutlinesModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SafeWalkModule;
import com.example.client.module.ScaffoldModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.SpiderModule;
import com.example.client.module.StepModule;
import com.example.client.module.TowerModule;
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
 * Persists module toggles, sliders, keybinds, last menu tab, and notifications to
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
		ModuleKeybinds.registerAll();
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
		FlightModule.loadMode(props.getProperty("flightMode", "VANILLA"));
		FlightModule.loadSpeed(floatVal(props, "flightSpeed", FlightModule.DEFAULT_SPEED));
		SpeedModule.loadEnabled(bool(props, "speed", false));
		SpeedModule.loadSpeedLevel(floatVal(props, "speedLevel", SpeedModule.DEFAULT_LEVEL));
		SpeedModule.loadMode(props.getProperty("speedMode", "NORMAL"));
		NoFallModule.loadEnabled(bool(props, "nofall", false));
		AutoSprintModule.loadEnabled(bool(props, "autosprint", false));
		StepModule.loadEnabled(bool(props, "step", false));
		StepModule.loadHeight(floatVal(props, "stepHeight", StepModule.DEFAULT_HEIGHT));
		SpiderModule.loadEnabled(bool(props, "spider", false));
		SpiderModule.loadClimbSpeed(floatVal(props, "spiderSpeed", SpiderModule.DEFAULT_SPEED));
		SafeWalkModule.loadEnabled(bool(props, "safewalk", false));
		JesusModule.loadEnabled(bool(props, "jesus", false));
		JesusModule.loadMode(props.getProperty("jesusMode", "WATER"));
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
		FinderModule.loadOutlineThickness(floatVal(props, "finderOutlineThickness", FinderModule.DEFAULT_OUTLINE_THICKNESS));
		FinderModule.loadDistance(floatVal(props, "finderDistance", FinderModule.DEFAULT_DISTANCE));
		FinderModule.loadSelectedBlocks(props.getProperty("finderBlocks", ""));
		FinderModule.loadBlockColors(props.getProperty("finderBlockColors", ""));
		AutoClickerModule.loadEnabled(bool(props, "autoclicker", false));
		AutoClickerModule.loadCps(floatVal(props, "autoclickerCps", AutoClickerModule.DEFAULT_CPS));
		AutoClickerModule.loadRandomizeMs(floatVal(props, "autoclickerRandomize", AutoClickerModule.DEFAULT_RANDOMIZE));
		VelocityModule.loadEnabled(bool(props, "velocity", false));
		VelocityModule.loadPercent(floatVal(props, "velocityPercent", VelocityModule.DEFAULT_PERCENT));
		ReachModule.loadEnabled(bool(props, "reach", false));
		ReachModule.loadBonus(floatVal(props, "reachBonus", ReachModule.DEFAULT_BONUS));

		CriticalsModule.loadEnabled(bool(props, "criticals", false));
		TriggerBotModule.loadEnabled(bool(props, "triggerbot", false));
		TriggerBotModule.loadDelay(floatVal(props, "triggerbotDelay", TriggerBotModule.DEFAULT_DELAY));
		AimAssistModule.loadEnabled(bool(props, "aimassist", false));
		AimAssistModule.loadStrength(floatVal(props, "aimassistStrength", AimAssistModule.DEFAULT_STRENGTH));
		AimAssistModule.loadRange(floatVal(props, "aimassistRange", AimAssistModule.DEFAULT_RANGE));
		HitboxesModule.loadEnabled(bool(props, "hitboxes", false));
		HitboxesModule.loadSize(floatVal(props, "hitboxesSize", HitboxesModule.DEFAULT_SIZE));
		AutoTotemModule.loadEnabled(bool(props, "autototem", false));
		ScaffoldModule.loadEnabled(bool(props, "scaffold", false));
		FastPlaceModule.loadEnabled(bool(props, "fastplace", false));
		TowerModule.loadEnabled(bool(props, "tower", false));
		NotificationsModule.loadEnabled(bool(props, "notifications", true));
		ModuleKeybinds.loadFrom(props);
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
			props.setProperty("flightMode", FlightModule.getMode().name());
			props.setProperty("flightSpeed", Float.toString(FlightModule.getSpeed()));
			props.setProperty("speed", String.valueOf(SpeedModule.isEnabled()));
			props.setProperty("speedLevel", Float.toString(SpeedModule.getSpeedLevel()));
			props.setProperty("speedMode", SpeedModule.getMode().name());
			props.setProperty("nofall", String.valueOf(NoFallModule.isEnabled()));
			props.setProperty("autosprint", String.valueOf(AutoSprintModule.isEnabled()));
			props.setProperty("step", String.valueOf(StepModule.isEnabled()));
			props.setProperty("stepHeight", Float.toString(StepModule.getHeight()));
			props.setProperty("spider", String.valueOf(SpiderModule.isEnabled()));
			props.setProperty("spiderSpeed", Float.toString(SpiderModule.getClimbSpeed()));
			props.setProperty("safewalk", String.valueOf(SafeWalkModule.isEnabled()));
			props.setProperty("jesus", String.valueOf(JesusModule.isEnabled()));
			props.setProperty("jesusMode", JesusModule.getMode().name());
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
			props.setProperty("finderOutlineThickness", Float.toString(FinderModule.getOutlineThickness()));
			props.setProperty("finderDistance", Float.toString(FinderModule.getDistance()));
			props.setProperty("finderBlocks", FinderModule.selectedBlocksCsv());
			props.setProperty("finderBlockColors", FinderModule.blockColorsCsv());
			props.setProperty("autoclicker", String.valueOf(AutoClickerModule.isEnabled()));
			props.setProperty("autoclickerCps", Float.toString(AutoClickerModule.getCps()));
			props.setProperty("autoclickerRandomize", Float.toString(AutoClickerModule.getRandomizeMs()));
			props.setProperty("velocity", String.valueOf(VelocityModule.isEnabled()));
			props.setProperty("velocityPercent", Float.toString(VelocityModule.getPercent()));
			props.setProperty("reach", String.valueOf(ReachModule.isEnabled()));
			props.setProperty("reachBonus", Float.toString(ReachModule.getBonus()));
			props.setProperty("criticals", String.valueOf(CriticalsModule.isEnabled()));
			props.setProperty("triggerbot", String.valueOf(TriggerBotModule.isEnabled()));
			props.setProperty("triggerbotDelay", Float.toString(TriggerBotModule.getDelay()));
			props.setProperty("aimassist", String.valueOf(AimAssistModule.isEnabled()));
			props.setProperty("aimassistStrength", Float.toString(AimAssistModule.getStrength()));
			props.setProperty("aimassistRange", Float.toString(AimAssistModule.getRange()));
			props.setProperty("hitboxes", String.valueOf(HitboxesModule.isEnabled()));
			props.setProperty("hitboxesSize", Float.toString(HitboxesModule.getSize()));
			props.setProperty("autototem", String.valueOf(AutoTotemModule.isEnabled()));
			props.setProperty("scaffold", String.valueOf(ScaffoldModule.isEnabled()));
			props.setProperty("fastplace", String.valueOf(FastPlaceModule.isEnabled()));
			props.setProperty("tower", String.valueOf(TowerModule.isEnabled()));
			props.setProperty("notifications", String.valueOf(NotificationsModule.isEnabled()));
			props.setProperty("lastTab", ExampleMenuScreen.getLastTabName());
			ModuleKeybinds.writeTo(props);

			Path path = configPath();
			try {
				Files.createDirectories(path.getParent());
				try (BufferedWriter writer = Files.newBufferedWriter(path)) {
					props.store(writer, "Rooty Menu module state (toggles, sliders, keybinds, last tab)");
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
