package com.example.client.mixin;

import com.example.client.module.FreecamModule;
import com.example.client.module.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	/** Projection uses calculateFov → fov field, not getFov(). */
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void rooty$zoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ZoomModule.modifyFov(cir.getReturnValue()));
	}

	@Inject(method = "alignWithEntity", at = @At("HEAD"))
	private void rooty$freecamPartialTick(float partialTicks, CallbackInfo ci) {
		if (FreecamModule.isEnabled()) {
			FreecamModule.setPartialTick(partialTicks);
		}
	}

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void rooty$freecamDetached(float partialTicks, CallbackInfo ci) {
		if (FreecamModule.isEnabled()) {
			this.detached = true;
		}
	}

	@Redirect(
			method = "alignWithEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
	)
	private void rooty$freecamSetPosition(Camera self, double x, double y, double z) {
		if (FreecamModule.isEnabled()) {
			this.setPosition(FreecamModule.getX(), FreecamModule.getY(), FreecamModule.getZ());
		} else {
			this.setPosition(x, y, z);
		}
	}

	@Redirect(
			method = "alignWithEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V")
	)
	private void rooty$freecamSetRotation(Camera self, float yRot, float xRot) {
		if (FreecamModule.isEnabled()) {
			this.setRotation(FreecamModule.getYaw(), FreecamModule.getPitch());
		} else {
			this.setRotation(yRot, xRot);
		}
	}
}
