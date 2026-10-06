package com.example.client.mixin;

import com.example.client.config.MenuTheme;
import com.example.client.module.NametagsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When Nametags is ON: scale nametags by the module slider, and set text
 * outlineColor so Font draws an 8x outline around the nametag.
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

	@ModifyArg(
			method = "nameTag",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"
			),
			index = 6
	)
	private static int rooty$nametagOutlineColor(int outlineColor) {
		if (NametagsModule.isEnabled()) {
			return ARGB.opaque(MenuTheme.get().outline);
		}
		return outlineColor;
	}
}
