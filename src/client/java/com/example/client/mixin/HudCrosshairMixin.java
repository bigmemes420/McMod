package com.example.client.mixin;

import com.example.client.module.CustomCrosshairModule;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla crosshair while Custom Crosshair is enabled.
 */
@Mixin(Hud.class)
public class HudCrosshairMixin {
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void rooty$hideVanillaCrosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		if (CustomCrosshairModule.isEnabled()) {
			ci.cancel();
		}
	}
}
