package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Visuals: top-left HUD radar showing nearby players and/or mobs with heads,
 * names, and optional height arrows relative to the local player.
 */
public final class RadarModule {
	public enum TargetMode {
		MOBS,
		PLAYERS,
		BOTH
	}

	public static final float MIN_RANGE = 16.0F;
	public static final float MAX_RANGE = 128.0F;
	public static final float DEFAULT_RANGE = 64.0F;

	private static final int PANEL_LEFT = 8;
	private static final int PANEL_TOP = 8;
	private static final int PANEL_SIZE = 110;
	private static final int HEAD_SIZE = 12;
	private static final int MAX_ENTRIES = 24;
	private static final int PANEL_BG = 0xC0101018;
	private static final int PANEL_OUTLINE = 0xFF707080;
	private static final int CROSSHAIR = 0x66FFFFFF;
	private static final int NAME_COLOR = 0xFFE8E8E8;
	private static final int ARROW_UP = 0xFF3DDC84;
	private static final int ARROW_DOWN = 0xFFFF6B6B;

	private static boolean enabled;
	private static TargetMode mode = TargetMode.BOTH;
	private static boolean showHeight = true;
	private static float range = DEFAULT_RANGE;

	private RadarModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static TargetMode getMode() {
		return mode;
	}

	public static boolean isShowHeight() {
		return showHeight;
	}

	public static float getRange() {
		return range;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.visuals.radar.enabled"
						: "screen.modid.menu.visuals.radar.disabled"
		);
		ModConfig.save();
	}

	public static void setMode(TargetMode value) {
		if (value == null || mode == value) {
			return;
		}
		mode = value;
		ModConfig.save();
	}

	public static void setShowHeight(boolean value) {
		if (showHeight == value) {
			return;
		}
		showHeight = value;
		ModConfig.save();
	}

	public static void setRange(float value) {
		float clamped = Mth.clamp(value, MIN_RANGE, MAX_RANGE);
		if (range == clamped) {
			return;
		}
		range = clamped;
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void loadMode(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			mode = TargetMode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			mode = TargetMode.BOTH;
		}
	}

	public static void loadShowHeight(boolean value) {
		showHeight = value;
	}

	public static void loadRange(float value) {
		range = Mth.clamp(value, MIN_RANGE, MAX_RANGE);
	}

	/** Fabric HUD element — draws in the top-left of the game HUD. */
	public static void extractHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		// Hide while a full screen (menu) is open — radar is an in-game HUD.
		if (client.gui.screen() != null) {
			return;
		}

		Player self = client.player;
		double selfX = self.getX();
		double selfY = self.getY();
		double selfZ = self.getZ();
		float yawRad = self.getYRot() * Mth.DEG_TO_RAD;

		List<LivingEntity> targets = collectTargets(client, self);
		targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(self)));

		int size = PANEL_SIZE;
		int left = PANEL_LEFT;
		int top = PANEL_TOP;
		graphics.fill(left, top, left + size, top + size, PANEL_BG);
		graphics.outline(left, top, size, size, PANEL_OUTLINE);

		int cx = left + size / 2;
		int cy = top + size / 2;
		graphics.fill(cx - 1, top + 4, cx, top + size - 4, CROSSHAIR);
		graphics.fill(left + 4, cy - 1, left + size - 4, cy, CROSSHAIR);
		// Local player center pip
		graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF8EC8FF);

		float radius = (size / 2.0F) - HEAD_SIZE - 4;
		int drawn = 0;
		for (LivingEntity entity : targets) {
			if (drawn >= MAX_ENTRIES) {
				break;
			}
			double dx = entity.getX() - selfX;
			double dz = entity.getZ() - selfZ;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist < 0.05) {
				continue;
			}
			// Rotate so +Z (south) aligns with player look; radar "up" = look direction.
			float cos = Mth.cos(-yawRad);
			float sin = Mth.sin(-yawRad);
			double rx = dx * cos - dz * sin;
			double rz = dx * sin + dz * cos;
			double scale = Math.min(1.0, dist / range);
			int hx = cx + (int) Math.round((rx / Math.max(dist, 0.001)) * radius * scale) - HEAD_SIZE / 2;
			int hy = cy + (int) Math.round((rz / Math.max(dist, 0.001)) * radius * scale) - HEAD_SIZE / 2;
			hx = Mth.clamp(hx, left + 2, left + size - HEAD_SIZE - 2);
			hy = Mth.clamp(hy, top + 10, top + size - HEAD_SIZE - 10);

			drawHead(graphics, client, entity, hx, hy, HEAD_SIZE);

			Component name = entity.getDisplayName();
			String nameStr = name.getString();
			if (nameStr.length() > 12) {
				nameStr = nameStr.substring(0, 11) + "…";
			}
			int nameW = client.font.width(nameStr);
			int nameX = hx + HEAD_SIZE / 2 - nameW / 2;
			int nameY = hy - client.font.lineHeight - 1;
			graphics.text(client.font, nameStr, nameX, nameY, NAME_COLOR, true);

			if (showHeight) {
				double dy = entity.getY() - selfY;
				if (dy > 0.5) {
					drawArrowUp(graphics, hx + HEAD_SIZE / 2, nameY - 5, ARROW_UP);
				} else if (dy < -0.5) {
					drawArrowDown(graphics, hx + HEAD_SIZE / 2, hy + HEAD_SIZE + 2, ARROW_DOWN);
				}
			}
			drawn++;
		}

		String label = "Radar";
		graphics.text(client.font, label, left + 4, top + 2, 0xFFAAAAAA, false);
	}

	private static List<LivingEntity> collectTargets(Minecraft client, Player self) {
		double r = range;
		AABB box = self.getBoundingBox().inflate(r, r * 0.75, r);
		List<LivingEntity> out = new ArrayList<>();
		boolean wantPlayers = mode == TargetMode.PLAYERS || mode == TargetMode.BOTH;
		boolean wantMobs = mode == TargetMode.MOBS || mode == TargetMode.BOTH;
		for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != self)) {
			if (entity instanceof Player) {
				if (wantPlayers) {
					out.add(entity);
				}
			} else if (entity instanceof Mob) {
				if (wantMobs) {
					out.add(entity);
				}
			}
		}
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void drawHead(GuiGraphicsExtractor graphics, Minecraft client, LivingEntity entity, int x, int y, int size) {
		try {
			if (entity instanceof AbstractClientPlayer player) {
				PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), x, y, size);
				return;
			}
			EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
			if (renderer instanceof LivingEntityRenderer) {
				LivingEntityRenderer living = (LivingEntityRenderer) renderer;
				LivingEntityRenderState state = (LivingEntityRenderState) living.createRenderState(entity, 0.0F);
				Identifier tex = living.getTextureLocation(state);
				if (tex != null) {
					// Standard entity texture head UV (works for most bipeds / animals with 64x layout).
					graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 8.0F, 8.0F, size, size, 64, 64, 0xFFFFFFFF);
					return;
				}
			}
		} catch (Throwable ignored) {
			// Fall through to placeholder
		}
		int color = 0xFF000000 | (entity.getType().toString().hashCode() & 0x00FFFFFF);
		graphics.fill(x, y, x + size, y + size, color);
		String ch = entity.getDisplayName().getString();
		if (!ch.isEmpty()) {
			graphics.centeredText(
					client.font,
					Component.literal(ch.substring(0, 1).toUpperCase()),
					x + size / 2,
					y + (size - client.font.lineHeight) / 2,
					0xFFFFFFFF
			);
		}
	}

	private static void drawArrowUp(GuiGraphicsExtractor graphics, int cx, int tipY, int color) {
		// Small filled triangle pointing up
		for (int i = 0; i < 4; i++) {
			graphics.fill(cx - i, tipY + i, cx + i + 1, tipY + i + 1, color);
		}
	}

	private static void drawArrowDown(GuiGraphicsExtractor graphics, int cx, int tipY, int color) {
		for (int i = 0; i < 4; i++) {
			graphics.fill(cx - (3 - i), tipY + i, cx + (3 - i) + 1, tipY + i + 1, color);
		}
	}

	/** Registers the HUD layer once from client init. */
	public static void registerHud() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				ExampleMod.id("radar"),
				RadarModule::extractHud
		);
	}
}
