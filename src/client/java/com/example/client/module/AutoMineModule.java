package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Automations (safe AutoMine): continuously mines the block under the crosshair
 * via vanilla MultiPlayerGameMode start/continueDestroyBlock. No packet mine,
 * no nuker, no reach extension.
 */
public final class AutoMineModule {
	private static boolean enabled;
	private static BlockPos lastPos;

	private AutoMineModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		lastPos = null;
		if (!enabled) {
			Minecraft client = Minecraft.getInstance();
			if (client.gameMode != null) {
				client.gameMode.stopDestroyBlock();
			}
		}
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.automations.automine.enabled"
						: "screen.rootymenu.menu.automations.automine.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null || client.level == null || client.gui.screen() != null) {
			stop(client);
			return;
		}
		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			stop(client);
			return;
		}
		BlockPos pos = blockHit.getBlockPos();
		Direction face = blockHit.getDirection();
		BlockState state = client.level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0.0F) {
			stop(client);
			return;
		}
		if (lastPos == null || !lastPos.equals(pos) || !client.gameMode.isDestroying()) {
			client.gameMode.startDestroyBlock(pos, face);
			lastPos = pos.immutable();
		} else {
			client.gameMode.continueDestroyBlock(pos, face);
		}
	}

	private static void stop(Minecraft client) {
		if (lastPos != null && client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
		}
		lastPos = null;
	}
}
