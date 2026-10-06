package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Client Movement module: flight with selectable modes.
 * <ul>
 *   <li>{@link Mode#VANILLA} — creative-style {@code mayfly} (best in singleplayer)</li>
 *   <li>{@link Mode#VELOCITY} — look/input-driven velocity flight (no abilities)</li>
 *   <li>{@link Mode#HOVER} — hold altitude; horizontal move from input</li>
 *   <li>{@link Mode#JETPACK} — boost upward while jump is held</li>
 * </ul>
 */
public final class FlightModule {
	public enum Mode {
		VANILLA,
		VELOCITY,
		HOVER,
		JETPACK;

		public static Mode fromString(String raw) {
			if (raw == null || raw.isBlank()) {
				return VANILLA;
			}
			String key = raw.trim().toUpperCase(Locale.ROOT);
			if ("CREATIVE".equals(key) || "NORMAL".equals(key)) {
				return VANILLA;
			}
			try {
				return Mode.valueOf(key);
			} catch (IllegalArgumentException e) {
				return VANILLA;
			}
		}
	}

	public static final float MIN_SPEED = 0.5F;
	public static final float MAX_SPEED = 5.0F;
	public static final float DEFAULT_SPEED = 1.0F;

	private static final double BASE_FLY_SPEED = 0.35D;

	private static boolean enabled;
	private static Mode mode = Mode.VANILLA;
	private static float speed = DEFAULT_SPEED;

	private FlightModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static float getSpeed() {
		return speed;
	}

	public static void setMode(Mode value) {
		if (value == null || mode == value) {
			return;
		}
		Mode previous = mode;
		mode = value;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player != null && enabled) {
			if (previous == Mode.VANILLA && mode != Mode.VANILLA) {
				clearVanillaFlight(player, client);
			} else if (mode == Mode.VANILLA) {
				applyVanillaFlight(player, client, true);
			}
		}
		ModConfig.save();
	}

	public static void loadMode(String raw) {
		mode = Mode.fromString(raw);
	}

	public static void setSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
		if (speed == clamped) {
			return;
		}
		speed = clamped;
		ModConfig.save();
	}

	public static void loadSpeed(float value) {
		speed = Mth.clamp(value, MIN_SPEED, MAX_SPEED);
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player != null) {
			if (enabled && mode == Mode.VANILLA) {
				applyVanillaFlight(player, client, true);
			} else if (!enabled) {
				clearVanillaFlight(player, client);
			}
		}
		NotificationsModule.notifyToggle(
			enabled ? "screen.modid.menu.movement.flight.enabled" : "screen.modid.menu.movement.flight.disabled"
		);
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
		if (!enabled) {
			if (!player.isCreative() && !player.isSpectator()) {
				Abilities abilities = player.getAbilities();
				if (abilities.mayfly || abilities.flying) {
					abilities.mayfly = false;
					abilities.flying = false;
					player.onUpdateAbilities();
				}
			}
			return;
		}

		switch (mode) {
			case VANILLA -> tickVanilla(client, player);
			case VELOCITY -> tickVelocity(player);
			case HOVER -> tickHover(player);
			case JETPACK -> tickJetpack(player);
		}
	}

	private static void tickVanilla(Minecraft client, LocalPlayer player) {
		Abilities abilities = player.getAbilities();
		if (!abilities.mayfly) {
			abilities.mayfly = true;
			player.onUpdateAbilities();
			syncIntegratedServer(client, true);
		}
	}

	private static void tickVelocity(LocalPlayer player) {
		if (player.input == null) {
			return;
		}
		double fly = speed * BASE_FLY_SPEED;
		Vec2 move = player.input.getMoveVector();
		float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yawRad);
		float cos = Mth.cos(yawRad);
		double mx = move.x * cos - move.y * sin;
		double mz = move.y * cos + move.x * sin;
		double len = Math.sqrt(mx * mx + mz * mz);
		if (len > 1.0E-8D) {
			mx = mx / len * fly;
			mz = mz / len * fly;
		} else {
			mx = 0.0D;
			mz = 0.0D;
		}
		double my = 0.0D;
		if (player.input.keyPresses.jump()) {
			my += fly;
		}
		if (player.input.keyPresses.shift()) {
			my -= fly;
		}
		player.setDeltaMovement(mx, my, mz);
		player.resetFallDistance();
		player.setOnGround(false);
	}

	private static void tickHover(LocalPlayer player) {
		if (player.input == null) {
			return;
		}
		double fly = speed * BASE_FLY_SPEED * 0.75D;
		Vec2 move = player.input.getMoveVector();
		float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yawRad);
		float cos = Mth.cos(yawRad);
		double mx = move.x * cos - move.y * sin;
		double mz = move.y * cos + move.x * sin;
		double len = Math.sqrt(mx * mx + mz * mz);
		if (len > 1.0E-8D) {
			mx = mx / len * fly;
			mz = mz / len * fly;
		} else {
			mx = 0.0D;
			mz = 0.0D;
		}
		double my = 0.0D;
		if (player.input.keyPresses.jump()) {
			my = fly;
		} else if (player.input.keyPresses.shift()) {
			my = -fly;
		}
		player.setDeltaMovement(mx, my, mz);
		player.resetFallDistance();
	}

	private static void tickJetpack(LocalPlayer player) {
		if (player.input == null) {
			return;
		}
		Vec3 vel = player.getDeltaMovement();
		if (player.input.keyPresses.jump()) {
			double boost = 0.12D * speed;
			player.setDeltaMovement(vel.x, Math.max(vel.y, 0.0D) + boost, vel.z);
			player.resetFallDistance();
		}
	}

	private static void applyVanillaFlight(LocalPlayer player, Minecraft client, boolean mayFly) {
		Abilities abilities = player.getAbilities();
		boolean creativeLike = player.isCreative() || player.isSpectator();
		if (mayFly) {
			abilities.mayfly = true;
		} else if (!creativeLike) {
			abilities.mayfly = false;
			abilities.flying = false;
		}
		player.onUpdateAbilities();
		syncIntegratedServer(client, mayFly);
	}

	private static void clearVanillaFlight(LocalPlayer player, Minecraft client) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.mayfly = false;
		abilities.flying = false;
		player.onUpdateAbilities();
		syncIntegratedServer(client, false);
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
