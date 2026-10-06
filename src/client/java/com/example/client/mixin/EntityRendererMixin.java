package com.example.client.mixin;

import com.example.client.module.MobEspModule;
import com.example.client.module.NametagsModule;
import com.example.client.module.PlayerEspModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametags: clear {@code isDiscrete} so submitNameTag uses SEE_THROUGH.
 * Player / Mob ESP outline modes: force through-walls outline color.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void rooty$afterExtract(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (NametagsModule.isEnabled()) {
			state.isDiscrete = false;
		}
		if (entity instanceof LivingEntity living) {
			if (PlayerEspModule.shouldOutline(living)) {
				state.outlineColor = ARGB.opaque(PlayerEspModule.outlineColor());
			} else if (MobEspModule.shouldOutline(living)) {
				state.outlineColor = ARGB.opaque(MobEspModule.outlineColor(living));
			}
		}
	}
}
