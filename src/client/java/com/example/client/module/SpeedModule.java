package com.example.client.module;

import com.example.ExampleMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Client Movement module: multiplies the local player's {@link Attributes#MOVEMENT_SPEED}
 * (and flying speed) only. Does not touch tick rate, Timer, or global game speed.
 */
public final class SpeedModule {
	private static final Identifier SPEED_MODIFIER_ID = ExampleMod.id("rooty_speed");
	private static final Identifier FLY_SPEED_MODIFIER_ID = ExampleMod.id("rooty_fly_speed");

	/** ADD_MULTIPLIED_TOTAL amount: 1.0 => roughly double walk speed. */
	private static final double SPEED_MULTIPLIER = 1.0;

	private static boolean enabled;

	private SpeedModule() {
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
			applyTo(player, enabled);
			player.sendSystemMessage(Component.translatable(
					enabled ? "screen.modid.menu.movement.speed.enabled" : "screen.modid.menu.movement.speed.disabled"
			));
		}
		syncIntegratedServer(client, enabled);
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Re-assert the movement-speed modifier each tick so sync packets do not clear it.
	 */
	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		applyTo(player, enabled);
		if (enabled) {
			syncIntegratedServer(client, true);
		}
	}

	private static void applyTo(LivingEntity entity, boolean on) {
		applyModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, on);
		applyModifier(entity, Attributes.FLYING_SPEED, FLY_SPEED_MODIFIER_ID, on);
	}

	private static void applyModifier(
			LivingEntity entity,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			Identifier id,
			boolean on
	) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (on) {
			AttributeModifier modifier = new AttributeModifier(id, SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			instance.addOrUpdateTransientModifier(modifier);
		} else {
			instance.removeModifier(id);
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
		});
	}
}
