package com.example.client.mixin;

import com.example.client.module.HitboxesModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inflates living-entity hitboxes while {@link HitboxesModule} is enabled.
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
}
