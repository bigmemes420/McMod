package com.example.client.mixin;

import com.example.client.module.NametagsModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When Nametags is ON, clear {@code isDiscrete} so submitNameTag uses SEE_THROUGH
 * (render through walls), including for sneaking entities.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void rooty$nametagsSeeThrough(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (NametagsModule.isEnabled()) {
			state.isDiscrete = false;
		}
	}
}
