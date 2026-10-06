package com.example.client.module;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * Draws through-world block ESP using MC 26.3 always-on-top gizmos.
 * Call from {@code LevelRenderEvents.BEFORE_GIZMOS} so boxes are finalized
 * into the current frame's always-on-top pass — no chunk remesh required.
 */
public final class WorldBlockEspRenderer {
	private WorldBlockEspRenderer() {
	}

	public static void drawOutlines(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb) {
		if (positions.isEmpty()) {
			return;
		}
		GizmoStyle style = GizmoStyle.stroke(ARGB.opaque(strokeArgb), 2.0F);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (BlockPos pos : positions) {
				Gizmos.cuboid(pos, style).setAlwaysOnTop();
			}
		}
	}

	public static void drawFilled(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb, int fillArgb) {
		if (positions.isEmpty()) {
			return;
		}
		GizmoStyle style = GizmoStyle.strokeAndFill(ARGB.opaque(strokeArgb), 2.0F, fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (BlockPos pos : positions) {
				Gizmos.cuboid(pos, style).setAlwaysOnTop();
			}
		}
	}

	/** Fill color with explicit alpha 0–1 (for live X-Ray opacity). */
	public static int fillWithAlpha(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
		return ARGB.color(a, rgb);
	}
}
