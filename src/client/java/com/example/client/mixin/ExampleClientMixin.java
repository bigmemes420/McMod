package com.example.client.mixin;

import com.example.client.module.FreecamModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class ExampleClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		// This code is injected into the start of Minecraft.run()V
	}

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void rooty$freecamBlockAttack(CallbackInfoReturnable<Boolean> cir) {
		if (FreecamModule.shouldBlockInteract()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void rooty$freecamBlockContinueAttack(boolean leftClick, CallbackInfo ci) {
		if (FreecamModule.shouldBlockInteract()) {
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void rooty$freecamBlockUse(CallbackInfo ci) {
		if (FreecamModule.shouldBlockInteract()) {
			ci.cancel();
		}
	}
}
