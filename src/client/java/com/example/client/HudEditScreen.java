package com.example.client;

import com.example.client.module.RadarModule;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Transparent overlay opened by the HUD Edit key (default Delete).
 * Toggle on key press while in-game; Delete or Escape exits.
 * Lets the player drag/resize Radar (and future HUD widgets).
 *
 * Mouse buttons use InputConstants values (LEFT=1), not GLFW 0-based indices.
 */
public class HudEditScreen extends Screen {
	private static final int HANDLE = 14;
	private static final int HIT_PAD = 2;
	private static final int BORDER = 0xFFE0C060;
	private static final int HANDLE_FILL = 0xFFFFD54A;
	private static final int DIM = 0x44000000;

	private enum DragMode {
		NONE,
		MOVE,
		RESIZE
	}

	private DragMode drag = DragMode.NONE;
	private double grabDx;
	private double grabDy;
	private int startSize;
	private int startX;
	private int startY;

	public HudEditScreen() {
		super(Component.translatable("screen.modid.hud_edit.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Light dim only — world stays visible underneath.
		graphics.fill(0, 0, this.width, this.height, DIM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		// Radar frame always editable here (even if module toggled off).
		RadarModule.drawEditFrame(graphics, BORDER, HANDLE_FILL);

		String hint = this.font.plainSubstrByWidth(
				Component.translatable("screen.modid.hud_edit.hint").getString(),
				Math.max(40, this.width - 16)
		);
		graphics.text(this.font, hint, 8, this.height - 14, 0xFFE8E8E8, true);
	}

	private static int handleX(int panelX, int size) {
		return panelX + size - HANDLE / 2;
	}

	private static int handleY(int panelY, int size) {
		return panelY + size - HANDLE / 2;
	}

	private boolean hitHandle(double mx, double my, int x, int y, int s) {
		int hx = handleX(x, s) - HIT_PAD;
		int hy = handleY(y, s) - HIT_PAD;
		int hs = HANDLE + HIT_PAD * 2;
		return mx >= hx && my >= hy && mx < hx + hs && my < hy + hs;
	}

	private boolean hitPanel(double mx, double my, int x, int y, int s) {
		return mx >= x - HIT_PAD && my >= y - HIT_PAD
				&& mx < x + s + HIT_PAD && my < y + s + HIT_PAD;
	}

	private boolean isLeftClick(MouseButtonEvent event) {
		return event.button() == InputConstants.MOUSE_BUTTON_LEFT;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isLeftClick(event)) {
			return false;
		}
		double mx = event.x();
		double my = event.y();
		int x = RadarModule.getHudX();
		int y = RadarModule.getHudY();
		int s = RadarModule.getHudSize();

		if (hitHandle(mx, my, x, y, s)) {
			drag = DragMode.RESIZE;
			grabDx = mx;
			grabDy = my;
			startSize = s;
			startX = x;
			startY = y;
			this.setDragging(true);
			return true;
		}
		if (hitPanel(mx, my, x, y, s)) {
			drag = DragMode.MOVE;
			grabDx = mx - x;
			grabDy = my - y;
			startX = x;
			startY = y;
			this.setDragging(true);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (drag == DragMode.NONE || !isLeftClick(event)) {
			return false;
		}
		// event.x/y are already GUI-scaled (MouseHandler.getScaledXPos/YPos).
		double mx = event.x();
		double my = event.y();
		if (drag == DragMode.MOVE) {
			int nx = (int) Math.round(mx - grabDx);
			int ny = (int) Math.round(my - grabDy);
			int size = RadarModule.getHudSize();
			nx = Mth.clamp(nx, 0, Math.max(0, this.width - size));
			ny = Mth.clamp(ny, 0, Math.max(0, this.height - size));
			RadarModule.setHudLayoutLive(nx, ny, size);
			return true;
		}
		if (drag == DragMode.RESIZE) {
			// Grow from bottom-right; use the larger axis delta so diagonal drags feel natural.
			int delta = (int) Math.round(Math.max(mx - grabDx, my - grabDy));
			int ns = startSize + delta;
			ns = Mth.clamp(ns, RadarModule.MIN_HUD_SIZE, RadarModule.MAX_HUD_SIZE);
			ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.width - startX));
			ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.height - startY));
			RadarModule.setHudLayoutLive(startX, startY, ns);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (drag != DragMode.NONE) {
			drag = DragMode.NONE;
			this.setDragging(false);
			RadarModule.setHudLayout(
					RadarModule.getHudX(),
					RadarModule.getHudY(),
					RadarModule.getHudSize()
			);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Delete toggles edit mode off (same key that opened us).
		if (ExampleModClient.matchesHudEditKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (drag != DragMode.NONE) {
			drag = DragMode.NONE;
			this.setDragging(false);
			RadarModule.setHudLayout(
					RadarModule.getHudX(),
					RadarModule.getHudY(),
					RadarModule.getHudSize()
			);
		}
		super.onClose();
	}
}
