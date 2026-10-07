package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

/**
 * Shared combat targeting filter: Players / Hostile mobs / Passive mobs.
 * Used by Aim Assist, TriggerBot, Reach, and AutoClicker.
 */
public final class CombatTargetingModule {
	public static final int INDEX_PLAYERS = 0;
	public static final int INDEX_HOSTILE = 1;
	public static final int INDEX_PASSIVE = 2;

	private static boolean players = true;
	private static boolean hostile = true;
	private static boolean passive = true;

	private CombatTargetingModule() {
	}

	public static boolean isPlayers() {
		return players;
	}

	public static boolean isHostile() {
		return hostile;
	}

	public static boolean isPassive() {
		return passive;
	}

	public static boolean[] getFlags() {
		return new boolean[] { players, hostile, passive };
	}

	public static void setFlag(int index, boolean value) {
		boolean changed = switch (index) {
			case INDEX_PLAYERS -> {
				if (players == value) {
					yield false;
				}
				players = value;
				yield true;
			}
			case INDEX_HOSTILE -> {
				if (hostile == value) {
					yield false;
				}
				hostile = value;
				yield true;
			}
			case INDEX_PASSIVE -> {
				if (passive == value) {
					yield false;
				}
				passive = value;
				yield true;
			}
			default -> false;
		};
		if (changed) {
			ModConfig.save();
		}
	}

	public static void setPlayers(boolean value) {
		setFlag(INDEX_PLAYERS, value);
	}

	public static void setHostile(boolean value) {
		setFlag(INDEX_HOSTILE, value);
	}

	public static void setPassive(boolean value) {
		setFlag(INDEX_PASSIVE, value);
	}

	public static void load(boolean playersFlag, boolean hostileFlag, boolean passiveFlag) {
		players = playersFlag;
		hostile = hostileFlag;
		passive = passiveFlag;
	}

	/** CSV: {@code players,hostile,passive} — missing tokens default to false when csv non-blank. */
	public static void loadCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			players = true;
			hostile = true;
			passive = true;
			return;
		}
		players = false;
		hostile = false;
		passive = false;
		for (String part : csv.split(",")) {
			String t = part.trim().toLowerCase();
			switch (t) {
				case "players", "player" -> players = true;
				case "hostile", "hostiles", "monster", "monsters" -> hostile = true;
				case "passive", "passives", "friendly", "creature" -> passive = true;
				default -> {
				}
			}
		}
	}

	public static String toCsv() {
		StringBuilder sb = new StringBuilder();
		if (players) {
			sb.append("players");
		}
		if (hostile) {
			if (!sb.isEmpty()) {
				sb.append(',');
			}
			sb.append("hostile");
		}
		if (passive) {
			if (!sb.isEmpty()) {
				sb.append(',');
			}
			sb.append("passive");
		}
		return sb.toString();
	}

	/**
	 * True when {@code entity} is allowed by the current targeting flags.
	 * Local player is never a valid target.
	 */
	public static boolean matches(Entity entity) {
		if (entity == null || !entity.isAlive()) {
			return false;
		}
		if (entity instanceof LocalPlayer) {
			return false;
		}
		if (entity instanceof Player) {
			return players;
		}
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		MobCategory cat = living.getType().getCategory();
		if (cat == MobCategory.MONSTER) {
			return hostile;
		}
		if (cat == MobCategory.MISC) {
			return false;
		}
		// CREATURE, AMBIENT, water/axolotl categories — treat as passive/friendly.
		return passive;
	}
}
