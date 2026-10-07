package com.example.client.config;

import com.example.ExampleMod;
import com.example.client.ExampleMenuScreen;
import com.example.client.module.AutoClickerModule;
import com.example.client.module.TriggerBotModule;
import com.example.client.module.HitboxesModule;
import com.example.client.module.CriticalsModule;
import com.example.client.module.AutoTotemModule;
import com.example.client.module.AirPlaceModule;
import com.example.client.module.AimAssistModule;
import com.example.client.module.AutoSprintModule;
import com.example.client.module.FastPlaceModule;
import com.example.client.module.FinderModule;
import com.example.client.module.ElytraControlModule;
import com.example.client.module.FlightModule;
import com.example.client.module.FullbrightModule;
import com.example.client.module.JesusModule;
import com.example.client.module.ModuleKeybinds;
import com.example.client.module.NametagsModule;
import com.example.client.module.NoFallModule;
import com.example.client.module.NotificationsModule;
import com.example.client.module.InventoryMoveModule;
import com.example.client.module.MobEspModule;
import com.example.client.module.NoSlowModule;
import com.example.client.module.PlayerEspModule;
import com.example.client.module.RadarModule;
import com.example.client.module.ReachModule;
import com.example.client.module.SafeWalkModule;
import com.example.client.module.SneakModule;
import com.example.client.module.ScaffoldModule;
import com.example.client.module.SpeedModule;
import com.example.client.module.SpiderModule;
import com.example.client.module.StepModule;
import com.example.client.module.TowerModule;
import com.example.client.module.VelocityModule;

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
		ElytraControlModule.loadEnabled(bool(props, "elytraControl", false));
		ElytraControlModule.loadSpeed(floatVal(props, "elytraControlSpeed", ElytraControlModule.DEFAULT_SPEED));
		SpeedModule.loadEnabled(bool(props, "speed", false));
		SpeedModule.loadSpeedLevel(floatVal(props, "speedLevel", SpeedModule.DEFAULT_LEVEL));
		SpeedModule.loadMode(props.getProperty("speedMode", "NORMAL"));
		NoFallModule.loadEnabled(bool(props, "nofall", false));
		AutoSprintModule.loadEnabled(bool(props, "autosprint", false));
		AutoSprintModule.loadMode(props.getProperty("autosprintMode", "LEGIT"));
		StepModule.loadEnabled(bool(props, "step", false));
		StepModule.loadHeight(floatVal(props, "stepHeight", StepModule.DEFAULT_HEIGHT));
		SpiderModule.loadEnabled(bool(props, "spider", false));
		SpiderModule.loadClimbSpeed(floatVal(props, "spiderSpeed", SpiderModule.DEFAULT_SPEED));
		SafeWalkModule.loadEnabled(bool(props, "safewalk", false));
		JesusModule.loadEnabled(bool(props, "jesus", false));
		JesusModule.loadMode(props.getProperty("jesusMode", "WATER"));
		NametagsModule.loadEnabled(bool(props, "nametags", false));
		NametagsModule.loadScale(floatVal(props, "nametagsScale", NametagsModule.DEFAULT_SCALE));
		PlayerEspModule.loadEnabled(bool(props, "playerEsp", bool(props, "playerOutlines", false)));
		PlayerEspModule.loadMode(props.getProperty("playerEspMode", "BOX_3D"));
		PlayerEspModule.loadColor(intVal(props, "playerEspColor", PlayerEspModule.DEFAULT_COLOR));
		PlayerEspModule.loadOutlineBoxes(bool(props, "playerEspOutlineBoxes", false));
		MobEspModule.loadEnabled(bool(props, "mobEsp", false));
		MobEspModule.loadMode(props.getProperty("mobEspMode", "OUTLINE"));
		MobEspModule.loadColor(intVal(props, "mobEspColor", MobEspModule.DEFAULT_COLOR));
		MobEspModule.loadSelectedMobs(props.getProperty("mobEspMobs", ""));
		MobEspModule.loadMobColors(props.getProperty("mobEspColors", ""));
		FullbrightModule.loadEnabled(bool(props, "fullbright", false));
		RadarModule.loadEnabled(bool(props, "radar", false));
		RadarModule.loadMode(props.getProperty("radarMode", "BOTH"));
		RadarModule.loadShape(props.getProperty("radarShape", "SQUARE"));
		RadarModule.loadShowHeight(bool(props, "radarShowHeight", true));
		RadarModule.loadRange(floatVal(props, "radarRange", RadarModule.DEFAULT_RANGE));
		RadarModule.loadHudLayout(
				intVal(props, "radarHudX", RadarModule.DEFAULT_HUD_X),
				intVal(props, "radarHudY", RadarModule.DEFAULT_HUD_Y),
				intVal(props, "radarHudSize", RadarModule.DEFAULT_HUD_SIZE)
		);
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
		ScaffoldModule.loadMode(props.getProperty("scaffoldMode", "FROM_INVENTORY"));
		FastPlaceModule.loadEnabled(bool(props, "fastplace", false));
		FastPlaceModule.loadSpeed(floatVal(props, "fastplaceSpeed", FastPlaceModule.DEFAULT_SPEED));
		TowerModule.loadEnabled(bool(props, "tower", false));
		AirPlaceModule.loadEnabled(bool(props, "airplace", false));
		AirPlaceModule.loadDistance(floatVal(props, "airplaceDistance", AirPlaceModule.DEFAULT_DISTANCE));
		InventoryMoveModule.loadEnabled(bool(props, "inventoryMove", false));
		InventoryMoveModule.loadRotateSpeed(floatVal(props, "inventoryMoveRotateSpeed", InventoryMoveModule.DEFAULT_ROTATE_SPEED));
		NoSlowModule.loadEnabled(bool(props, "noslow", false));
		SneakModule.loadEnabled(bool(props, "sneak", false));
		SneakModule.loadMode(props.getProperty("sneakMode", "LEGIT"));
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
			props.setProperty("elytraControl", String.valueOf(ElytraControlModule.isEnabled()));
			props.setProperty("elytraControlSpeed", Float.toString(ElytraControlModule.getSpeed()));
			props.setProperty("speed", String.valueOf(SpeedModule.isEnabled()));
			props.setProperty("speedLevel", Float.toString(SpeedModule.getSpeedLevel()));
			props.setProperty("speedMode", SpeedModule.getMode().name());
			props.setProperty("nofall", String.valueOf(NoFallModule.isEnabled()));
			props.setProperty("autosprint", String.valueOf(AutoSprintModule.isEnabled()));
			props.setProperty("autosprintMode", AutoSprintModule.getMode().name());
			props.setProperty("step", String.valueOf(StepModule.isEnabled()));
			props.setProperty("stepHeight", Float.toString(StepModule.getHeight()));
			props.setProperty("spider", String.valueOf(SpiderModule.isEnabled()));
			props.setProperty("spiderSpeed", Float.toString(SpiderModule.getClimbSpeed()));
			props.setProperty("safewalk", String.valueOf(SafeWalkModule.isEnabled()));
			props.setProperty("jesus", String.valueOf(JesusModule.isEnabled()));
			props.setProperty("jesusMode", JesusModule.getMode().name());
			props.setProperty("nametags", String.valueOf(NametagsModule.isEnabled()));
			props.setProperty("nametagsScale", Float.toString(NametagsModule.getScale()));
			props.setProperty("playerEsp", String.valueOf(PlayerEspModule.isEnabled()));
			props.setProperty("playerEspMode", PlayerEspModule.getMode().name());
			props.setProperty("playerEspColor", Integer.toString(PlayerEspModule.getColor()));
			props.setProperty("playerEspOutlineBoxes", String.valueOf(PlayerEspModule.isOutlineBoxes()));
			props.setProperty("mobEsp", String.valueOf(MobEspModule.isEnabled()));
			props.setProperty("mobEspMode", MobEspModule.getMode().name());
			props.setProperty("mobEspColor", Integer.toString(MobEspModule.getColor()));
			props.setProperty("mobEspMobs", MobEspModule.selectedMobsCsv());
			props.setProperty("mobEspColors", MobEspModule.mobColorsCsv());
			props.setProperty("fullbright", String.valueOf(FullbrightModule.isEnabled()));
			props.setProperty("radar", String.valueOf(RadarModule.isEnabled()));
			props.setProperty("radarMode", RadarModule.getMode().name());
			props.setProperty("radarShape", RadarModule.getShape().name());
			props.setProperty("radarShowHeight", String.valueOf(RadarModule.isShowHeight()));
			props.setProperty("radarRange", Float.toString(RadarModule.getRange()));
			props.setProperty("radarHudX", Integer.toString(RadarModule.getHudX()));
			props.setProperty("radarHudY", Integer.toString(RadarModule.getHudY()));
			props.setProperty("radarHudSize", Integer.toString(RadarModule.getHudSize()));
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
			props.setProperty("scaffoldMode", ScaffoldModule.getMode().name());
			props.setProperty("fastplace", String.valueOf(FastPlaceModule.isEnabled()));
			props.setProperty("fastplaceSpeed", Float.toString(FastPlaceModule.getSpeed()));
			props.setProperty("tower", String.valueOf(TowerModule.isEnabled()));
			props.setProperty("airplace", String.valueOf(AirPlaceModule.isEnabled()));
			props.setProperty("airplaceDistance", Float.toString(AirPlaceModule.getDistance()));
			props.setProperty("inventoryMove", String.valueOf(InventoryMoveModule.isEnabled()));
			props.setProperty("inventoryMoveRotateSpeed", Float.toString(InventoryMoveModule.getRotateSpeed()));
			props.setProperty("noslow", String.valueOf(NoSlowModule.isEnabled()));
			props.setProperty("sneak", String.valueOf(SneakModule.isEnabled()));
			props.setProperty("sneakMode", SneakModule.getMode().name());
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


	/**
	 * Prefer indexed {@code prefix.0..} keys when present (avoids fragile long
	 * CSV + colon escaping in a single Properties value); fall back to the CSV key.
	 */
	private static String loadBlockList(Properties props, String csvKey, String indexPrefix) {
		String countRaw = props.getProperty(indexPrefix + "Count");
		if (countRaw != null) {
			try {
				int count = Integer.parseInt(countRaw.trim());
				if (count <= 0) {
					return com.example.client.module.BlockEspDefaults.EMPTY_SENTINEL;
				}
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < count; i++) {
					String id = props.getProperty(indexPrefix + "." + i);
					if (id == null || id.isBlank()) {
						continue;
					}
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(id.trim());
				}
				if (sb.length() > 0) {
					return sb.toString();
				}
				return com.example.client.module.BlockEspDefaults.EMPTY_SENTINEL;
			} catch (NumberFormatException ignored) {
				// fall through to CSV
			}
		}
		return props.getProperty(csvKey, "");
	}

	private static void writeBlockList(Properties props, String csvKey, String indexPrefix, String csv) {
		props.setProperty(csvKey, csv == null ? "" : csv);
		if (csv == null || csv.isBlank()
				|| com.example.client.module.BlockEspDefaults.EMPTY_SENTINEL.equals(csv.trim())) {
			props.setProperty(indexPrefix + "Count", "0");
			return;
		}
		String[] parts = csv.split(",");
		int written = 0;
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty() || com.example.client.module.BlockEspDefaults.EMPTY_SENTINEL.equals(trimmed)) {
				continue;
			}
			props.setProperty(indexPrefix + "." + written, trimmed);
			written++;
		}
		props.setProperty(indexPrefix + "Count", Integer.toString(written));
	}

	private static boolean bool(Properties props, String key, boolean def) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return def;
		}
		return Boolean.parseBoolean(raw.trim());
	}

	private static int intVal(Properties props, String key, int def) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return def;
		}
		try {
			String s = raw.trim();
			if (s.startsWith("#") || s.startsWith("0x") || s.startsWith("0X") || s.length() == 8) {
				Integer parsed = MenuTheme.parseHex(s);
				return parsed != null ? parsed : def;
			}
			return (int) Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
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
