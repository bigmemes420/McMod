package com.example.client.module;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Draws through-world block ESP using MC 26.3 always-on-top gizmos.
 * Prefer merged AABB draws ({@link #drawOutlinesMerged}, {@link #drawFilledMerged})
 * to keep gizmo count low for FPS.
 */
public final class WorldBlockEspRenderer {
	private static final BlockPos[] FACE_OFFSETS = {
			new BlockPos(1, 0, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(0, 1, 0),
			new BlockPos(0, -1, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(0, 0, -1)
	};

	private static final Direction[] FACES = Direction.values();

	private WorldBlockEspRenderer() {
	}

	public static void drawOutlines(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb, float strokeWidth) {
		drawOutlinesMerged(levelRenderer, positions, strokeArgb, strokeWidth);
	}

	/** Stroke merged solid AABBs (far fewer gizmos than per-block cuboids). */
	public static void drawOutlinesMerged(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb, float strokeWidth) {
		if (positions.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		GizmoStyle style = GizmoStyle.stroke(ARGB.opaque(strokeArgb), width);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : mergeConnectedSolid(positions)) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	public static void drawFilled(
			LevelRenderer levelRenderer,
			List<BlockPos> positions,
			int strokeArgb,
			int fillArgb,
			float strokeWidth
	) {
		drawFilledMerged(levelRenderer, positions, strokeArgb, fillArgb, strokeWidth);
	}

	public static void drawFilledMerged(
			LevelRenderer levelRenderer,
			List<BlockPos> positions,
			int strokeArgb,
			int fillArgb,
			float strokeWidth
	) {
		if (positions.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		GizmoStyle style = GizmoStyle.strokeAndFill(ARGB.opaque(strokeArgb), width, fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : mergeConnectedSolid(positions)) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	/** Fill-only (no outline stroke) per block position. */
	public static void drawFilledOnly(LevelRenderer levelRenderer, List<BlockPos> positions, int fillArgb) {
		if (positions.isEmpty()) {
			return;
		}
		GizmoStyle style = GizmoStyle.fill(fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : mergeConnectedSolid(positions)) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	/** Fill-only for pre-merged AABBs (X-Ray ghost / cached Finder). */
	public static void drawFilledAabbs(LevelRenderer levelRenderer, List<AABB> boxes, int fillArgb) {
		if (boxes.isEmpty()) {
			return;
		}
		GizmoStyle style = GizmoStyle.fill(fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : boxes) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	/** Stroke-only for pre-merged AABBs. */
	public static void drawOutlineAabbs(LevelRenderer levelRenderer, List<AABB> boxes, int strokeArgb, float strokeWidth) {
		if (boxes.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		GizmoStyle style = GizmoStyle.stroke(ARGB.opaque(strokeArgb), width);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : boxes) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	/** Stroke+fill for pre-merged AABBs. */
	public static void drawFilledAabbs(
			LevelRenderer levelRenderer,
			List<AABB> boxes,
			int strokeArgb,
			int fillArgb,
			float strokeWidth
	) {
		if (boxes.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		GizmoStyle style = GizmoStyle.strokeAndFill(ARGB.opaque(strokeArgb), width, fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (AABB box : boxes) {
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
		}
	}

	/**
	 * Combined Fill: merge face-connected same-type blocks into solid AABBs for
	 * the fill, then stroke only external silhouette edges.
	 */
	public static void drawCombinedFill(
			LevelRenderer levelRenderer,
			List<BlockPos> positions,
			Function<BlockPos, Identifier> idAt,
			Function<Identifier, Integer> fillColorFor,
			Function<Identifier, Integer> strokeColorFor,
			float strokeWidth
	) {
		if (positions.isEmpty()) {
			return;
		}

		Map<Identifier, List<BlockPos>> byType = new HashMap<>();
		for (BlockPos pos : positions) {
			Identifier id = idAt.apply(pos);
			if (id == null) {
				continue;
			}
			byType.computeIfAbsent(id, k -> new ArrayList<>()).add(pos);
		}

		float width = Math.max(0.5F, strokeWidth);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (Map.Entry<Identifier, List<BlockPos>> entry : byType.entrySet()) {
				int fill = fillColorFor.apply(entry.getKey());
				int stroke = ARGB.opaque(strokeColorFor.apply(entry.getKey()));
				GizmoStyle fillStyle = GizmoStyle.fill(fill);

				List<BlockPos> group = entry.getValue();
				for (AABB box : mergeConnectedSolid(group)) {
					Gizmos.cuboid(box, fillStyle).setAlwaysOnTop();
				}
				drawSilhouetteEdges(group, stroke, width);
			}
		}
	}

	private static void drawSilhouetteEdges(List<BlockPos> group, int strokeArgb, float strokeWidth) {
		Set<Long> set = new HashSet<>(group.size() * 2);
		for (BlockPos p : group) {
			set.add(p.asLong());
		}
		for (BlockPos pos : group) {
			for (Direction face : FACES) {
				if (set.contains(pos.relative(face).asLong())) {
					continue;
				}
				emitExposedFaceEdges(pos, face, set, strokeArgb, strokeWidth);
			}
		}
	}

	private static void emitExposedFaceEdges(
			BlockPos pos,
			Direction face,
			Set<Long> set,
			int strokeArgb,
			float strokeWidth
	) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		switch (face) {
			case UP -> {
				maybeEdge(set, pos, Direction.NORTH, face, strokeArgb, strokeWidth, x, y + 1, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, strokeArgb, strokeWidth, x, y + 1, z + 1, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, strokeArgb, strokeWidth, x, y + 1, z, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, strokeArgb, strokeWidth, x + 1, y + 1, z, x + 1, y + 1, z + 1);
			}
			case DOWN -> {
				maybeEdge(set, pos, Direction.NORTH, face, strokeArgb, strokeWidth, x, y, z, x + 1, y, z);
				maybeEdge(set, pos, Direction.SOUTH, face, strokeArgb, strokeWidth, x, y, z + 1, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, strokeArgb, strokeWidth, x, y, z, x, y, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, strokeArgb, strokeWidth, x + 1, y, z, x + 1, y, z + 1);
			}
			case NORTH -> {
				maybeEdge(set, pos, Direction.DOWN, face, strokeArgb, strokeWidth, x, y, z, x + 1, y, z);
				maybeEdge(set, pos, Direction.UP, face, strokeArgb, strokeWidth, x, y + 1, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.WEST, face, strokeArgb, strokeWidth, x, y, z, x, y + 1, z);
				maybeEdge(set, pos, Direction.EAST, face, strokeArgb, strokeWidth, x + 1, y, z, x + 1, y + 1, z);
			}
			case SOUTH -> {
				maybeEdge(set, pos, Direction.DOWN, face, strokeArgb, strokeWidth, x, y, z + 1, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, strokeArgb, strokeWidth, x, y + 1, z + 1, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, strokeArgb, strokeWidth, x, y, z + 1, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, strokeArgb, strokeWidth, x + 1, y, z + 1, x + 1, y + 1, z + 1);
			}
			case WEST -> {
				maybeEdge(set, pos, Direction.DOWN, face, strokeArgb, strokeWidth, x, y, z, x, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, strokeArgb, strokeWidth, x, y + 1, z, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.NORTH, face, strokeArgb, strokeWidth, x, y, z, x, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, strokeArgb, strokeWidth, x, y, z + 1, x, y + 1, z + 1);
			}
			case EAST -> {
				maybeEdge(set, pos, Direction.DOWN, face, strokeArgb, strokeWidth, x + 1, y, z, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, strokeArgb, strokeWidth, x + 1, y + 1, z, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.NORTH, face, strokeArgb, strokeWidth, x + 1, y, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, strokeArgb, strokeWidth, x + 1, y, z + 1, x + 1, y + 1, z + 1);
			}
		}
	}

	private static void maybeEdge(
			Set<Long> set,
			BlockPos pos,
			Direction edgeDir,
			Direction face,
			int strokeArgb,
			float strokeWidth,
			double x0,
			double y0,
			double z0,
			double x1,
			double y1,
			double z1
	) {
		BlockPos neighbor = pos.relative(edgeDir);
		if (set.contains(neighbor.asLong()) && !set.contains(neighbor.relative(face).asLong())) {
			return;
		}
		Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), strokeArgb, strokeWidth).setAlwaysOnTop();
	}

	/**
	 * Flood-fill connected components, then greedily expand each into the
	 * largest solid AABB fully contained in the component.
	 */
	public static List<AABB> mergeConnectedSolid(List<BlockPos> positions) {
		if (positions.isEmpty()) {
			return List.of();
		}
		Set<Long> all = new HashSet<>(positions.size() * 2);
		for (BlockPos p : positions) {
			all.add(p.asLong());
		}
		Set<Long> visited = new HashSet<>();
		List<AABB> out = new ArrayList<>();

		for (BlockPos seed : positions) {
			long seedKey = seed.asLong();
			if (!visited.add(seedKey)) {
				continue;
			}
			List<BlockPos> component = new ArrayList<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(seed);
			component.add(seed);
			while (!queue.isEmpty()) {
				BlockPos cur = queue.removeFirst();
				for (BlockPos off : FACE_OFFSETS) {
					BlockPos n = cur.offset(off);
					long nk = n.asLong();
					if (all.contains(nk) && visited.add(nk)) {
						queue.add(n);
						component.add(n);
					}
				}
			}
			out.addAll(packSolidAabbs(component));
		}
		return out;
	}

	private static List<AABB> packSolidAabbs(List<BlockPos> component) {
		Set<Long> remaining = new HashSet<>(component.size() * 2);
		for (BlockPos p : component) {
			remaining.add(p.asLong());
		}
		List<AABB> boxes = new ArrayList<>();
		while (!remaining.isEmpty()) {
			long key = remaining.iterator().next();
			BlockPos start = BlockPos.of(key);
			remaining.remove(key);
			int minX = start.getX();
			int minY = start.getY();
			int minZ = start.getZ();
			int maxX = start.getX();
			int maxY = start.getY();
			int maxZ = start.getZ();

			boolean grew;
			do {
				grew = false;
				if (slicePresent(remaining, maxX + 1, maxX + 1, minY, maxY, minZ, maxZ)) {
					removeSlice(remaining, maxX + 1, maxX + 1, minY, maxY, minZ, maxZ);
					maxX++;
					grew = true;
				}
				if (slicePresent(remaining, minX - 1, minX - 1, minY, maxY, minZ, maxZ)) {
					removeSlice(remaining, minX - 1, minX - 1, minY, maxY, minZ, maxZ);
					minX--;
					grew = true;
				}
				if (slicePresent(remaining, minX, maxX, maxY + 1, maxY + 1, minZ, maxZ)) {
					removeSlice(remaining, minX, maxX, maxY + 1, maxY + 1, minZ, maxZ);
					maxY++;
					grew = true;
				}
				if (slicePresent(remaining, minX, maxX, minY - 1, minY - 1, minZ, maxZ)) {
					removeSlice(remaining, minX, maxX, minY - 1, minY - 1, minZ, maxZ);
					minY--;
					grew = true;
				}
				if (slicePresent(remaining, minX, maxX, minY, maxY, maxZ + 1, maxZ + 1)) {
					removeSlice(remaining, minX, maxX, minY, maxY, maxZ + 1, maxZ + 1);
					maxZ++;
					grew = true;
				}
				if (slicePresent(remaining, minX, maxX, minY, maxY, minZ - 1, minZ - 1)) {
					removeSlice(remaining, minX, maxX, minY, maxY, minZ - 1, minZ - 1);
					minZ--;
					grew = true;
				}
			} while (grew);

			boxes.add(AABB.encapsulatingFullBlocks(
					new BlockPos(minX, minY, minZ),
					new BlockPos(maxX, maxY, maxZ)
			));
		}
		return boxes;
	}

	private static boolean slicePresent(Set<Long> set, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (!set.contains(BlockPos.asLong(x, y, z))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void removeSlice(Set<Long> set, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					set.remove(BlockPos.asLong(x, y, z));
				}
			}
		}
	}

	/** Fill color with explicit alpha 0–1. */
	public static int fillWithAlpha(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
		return ARGB.color(a, rgb);
	}
}
