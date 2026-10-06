package com.example.client.module;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared 2D (camera-facing) / 3D box ESP drawing via always-on-top gizmos.
 * Optional black outer stroke ("outline boxes") is drawn under the colored stroke.
 */
public final class EntityEspRenderer {
	public enum Mode {
		BOX_2D,
		BOX_3D
	}

	private static final int BLACK_OUTLINE = 0xFF000000;
	private static final float COLOR_WIDTH = 2.0F;
	private static final float BLACK_WIDTH = 4.0F;

	private EntityEspRenderer() {
	}

	public static void draw(LevelRenderer levelRenderer, Entity entity, Mode mode, int strokeArgb) {
		draw(levelRenderer, entity, mode, strokeArgb, false);
	}

	public static void draw(
			LevelRenderer levelRenderer,
			Entity entity,
			Mode mode,
			int strokeArgb,
			boolean outlineBoxes
	) {
		if (entity == null || levelRenderer == null) {
			return;
		}
		AABB box = entity.getBoundingBox();
		int stroke = ARGB.opaque(strokeArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			if (mode == Mode.BOX_3D) {
				if (outlineBoxes) {
					Gizmos.cuboid(box, GizmoStyle.stroke(BLACK_OUTLINE, BLACK_WIDTH)).setAlwaysOnTop();
				}
				Gizmos.cuboid(box, GizmoStyle.stroke(stroke, COLOR_WIDTH)).setAlwaysOnTop();
				return;
			}
			drawBillboardBox(box, stroke, outlineBoxes);
		}
	}

	/** Flat rectangle facing the camera around the entity AABB. */
	private static void drawBillboardBox(AABB box, int strokeArgb, boolean outlineBoxes) {
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		double cx = (box.minX + box.maxX) * 0.5D;
		double cy = (box.minY + box.maxY) * 0.5D;
		double cz = (box.minZ + box.maxZ) * 0.5D;
		double halfW = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.5D + 0.05D;
		double halfH = (box.maxY - box.minY) * 0.5D + 0.05D;

		Vec3 forward = new Vec3(cx - cam.x, 0.0D, cz - cam.z);
		if (forward.lengthSqr() < 1.0E-6D) {
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			forward = forward.normalize();
		}
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 center = new Vec3(cx, cy, cz);

		Vec3 bl = center.add(right.scale(-halfW)).add(up.scale(-halfH));
		Vec3 br = center.add(right.scale(halfW)).add(up.scale(-halfH));
		Vec3 tr = center.add(right.scale(halfW)).add(up.scale(halfH));
		Vec3 tl = center.add(right.scale(-halfW)).add(up.scale(halfH));

		if (outlineBoxes) {
			emitRect(bl, br, tr, tl, BLACK_OUTLINE, BLACK_WIDTH);
		}
		emitRect(bl, br, tr, tl, strokeArgb, COLOR_WIDTH);
	}

	private static void emitRect(Vec3 bl, Vec3 br, Vec3 tr, Vec3 tl, int color, float width) {
		Gizmos.line(bl, br, color, width).setAlwaysOnTop();
		Gizmos.line(br, tr, color, width).setAlwaysOnTop();
		Gizmos.line(tr, tl, color, width).setAlwaysOnTop();
		Gizmos.line(tl, bl, color, width).setAlwaysOnTop();
	}
}
