package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;
import com.example.client.widget.MenuShapes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Visuals: HUD radar showing nearby players and/or mobs with heads,
 * names, and optional height arrows relative to the local player.
 * Panel can be square, circle, triangle, or a 6-pointed star.
 */
public final class RadarModule {
	public enum TargetMode {
		MOBS,
		PLAYERS,
		BOTH
	}

	public enum Shape {
		SQUARE,
		CIRCLE,
		TRIANGLE,
		STAR
	}

	public static final float MIN_RANGE = 16.0F;
	public static final float MAX_RANGE = 128.0F;
	public static final float DEFAULT_RANGE = 64.0F;

	public static final int DEFAULT_HUD_X = 8;
	public static final int DEFAULT_HUD_Y = 8;
	public static final int DEFAULT_HUD_SIZE = 110;
	public static final int MIN_HUD_SIZE = 64;
	public static final int MAX_HUD_SIZE = 280;
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
	private static Shape shape = Shape.SQUARE;
	private static boolean showHeight = true;
	private static float range = DEFAULT_RANGE;
	private static int hudX = DEFAULT_HUD_X;
	private static int hudY = DEFAULT_HUD_Y;
	private static int hudSize = DEFAULT_HUD_SIZE;

	private RadarModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static TargetMode getMode() {
		return mode;
	}

	public static Shape getShape() {
		return shape;
	}

	public static boolean isShowHeight() {
		return showHeight;
	}

	public static float getRange() {
		return range;
	}

	public static int getHudX() {
		return hudX;
	}

	public static int getHudY() {
		return hudY;
	}

	public static int getHudSize() {
		return hudSize;
	}

	/** Live layout update while dragging (no disk write). */
	public static void setHudLayoutLive(int x, int y, int size) {
		hudX = Math.max(0, x);
		hudY = Math.max(0, y);
		hudSize = Mth.clamp(size, MIN_HUD_SIZE, MAX_HUD_SIZE);
	}

	public static void setHudLayout(int x, int y, int size) {
		setHudLayoutLive(x, y, size);
		ModConfig.save();
	}

	public static void loadHudLayout(int x, int y, int size) {
		hudX = Math.max(0, x);
		hudY = Math.max(0, y);
		hudSize = Mth.clamp(size, MIN_HUD_SIZE, MAX_HUD_SIZE);
	}

	/** Bounding-box hit test used by HUD edit (resize handle stays on the box). */
	public static boolean containsPoint(double mx, double my) {
		return mx >= hudX && my >= hudY && mx < hudX + hudSize && my < hudY + hudSize;
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

	public static void setShape(Shape value) {
		if (value == null || shape == value) {
			return;
		}
		shape = value;
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

	public static void loadShape(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		try {
			shape = Shape.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			shape = Shape.SQUARE;
		}
	}

	public static void loadShowHeight(boolean value) {
		showHeight = value;
	}

	public static void loadRange(float value) {
		range = Mth.clamp(value, MIN_RANGE, MAX_RANGE);
	}

	/** Fabric HUD element — draws the radar panel on the game HUD. */
	public static void extractHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		// Hide under normal screens; keep drawing under HudEditScreen so layout is visible.
		var screen = client.gui.screen();
		if (screen != null && !(screen instanceof com.example.client.HudEditScreen)) {
			return;
		}

		Player self = client.player;
		double selfX = self.getX();
		double selfY = self.getY();
		double selfZ = self.getZ();
		float yawRad = self.getYRot() * Mth.DEG_TO_RAD;

		List<LivingEntity> targets = collectTargets(client, self);
		targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(self)));

		int size = hudSize;
		int left = hudX;
		int top = hudY;
		drawPanel(graphics, left, top, size, PANEL_BG, PANEL_OUTLINE);

		int cx = left + size / 2;
		int cy = top + size / 2;
		drawCrosshair(graphics, left, top, size, cx, cy);

		float radius = plotRadius(size);
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
			// Rotate so radar "up" = look direction.
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

		graphics.text(client.font, "Radar", left + 4, top + 2, 0xFFAAAAAA, false);
	}

	/** Used by HudEditScreen to match the live panel silhouette. */
	public static void drawEditFrame(GuiGraphicsExtractor graphics, int borderColor, int handleFill) {
		drawPanel(graphics, hudX, hudY, hudSize, 0x00000000, borderColor);
		int handle = 14;
		int hx = hudX + hudSize - handle / 2;
		int hy = hudY + hudSize - handle / 2;
		graphics.fill(hx, hy, hx + handle, hy + handle, handleFill);
		graphics.outline(hx, hy, handle, handle, 0xFFFFFFFF);
	}

	private static float plotRadius(int size) {
		// Inscribe plot area so blips stay inside non-square shapes.
		float half = size / 2.0F;
		return switch (shape) {
			case SQUARE -> half - HEAD_SIZE - 4;
			case CIRCLE, STAR -> half * 0.72F - HEAD_SIZE / 2.0F;
			case TRIANGLE -> half * 0.55F - HEAD_SIZE / 2.0F;
		};
	}

	private static void drawCrosshair(GuiGraphicsExtractor graphics, int left, int top, int size, int cx, int cy) {
		int pad = switch (shape) {
			case SQUARE -> 4;
			case CIRCLE, STAR -> Math.max(8, size / 8);
			case TRIANGLE -> Math.max(10, size / 6);
		};
		graphics.fill(cx - 1, top + pad, cx, top + size - pad, CROSSHAIR);
		graphics.fill(left + pad, cy - 1, left + size - pad, cy, CROSSHAIR);
		graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF8EC8FF);
	}

	static void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int size, int fill, int outline) {
		int cx = left + size / 2;
		int cy = top + size / 2;
		switch (shape) {
			case SQUARE -> {
				if ((fill >>> 24) != 0) {
					graphics.fill(left, top, left + size, top + size, fill);
				}
				graphics.outline(left, top, size, size, outline);
			}
			case CIRCLE -> {
				int r = size / 2 - 1;
				if ((fill >>> 24) != 0) {
					MenuShapes.fillCircle(graphics, cx, cy, r, fill);
				}
				MenuShapes.outlineCircle(graphics, cx, cy, r, outline);
			}
			case TRIANGLE -> drawTrianglePanel(graphics, left, top, size, fill, outline);
			case STAR -> drawStarPanel(graphics, left, top, size, fill, outline);
		}
	}

	private static void drawTrianglePanel(GuiGraphicsExtractor graphics, int left, int top, int size, int fill, int outline) {
		int x0 = left + size / 2;
		int y0 = top + 2;
		int x1 = left + 2;
		int y1 = top + size - 2;
		int x2 = left + size - 2;
		int y2 = top + size - 2;
		if ((fill >>> 24) != 0) {
			fillTriangle(graphics, x0, y0, x1, y1, x2, y2, fill);
		}
		drawLine(graphics, x0, y0, x1, y1, outline);
		drawLine(graphics, x1, y1, x2, y2, outline);
		drawLine(graphics, x2, y2, x0, y0, outline);
	}

	/** Six-pointed star (hexagram): upright + inverted triangle. */
	private static void drawStarPanel(GuiGraphicsExtractor graphics, int left, int top, int size, int fill, int outline) {
		int cx = left + size / 2;
		int cy = top + size / 2;
		int r = size / 2 - 2;
		// Upright
		int u0x = cx;
		int u0y = cy - r;
		int u1x = cx - (int) Math.round(r * 0.866);
		int u1y = cy + r / 2;
		int u2x = cx + (int) Math.round(r * 0.866);
		int u2y = cy + r / 2;
		// Inverted
		int d0x = cx;
		int d0y = cy + r;
		int d1x = cx - (int) Math.round(r * 0.866);
		int d1y = cy - r / 2;
		int d2x = cx + (int) Math.round(r * 0.866);
		int d2y = cy - r / 2;
		if ((fill >>> 24) != 0) {
			fillTriangle(graphics, u0x, u0y, u1x, u1y, u2x, u2y, fill);
			fillTriangle(graphics, d0x, d0y, d1x, d1y, d2x, d2y, fill);
		}
		drawLine(graphics, u0x, u0y, u1x, u1y, outline);
		drawLine(graphics, u1x, u1y, u2x, u2y, outline);
		drawLine(graphics, u2x, u2y, u0x, u0y, outline);
		drawLine(graphics, d0x, d0y, d1x, d1y, outline);
		drawLine(graphics, d1x, d1y, d2x, d2y, outline);
		drawLine(graphics, d2x, d2y, d0x, d0y, outline);
	}

	private static void fillTriangle(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
		int minX = Math.min(x0, Math.min(x1, x2));
		int maxX = Math.max(x0, Math.max(x1, x2));
		int minY = Math.min(y0, Math.min(y1, y2));
		int maxY = Math.max(y0, Math.max(y1, y2));
		for (int y = minY; y <= maxY; y++) {
			int rowMin = maxX + 1;
			int rowMax = minX - 1;
			for (int x = minX; x <= maxX; x++) {
				if (pointInTriangle(x, y, x0, y0, x1, y1, x2, y2)) {
					rowMin = Math.min(rowMin, x);
					rowMax = Math.max(rowMax, x);
				}
			}
			if (rowMin <= rowMax) {
				graphics.fill(rowMin, y, rowMax + 1, y + 1, color);
			}
		}
	}

	private static boolean pointInTriangle(int px, int py, int x0, int y0, int x1, int y1, int x2, int y2) {
		long d0 = (long) (px - x1) * (y0 - y1) - (long) (x0 - x1) * (py - y1);
		long d1 = (long) (px - x2) * (y1 - y2) - (long) (x1 - x2) * (py - y2);
		long d2 = (long) (px - x0) * (y2 - y0) - (long) (x2 - x0) * (py - y0);
		boolean hasNeg = d0 < 0 || d1 < 0 || d2 < 0;
		boolean hasPos = d0 > 0 || d1 > 0 || d2 > 0;
		return !(hasNeg && hasPos);
	}

	private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int x = x0;
		int y = y0;
		while (true) {
			graphics.fill(x, y, x + 1, y + 1, color);
			if (x == x1 && y == y1) {
				break;
			}
			int e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				x += sx;
			}
			if (e2 < dx) {
				err += dx;
				y += sy;
			}
		}
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
			if (renderer instanceof LivingEntityRenderer living) {
				boolean humanoid = renderer instanceof HumanoidMobRenderer
						|| living.getModel() instanceof HumanoidModel;
				boolean quadruped = living.getModel() instanceof QuadrupedModel;
				LivingEntityRenderState state = (LivingEntityRenderState) living.createRenderState(entity, 0.0F);
				Identifier tex = living.getTextureLocation(state);
				if (humanoid && tex != null) {
					// Same UV path as player skins: 8×8 face + hat, scaled to size.
					PlayerFaceExtractor.extractRenderState(graphics, tex, x, y, size, true, false, 0xFFFFFFFF);
					return;
				}
				// Quadruped skins do not use the steve head atlas — spawn-egg icon instead.
				if (quadruped && drawSpawnEggIcon(graphics, entity, x, y)) {
					return;
				}
				if (tex != null) {
					// Creeper / cube-head mobs: sample exactly 8×8 at UV 8,8 scaled to size.
					// (10/11-arg blit sets region=drawSize and showed the wrong texels.)
					graphics.blit(
							RenderPipelines.GUI_TEXTURED,
							tex,
							x,
							y,
							8.0F,
							8.0F,
							size,
							size,
							8,
							8,
							64,
							64,
							0xFFFFFFFF
					);
					return;
				}
			}
		} catch (Throwable ignored) {
			// Fall through
		}
		if (drawSpawnEggIcon(graphics, entity, x, y)) {
			return;
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

	private static boolean drawSpawnEggIcon(GuiGraphicsExtractor graphics, LivingEntity entity, int x, int y) {
		try {
			Optional<Holder<Item>> egg = SpawnEggItem.byId(entity.getType());
			if (egg.isEmpty()) {
				return false;
			}
			// Item icons are 16×16; nudge so they sit roughly in the 12×12 head slot.
			graphics.item(new ItemStack(egg.get().value()), x - 2, y - 2);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void drawArrowUp(GuiGraphicsExtractor graphics, int cx, int tipY, int color) {
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
