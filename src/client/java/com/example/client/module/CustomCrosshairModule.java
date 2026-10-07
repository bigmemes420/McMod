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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Visuals: replaces the vanilla crosshair with either a pixel pattern (16–512) or a
 * PNG/GIF from {@code <gameDir>/crosshairs/} (full ARGB alpha; GIFs animate). Both modes bake to a
 * DynamicTexture and blit once per frame; spin uses pose translate→rotate→translate
 * (no per-pixel fills). Rotate / spin apply to both sources; PNG can use
 * custom tint or direct file colors ({@link ColorMode}).
 */
public final class CustomCrosshairModule {
	public enum Source {
		PIXELS,
		PNG
	}

	/** PNG blit: multiply by custom color, or keep file colors as-is. */
	public enum ColorMode {
		/** Multiply PNG by {@link #getColor()} (in-game tint). */
		TINT,
		/** Blit with white tint — original PNG ARGB. */
		DIRECT
	}

	/** Default square resolution size (64×64). */
	public static final int DEFAULT_GRID = 64;
	public static final int DEFAULT_COLOR = 0xFFFFFFFF;
	public static final float MIN_SPIN_SPEED = 15.0F;
	public static final float MAX_SPIN_SPEED = 720.0F;
	public static final float DEFAULT_SPIN_SPEED = 90.0F;
	public static final String CROSSHAIRS_FOLDER = "crosshairs";

	/** Square pixel / HUD draw size. */
	public enum Resolution {
		X16(16),
		X32(32),
		X64(64),
		X128(128),
		X256(256),
		X512(512);

		private final int size;

		Resolution(int size) {
			this.size = size;
		}

		public int size() {
			return size;
		}

		public String label() {
			return size + "x" + size;
		}

		public static Resolution fromSize(int size) {
			for (Resolution r : values()) {
				if (r.size == size) {
					return r;
				}
			}
			return X64;
		}

		public static Resolution fromConfig(String raw) {
			if (raw == null || raw.isBlank()) {
				return X64;
			}
			String s = raw.trim();
			try {
				return valueOf(s);
			} catch (IllegalArgumentException ignored) {
			}
			// Accept bare size "64" or "64x64"
			String digits = s.toLowerCase(Locale.ROOT).replace("x", " ").trim().split("\s+")[0];
			try {
				return fromSize(Integer.parseInt(digits));
			} catch (NumberFormatException ignored) {
				return X64;
			}
		}
	}

	private static final Identifier PNG_TEXTURE_ID = ExampleMod.id("dynamic/crosshair_png");
	private static final Identifier PIXELS_TEXTURE_ID = ExampleMod.id("dynamic/crosshair_pixels");

	private static boolean enabled;
	private static Source source = Source.PIXELS;
	private static Resolution resolution = Resolution.X64;
	private static boolean[] pixels = new boolean[DEFAULT_GRID * DEFAULT_GRID];
	private static int color = DEFAULT_COLOR;
	private static ColorMode colorMode = ColorMode.TINT;
	private static boolean rotate;
	private static float spinSpeed = DEFAULT_SPIN_SPEED;
	private static float angleDeg;
	/** Filename only (e.g. {@code dot.png}) inside {@link #crosshairsDir()}. */
	private static String selectedPng = "";

	private static DynamicTexture pngTexture;
	/** Backing pixels for {@link #pngTexture} (PNG or current GIF frame). */
	private static NativeImage imagePixels;
	private static int pngWidth;
	private static int pngHeight;
	private static boolean pngReady;
	/** False until first in-game ensure when texture manager is ready. */
	private static boolean crosshairBootstrapped;

	/** Decoded GIF animation (null when static PNG). */
	private static int[][] gifFrames;
	private static int[] gifDelaysMs;
	private static int gifFrameIndex;
	private static float gifAccumSec;
	private static boolean gifAnimated;

	private static DynamicTexture pixelsTexture;
	private static boolean pixelsTextureDirty = true;

	private static String statusMessage = "";
	private static List<String> cachedPngList = List.of();

	static {
		applyDefaultPattern(pixels);
	}

	private CustomCrosshairModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static Source getSource() {
		return source;
	}

	public static Resolution getResolution() {
		return resolution;
	}

	/** Current square grid / HUD draw size (16–512). */
	public static int getGrid() {
		return resolution.size();
	}

	public static boolean isPngMode() {
		return source == Source.PNG && pngReady;
	}

	public static int getColor() {
		return color;
	}

	public static ColorMode getColorMode() {
		return colorMode;
	}

	/** Tint used when blitting PNG (white when DIRECT). */
	public static int pngBlitColor() {
		return colorMode == ColorMode.DIRECT ? 0xFFFFFFFF : color;
	}

	public static boolean isRotate() {
		return rotate;
	}

	public static float getSpinSpeed() {
		return spinSpeed;
	}

	public static String getStatusMessage() {
		return statusMessage;
	}

	public static String getSelectedPng() {
		return selectedPng == null ? "" : selectedPng;
	}

	public static int getPngWidth() {
		return pngWidth;
	}

	public static int getPngHeight() {
		return pngHeight;
	}

	/**
	 * {@code <gameDir>/crosshairs/} — uses {@link Minecraft#gameDirectory} when
	 * available, otherwise Fabric's game dir.
	 */
	public static Path crosshairsDir() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.gameDirectory != null) {
			return client.gameDirectory.toPath().resolve(CROSSHAIRS_FOLDER);
		}
		return FabricLoader.getInstance().getGameDir().resolve(CROSSHAIRS_FOLDER);
	}

	public static Path selectedPngPath() {
		if (selectedPng == null || selectedPng.isBlank()) {
			return null;
		}
		Path path = crosshairsDir().resolve(selectedPng).normalize();
		if (!path.startsWith(crosshairsDir().normalize())) {
			return null; // path traversal guard
		}
		return path;
	}

	public static void ensureCrosshairsDir() {
		try {
			Files.createDirectories(crosshairsDir());
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to create crosshairs dir: {}", e.toString());
		}
	}

	/**
	 * Rescan {@link #crosshairsDir()} for {@code .png} / {@code .gif} files.
	 * Creates the folder if missing. Updates the cached list.
	 */
	public static List<String> refreshPngList() {
		ensureCrosshairsDir();
		Path dir = crosshairsDir();
		List<String> found = new ArrayList<>();
		try (var stream = Files.list(dir)) {
			// Extension-only scan (no decode) — validate on select.
			stream.filter(Files::isRegularFile)
					.map(p -> p.getFileName().toString())
					.filter(CustomCrosshairModule::isImageFilename)
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.forEach(found::add);
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to list crosshairs: {}", e.toString());
		}
		cachedPngList = Collections.unmodifiableList(found);
		statusMessage = found.isEmpty()
				? "No PNG/GIF in " + dir
				: found.size() + " image(s) in crosshairs/";
		return cachedPngList;
	}

	public static boolean isImageFilename(String name) {
		if (name == null) {
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		return n.endsWith(".png") || n.endsWith(".gif");
	}

	public static boolean isGifFilename(String name) {
		return name != null && name.toLowerCase(Locale.ROOT).endsWith(".gif");
	}

	public static List<String> getPngList() {
		if (cachedPngList.isEmpty()) {
			return refreshPngList();
		}
		return cachedPngList;
	}

	private static boolean isCompatiblePng(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			NativeImage img = NativeImage.read(in);
			boolean ok = img.getWidth() > 0 && img.getHeight() > 0;
			img.close();
			return ok;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean getPixel(int x, int y) {
		int g = getGrid();
		if (x < 0 || y < 0 || x >= g || y >= g) {
			return false;
		}
		return pixels[y * g + x];
	}

	/** Copy working buffer into module state, switch to pixel mode, persist. */
	public static void setPixelsAndSave(boolean[] src) {
		if (src == null || src.length != pixels.length) {
			return;
		}
		System.arraycopy(src, 0, pixels, 0, pixels.length);
		source = Source.PIXELS;
		markPixelsTextureDirty();
		statusMessage = "Using pixel grid";
		ModConfig.save();
	}

	public static boolean[] copyPixels() {
		return Arrays.copyOf(pixels, pixels.length);
	}

	public static void applyDefaultPattern(boolean[] dest) {
		int g = (int) Math.round(Math.sqrt(dest.length));
		if (g * g != dest.length) {
			g = getGrid();
		}
		Arrays.fill(dest, false);
		int c = g / 2;
		int arm = Math.max(2, g * 12 / 64);
		int gap = Math.max(1, g / 32);
		for (int i = c - arm; i <= c + arm - 1; i++) {
			if (Math.abs(i - c) <= gap) {
				continue;
			}
			if (i >= 0 && i < g) {
				dest[c * g + i] = true;
				dest[i * g + c] = true;
			}
		}
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		NotificationsModule.notifyToggle(
				enabled ? "screen.rootymenu.menu.visuals.custom_crosshair.enabled"
						: "screen.rootymenu.menu.visuals.custom_crosshair.disabled"
		);
		ModConfig.save();
	}

	/** Full ARGB tint — alpha is preserved (do not force opaque). */
	public static void setColor(int argb) {
		if (color == argb) {
			return;
		}
		color = argb;
		markPixelsTextureDirty();
		ModConfig.save();
	}

	public static void setColorMode(ColorMode value) {
		if (value == null || colorMode == value) {
			return;
		}
		colorMode = value;
		ModConfig.save();
	}

	public static void setResolution(Resolution value) {
		if (value == null || resolution == value) {
			return;
		}
		int oldG = getGrid();
		boolean[] old = pixels;
		resolution = value;
		int g = value.size();
		boolean[] next = new boolean[g * g];
		if (old != null && oldG > 0) {
			for (int y = 0; y < g; y++) {
				for (int x = 0; x < g; x++) {
					int ox = x * oldG / g;
					int oy = y * oldG / g;
					ox = Mth.clamp(ox, 0, oldG - 1);
					oy = Mth.clamp(oy, 0, oldG - 1);
					next[y * g + x] = old[oy * oldG + ox];
				}
			}
		} else {
			applyDefaultPattern(next);
		}
		pixels = next;
		releasePixelsTexture();
		markPixelsTextureDirty();
		ModConfig.save();
	}

	public static void setRotate(boolean value) {
		if (rotate == value) {
			return;
		}
		rotate = value;
		if (!rotate) {
			angleDeg = 0.0F;
		}
		ModConfig.save();
	}

	public static void setSpinSpeed(float value) {
		float clamped = Mth.clamp(value, MIN_SPIN_SPEED, MAX_SPIN_SPEED);
		if (spinSpeed == clamped) {
			return;
		}
		spinSpeed = clamped;
		ModConfig.save();
	}

	public static void setSource(Source value) {
		if (value == null) {
			return;
		}
		if (value == Source.PIXELS) {
			source = Source.PIXELS;
			statusMessage = "Using pixel grid";
			ModConfig.save();
			return;
		}
		// Switch to PNG: use selected file, or first available in crosshairs/.
		source = Source.PNG;
		if (selectedPng == null || selectedPng.isBlank()) {
			List<String> list = getPngList();
			if (!list.isEmpty()) {
				selectPng(list.getFirst(), true);
				return;
			}
			statusMessage = "No PNG/GIF in crosshairs/ — drop files then Refresh";
			ModConfig.save();
			return;
		}
		selectPng(selectedPng, true);
	}

	/**
	 * Select a PNG/GIF by filename inside {@link #crosshairsDir()}, load it, switch
	 * to image mode, and persist ({@code customCrosshairPng}).
	 */
	public static boolean selectPng(String filename) {
		return selectPng(filename, true);
	}

	public static boolean selectPng(String filename, boolean save) {
		if (filename == null || filename.isBlank()) {
			pngReady = false;
			statusMessage = "No image selected";
			return false;
		}
		// Normalize to basename only
		String name = Path.of(filename).getFileName().toString();
		if (!isImageFilename(name)) {
			statusMessage = "Not a PNG/GIF: " + name;
			return false;
		}
		selectedPng = name;
		Path path = selectedPngPath();
		if (path == null || !Files.isRegularFile(path)) {
			pngReady = false;
			statusMessage = "Missing " + name + " in crosshairs/";
			if (save) {
				ModConfig.save();
			}
			return false;
		}
		boolean ok = loadImageTexture(path);
		if (ok) {
			source = Source.PNG;
			statusMessage = "Using " + name + (gifAnimated ? " (" + gifFrames.length + " frames)" : "");
		} else {
			statusMessage = "Failed to decode " + name;
		}
		if (save) {
			ModConfig.save();
		}
		return ok;
	}

	public static void loadEnabled(boolean value) {
		enabled = value;
	}

	public static void loadColor(int argb) {
		color = argb;
		markPixelsTextureDirty();
	}

	public static void loadColorMode(String raw) {
		if (raw == null || raw.isBlank()) {
			colorMode = ColorMode.TINT;
			return;
		}
		try {
			colorMode = ColorMode.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			colorMode = ColorMode.TINT;
		}
	}

	public static void loadResolution(String raw) {
		Resolution next = Resolution.fromConfig(raw);
		if (resolution == next && pixels != null && pixels.length == next.size() * next.size()) {
			return;
		}
		resolution = next;
		int g = next.size();
		if (pixels == null || pixels.length != g * g) {
			pixels = new boolean[g * g];
			applyDefaultPattern(pixels);
			markPixelsTextureDirty();
		}
	}

	public static void loadRotate(boolean value) {
		rotate = value;
	}

	public static void loadSpinSpeed(float value) {
		spinSpeed = Mth.clamp(value, MIN_SPIN_SPEED, MAX_SPIN_SPEED);
	}

	public static void loadSource(String raw) {
		if (raw == null || raw.isBlank()) {
			source = Source.PIXELS;
			return;
		}
		try {
			source = Source.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			source = Source.PIXELS;
		}
	}

	public static void loadSelectedPng(String raw) {
		selectedPng = raw == null ? "" : raw.trim();
	}

	public static void loadPixelsBase64(String raw) {
		int g = getGrid();
		if (pixels == null || pixels.length != g * g) {
			pixels = new boolean[g * g];
		}
		if (raw == null || raw.isBlank()) {
			applyDefaultPattern(pixels);
			markPixelsTextureDirty();
			return;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(raw.trim());
			int need = (g * g + 7) / 8;
			if (bytes.length < need) {
				applyDefaultPattern(pixels);
				markPixelsTextureDirty();
				return;
			}
			Arrays.fill(pixels, false);
			for (int i = 0; i < g * g; i++) {
				int b = bytes[i >>> 3] & 0xFF;
				pixels[i] = ((b >> (i & 7)) & 1) != 0;
			}
			markPixelsTextureDirty();
		} catch (IllegalArgumentException ex) {
			applyDefaultPattern(pixels);
			markPixelsTextureDirty();
		}
	}

	public static String pixelsToBase64() {
		int g = getGrid();
		byte[] bytes = new byte[(g * g + 7) / 8];
		for (int i = 0; i < g * g; i++) {
			if (pixels[i]) {
				bytes[i >>> 3] |= (byte) (1 << (i & 7));
			}
		}
		return Base64.getEncoder().encodeToString(bytes);
	}

	/** Called after ModConfig finishes loading module fields. */
	public static void afterConfigLoaded() {
		crosshairBootstrapped = false;
		ensureCrosshairsDir();
		refreshPngList();
		// Try early load; may fail before texture manager is ready — HUD retries.
		reloadSelectedImage(false);
		markPixelsTextureDirty();
	}

	/** Re-decode the configured PNG/GIF when source is image mode. */
	private static void reloadSelectedImage(boolean save) {
		if (source != Source.PNG) {
			return;
		}
		if (selectedPng == null || selectedPng.isBlank()) {
			if (!cachedPngList.isEmpty()) {
				selectPng(cachedPngList.getFirst(), save);
			}
			return;
		}
		selectPng(selectedPng, save);
	}

	/**
	 * Ensures PNG/GIF textures (and pixel bake) after the client texture manager
	 * exists — config load often runs too early for DynamicTexture registration.
	 */
	private static void ensureCrosshairAssets(Minecraft client) {
		if (client == null || client.getTextureManager() == null) {
			return;
		}
		if (!crosshairBootstrapped) {
			ensureCrosshairsDir();
			refreshPngList();
			reloadSelectedImage(false);
			markPixelsTextureDirty();
			crosshairBootstrapped = true;
			return;
		}
		if (source == Source.PNG && !pngReady && selectedPng != null && !selectedPng.isBlank()) {
			reloadSelectedImage(false);
		}
	}

	public static void tick(float deltaSeconds) {
		if (rotate && enabled) {
			angleDeg = (angleDeg + spinSpeed * deltaSeconds) % 360.0F;
			if (angleDeg < 0.0F) {
				angleDeg += 360.0F;
			}
		}
		tickGif(deltaSeconds);
	}

	/** Advance GIF frame clock (safe to call from editor while menu is open). */
	public static void tickGif(float deltaSeconds) {
		if (!gifAnimated || !pngReady || gifFrames == null || gifFrames.length <= 1) {
			return;
		}
		if (deltaSeconds <= 0.0F) {
			return;
		}
		gifAccumSec += deltaSeconds;
		// Cap catch-up to avoid spiral after hitch
		int guard = 0;
		while (guard++ < 64) {
			float delaySec = gifDelaysMs[gifFrameIndex] / 1000.0F;
			if (gifAccumSec < delaySec) {
				break;
			}
			gifAccumSec -= delaySec;
			gifFrameIndex = (gifFrameIndex + 1) % gifFrames.length;
			uploadGifFrame(gifFrameIndex);
		}
	}

	public static boolean isGifAnimated() {
		return gifAnimated && gifFrames != null && gifFrames.length > 1;
	}

	private static boolean loadImageTexture(Path path) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		String name = path.getFileName().toString();
		if (isGifFilename(name)) {
			return loadGifTexture(path);
		}
		try (InputStream in = Files.newInputStream(path)) {
			NativeImage image = NativeImage.read(in);
			clearGifState();
			seedPixelsFromImage(image);
			releasePngTexture();
			imagePixels = image;
			pngTexture = new DynamicTexture(() -> "rooty_crosshair_png", image);
			client.getTextureManager().register(PNG_TEXTURE_ID, pngTexture);
			pngWidth = image.getWidth();
			pngHeight = image.getHeight();
			pngReady = pngWidth > 0 && pngHeight > 0;
			return pngReady;
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to load crosshair PNG {}: {}", path, e.toString());
			pngReady = false;
			return false;
		}
	}

	private static boolean loadGifTexture(Path path) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		try {
			GifDecoder.Result decoded = GifDecoder.decode(path);
			if (decoded.frameCount() <= 0 || decoded.width <= 0 || decoded.height <= 0) {
				pngReady = false;
				return false;
			}
			releasePngTexture();
			clearGifState();
			gifFrames = decoded.frames;
			gifDelaysMs = decoded.delaysMs;
			gifFrameIndex = 0;
			gifAccumSec = 0.0F;
			gifAnimated = decoded.frameCount() > 1;
			pngWidth = decoded.width;
			pngHeight = decoded.height;
			NativeImage image = new NativeImage(pngWidth, pngHeight, true);
			writeArgbToNative(image, gifFrames[0], pngWidth, pngHeight);
			seedPixelsFromImage(image);
			imagePixels = image;
			pngTexture = new DynamicTexture(() -> "rooty_crosshair_gif", image);
			client.getTextureManager().register(PNG_TEXTURE_ID, pngTexture);
			pngReady = true;
			return true;
		} catch (Exception e) {
			ExampleMod.LOGGER.warn("Failed to load crosshair GIF {}: {}", path, e.toString());
			clearGifState();
			pngReady = false;
			return false;
		}
	}

	private static void uploadGifFrame(int index) {
		if (imagePixels == null || pngTexture == null || gifFrames == null) {
			return;
		}
		if (index < 0 || index >= gifFrames.length) {
			return;
		}
		writeArgbToNative(imagePixels, gifFrames[index], pngWidth, pngHeight);
		pngTexture.upload();
	}

	private static void writeArgbToNative(NativeImage image, int[] argb, int w, int h) {
		int len = Math.min(argb.length, w * h);
		for (int i = 0; i < len; i++) {
			int x = i % w;
			int y = i / w;
			image.setPixel(x, y, argb[i]);
		}
	}

	private static void clearGifState() {
		gifFrames = null;
		gifDelaysMs = null;
		gifFrameIndex = 0;
		gifAccumSec = 0.0F;
		gifAnimated = false;
	}

	private static void seedPixelsFromImage(NativeImage image) {
		int g = getGrid();
		if (pixels == null || pixels.length != g * g) {
			pixels = new boolean[g * g];
		}
		Arrays.fill(pixels, false);
		markPixelsTextureDirty();
		int w = image.getWidth();
		int h = image.getHeight();
		for (int y = 0; y < g; y++) {
			for (int x = 0; x < g; x++) {
				int sx = x * w / g;
				int sy = y * h / g;
				sx = Mth.clamp(sx, 0, w - 1);
				sy = Mth.clamp(sy, 0, h - 1);
				int argb = image.getPixel(sx, sy);
				if (ARGB.alpha(argb) > 32) {
					pixels[y * g + x] = true;
				}
			}
		}
	}

	private static void releasePngTexture() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			try {
				client.getTextureManager().release(PNG_TEXTURE_ID);
			} catch (Exception ignored) {
			}
		}
		if (pngTexture != null) {
			try {
				pngTexture.close();
			} catch (Exception ignored) {
			}
			pngTexture = null;
		}
		imagePixels = null;
		pngReady = false;
		clearGifState();
	}

	private static void markPixelsTextureDirty() {
		pixelsTextureDirty = true;
	}

	private static void releasePixelsTexture() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			try {
				client.getTextureManager().release(PIXELS_TEXTURE_ID);
			} catch (Exception ignored) {
			}
		}
		if (pixelsTexture != null) {
			try {
				pixelsTexture.close();
			} catch (Exception ignored) {
			}
			pixelsTexture = null;
		}
		pixelsTextureDirty = true;
	}

	/** Bake boolean grid + ARGB color into a grid×grid texture (only when dirty). */
	private static void ensurePixelsTexture() {
		if (!pixelsTextureDirty && pixelsTexture != null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		int g = getGrid();
		releasePixelsTexture();
		NativeImage image = new NativeImage(g, g, true);
		image.fillRect(0, 0, g, g, 0x00000000);
		for (int y = 0; y < g; y++) {
			for (int x = 0; x < g; x++) {
				if (pixels[y * g + x]) {
					image.setPixel(x, y, color);
				}
			}
		}
		pixelsTexture = new DynamicTexture(() -> "rooty_crosshair_pixels", image);
		client.getTextureManager().register(PIXELS_TEXTURE_ID, pixelsTexture);
		pixelsTextureDirty = false;
	}

	public static void extractHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
		if (!enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) {
			return;
		}
		if (client.gui.screen() != null) {
			return;
		}
		ensureCrosshairAssets(client);
		float dt = delta.getRealtimeDeltaTicks() / 20.0F;
		tick(dt);

		int sw = client.getWindow().getGuiScaledWidth();
		int sh = client.getWindow().getGuiScaledHeight();
		float cx = sw / 2.0F;
		float cy = sh / 2.0F;

		if (source == Source.PNG && pngReady) {
			drawPng(graphics, cx, cy);
		} else {
			drawPixels(graphics, cx, cy);
		}
	}

	private static void drawRotatedBlit(
			GuiGraphicsExtractor graphics,
			Identifier texture,
			float cx,
			float cy,
			int drawW,
			int drawH,
			int texW,
			int texH,
			int tintArgb
	) {
		float angle = rotate ? angleDeg * Mth.DEG_TO_RAD : 0.0F;
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		if (angle != 0.0F) {
			pose.rotate(angle);
		}
		pose.translate(-drawW / 2.0F, -drawH / 2.0F);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				texture,
				0,
				0,
				0.0F,
				0.0F,
				drawW,
				drawH,
				texW,
				texH,
				texW,
				texH,
				tintArgb
		);
		pose.popMatrix();
	}

	private static void drawPng(GuiGraphicsExtractor graphics, float cx, float cy) {
		int drawW = pngWidth;
		int drawH = pngHeight;
		int maxSide = Math.max(drawW, drawH);
		int target = getGrid();
		if (maxSide > 0) {
			float scale = target / (float) maxSide;
			drawW = Math.max(1, Math.round(drawW * scale));
			drawH = Math.max(1, Math.round(drawH * scale));
		}
		drawRotatedBlit(graphics, PNG_TEXTURE_ID, cx, cy, drawW, drawH, pngWidth, pngHeight, pngBlitColor());
	}

	private static void drawPixels(GuiGraphicsExtractor graphics, float cx, float cy) {
		if (ARGB.alpha(color) == 0) {
			return;
		}
		ensurePixelsTexture();
		if (pixelsTexture == null) {
			return;
		}
		// Color baked into texture; white tint preserves baked ARGB/alpha.
		int g = getGrid();
		drawRotatedBlit(graphics, PIXELS_TEXTURE_ID, cx, cy, g, g, g, g, 0xFFFFFFFF);
	}

	public static void drawPngPreview(GuiGraphicsExtractor graphics, int x, int y, int maxSide) {
		if (!pngReady) {
			return;
		}
		int side = Math.max(pngWidth, pngHeight);
		float scale = maxSide / (float) Math.max(1, side);
		int drawW = Math.max(1, Math.round(pngWidth * scale));
		int drawH = Math.max(1, Math.round(pngHeight * scale));
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				PNG_TEXTURE_ID,
				x,
				y,
				0.0F,
				0.0F,
				drawW,
				drawH,
				pngWidth,
				pngHeight,
				pngWidth,
				pngHeight,
				pngBlitColor()
		);
	}

	public static void registerHud() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				ExampleMod.id("custom_crosshair"),
				CustomCrosshairModule::extractHud
		);
	}
}
