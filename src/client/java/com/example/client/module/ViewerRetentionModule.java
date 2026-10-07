package com.example.client.module;

import com.example.ExampleMod;
import com.example.client.config.ModConfig;
import com.example.client.util.GifDecoder;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Misc: place PNG/JPEG/GIF viewers on the HUD from {@code <gameDir>/Media/}.
 * GIFs animate with Graphic Control delays (same approach as Custom Crosshair).
 * JPEG/PNG load as stills; MP4/WebM are not decoded (no video decoder on classpath).
 * Layout via HudEditScreen (Delete key): drag/resize; right-click deletes an instance.
 */
public final class ViewerRetentionModule {
	public static final String MEDIA_FOLDER = "Media";
	public static final int DEFAULT_SIZE = 96;
	public static final int MIN_SIZE = 32;
	public static final int MAX_SIZE = 512;
	public static final int DEFAULT_X = 16;
	public static final int DEFAULT_Y = 120;

	private static final AtomicInteger ID_SEQ = new AtomicInteger(1);

	public static final class Instance {
		public final int id;
		public String file;
		public int x;
		public int y;
		public int size;

		transient DynamicTexture texture;
		transient NativeImage imagePixels;
		transient Identifier textureId;
		transient int texW;
		transient int texH;
		transient boolean ready;
		transient int[][] gifFrames;
		transient int[] gifDelaysMs;
		transient int gifIndex;
		transient float gifAccumSec;
		transient boolean gifAnimated;

		Instance(String file, int x, int y, int size) {
			this.id = ID_SEQ.getAndIncrement();
			this.file = file;
			this.x = x;
			this.y = y;
			this.size = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
			this.textureId = ExampleMod.id("dynamic/viewer_retention_" + this.id);
		}
	}

	private static boolean enabled;
	private static final List<Instance> instances = new ArrayList<>();
	private static List<String> cachedMediaList = List.of();
	private static String menuSelectedFile = "";
	private static String statusMessage = "";

	private ViewerRetentionModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.modid.menu.misc.viewer_retention.enabled"
						: "screen.modid.menu.misc.viewer_retention.disabled"
		);
		if (enabled) {
			ensureAllLoaded();
		}
		ModConfig.save();
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static String getStatusMessage() {
		return statusMessage == null ? "" : statusMessage;
	}

	public static String getMenuSelectedFile() {
		return menuSelectedFile == null ? "" : menuSelectedFile;
	}

	public static void setMenuSelectedFile(String name) {
		menuSelectedFile = name == null ? "" : name;
	}

	public static List<Instance> getInstances() {
		return Collections.unmodifiableList(instances);
	}

	public static Path mediaDir() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.gameDirectory != null) {
			return client.gameDirectory.toPath().resolve(MEDIA_FOLDER);
		}
		return FabricLoader.getInstance().getGameDir().resolve(MEDIA_FOLDER);
	}

	public static void ensureMediaDir() {
		try {
			Files.createDirectories(mediaDir());
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to create Media dir: {}", e.toString());
		}
	}

	public static boolean isCompatibleFilename(String name) {
		if (name == null) {
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif");
	}

	public static boolean isJpegFilename(String name) {
		if (name == null) {
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		return n.endsWith(".jpg") || n.endsWith(".jpeg");
	}

	/** Extension-only scan of Media/ for PNG, JPEG, GIF. */
	public static List<String> refreshMediaList() {
		ensureMediaDir();
		Path dir = mediaDir();
		List<String> found = new ArrayList<>();
		try (var stream = Files.list(dir)) {
			stream.filter(Files::isRegularFile)
					.map(p -> p.getFileName().toString())
					.filter(ViewerRetentionModule::isCompatibleFilename)
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.forEach(found::add);
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to list Media/: {}", e.toString());
		}
		cachedMediaList = Collections.unmodifiableList(found);
		statusMessage = found.isEmpty()
				? "No PNG/JPEG/GIF in Media/"
				: found.size() + " file(s) in Media/";
		if (menuSelectedFile.isBlank() && !found.isEmpty()) {
			menuSelectedFile = found.getFirst();
		} else if (!found.isEmpty() && found.stream().noneMatch(f -> f.equalsIgnoreCase(menuSelectedFile))) {
			menuSelectedFile = found.getFirst();
		}
		return cachedMediaList;
	}

	public static List<String> getMediaList() {
		if (cachedMediaList.isEmpty()) {
			return refreshMediaList();
		}
		return cachedMediaList;
	}

	/** Place the currently selected Media file on the HUD. */
	public static boolean addSelectedInstance() {
		String name = menuSelectedFile;
		if (name == null || name.isBlank()) {
			List<String> list = getMediaList();
			if (list.isEmpty()) {
				statusMessage = "No PNG/JPEG/GIF in Media/ — drop files then Refresh";
				return false;
			}
			name = list.getFirst();
		}
		return addInstance(name);
	}

	public static boolean addInstance(String filename) {
		if (!isCompatibleFilename(filename)) {
			statusMessage = "Not a PNG/JPEG/GIF: " + filename;
			return false;
		}
		String name = Path.of(filename).getFileName().toString();
		Path path = mediaDir().resolve(name).normalize();
		if (!path.startsWith(mediaDir().normalize()) || !Files.isRegularFile(path)) {
			statusMessage = "Missing " + name + " in Media/";
			return false;
		}
		int offset = instances.size() * 12;
		Instance inst = new Instance(
				name,
				DEFAULT_X + offset,
				DEFAULT_Y + offset,
				DEFAULT_SIZE
		);
		if (!loadMedia(inst, path)) {
			statusMessage = "Failed to decode " + name;
			return false;
		}
		instances.add(inst);
		statusMessage = "Added " + name;
		ModConfig.save();
		return true;
	}

	public static void removeInstance(Instance inst) {
		if (inst == null) {
			return;
		}
		releaseInstance(inst);
		instances.remove(inst);
		ModConfig.save();
	}

	public static void removeInstanceAt(int index) {
		if (index < 0 || index >= instances.size()) {
			return;
		}
		removeInstance(instances.get(index));
	}

	/** Topmost instance under point (last in list wins). */
	public static Instance hitTest(double mx, double my) {
		for (int i = instances.size() - 1; i >= 0; i--) {
			Instance inst = instances.get(i);
			if (mx >= inst.x && my >= inst.y && mx < inst.x + inst.size && my < inst.y + inst.size) {
				return inst;
			}
		}
		return null;
	}

	public static void setInstanceLayoutLive(Instance inst, int x, int y, int size) {
		if (inst == null) {
			return;
		}
		inst.x = Math.max(0, x);
		inst.y = Math.max(0, y);
		inst.size = Mth.clamp(size, MIN_SIZE, MAX_SIZE);
	}

	public static void persistLayout() {
		ModConfig.save();
	}

	public static void drawEditFrames(GuiGraphicsExtractor graphics, int borderColor, int handleFill) {
		int handle = 14;
		for (Instance inst : instances) {
			graphics.outline(inst.x - 1, inst.y - 1, inst.size + 2, inst.size + 2, borderColor);
			int hx = inst.x + inst.size - handle / 2;
			int hy = inst.y + inst.size - handle / 2;
			graphics.fill(hx, hy, hx + handle, hy + handle, handleFill);
			graphics.outline(hx, hy, handle, handle, 0xFFFFFFFF);
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				String label = inst.file;
				if (client.font != null) {
					label = client.font.plainSubstrByWidth(label, Math.max(20, inst.size - 4));
				}
				graphics.text(client.font, label, inst.x + 2, inst.y + 2, 0xFFE8E8E8, true);
			}
		}
	}

	public static void extractHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) {
			return;
		}
		var screen = client.gui.screen();
		if (screen != null && !(screen instanceof com.example.client.HudEditScreen)) {
			return;
		}
		float dt = delta.getRealtimeDeltaTicks() / 20.0F;
		for (Instance inst : instances) {
			tickGif(inst, dt);
			drawInstance(graphics, inst);
		}
	}

	private static void tickGif(Instance inst, float deltaSeconds) {
		if (!inst.gifAnimated || !inst.ready || inst.gifFrames == null || inst.gifFrames.length <= 1) {
			return;
		}
		if (deltaSeconds <= 0.0F) {
			return;
		}
		inst.gifAccumSec += deltaSeconds;
		int guard = 0;
		while (guard++ < 64) {
			float delaySec = inst.gifDelaysMs[inst.gifIndex] / 1000.0F;
			if (inst.gifAccumSec < delaySec) {
				break;
			}
			inst.gifAccumSec -= delaySec;
			inst.gifIndex = (inst.gifIndex + 1) % inst.gifFrames.length;
			uploadGifFrame(inst, inst.gifIndex);
		}
	}

	private static void drawInstance(GuiGraphicsExtractor graphics, Instance inst) {
		if (!inst.ready || inst.texture == null) {
			return;
		}
		int draw = inst.size;
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				inst.textureId,
				inst.x,
				inst.y,
				0.0F,
				0.0F,
				draw,
				draw,
				inst.texW,
				inst.texH,
				inst.texW,
				inst.texH,
				0xFFFFFFFF
		);
	}

	private static boolean loadMedia(Instance inst, Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (name.endsWith(".gif")) {
			return loadGif(inst, path);
		}
		return loadStill(inst, path);
	}

	/** PNG via NativeImage; JPEG (and PNG fallback) via ImageIO → ARGB NativeImage. */
	private static boolean loadStill(Instance inst, Path path) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		try {
			NativeImage image;
			String name = path.getFileName().toString();
			if (isJpegFilename(name)) {
				image = readStillViaImageIo(path);
			} else {
				try (InputStream in = Files.newInputStream(path)) {
					image = NativeImage.read(in);
				} catch (Exception pngFail) {
					// Fallback if NativeImage rejects the file
					image = readStillViaImageIo(path);
				}
			}
			releaseInstance(inst);
			inst.imagePixels = image;
			inst.texW = image.getWidth();
			inst.texH = image.getHeight();
			inst.texture = new DynamicTexture(() -> "viewer_" + inst.id, image);
			client.getTextureManager().register(inst.textureId, inst.texture);
			inst.ready = inst.texW > 0 && inst.texH > 0;
			inst.gifAnimated = false;
			return inst.ready;
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("ViewerRetention still {}: {}", path, e.toString());
			inst.ready = false;
			return false;
		}
	}

	private static NativeImage readStillViaImageIo(Path path) throws Exception {
		java.awt.image.BufferedImage buf = javax.imageio.ImageIO.read(path.toFile());
		if (buf == null) {
			throw new IllegalStateException("ImageIO returned null for " + path);
		}
		int w = buf.getWidth();
		int h = buf.getHeight();
		NativeImage image = new NativeImage(w, h, true);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				image.setPixel(x, y, buf.getRGB(x, y));
			}
		}
		return image;
	}

	private static boolean loadGif(Instance inst, Path path) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		try {
			GifDecoder.Result decoded = GifDecoder.decode(path);
			if (decoded.frameCount() <= 0 || decoded.width <= 0 || decoded.height <= 0) {
				return false;
			}
			releaseInstance(inst);
			inst.gifFrames = decoded.frames;
			inst.gifDelaysMs = decoded.delaysMs;
			inst.gifIndex = 0;
			inst.gifAccumSec = 0.0F;
			inst.gifAnimated = decoded.frameCount() > 1;
			inst.texW = decoded.width;
			inst.texH = decoded.height;
			NativeImage image = new NativeImage(inst.texW, inst.texH, true);
			writeArgb(image, inst.gifFrames[0], inst.texW, inst.texH);
			inst.imagePixels = image;
			inst.texture = new DynamicTexture(() -> "viewer_gif_" + inst.id, image);
			client.getTextureManager().register(inst.textureId, inst.texture);
			inst.ready = true;
			return true;
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("ViewerRetention GIF {}: {}", path, e.toString());
			inst.ready = false;
			return false;
		}
	}

	private static void uploadGifFrame(Instance inst, int index) {
		if (inst.imagePixels == null || inst.texture == null || inst.gifFrames == null) {
			return;
		}
		if (index < 0 || index >= inst.gifFrames.length) {
			return;
		}
		writeArgb(inst.imagePixels, inst.gifFrames[index], inst.texW, inst.texH);
		inst.texture.upload();
	}

	private static void writeArgb(NativeImage image, int[] argb, int w, int h) {
		int len = Math.min(argb.length, w * h);
		for (int i = 0; i < len; i++) {
			image.setPixel(i % w, i / w, argb[i]);
		}
	}

	private static void releaseInstance(Instance inst) {
		Minecraft client = Minecraft.getInstance();
		if (client != null && inst.textureId != null) {
			try {
				client.getTextureManager().release(inst.textureId);
			} catch (Exception ignored) {
			}
		}
		if (inst.texture != null) {
			try {
				inst.texture.close();
			} catch (Exception ignored) {
			}
		}
		inst.texture = null;
		inst.imagePixels = null;
		inst.ready = false;
		inst.gifFrames = null;
		inst.gifDelaysMs = null;
		inst.gifAnimated = false;
	}

	private static void ensureAllLoaded() {
		ensureMediaDir();
		for (Instance inst : instances) {
			if (inst.ready) {
				continue;
			}
			Path path = mediaDir().resolve(inst.file).normalize();
			if (Files.isRegularFile(path)) {
				loadMedia(inst, path);
			}
		}
	}

	/** Serialize: {@code file:x:y:size;file2:...} */
	public static String instancesToConfig() {
		StringBuilder sb = new StringBuilder();
		for (Instance inst : instances) {
			if (!sb.isEmpty()) {
				sb.append(';');
			}
			sb.append(inst.file.replace(";", "").replace(":", "_"))
					.append(':').append(inst.x)
					.append(':').append(inst.y)
					.append(':').append(inst.size);
		}
		return sb.toString();
	}

	public static void loadInstances(String raw) {
		for (Instance inst : instances) {
			releaseInstance(inst);
		}
		instances.clear();
		if (raw == null || raw.isBlank()) {
			return;
		}
		String[] parts = raw.split(";");
		for (String part : parts) {
			String[] f = part.split(":");
			if (f.length < 4) {
				continue;
			}
			try {
				String file = f[0].trim();
				int x = Integer.parseInt(f[1].trim());
				int y = Integer.parseInt(f[2].trim());
				int size = Integer.parseInt(f[3].trim());
				if (!isCompatibleFilename(file)) {
					continue;
				}
				Instance inst = new Instance(file, x, y, size);
				instances.add(inst);
			} catch (NumberFormatException ignored) {
			}
		}
	}

	/** After ModConfig load — create Media/ and decode textures. */
	public static void afterConfigLoaded() {
		ensureMediaDir();
		refreshMediaList();
		ensureAllLoaded();
	}

	public static void registerHud() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				ExampleMod.id("viewer_retention"),
				ViewerRetentionModule::extractHud
		);
	}
}
