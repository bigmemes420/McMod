package com.example.client.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Decodes animated GIFs via ImageIO into full-canvas ARGB frames with delays (ms).
 * Applies Graphic Control disposal so each frame is a composited snapshot.
 */
public final class GifDecoder {
	public static final class Result {
		public final int width;
		public final int height;
		/** Packed ARGB pixels, length {@code width * height}, one per frame. */
		public final int[][] frames;
		/** Delay for each frame in milliseconds (minimum 20). */
		public final int[] delaysMs;

		public Result(int width, int height, int[][] frames, int[] delaysMs) {
			this.width = width;
			this.height = height;
			this.frames = frames;
			this.delaysMs = delaysMs;
		}

		public int frameCount() {
			return frames.length;
		}
	}

	private GifDecoder() {
	}

	public static Result decode(Path path) throws IOException {
		try (InputStream in = Files.newInputStream(path);
				ImageInputStream stream = ImageIO.createImageInputStream(in)) {
			if (stream == null) {
				throw new IOException("Cannot open image stream: " + path);
			}
			return decode(stream);
		}
	}

	public static Result decode(ImageInputStream stream) throws IOException {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
		if (!readers.hasNext()) {
			throw new IOException("No GIF ImageReader available");
		}
		ImageReader reader = readers.next();
		try {
			reader.setInput(stream, false, false);
			int count = reader.getNumImages(true);
			if (count <= 0) {
				throw new IOException("GIF has no frames");
			}

			int canvasW = 0;
			int canvasH = 0;
			IIOMetadata streamMeta = reader.getStreamMetadata();
			if (streamMeta != null) {
				IIOMetadataNode root = (IIOMetadataNode) streamMeta.getAsTree(streamMeta.getNativeMetadataFormatName());
				IIOMetadataNode desc = findNode(root, "LogicalScreenDescriptor");
				if (desc != null) {
					canvasW = parseInt(desc.getAttribute("logicalScreenWidth"), 0);
					canvasH = parseInt(desc.getAttribute("logicalScreenHeight"), 0);
				}
			}

			List<int[]> framePixels = new ArrayList<>(count);
			List<Integer> delays = new ArrayList<>(count);

			BufferedImage canvas = null;
			BufferedImage backup = null;
			int prevDisposal = 0;
			int prevLeft = 0;
			int prevTop = 0;
			int prevW = 0;
			int prevH = 0;

			for (int i = 0; i < count; i++) {
				BufferedImage frame = reader.read(i);
				FrameInfo info = parseFrameInfo(reader.getImageMetadata(i));

				if (canvasW <= 0) {
					canvasW = Math.max(frame.getWidth() + info.left, frame.getWidth());
				}
				if (canvasH <= 0) {
					canvasH = Math.max(frame.getHeight() + info.top, frame.getHeight());
				}
				if (canvas == null) {
					canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
				}

				if (i > 0) {
					if (prevDisposal == 2) {
						clearRect(canvas, prevLeft, prevTop, prevW, prevH);
					} else if (prevDisposal == 3 && backup != null) {
						Graphics2D g = canvas.createGraphics();
						g.drawImage(backup, 0, 0, null);
						g.dispose();
					}
				}

				if (info.disposal == 3) {
					backup = copyImage(canvas);
				}

				Graphics2D g = canvas.createGraphics();
				g.drawImage(frame, info.left, info.top, null);
				g.dispose();

				framePixels.add(copyArgb(canvas));
				delays.add(info.delayMs);

				prevDisposal = info.disposal;
				prevLeft = info.left;
				prevTop = info.top;
				prevW = frame.getWidth();
				prevH = frame.getHeight();
			}

			int[][] frames = framePixels.toArray(new int[0][]);
			int[] delayArr = new int[delays.size()];
			for (int i = 0; i < delays.size(); i++) {
				delayArr[i] = delays.get(i);
			}
			return new Result(canvasW, canvasH, frames, delayArr);
		} finally {
			reader.dispose();
		}
	}

	private static final class FrameInfo {
		int left;
		int top;
		int delayMs = 100;
		/** 0 unspecified, 1 do not dispose, 2 restore background, 3 restore previous */
		int disposal;
	}

	private static FrameInfo parseFrameInfo(IIOMetadata meta) {
		FrameInfo info = new FrameInfo();
		if (meta == null) {
			return info;
		}
		try {
			String format = meta.getNativeMetadataFormatName();
			IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
			IIOMetadataNode gce = findNode(root, "GraphicControlExtension");
			if (gce != null) {
				int cs = parseInt(gce.getAttribute("delayTime"), 10);
				if (cs <= 0) {
					cs = 10; // browsers treat 0 as ~10 centiseconds
				}
				info.delayMs = Math.max(20, cs * 10);
				String disp = gce.getAttribute("disposalMethod");
				if (disp != null && !disp.isBlank()) {
					if (Character.isDigit(disp.charAt(0))) {
						info.disposal = parseInt(disp, 0);
					} else {
						info.disposal = switch (disp) {
							case "doNotDispose" -> 1;
							case "restoreToBackgroundColor" -> 2;
							case "restoreToPrevious" -> 3;
							default -> 0;
						};
					}
				}
			}
			IIOMetadataNode id = findNode(root, "ImageDescriptor");
			if (id != null) {
				info.left = parseInt(id.getAttribute("imageLeftPosition"), 0);
				info.top = parseInt(id.getAttribute("imageTopPosition"), 0);
			}
		} catch (Exception ignored) {
		}
		return info;
	}

	private static IIOMetadataNode findNode(IIOMetadataNode root, String name) {
		if (root == null) {
			return null;
		}
		if (name.equals(root.getNodeName())) {
			return root;
		}
		for (int i = 0; i < root.getLength(); i++) {
			IIOMetadataNode found = findNode((IIOMetadataNode) root.item(i), name);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static int parseInt(String s, int def) {
		if (s == null || s.isBlank()) {
			return def;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static void clearRect(BufferedImage img, int x, int y, int w, int h) {
		x = Math.max(0, x);
		y = Math.max(0, y);
		w = Math.min(w, img.getWidth() - x);
		h = Math.min(h, img.getHeight() - y);
		if (w <= 0 || h <= 0) {
			return;
		}
		int[] clear = new int[w * h];
		img.setRGB(x, y, w, h, clear, 0, w);
	}

	private static BufferedImage copyImage(BufferedImage src) {
		BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copy.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return copy;
	}

	private static int[] copyArgb(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		int[] out = new int[w * h];
		img.getRGB(0, 0, w, h, out, 0, w);
		return out;
	}
}
