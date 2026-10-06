package com.example.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

/**
 * Client Movement module: creative-style flight toggle.
 * Best-effort — sets {@link Abilities#mayfly} on the local player every tick while
 * enabled, and also updates the integrated-server player in singleplayer so survival
 * flight works locally. Dedicated multiplayer servers may still reject the ability.
 */
public final class FlightModule {
	private static boolean enabled;

	private FlightModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player != null) {
			applyToLocal(player, enabled);
			player.sendSystemMessage(Component.translatable(
					enabled ? "screen.modid.menu.movement.flight.enabled" : "screen.modid.menu.movement.flight.disabled"
			));
		}
		syncIntegratedServer(client, enabled);
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Re-assert mayfly each client tick so ability sync packets do not immediately
	 * clear a locally enabled flight state in singleplayer.
	 */
	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		if (enabled) {
			Abilities abilities = player.getAbilities();
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				player.onUpdateAbilities();
				syncIntegratedServer(client, true);
			}
		} else if (!player.isCreative() && !player.isSpectator()) {
			Abilities abilities = player.getAbilities();
			if (abilities.mayfly || abilities.flying) {
				abilities.mayfly = false;
				abilities.flying = false;
				player.onUpdateAbilities();
			}
		}
	}

	private static void applyToLocal(LocalPlayer player, boolean mayFly) {
		Abilities abilities = player.getAbilities();
		boolean creativeLike = player.isCreative() || player.isSpectator();
		if (mayFly) {
			abilities.mayfly = true;
		} else if (!creativeLike) {
			abilities.mayfly = false;
			abilities.flying = false;
		}
		player.onUpdateAbilities();
	}

	private static void syncIntegratedServer(Minecraft client, boolean mayFly) {
		if (!client.hasSingleplayerServer() || client.player == null) {
			return;
		}
		var server = client.getSingleplayerServer();
		if (server == null) {
			return;
		}
		var uuid = client.player.getUUID();
		server.execute(() -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayer(uuid);
			if (serverPlayer == null) {
				return;
			}
			Abilities abilities = serverPlayer.getAbilities();
			boolean creativeLike = serverPlayer.isCreative() || serverPlayer.isSpectator();
			if (mayFly) {
				if (!abilities.mayfly) {
					abilities.mayfly = true;
					serverPlayer.onUpdateAbilities();
				}
			} else if (!creativeLike && (abilities.mayfly || abilities.flying)) {
				abilities.mayfly = false;
				abilities.flying = false;
				serverPlayer.onUpdateAbilities();
			}
		});
	}
}
