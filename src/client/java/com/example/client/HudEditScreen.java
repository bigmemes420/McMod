package com.example.client;

import com.example.client.module.RadarModule;
import com.example.client.module.ViewerRetentionModule;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Transparent overlay opened by the HUD Edit key (default Delete).
 * Drag/resize Radar and Viewer Retention instances; right-click a viewer to delete it.
 *
 * Mouse buttons use InputConstants values (LEFT=1, RIGHT=0), not GLFW 0-based indices.
 */
public class HudEditScreen extends Screen {
	private static final int HANDLE = 14;
	private static final int HIT_PAD = 2;
	private static final int BORDER = 0xFFE0C060;
	private static final int HANDLE_FILL = 0xFFFFD54A;
	private static final int VIEWER_BORDER = 0xFF60C0E0;
	private static final int DIM = 0x44000000;

	private enum Target {
		NONE,
		RADAR,
		VIEWER
	}

	private enum DragMode {
		NONE,
		MOVE,
		RESIZE
	}

	private Target target = Target.NONE;
	private DragMode drag = DragMode.NONE;
	private ViewerRetentionModule.Instance viewerTarget;
	private double grabDx;
	private double grabDy;
	private int startSize;
	private int startX;
	private int startY;

	public HudEditScreen() {
		super(Component.translatable("screen.rootymenu.hud_edit.title"));
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
		graphics.fill(0, 0, this.width, this.height, DIM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		RadarModule.drawEditFrame(graphics, BORDER, HANDLE_FILL);
		ViewerRetentionModule.drawEditFrames(graphics, VIEWER_BORDER, HANDLE_FILL);

		String hint = this.font.plainSubstrByWidth(
				Component.translatable("screen.rootymenu.hud_edit.hint").getString(),
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

	private boolean isRightClick(MouseButtonEvent event) {
		return event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();

		if (isRightClick(event)) {
			ViewerRetentionModule.Instance hit = ViewerRetentionModule.hitTest(mx, my);
			if (hit != null) {
				ViewerRetentionModule.removeInstance(hit);
				clearDrag();
				return true;
			}
			return false;
		}

		if (!isLeftClick(event)) {
			return false;
		}

		// Prefer viewers (topmost) over radar when overlapping.
		ViewerRetentionModule.Instance viewer = ViewerRetentionModule.hitTest(mx, my);
		if (viewer != null) {
			if (hitHandle(mx, my, viewer.x, viewer.y, viewer.size)) {
				beginResize(Target.VIEWER, viewer, mx, my, viewer.x, viewer.y, viewer.size);
				return true;
			}
			if (hitPanel(mx, my, viewer.x, viewer.y, viewer.size)) {
				beginMove(Target.VIEWER, viewer, mx, my, viewer.x, viewer.y);
				return true;
			}
		}

		int x = RadarModule.getHudX();
		int y = RadarModule.getHudY();
		int s = RadarModule.getHudSize();
		if (hitHandle(mx, my, x, y, s)) {
			beginResize(Target.RADAR, null, mx, my, x, y, s);
			return true;
		}
		if (hitPanel(mx, my, x, y, s)) {
			beginMove(Target.RADAR, null, mx, my, x, y);
			return true;
		}
		return false;
	}

	private void beginMove(Target t, ViewerRetentionModule.Instance viewer, double mx, double my, int x, int y) {
		target = t;
		viewerTarget = viewer;
		drag = DragMode.MOVE;
		grabDx = mx - x;
		grabDy = my - y;
		startX = x;
		startY = y;
		this.setDragging(true);
	}

	private void beginResize(
			Target t,
			ViewerRetentionModule.Instance viewer,
			double mx,
			double my,
			int x,
			int y,
			int s
	) {
		target = t;
		viewerTarget = viewer;
		drag = DragMode.RESIZE;
		grabDx = mx;
		grabDy = my;
		startSize = s;
		startX = x;
		startY = y;
		this.setDragging(true);
	}

	private void clearDrag() {
		drag = DragMode.NONE;
		target = Target.NONE;
		viewerTarget = null;
		this.setDragging(false);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (drag == DragMode.NONE || !isLeftClick(event)) {
			return false;
		}
		double mx = event.x();
		double my = event.y();
		if (drag == DragMode.MOVE) {
			int nx = (int) Math.round(mx - grabDx);
			int ny = (int) Math.round(my - grabDy);
			if (target == Target.RADAR) {
				int size = RadarModule.getHudSize();
				nx = Mth.clamp(nx, 0, Math.max(0, this.width - size));
				ny = Mth.clamp(ny, 0, Math.max(0, this.height - size));
				RadarModule.setHudLayoutLive(nx, ny, size);
			} else if (target == Target.VIEWER && viewerTarget != null) {
				int size = viewerTarget.size;
				nx = Mth.clamp(nx, 0, Math.max(0, this.width - size));
				ny = Mth.clamp(ny, 0, Math.max(0, this.height - size));
				ViewerRetentionModule.setInstanceLayoutLive(viewerTarget, nx, ny, size);
			}
			return true;
		}
		if (drag == DragMode.RESIZE) {
			int delta = (int) Math.round(Math.max(mx - grabDx, my - grabDy));
			if (target == Target.RADAR) {
				int ns = startSize + delta;
				ns = Mth.clamp(ns, RadarModule.MIN_HUD_SIZE, RadarModule.MAX_HUD_SIZE);
				ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.width - startX));
				ns = Math.min(ns, Math.max(RadarModule.MIN_HUD_SIZE, this.height - startY));
				RadarModule.setHudLayoutLive(startX, startY, ns);
			} else if (target == Target.VIEWER && viewerTarget != null) {
				int ns = startSize + delta;
				ns = Mth.clamp(ns, ViewerRetentionModule.MIN_SIZE, ViewerRetentionModule.MAX_SIZE);
				ns = Math.min(ns, Math.max(ViewerRetentionModule.MIN_SIZE, this.width - startX));
				ns = Math.min(ns, Math.max(ViewerRetentionModule.MIN_SIZE, this.height - startY));
				ViewerRetentionModule.setInstanceLayoutLive(viewerTarget, startX, startY, ns);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (drag != DragMode.NONE) {
			if (target == Target.RADAR) {
				RadarModule.setHudLayout(
						RadarModule.getHudX(),
						RadarModule.getHudY(),
						RadarModule.getHudSize()
				);
			} else if (target == Target.VIEWER) {
				ViewerRetentionModule.persistLayout();
			}
			clearDrag();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ExampleModClient.matchesHudEditKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (drag != DragMode.NONE) {
			if (target == Target.RADAR) {
				RadarModule.setHudLayout(
						RadarModule.getHudX(),
						RadarModule.getHudY(),
						RadarModule.getHudSize()
				);
			} else if (target == Target.VIEWER) {
				ViewerRetentionModule.persistLayout();
			}
			clearDrag();
		}
		super.onClose();
	}
}
