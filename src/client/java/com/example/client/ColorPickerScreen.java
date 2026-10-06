package com.example.client;

import com.example.client.config.MenuTheme;
import com.example.client.widget.CapsuleButton;
import com.example.client.widget.ColorChannelSlider;
import com.example.client.widget.HueBarWidget;
import com.example.client.widget.SaturationValueWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
/**
 * Interactive ARGB color picker (HSV square + hue bar + RGB sliders) with live
 * preview. Apply writes through {@link MenuTheme#set(String, int)} + save.
 */
public class ColorPickerScreen extends Screen {
	private static final int PANEL_WIDTH = 360;
	private static final int PANEL_HEIGHT = 280;
	private static final int SV_SIZE = 140;
	private static final int HUE_HEIGHT = 16;
	private static final int SLIDER_WIDTH = 160;
	private static final int SLIDER_HEIGHT = 20;

	private final Screen parent;
	private final String themeKey;
	private final int originalArgb;
	private final int alpha;

	private float hue;
	private float saturation;
	private float value;
	private int red;
	private int green;
	private int blue;

	private SaturationValueWidget svWidget;
	private HueBarWidget hueWidget;
	private ColorChannelSlider redSlider;
	private ColorChannelSlider greenSlider;
	private ColorChannelSlider blueSlider;

	/** Guards against feedback loops when syncing HSV ↔ RGB controls. */
	private boolean syncing;

	public ColorPickerScreen(Screen parent, String themeKey, int argb) {
		super(Component.translatable("screen.modid.menu.colors.picker", MenuTheme.displayName(themeKey)));
		this.parent = parent;
		this.themeKey = themeKey;
		this.originalArgb = argb;
		this.alpha = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;
		float[] hsv = rgbToHsv(r, g, b);
		this.hue = hsv[0];
		this.saturation = hsv[1];
		this.value = hsv[2];
		this.red = r;
		this.green = g;
		this.blue = b;
	}

	@Override
	protected void init() {
		int panelX = (this.width - PANEL_WIDTH) / 2;
		int panelY = (this.height - PANEL_HEIGHT) / 2;

		int svX = panelX + 16;
		int svY = panelY + 36;

		this.svWidget = this.addRenderableWidget(new SaturationValueWidget(
				svX,
				svY,
				SV_SIZE,
				this.hue,
				this.saturation,
				this.value,
				(s, v) -> {
					if (this.syncing) {
						return;
					}
					this.saturation = s;
					this.value = v;
					syncFromHsv();
				}
		));

		this.hueWidget = this.addRenderableWidget(new HueBarWidget(
				svX,
				svY + SV_SIZE + 10,
				SV_SIZE,
				HUE_HEIGHT,
				this.hue,
				h -> {
					if (this.syncing) {
						return;
					}
					this.hue = (float) h;
					this.svWidget.setHue(this.hue);
					syncFromHsv();
				}
		));

		int sliderX = panelX + 16 + SV_SIZE + 24;
		int sliderY = panelY + 36;
		this.redSlider = this.addRenderableWidget(new ColorChannelSlider(
				sliderX,
				sliderY,
				SLIDER_WIDTH,
				SLIDER_HEIGHT,
				"R",
				this.red,
				v -> {
					if (this.syncing) {
						return;
					}
					this.red = v;
					syncFromRgb();
				}
		));
		this.greenSlider = this.addRenderableWidget(new ColorChannelSlider(
				sliderX,
				sliderY + SLIDER_HEIGHT + 8,
				SLIDER_WIDTH,
				SLIDER_HEIGHT,
				"G",
				this.green,
				v -> {
					if (this.syncing) {
						return;
					}
					this.green = v;
					syncFromRgb();
				}
		));
		this.blueSlider = this.addRenderableWidget(new ColorChannelSlider(
				sliderX,
				sliderY + 2 * (SLIDER_HEIGHT + 8),
				SLIDER_WIDTH,
				SLIDER_HEIGHT,
				"B",
				this.blue,
				v -> {
					if (this.syncing) {
						return;
					}
					this.blue = v;
					syncFromRgb();
				}
		));

		int btnY = panelY + PANEL_HEIGHT - 36;
		int btnW = 100;
		int btnH = 24;
		this.addRenderableWidget(new CapsuleButton(
				panelX + 16,
				btnY,
				btnW,
				btnH,
				Component.translatable("screen.modid.menu.colors.apply"),
				button -> applyAndClose()
		));
		this.addRenderableWidget(new CapsuleButton(
				panelX + 16 + btnW + 12,
				btnY,
				btnW,
				btnH,
				Component.translatable("screen.modid.menu.colors.cancel"),
				button -> this.onClose()
		));
	}

	private void syncFromHsv() {
		this.syncing = true;
		int rgb = HueBarWidget.hsvToRgb(this.hue, this.saturation, this.value);
		this.red = (rgb >>> 16) & 0xFF;
		this.green = (rgb >>> 8) & 0xFF;
		this.blue = rgb & 0xFF;
		if (this.redSlider != null) {
			this.redSlider.setChannel(this.red);
			this.greenSlider.setChannel(this.green);
			this.blueSlider.setChannel(this.blue);
		}
		this.syncing = false;
	}

	private void syncFromRgb() {
		this.syncing = true;
		float[] hsv = rgbToHsv(this.red, this.green, this.blue);
		this.hue = hsv[0];
		this.saturation = hsv[1];
		this.value = hsv[2];
		if (this.svWidget != null) {
			this.svWidget.setHue(this.hue);
			this.svWidget.setSaturationValue(this.saturation, this.value);
		}
		if (this.hueWidget != null) {
			this.hueWidget.setHue(this.hue);
		}
		this.syncing = false;
	}

	private int currentArgb() {
		return (this.alpha << 24) | (this.red << 16) | (this.green << 8) | this.blue;
	}

	private void applyAndClose() {
		MenuTheme theme = MenuTheme.get();
		theme.set(this.themeKey, currentArgb());
		theme.save();
		if (this.parent instanceof ExampleMenuScreen menu) {
			menu.refreshColorMenu();
		}
		this.onClose();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	/** Open/close menu keybind dismisses the color picker and the parent menu. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ExampleModClient.matchesOpenMenuKey(event)) {
			this.minecraft.gui.setScreen(null);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int panelX = (this.width - PANEL_WIDTH) / 2;
		int panelY = (this.height - PANEL_HEIGHT) / 2;
		graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xEE101018);
		graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF707080);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int panelX = (this.width - PANEL_WIDTH) / 2;
		int panelY = (this.height - PANEL_HEIGHT) / 2;

		graphics.text(this.font, this.title, panelX + 16, panelY + 12, 0xFFFFFFFF, true);

		int previewX = panelX + 16 + SV_SIZE + 24;
		int previewY = panelY + 36 + 3 * (SLIDER_HEIGHT + 8) + 8;
		int swatchW = 48;
		int swatchH = 32;

		graphics.text(this.font, Component.translatable("screen.modid.menu.colors.original"), previewX, previewY, 0xFFAAAAAA, false);
		graphics.fill(previewX, previewY + 12, previewX + swatchW, previewY + 12 + swatchH, this.originalArgb | 0xFF000000);
		graphics.outline(previewX, previewY + 12, swatchW, swatchH, 0xFFFFFFFF);

		int newX = previewX + swatchW + 16;
		graphics.text(this.font, Component.translatable("screen.modid.menu.colors.preview"), newX, previewY, 0xFFAAAAAA, false);
		int current = currentArgb();
		graphics.fill(newX, previewY + 12, newX + swatchW, previewY + 12 + swatchH, current | 0xFF000000);
		graphics.outline(newX, previewY + 12, swatchW, swatchH, 0xFFFFFFFF);

		String hex = "#" + MenuTheme.toHex(current);
		graphics.text(this.font, hex, previewX, previewY + 12 + swatchH + 8, 0xFFE8E8E8, false);
		String rgbLabel = "RGB(" + this.red + ", " + this.green + ", " + this.blue + ")";
		graphics.text(this.font, rgbLabel, previewX, previewY + 12 + swatchH + 20, 0xFFB0B0C0, false);
	}

	/** @return hue [0,1], saturation [0,1], value [0,1] */
	static float[] rgbToHsv(int r, int g, int b) {
		float rf = r / 255f;
		float gf = g / 255f;
		float bf = b / 255f;
		float max = Math.max(rf, Math.max(gf, bf));
		float min = Math.min(rf, Math.min(gf, bf));
		float delta = max - min;
		float h;
		if (delta == 0f) {
			h = 0f;
		} else if (max == rf) {
			h = ((gf - bf) / delta) % 6f;
		} else if (max == gf) {
			h = (bf - rf) / delta + 2f;
		} else {
			h = (rf - gf) / delta + 4f;
		}
		h /= 6f;
		if (h < 0f) {
			h += 1f;
		}
		float s = max == 0f ? 0f : delta / max;
		return new float[] {h, s, max};
	}
}
