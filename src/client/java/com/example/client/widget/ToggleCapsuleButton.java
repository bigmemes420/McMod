package com.example.client.widget;

import com.example.client.config.MenuTheme;
import com.example.client.module.ModuleKeybinds;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Capsule-style module row. Left-click toggles; right-click binds a key
 * ({@link ModuleKeybinds}). The left circle turns green when ON.
 */
public class ToggleCapsuleButton extends AbstractWidget {
	@FunctionalInterface
	public interface OnToggle {
		void onToggle(ToggleCapsuleButton button, boolean enabled);
	}

	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 4;

	private final OnToggle onToggle;
	private final ItemStack icon;
	private final String moduleId;
	private boolean enabled;
	private boolean pressed;

	public ToggleCapsuleButton(int x, int y, int width, int height, Component message, boolean enabled, OnToggle onToggle) {
		this(x, y, width, height, message, enabled, ItemStack.EMPTY, null, onToggle);
	}

	public ToggleCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			boolean enabled,
			String moduleId,
			OnToggle onToggle
	) {
		this(x, y, width, height, message, enabled, ItemStack.EMPTY, moduleId, onToggle);
	}

	public ToggleCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			boolean enabled,
			ItemStack icon,
			OnToggle onToggle
	) {
		this(x, y, width, height, message, enabled, icon, null, onToggle);
	}

	public ToggleCapsuleButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			boolean enabled,
			ItemStack icon,
			String moduleId,
			OnToggle onToggle
	) {
		super(x, y, width, height, message);
		this.enabled = enabled;
		this.icon = icon == null || icon.isEmpty() ? ItemStack.EMPTY : icon.copy();
		this.moduleId = moduleId;
		this.onToggle = onToggle;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getModuleId() {
		return this.moduleId;
	}

	@Override
	protected boolean isValidClickButton(MouseButtonInfo info) {
		int b = info.button();
		return b == InputConstants.MOUSE_BUTTON_LEFT || b == InputConstants.MOUSE_BUTTON_RIGHT;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		MenuTheme theme = MenuTheme.get();
		boolean hovered = this.isHoveredOrFocused();
		boolean listening = this.moduleId != null && ModuleKeybinds.isListening(this.moduleId);
		int fill;
		int outline;
		int textColor;
		int leftCircle;

		if (!this.active) {
			fill = theme.capsuleDisabledFill;
			outline = theme.capsuleDisabledOutline;
			textColor = theme.capsuleDisabledText;
			leftCircle = fill;
		} else if (listening) {
			fill = theme.capsuleHoverFill;
			outline = theme.accentOn;
			textColor = theme.capsuleText;
			leftCircle = theme.accentOn;
		} else if (this.pressed) {
			fill = theme.capsulePressedFill;
			outline = theme.capsulePressedOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		} else if (hovered) {
			fill = theme.capsuleHoverFill;
			outline = theme.capsuleHoverOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		} else {
			fill = theme.capsuleFill;
			outline = theme.capsuleOutline;
			textColor = theme.capsuleText;
			leftCircle = this.enabled ? theme.accentOn : fill;
		}

		MenuShapes.drawCapsule(graphics, this.getX(), this.getY(), this.width, this.height, fill, outline, leftCircle);

		var font = Minecraft.getInstance().font;
		int radius = this.height / 2;
		int contentX = this.getX() + radius * 2 + 4;
		int textY = this.getY() + (this.height - font.lineHeight) / 2;

		if (!this.icon.isEmpty()) {
			int iconY = this.getY() + (this.height - ICON_SIZE) / 2;
			graphics.fakeItem(this.icon, contentX, iconY);
			contentX += ICON_SIZE + ICON_GAP;
		}

		Component label = this.getMessage();
		if (listening) {
			label = Component.translatable("screen.rootymenu.menu.keybind.listening");
		} else if (this.moduleId != null) {
			String bind = ModuleKeybinds.getBindName(this.moduleId);
			if (bind != null && !bind.isBlank()) {
				label = Component.empty()
						.append(this.getMessage())
						.append(Component.literal(" ["))
						.append(ModuleKeybinds.getBindDisplay(this.moduleId))
						.append(Component.literal("]"));
			}
		}
		// Keep the label inside the capsule (sub-option toggles are narrower).
		int maxTextWidth = this.getX() + this.width - radius / 2 - 4 - contentX;
		if (maxTextWidth > 0 && font.width(label) > maxTextWidth) {
			String ellipsis = "...";
			String cut = font.plainSubstrByWidth(label.getString(), Math.max(0, maxTextWidth - font.width(ellipsis)));
			label = Component.literal(cut + ellipsis);
		}
		graphics.text(font, label, contentX, textY, textColor, false);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.pressed = true;
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && this.moduleId != null) {
			if (ModuleKeybinds.isListening(this.moduleId)) {
				ModuleKeybinds.cancelListening();
			} else {
				ModuleKeybinds.startListening(this.moduleId);
			}
			return;
		}
		this.enabled = !this.enabled;
		this.onToggle.onToggle(this, this.enabled);
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.pressed = false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
