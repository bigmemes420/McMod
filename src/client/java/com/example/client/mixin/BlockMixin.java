package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meteor-style X-Ray face force: selected blocks always draw every face while
 * X-Ray is active ({@code Block.shouldRenderFace} ≡ classic shouldDrawSide).
 */
@Mixin(Block.class)
public class BlockMixin {
	@Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
	private static void rooty$xrayForceFace(
			BlockState state,
			BlockState neighborState,
			Direction direction,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (XRayModule.shouldForceRenderFace(state, neighborState, direction)) {
			cir.setReturnValue(true);
		}
	}
}
