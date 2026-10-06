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
 */
public final class EntityEspRenderer {
	public enum Mode {
		BOX_2D,
		BOX_3D
	}

	private EntityEspRenderer() {
	}

	public static void draw(LevelRenderer levelRenderer, Entity entity, Mode mode, int strokeArgb) {
		if (entity == null || levelRenderer == null) {
			return;
		}
		AABB box = entity.getBoundingBox();
		int stroke = ARGB.opaque(strokeArgb);
		float width = 2.0F;
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			if (mode == Mode.BOX_3D) {
				Gizmos.cuboid(box, GizmoStyle.stroke(stroke, width)).setAlwaysOnTop();
				return;
			}
			drawBillboardBox(box, stroke, width);
		}
	}

	/** Flat rectangle facing the camera around the entity AABB. */
	private static void drawBillboardBox(AABB box, int strokeArgb, float strokeWidth) {
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

		Gizmos.line(bl, br, strokeArgb, strokeWidth).setAlwaysOnTop();
		Gizmos.line(br, tr, strokeArgb, strokeWidth).setAlwaysOnTop();
		Gizmos.line(tr, tl, strokeArgb, strokeWidth).setAlwaysOnTop();
		Gizmos.line(tl, bl, strokeArgb, strokeWidth).setAlwaysOnTop();
	}
}
