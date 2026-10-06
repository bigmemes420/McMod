package com.example.client.mixin;

import com.example.client.module.NametagsModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When Nametags is ON, ignore the discrete/sneaking distance clamp so names
 * still show while the entity is sneaking (through-walls path also needs this).
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Redirect(
			method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;isDiscrete()Z"
			)
	)
	private boolean rooty$ignoreDiscreteForNametags(LivingEntity entity) {
		if (NametagsModule.isEnabled()) {
			return false;
		}
		return entity.isDiscrete();
	}
}
