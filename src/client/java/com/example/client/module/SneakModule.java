package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * Player: force sneak. Legit holds sneak like the key (unsneaks in inventory);
 * Cheat forces the shift flag on the input packet sent to the server.
 */
public final class SneakModule {
	public enum Mode {
		LEGIT,
		CHEAT
	}

	private static boolean enabled;
	private static Mode mode = Mode.LEGIT;

	private SneakModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static void setMode(Mode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			mode = Mode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			mode = Mode.LEGIT;
		}
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.player.sneak.enabled"
						: "screen.modid.menu.player.sneak.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/**
	 * Whether KeyboardInput should force the shift bit this tick.
	 * Legit: yes when no inventory GUI; Cheat: always (server flag via input packet).
	 */
	public static boolean shouldForceShift(Minecraft client) {
		if (!enabled || client == null) {
			return false;
		}
		if (mode == Mode.CHEAT) {
			return true;
		}
		Screen screen = client.gui.screen();
		return !(screen instanceof AbstractContainerScreen);
	}

	/** Apply forced shift onto the player's ClientInput after KeyboardInput.tick. */
	public static void applyInput(Minecraft client) {
		if (!shouldForceShift(client)) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}
		Input cur = player.input.keyPresses;
		if (cur.shift()) {
			return;
		}
		player.input.keyPresses = new Input(
				cur.forward(),
				cur.backward(),
				cur.left(),
				cur.right(),
				cur.jump(),
				true,
				cur.sprint()
		);
	}
}
