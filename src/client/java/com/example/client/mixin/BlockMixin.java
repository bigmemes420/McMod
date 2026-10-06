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
 * Meteor {@code BlockMixin}: force-render every face of whitelisted X-Ray
 * blocks so ores stay visible through faded/hidden neighbors (runs at HEAD
 * for More Culling compatibility).
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
