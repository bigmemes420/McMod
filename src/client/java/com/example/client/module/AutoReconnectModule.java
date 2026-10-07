package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.util.Mth;

/**
 * Misc (Meteor AutoReconnect): remembers the last multiplayer server and
 * reconnects after a delay when the disconnect screen is shown.
 */
public final class AutoReconnectModule {
	public static final float MIN_DELAY = 1.0F;
	public static final float MAX_DELAY = 30.0F;
	public static final float DEFAULT_DELAY = 5.0F;

	private static boolean enabled;
	private static float delaySeconds = DEFAULT_DELAY;
	private static ServerData lastServer;
	private static long disconnectAtMs = -1L;

	private AutoReconnectModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static float getDelaySeconds() {
		return delaySeconds;
	}

	public static void setDelaySeconds(float value) {
		float clamped = Mth.clamp(value, MIN_DELAY, MAX_DELAY);
		if (delaySeconds == clamped) {
			return;
		}
		delaySeconds = clamped;
		ModConfig.save();
	}

	public static void loadDelaySeconds(float value) {
		delaySeconds = Mth.clamp(value, MIN_DELAY, MAX_DELAY);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		disconnectAtMs = -1L;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.misc.autoreconnect.enabled"
						: "screen.rootymenu.menu.misc.autoreconnect.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** Call while connected so disconnect can reconnect. */
	public static void rememberServer(Minecraft client) {
		if (client == null) {
			return;
		}
		ServerData data = client.getCurrentServer();
		if (data != null) {
			lastServer = data;
		}
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			disconnectAtMs = -1L;
			return;
		}
		rememberServer(client);
		if (!(client.gui.screen() instanceof DisconnectedScreen)) {
			disconnectAtMs = -1L;
			return;
		}
		if (lastServer == null || lastServer.ip == null || lastServer.ip.isBlank()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (disconnectAtMs < 0L) {
			disconnectAtMs = now;
			return;
		}
		if (now - disconnectAtMs < Math.round(delaySeconds * 1000.0F)) {
			return;
		}
		disconnectAtMs = -1L;
		ServerAddress address = ServerAddress.parseString(lastServer.ip);
		ConnectScreen.startConnecting(
				new JoinMultiplayerScreen(new TitleScreen()),
				client,
				address,
				lastServer,
				false,
				null
		);
	}
}
