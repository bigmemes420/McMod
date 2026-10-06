package com.example.client.mixin;

import com.example.client.module.NametagsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When Nametags is ON, multiply the hardcoded nametag PoseStack scale by the
 * module slider (1.0 = vanilla size).
 */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
	@Redirect(
			method = "submitNameTag",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
			)
	)
	private void rooty$applyNametagScale(PoseStack poseStack, float x, float y, float z) {
		if (NametagsModule.isEnabled()) {
			float s = NametagsModule.getScale();
			poseStack.scale(x * s, y * s, z * s);
		} else {
			poseStack.scale(x, y, z);
		}
	}
}
