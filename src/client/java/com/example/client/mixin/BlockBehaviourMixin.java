package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meteor {@code BlockBehaviourMixin} / {@code AmbientOcclusionEvent}: full
 * shade brightness while X-Ray is active so faded meshes are not crushed dark.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
	@Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayAmbientOcclusion(
			BlockState state,
			BlockGetter level,
			BlockPos pos,
			CallbackInfoReturnable<Float> cir
	) {
		if (XRayModule.isActive()) {
			cir.setReturnValue(1.0F);
		}
	}
}
