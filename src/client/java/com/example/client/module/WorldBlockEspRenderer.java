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
 * Through-world block ESP via MC 26.3 always-on-top gizmos.
 * Fills use <em>exposed faces only</em> ({@link Gizmos#rect}) so shared faces
 * between adjacent selected blocks never double-fill. Outlines use silhouette
 * edges only. Meshes are meant to be cached across frames by the caller.
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

	/**
	 * Soft cap on exposed faces per mesh (FPS). Edges are always emitted with each
	 * face (up to 4), so {@link #MAX_EDGES} is 4× faces — never leave fill without outline.
	 */
	public static final int MAX_FACES = 16_000;
	public static final int MAX_EDGES = MAX_FACES * 4;

	private WorldBlockEspRenderer() {
	}

	/** One outward face of a unit cube at block coords. */
	public record Face(int x, int y, int z, Direction direction) {
		public void draw(GizmoStyle style) {
			Gizmos.rect(
					new Vec3(x, y, z),
					new Vec3(x + 1, y + 1, z + 1),
					direction,
					style
			).setAlwaysOnTop();
		}
	}

	/** Silhouette edge segment in world space. */
	public record Edge(float x0, float y0, float z0, float x1, float y1, float z1) {
		public void draw(int strokeArgb, float strokeWidth) {
			Gizmos.line(
					new Vec3(x0, y0, z0),
					new Vec3(x1, y1, z1),
					strokeArgb,
					strokeWidth
			).setAlwaysOnTop();
		}
	}

	/** Cached exposed faces + silhouette edges for a connected selection. */
	public static final class Mesh {
		public final List<Face> faces;
		public final List<Edge> edges;

		public Mesh(List<Face> faces, List<Edge> edges) {
			this.faces = faces;
			this.edges = edges;
		}

		public boolean isEmpty() {
			return faces.isEmpty() && edges.isEmpty();
		}
	}

	/** Build exposed-face fill + silhouette edges. Skips internal shared faces. */
	public static Mesh buildMesh(List<BlockPos> positions) {
		if (positions.isEmpty()) {
			return new Mesh(List.of(), List.of());
		}
		Set<Long> set = new HashSet<>(Math.max(16, positions.size() * 2));
		for (BlockPos p : positions) {
			set.add(p.asLong());
		}

		List<Face> faces = new ArrayList<>(Math.min(positions.size() * 3, MAX_FACES));
		List<Edge> edges = new ArrayList<>(Math.min(positions.size() * 6, MAX_EDGES));

		outer:
		for (BlockPos pos : positions) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			for (Direction face : FACES) {
				if (set.contains(pos.relative(face).asLong())) {
					continue; // internal — neighbor in selection
				}
				// Room for this face + up to 4 silhouette edges — never add fill alone.
				if (faces.size() >= MAX_FACES || edges.size() + 4 > MAX_EDGES) {
					break outer;
				}
				int edgeBefore = edges.size();
				collectExposedFaceEdges(pos, face, set, edges);
				faces.add(new Face(x, y, z, face));
				// If somehow edges overflowed past budget, roll back the unpaired face.
				if (edges.size() > MAX_EDGES) {
					edges.subList(edgeBefore, edges.size()).clear();
					faces.remove(faces.size() - 1);
					break outer;
				}
			}
		}
		return new Mesh(faces, edges);
	}

	public static void drawMeshFill(LevelRenderer levelRenderer, Mesh mesh, int fillArgb) {
		if (mesh == null || mesh.faces.isEmpty()) {
			return;
		}
		GizmoStyle style = GizmoStyle.fill(fillArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (Face face : mesh.faces) {
				face.draw(style);
			}
		}
	}

	public static void drawMeshEdges(LevelRenderer levelRenderer, Mesh mesh, int strokeArgb, float strokeWidth) {
		if (mesh == null || mesh.edges.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		int stroke = ARGB.opaque(strokeArgb);
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			for (Edge edge : mesh.edges) {
				edge.draw(stroke, width);
			}
		}
	}

	/** Fill exposed faces + silhouette edges in one gizmo pass. */
	public static void drawMesh(
			LevelRenderer levelRenderer,
			Mesh mesh,
			int fillArgb,
			int strokeArgb,
			float strokeWidth,
			boolean drawFill,
			boolean drawEdges
	) {
		if (mesh == null || mesh.isEmpty()) {
			return;
		}
		float width = Math.max(0.5F, strokeWidth);
		int stroke = ARGB.opaque(strokeArgb);
		GizmoStyle fillStyle = drawFill ? GizmoStyle.fill(fillArgb) : null;
		try (var ignored = levelRenderer.collectPerFrameRenderThreadGizmos()) {
			if (drawFill && fillStyle != null) {
				for (Face face : mesh.faces) {
					face.draw(fillStyle);
				}
			}
			if (drawEdges) {
				for (Edge edge : mesh.edges) {
					edge.draw(stroke, width);
				}
			}
		}
	}

	/**
	 * Combined Fill: per block-type mesh — exposed-face fill (no internal seams)
	 * + silhouette edges. Prefer {@link #buildMesh} + cache via Finder.
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
				Mesh mesh = buildMesh(entry.getValue());
				int fill = fillColorFor.apply(entry.getKey());
				int stroke = ARGB.opaque(strokeColorFor.apply(entry.getKey()));
				GizmoStyle fillStyle = GizmoStyle.fill(fill);
				for (Face face : mesh.faces) {
					face.draw(fillStyle);
				}
				for (Edge edge : mesh.edges) {
					edge.draw(stroke, width);
				}
			}
		}
	}

	/** Group positions by a key, build one mesh per group (for Finder cache). */
	public static <K> Map<K, Mesh> buildMeshesByKey(List<BlockPos> positions, Function<BlockPos, K> keyAt) {
		Map<K, List<BlockPos>> groups = new HashMap<>();
		for (BlockPos pos : positions) {
			K key = keyAt.apply(pos);
			if (key == null) {
				continue;
			}
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
		}
		Map<K, Mesh> out = new HashMap<>(groups.size() * 2);
		for (Map.Entry<K, List<BlockPos>> e : groups.entrySet()) {
			out.put(e.getKey(), buildMesh(e.getValue()));
		}
		return out;
	}

	public static void drawOutlines(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb, float strokeWidth) {
		drawMesh(levelRenderer, buildMesh(positions), 0, strokeArgb, strokeWidth, false, true);
	}

	public static void drawOutlinesMerged(LevelRenderer levelRenderer, List<BlockPos> positions, int strokeArgb, float strokeWidth) {
		drawOutlines(levelRenderer, positions, strokeArgb, strokeWidth);
	}

	public static void drawFilled(
			LevelRenderer levelRenderer,
			List<BlockPos> positions,
			int strokeArgb,
			int fillArgb,
			float strokeWidth
	) {
		drawMesh(levelRenderer, buildMesh(positions), fillArgb, strokeArgb, strokeWidth, true, true);
	}

	public static void drawFilledMerged(
			LevelRenderer levelRenderer,
			List<BlockPos> positions,
			int strokeArgb,
			int fillArgb,
			float strokeWidth
	) {
		drawFilled(levelRenderer, positions, strokeArgb, fillArgb, strokeWidth);
	}

	public static void drawFilledOnly(LevelRenderer levelRenderer, List<BlockPos> positions, int fillArgb) {
		drawMesh(levelRenderer, buildMesh(positions), fillArgb, 0xFFFFFFFF, 1.0F, true, false);
	}

	/** Legacy AABB fill (may seam); Finder uses {@link Mesh}. */
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

	private static void collectExposedFaceEdges(BlockPos pos, Direction face, Set<Long> set, List<Edge> edges) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		switch (face) {
			case UP -> {
				maybeEdge(set, pos, Direction.NORTH, face, edges, x, y + 1, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, edges, x, y + 1, z + 1, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, edges, x, y + 1, z, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, edges, x + 1, y + 1, z, x + 1, y + 1, z + 1);
			}
			case DOWN -> {
				maybeEdge(set, pos, Direction.NORTH, face, edges, x, y, z, x + 1, y, z);
				maybeEdge(set, pos, Direction.SOUTH, face, edges, x, y, z + 1, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, edges, x, y, z, x, y, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, edges, x + 1, y, z, x + 1, y, z + 1);
			}
			case NORTH -> {
				maybeEdge(set, pos, Direction.DOWN, face, edges, x, y, z, x + 1, y, z);
				maybeEdge(set, pos, Direction.UP, face, edges, x, y + 1, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.WEST, face, edges, x, y, z, x, y + 1, z);
				maybeEdge(set, pos, Direction.EAST, face, edges, x + 1, y, z, x + 1, y + 1, z);
			}
			case SOUTH -> {
				maybeEdge(set, pos, Direction.DOWN, face, edges, x, y, z + 1, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, edges, x, y + 1, z + 1, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.WEST, face, edges, x, y, z + 1, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.EAST, face, edges, x + 1, y, z + 1, x + 1, y + 1, z + 1);
			}
			case WEST -> {
				maybeEdge(set, pos, Direction.DOWN, face, edges, x, y, z, x, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, edges, x, y + 1, z, x, y + 1, z + 1);
				maybeEdge(set, pos, Direction.NORTH, face, edges, x, y, z, x, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, edges, x, y, z + 1, x, y + 1, z + 1);
			}
			case EAST -> {
				maybeEdge(set, pos, Direction.DOWN, face, edges, x + 1, y, z, x + 1, y, z + 1);
				maybeEdge(set, pos, Direction.UP, face, edges, x + 1, y + 1, z, x + 1, y + 1, z + 1);
				maybeEdge(set, pos, Direction.NORTH, face, edges, x + 1, y, z, x + 1, y + 1, z);
				maybeEdge(set, pos, Direction.SOUTH, face, edges, x + 1, y, z + 1, x + 1, y + 1, z + 1);
			}
		}
	}

	private static void maybeEdge(
			Set<Long> set,
			BlockPos pos,
			Direction edgeDir,
			Direction face,
			List<Edge> edges,
			double x0,
			double y0,
			double z0,
			double x1,
			double y1,
			double z1
	) {
		BlockPos neighbor = pos.relative(edgeDir);
		// Shared boundary with neighbor that also exposes this face → one edge only (skip duplicate).
		if (set.contains(neighbor.asLong()) && !set.contains(neighbor.relative(face).asLong())) {
			return;
		}
		edges.add(new Edge((float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1));
	}

	/**
	 * Flood-fill connected components, then greedily pack solid AABBs.
	 * Kept for tools that still want solid volumes; Finder fill no longer uses this
	 * for translucent overlays (seams).
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

	public static int fillWithAlpha(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
		return ARGB.color(a, rgb);
	}
}
