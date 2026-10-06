package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Client Movement module: cancels / zeroes fall damage for the local player when enabled.
 * Uses {@link Attributes#FALL_DAMAGE_MULTIPLIER}, fall-distance reset, and (in singleplayer)
 * {@link ServerLivingEntityEvents#ALLOW_DAMAGE} to reject fall damage on the integrated server.
 */
public final class NoFallModule {
	private static final Identifier FALL_MULT_ID = ExampleMod.id("rooty_no_fall_mult");
	private static final Identifier SAFE_FALL_ID = ExampleMod.id("rooty_no_fall_safe");

	private static boolean enabled;
	private static boolean hooksRegistered;

	private NoFallModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void registerHooks() {
		if (hooksRegistered) {
			return;
		}
		hooksRegistered = true;
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!enabled) {
				return true;
			}
			if (!(entity instanceof Player player)) {
				return true;
			}
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || !player.getUUID().equals(client.player.getUUID())) {
				return true;
			}
			if (source.is(DamageTypes.FALL)) {
				player.resetFallDistance();
				return false;
			}
			return true;
		});
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player != null) {
			applyTo(player, enabled);
			if (enabled) {
				player.resetFallDistance();
			}
		}
		NotificationsModule.notifyToggle(
			enabled ? "screen.modid.menu.movement.nofall.enabled" : "screen.modid.menu.movement.nofall.disabled"
		);
		syncIntegratedServer(client, enabled);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		applyTo(player, enabled);
		if (enabled) {
			player.resetFallDistance();
			syncIntegratedServer(client, true);
		}
	}

	private static void applyTo(LivingEntity entity, boolean on) {
		AttributeInstance fallMult = entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
		AttributeInstance safeFall = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
		if (fallMult != null) {
			if (on) {
				fallMult.addOrUpdateTransientModifier(new AttributeModifier(
						FALL_MULT_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
				));
			} else {
				fallMult.removeModifier(FALL_MULT_ID);
			}
		}
		if (safeFall != null) {
			if (on) {
				// Large additive bump so almost any fall is "safe"
				safeFall.addOrUpdateTransientModifier(new AttributeModifier(
						SAFE_FALL_ID, 1024.0, AttributeModifier.Operation.ADD_VALUE
				));
			} else {
				safeFall.removeModifier(SAFE_FALL_ID);
			}
		}
	}

	private static void syncIntegratedServer(Minecraft client, boolean on) {
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
			applyTo(serverPlayer, on);
			if (on) {
				serverPlayer.resetFallDistance();
			}
		});
	}
}
