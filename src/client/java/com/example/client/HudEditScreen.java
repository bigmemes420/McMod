package com.example.client;

import com.example.client.module.RadarModule;

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
 */
public class HudEditScreen extends Screen {
	private static final int HANDLE = 10;
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
		int x = RadarModule.getHudX();
		int y = RadarModule.getHudY();
		int s = RadarModule.getHudSize();
		graphics.outline(x - 1, y - 1, s + 2, s + 2, BORDER);
		int hx = x + s - HANDLE / 2;
		int hy = y + s - HANDLE / 2;
		graphics.fill(hx, hy, hx + HANDLE, hy + HANDLE, HANDLE_FILL);
		graphics.outline(hx, hy, HANDLE, HANDLE, 0xFFFFFFFF);

		String hint = this.font.plainSubstrByWidth(
				Component.translatable("screen.modid.hud_edit.hint").getString(),
				Math.max(40, this.width - 16)
		);
		graphics.text(this.font, hint, 8, this.height - 14, 0xFFE8E8E8, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double mx = event.x();
		double my = event.y();
		int x = RadarModule.getHudX();
		int y = RadarModule.getHudY();
		int s = RadarModule.getHudSize();
		int hx = x + s - HANDLE / 2;
		int hy = y + s - HANDLE / 2;
		if (mx >= hx && my >= hy && mx < hx + HANDLE && my < hy + HANDLE) {
			drag = DragMode.RESIZE;
			grabDx = mx;
			grabDy = my;
			startSize = s;
			return true;
		}
		if (RadarModule.containsPoint(mx, my)) {
			drag = DragMode.MOVE;
			grabDx = mx - x;
			grabDy = my - y;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (drag == DragMode.NONE || event.button() != 0) {
			return super.mouseDragged(event, dragX, dragY);
		}
		double mx = event.x();
		double my = event.y();
		if (drag == DragMode.MOVE) {
			int nx = (int) Math.round(mx - grabDx);
			int ny = (int) Math.round(my - grabDy);
			nx = Mth.clamp(nx, 0, Math.max(0, this.width - RadarModule.getHudSize()));
			ny = Mth.clamp(ny, 0, Math.max(0, this.height - RadarModule.getHudSize()));
			RadarModule.setHudLayoutLive(nx, ny, RadarModule.getHudSize());
			return true;
		}
		if (drag == DragMode.RESIZE) {
			int delta = (int) Math.round(Math.max(mx - grabDx, my - grabDy));
			int ns = startSize + delta;
			int nx = RadarModule.getHudX();
			int ny = RadarModule.getHudY();
			ns = Mth.clamp(ns, RadarModule.MIN_HUD_SIZE, RadarModule.MAX_HUD_SIZE);
			ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.width - nx));
			ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.height - ny));
			RadarModule.setHudLayoutLive(nx, ny, ns);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (drag != DragMode.NONE) {
			drag = DragMode.NONE;
			RadarModule.setHudLayout(
					RadarModule.getHudX(),
					RadarModule.getHudY(),
					RadarModule.getHudSize()
			);
			return true;
		}
		return super.mouseReleased(event);
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
}
