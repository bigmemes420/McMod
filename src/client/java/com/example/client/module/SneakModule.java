package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * Player: force sneak.
 * <ul>
 *   <li>Legit — holds sneak like the key (client pose + movement; unsneaks in inventory).</li>
 *   <li>Cheat — packet-only: tells the server the player is sneaking via
 *       {@code ServerboundPlayerInputPacket}, without forcing client sneak pose
 *       or movement slowdown.</li>
 * </ul>
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
				enabled ? "screen.rootymenu.menu.player.sneak.enabled"
						: "screen.rootymenu.menu.player.sneak.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/**
	 * Legit only: KeyboardInput should force the shift bit (client pose/movement).
	 */
	public static boolean shouldForceClientShift(Minecraft client) {
		if (!enabled || client == null || mode != Mode.LEGIT) {
			return false;
		}
		Screen screen = client.gui.screen();
		return !(screen instanceof AbstractContainerScreen);
	}

	/** Cheat: force shift on the input packet sent to the server only. */
	public static boolean shouldPacketSneak() {
		return enabled && mode == Mode.CHEAT;
	}

	/** Apply forced shift onto the player's ClientInput after KeyboardInput.tick (Legit). */
	public static void applyInput(Minecraft client) {
		if (!shouldForceClientShift(client)) {
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

	/** Rewrite an input snapshot so Cheat mode reports sneaking to the server. */
	public static Input maybePacketSneak(Input cur) {
		if (cur == null || !shouldPacketSneak() || cur.shift()) {
			return cur;
		}
		return new Input(
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
