package com.example.client.mixin;

import com.example.client.module.FreecamModule;
import com.example.client.module.HitboxesModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hitboxes inflate; Freecam mouse look (player body does not turn).
 */
@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
	private void rooty$hitboxes(CallbackInfoReturnable<AABB> cir) {
		Entity self = (Entity) (Object) this;
		AABB modified = HitboxesModule.modifyBoundingBox(self, cir.getReturnValue());
		if (modified != cir.getReturnValue()) {
			cir.setReturnValue(modified);
		}
	}

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void rooty$freecamLook(double yRot, double xRot, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (FreecamModule.shouldRedirectLook(self)) {
			FreecamModule.changeLookDirection(yRot * 0.15, xRot * 0.15);
			ci.cancel();
		}
	}
}
