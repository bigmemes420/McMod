package com.example.client.module;

import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Visuals: Mob ESP with Outline / 2D / 3D options. Color lives on this module's
 * settings (not the central Menu theme). Outline mode uses the vanilla
 * through-walls outline path.
 */
public final class MobEspModule {
	public enum Mode {
		OUTLINE,
		BOX_2D,
		BOX_3D
	}

	public static final int DEFAULT_COLOR = 0xFFFF5555;

	private static boolean enabled;
	private static Mode mode = Mode.OUTLINE;
	private static int color = DEFAULT_COLOR;

	private MobEspModule() {
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
			mode = Mode.OUTLINE;
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

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.mob_esp.enabled"
						: "screen.modid.menu.visuals.mob_esp.disabled"
		);
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	/** True when outline mode should force through-walls outline on this mob. */
	public static boolean shouldOutline(LivingEntity entity) {
		return enabled
				&& mode == Mode.OUTLINE
				&& entity instanceof Mob
				&& !(entity instanceof Player);
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
		int stroke = ARGB.opaque(color);
		EntityEspRenderer.Mode drawMode = mode == Mode.BOX_2D
				? EntityEspRenderer.Mode.BOX_2D
				: EntityEspRenderer.Mode.BOX_3D;
		for (Mob mob : client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(128.0D))) {
			if (mob.isRemoved()) {
				continue;
			}
			EntityEspRenderer.draw(levelRenderer, mob, drawMode, stroke);
		}
	}
}
