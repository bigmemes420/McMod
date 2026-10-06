package com.example.client.mixin;

import com.example.client.config.MenuTheme;
import com.example.client.module.NametagsModule;
import com.example.client.module.PlayerOutlinesModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametags: clear {@code isDiscrete} so submitNameTag uses SEE_THROUGH.
 * Player Outlines: force through-walls outline color on other players.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void rooty$afterExtract(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (NametagsModule.isEnabled()) {
			state.isDiscrete = false;
		}
		if (PlayerOutlinesModule.isEnabled()
				&& entity instanceof Player
				&& !(entity instanceof LocalPlayer)) {
			state.outlineColor = ARGB.opaque(MenuTheme.get().outline);
		}
	}
}
