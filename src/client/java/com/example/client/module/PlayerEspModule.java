package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Visuals: Player ESP with Outline / 2D / 3D modes. "Outline Boxes" draws a
 * black outer stroke on the ESP boxes (not a vanilla player entity outline).
 * Color lives on this module's settings (not the central Menu theme).
 */
public final class PlayerEspModule {
	public enum Mode {
		OUTLINE,
		BOX_2D,
		BOX_3D
	}

	public static final int DEFAULT_COLOR = 0xFFFFFFFF;

	private static boolean enabled;
	private static Mode mode = Mode.BOX_3D;
	private static int color = DEFAULT_COLOR;
	/** When true, draw a black outer stroke under 2D/3D ESP boxes. */
	private static boolean outlineBoxes;

	private PlayerEspModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Mode getMode() {
		return mode;
	}

	public static int getColor() {
		return color;
	}

	public static boolean isOutlineBoxes() {
		return outlineBoxes;
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
			mode = Mode.BOX_3D;
		}
	}

	public static void setColor(int argb) {
		int opaque = ARGB.opaque(argb);
		if (color == opaque) {
			return;
		}
		color = opaque;
		ModConfig.save();
	}

	public static void loadColor(int argb) {
		color = ARGB.opaque(argb);
	}

	public static void setOutlineBoxes(boolean value) {
		if (outlineBoxes == value) {
			return;
		}
		outlineBoxes = value;
		ModConfig.save();
	}

	public static void loadOutlineBoxes(boolean value) {
		outlineBoxes = value;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.player_esp.enabled"
						: "screen.modid.menu.visuals.player_esp.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** True when vanilla outline mode should force through-walls outline on this player. */
	public static boolean shouldOutline(LivingEntity entity) {
		if (!enabled || !(entity instanceof Player) || entity instanceof LocalPlayer) {
			return false;
		}
		return mode == Mode.OUTLINE;
	}

	public static int outlineColor() {
		return ARGB.opaque(color);
	}

	/** Invoked from {@code LevelRenderEvents.BEFORE_GIZMOS}. */
	public static void render(LevelRenderer levelRenderer) {
		if (!enabled || mode == Mode.OUTLINE) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}
		LocalPlayer self = client.player;
		int stroke = ARGB.opaque(color);
		EntityEspRenderer.Mode drawMode = mode == Mode.BOX_2D
				? EntityEspRenderer.Mode.BOX_2D
				: EntityEspRenderer.Mode.BOX_3D;
		for (Player player : client.level.players()) {
			if (player == self || player.isRemoved()) {
				continue;
			}
			EntityEspRenderer.draw(levelRenderer, player, drawMode, stroke, outlineBoxes);
		}
	}
}
