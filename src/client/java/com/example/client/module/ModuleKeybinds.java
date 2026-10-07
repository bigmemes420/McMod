package com.example.client.module;

import com.example.client.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Per-module keybinds: right-click a module capsule to bind, Esc clears.
 * Keys are stored as InputConstants names in module config.
 */
public final class ModuleKeybinds {
	public record ModuleEntry(String id, Component label, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled) {
		public void toggle() {
			setEnabled.accept(!isEnabled.getAsBoolean());
		}
	}

	private static final LinkedHashMap<String, ModuleEntry> MODULES = new LinkedHashMap<>();
	/** InputConstants key name → none when missing / UNKNOWN. */
	private static final LinkedHashMap<String, String> binds = new LinkedHashMap<>();
	private static final LinkedHashMap<String, Boolean> wasDown = new LinkedHashMap<>();

	private static String listeningId;

	private ModuleKeybinds() {
	}

	public static void register(String id, Component label, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled) {
		MODULES.put(id, new ModuleEntry(id, label, isEnabled, setEnabled));
	}

	public static void registerAll() {
		MODULES.clear();
		register("nametags", Component.translatable("screen.rootymenu.menu.visuals.nametags"), NametagsModule::isEnabled, NametagsModule::setEnabled);
		register("player_esp", Component.translatable("screen.rootymenu.menu.visuals.player_esp"), PlayerEspModule::isEnabled, PlayerEspModule::setEnabled);
		register("mob_esp", Component.translatable("screen.rootymenu.menu.visuals.mob_esp"), MobEspModule::isEnabled, MobEspModule::setEnabled);
		register("fullbright", Component.translatable("screen.rootymenu.menu.visuals.fullbright"), FullbrightModule::isEnabled, FullbrightModule::setEnabled);
		register("finder", Component.translatable("screen.rootymenu.menu.visuals.finder"), FinderModule::isEnabled, FinderModule::setEnabled);
		register("radar", Component.translatable("screen.rootymenu.menu.visuals.radar"), RadarModule::isEnabled, RadarModule::setEnabled);
		register("custom_crosshair", Component.translatable("screen.rootymenu.menu.visuals.custom_crosshair"), CustomCrosshairModule::isEnabled, CustomCrosshairModule::setEnabled);
		register("autoclicker", Component.translatable("screen.rootymenu.menu.combat.autoclicker"), AutoClickerModule::isEnabled, AutoClickerModule::setEnabled);
		register("velocity", Component.translatable("screen.rootymenu.menu.combat.velocity"), VelocityModule::isEnabled, VelocityModule::setEnabled);
		register("reach", Component.translatable("screen.rootymenu.menu.combat.reach"), ReachModule::isEnabled, ReachModule::setEnabled);
		register("criticals", Component.translatable("screen.rootymenu.menu.combat.criticals"), CriticalsModule::isEnabled, CriticalsModule::setEnabled);
		register("triggerbot", Component.translatable("screen.rootymenu.menu.combat.triggerbot"), TriggerBotModule::isEnabled, TriggerBotModule::setEnabled);
		register("aimassist", Component.translatable("screen.rootymenu.menu.combat.aimassist"), AimAssistModule::isEnabled, AimAssistModule::setEnabled);
		register("hitboxes", Component.translatable("screen.rootymenu.menu.combat.hitboxes"), HitboxesModule::isEnabled, HitboxesModule::setEnabled);
		register("autototem", Component.translatable("screen.rootymenu.menu.combat.autototem"), AutoTotemModule::isEnabled, AutoTotemModule::setEnabled);
		register("scaffold", Component.translatable("screen.rootymenu.menu.world.scaffold"), ScaffoldModule::isEnabled, ScaffoldModule::setEnabled);
		register("fastplace", Component.translatable("screen.rootymenu.menu.world.fastplace"), FastPlaceModule::isEnabled, FastPlaceModule::setEnabled);
		register("tower", Component.translatable("screen.rootymenu.menu.world.tower"), TowerModule::isEnabled, TowerModule::setEnabled);
		register("airplace", Component.translatable("screen.rootymenu.menu.world.airplace"), AirPlaceModule::isEnabled, AirPlaceModule::setEnabled);
		register("inventory_move", Component.translatable("screen.rootymenu.menu.player.inventory_move"), InventoryMoveModule::isEnabled, InventoryMoveModule::setEnabled);
		register("noslow", Component.translatable("screen.rootymenu.menu.player.noslow"), NoSlowModule::isEnabled, NoSlowModule::setEnabled);
		register("sneak", Component.translatable("screen.rootymenu.menu.player.sneak"), SneakModule::isEnabled, SneakModule::setEnabled);
		register("flight", Component.translatable("screen.rootymenu.menu.movement.flight"), FlightModule::isEnabled, FlightModule::setEnabled);
		register("elytra_control", Component.translatable("screen.rootymenu.menu.movement.elytra_control"), ElytraControlModule::isEnabled, ElytraControlModule::setEnabled);
		register("speed", Component.translatable("screen.rootymenu.menu.movement.speed"), SpeedModule::isEnabled, SpeedModule::setEnabled);
		register("nofall", Component.translatable("screen.rootymenu.menu.movement.nofall"), NoFallModule::isEnabled, NoFallModule::setEnabled);
		register("autosprint", Component.translatable("screen.rootymenu.menu.movement.autosprint"), AutoSprintModule::isEnabled, AutoSprintModule::setEnabled);
		register("step", Component.translatable("screen.rootymenu.menu.movement.step"), StepModule::isEnabled, StepModule::setEnabled);
		register("spider", Component.translatable("screen.rootymenu.menu.movement.spider"), SpiderModule::isEnabled, SpiderModule::setEnabled);
		register("safewalk", Component.translatable("screen.rootymenu.menu.movement.safewalk"), SafeWalkModule::isEnabled, SafeWalkModule::setEnabled);
		register("jesus", Component.translatable("screen.rootymenu.menu.movement.jesus"), JesusModule::isEnabled, JesusModule::setEnabled);
		register("notifications", Component.translatable("screen.rootymenu.menu.misc.notifications"), NotificationsModule::isEnabled, NotificationsModule::setEnabled);
		register("viewer_retention", Component.translatable("screen.rootymenu.menu.misc.viewer_retention"), ViewerRetentionModule::isEnabled, ViewerRetentionModule::setEnabled);
		register("zoom", Component.translatable("screen.rootymenu.menu.visuals.zoom"), ZoomModule::isEnabled, ZoomModule::setEnabled);
		register("nohurtcam", Component.translatable("screen.rootymenu.menu.visuals.nohurtcam"), NoHurtCamModule::isEnabled, NoHurtCamModule::setEnabled);
		register("tracers", Component.translatable("screen.rootymenu.menu.visuals.tracers"), TracersModule::isEnabled, TracersModule::setEnabled);
		register("breadcrumbs", Component.translatable("screen.rootymenu.menu.visuals.breadcrumbs"), BreadcrumbsModule::isEnabled, BreadcrumbsModule::setEnabled);
		register("freecam", Component.translatable("screen.rootymenu.menu.visuals.freecam"), FreecamModule::isEnabled, FreecamModule::setEnabled);
		register("projectile_trajectory", Component.translatable("screen.rootymenu.menu.visuals.projectile_trajectory"), ProjectileTrajectoryModule::isEnabled, ProjectileTrajectoryModule::setEnabled);
		register("autoarmor", Component.translatable("screen.rootymenu.menu.combat.autoarmor"), AutoArmorModule::isEnabled, AutoArmorModule::setEnabled);
		register("autorespawn", Component.translatable("screen.rootymenu.menu.player.autorespawn"), AutoRespawnModule::isEnabled, AutoRespawnModule::setEnabled);
		register("antiafk", Component.translatable("screen.rootymenu.menu.player.antiafk"), AntiAFKModule::isEnabled, AntiAFKModule::setEnabled);
		register("autotool", Component.translatable("screen.rootymenu.menu.player.autotool"), AutoToolModule::isEnabled, AutoToolModule::setEnabled);
		register("autoeat", Component.translatable("screen.rootymenu.menu.player.autoeat"), AutoEatModule::isEnabled, AutoEatModule::setEnabled);
		register("autowalk", Component.translatable("screen.rootymenu.menu.movement.autowalk"), AutoWalkModule::isEnabled, AutoWalkModule::setEnabled);
		register("parkour", Component.translatable("screen.rootymenu.menu.movement.parkour"), ParkourModule::isEnabled, ParkourModule::setEnabled);
		register("autoreconnect", Component.translatable("screen.rootymenu.menu.misc.autoreconnect"), AutoReconnectModule::isEnabled, AutoReconnectModule::setEnabled);
		register("autofish", Component.translatable("screen.rootymenu.menu.automations.autofish"), AutoFishModule::isEnabled, AutoFishModule::setEnabled);
		register("automine", Component.translatable("screen.rootymenu.menu.automations.automine"), AutoMineModule::isEnabled, AutoMineModule::setEnabled);
	}

	public static Map<String, ModuleEntry> modules() {
		return Collections.unmodifiableMap(MODULES);
	}

	public static boolean isListening() {
		return listeningId != null;
	}

	public static String getListeningId() {
		return listeningId;
	}

	public static boolean isListening(String moduleId) {
		return moduleId != null && moduleId.equals(listeningId);
	}

	public static void startListening(String moduleId) {
		if (moduleId == null || !MODULES.containsKey(moduleId)) {
			return;
		}
		listeningId = moduleId;
	}

	public static void cancelListening() {
		listeningId = null;
	}

	public static String getBindName(String moduleId) {
		return binds.getOrDefault(moduleId, "");
	}

	public static Component getBindDisplay(String moduleId) {
		String name = getBindName(moduleId);
		if (name == null || name.isBlank() || InputConstants.UNKNOWN.getName().equals(name)) {
			return Component.translatable("screen.rootymenu.menu.keybind.none");
		}
		try {
			return InputConstants.getKey(name).getDisplayName();
		} catch (Exception e) {
			return Component.literal(name);
		}
	}

	public static void setBind(String moduleId, InputConstants.Key key) {
		if (moduleId == null) {
			return;
		}
		if (key == null || key.equals(InputConstants.UNKNOWN)) {
			binds.remove(moduleId);
		} else {
			binds.put(moduleId, key.getName());
		}
		ModConfig.save();
	}

	public static void clearBind(String moduleId) {
		setBind(moduleId, InputConstants.UNKNOWN);
	}

	/** Persist as {@code keybind.<id>=keyName} lines. */
	public static void writeTo(java.util.Properties props) {
		for (String id : MODULES.keySet()) {
			String name = binds.get(id);
			if (name != null && !name.isBlank()) {
				props.setProperty("keybind." + id, name);
			}
		}
	}

	public static void loadFrom(java.util.Properties props) {
		binds.clear();
		for (String id : MODULES.keySet()) {
			String name = props.getProperty("keybind." + id);
			if (name != null && !name.isBlank() && !InputConstants.UNKNOWN.getName().equals(name)) {
				binds.put(id, name.trim());
			}
		}
	}

	/**
	 * While listening: capture next key (Esc clears). Returns true if consumed.
	 */
	public static boolean handleKeyPressed(KeyEvent event) {
		if (listeningId == null) {
			return false;
		}
		String id = listeningId;
		listeningId = null;
		if (event.key() == InputConstants.KEY_ESCAPE) {
			clearBind(id);
			NotificationsModule.notifyMessage(Component.translatable("screen.rootymenu.menu.keybind.cleared"));
			return true;
		}
		InputConstants.Key key = InputConstants.getKey(event);
		setBind(id, key);
		NotificationsModule.notifyMessage(Component.translatable(
				"screen.rootymenu.menu.keybind.set",
				MODULES.get(id).label(),
				key.getDisplayName()
		));
		return true;
	}

	public static void tick(Minecraft client) {
		if (listeningId != null) {
			return;
		}
		if (client.gui.screen() != null) {
			return;
		}
		for (Map.Entry<String, String> e : binds.entrySet()) {
			ModuleEntry mod = MODULES.get(e.getKey());
			if (mod == null) {
				continue;
			}
			InputConstants.Key key;
			try {
				key = InputConstants.getKey(e.getValue());
			} catch (Exception ex) {
				continue;
			}
			if (key.getType() != InputConstants.Type.KEYBOARD) {
				continue;
			}
			boolean down = InputConstants.isKeyDown(key.getValue());
			boolean prev = Boolean.TRUE.equals(wasDown.get(e.getKey()));
			wasDown.put(e.getKey(), down);
			if (down && !prev) {
				mod.toggle();
			}
		}
	}
}
