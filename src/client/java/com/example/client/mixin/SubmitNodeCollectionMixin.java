package com.example.client.mixin;

import com.example.client.config.MenuTheme;
import com.example.client.module.NametagsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When Nametags is ON: scale nametags by the module slider, and draw a colored
 * outline around the nametag plate/background (not a text glyph outline).
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

	@Inject(
			method = "submitNameTag",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
			)
	)
	private void rooty$nametagPlateOutline(
			PoseStack poseStack,
			Vec3 attachment,
			int yOffset,
			Component name,
			boolean discrete,
			int lightCoords,
			CameraRenderState camera,
			CallbackInfo ci
	) {
		if (!NametagsModule.isEnabled() || name == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		float width = client.font.width(name);
		float x = -width / 2.0F;
		float y = (float) yOffset;

		// Match Font background plate: (x-1, y-1) .. (x+width, y+9)
		float x0 = x - 1.0F;
		float y0 = y - 1.0F;
		float x1 = x + width;
		float y1 = y + 9.0F;
		float t = 1.0F;

		int color = ARGB.opaque(MenuTheme.get().outline);
		SubmitNodeCollection self = (SubmitNodeCollection) (Object) this;
		Font.DisplayMode mode = Font.DisplayMode.SEE_THROUGH;

		// Outer frame around the plate (top / bottom / left / right)
		self.submitTextBackground(poseStack, x0 - t, y0 - t, x1 + t, y0, color, mode, lightCoords);
		self.submitTextBackground(poseStack, x0 - t, y1, x1 + t, y1 + t, color, mode, lightCoords);
		self.submitTextBackground(poseStack, x0 - t, y0, x0, y1, color, mode, lightCoords);
		self.submitTextBackground(poseStack, x1, y0, x1 + t, y1, color, mode, lightCoords);
	}
}
