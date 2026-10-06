package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * X-Ray: force non-selected blocks to {@link RenderShape#INVISIBLE} so ores
 * and other full-opacity picks stay visible through terrain.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
	@Inject(method = "getRenderShape", at = @At("RETURN"), cancellable = true)
	private void rooty$xrayRenderShape(CallbackInfoReturnable<RenderShape> cir) {
		BlockState state = (BlockState) (Object) this;
		RenderShape modified = XRayModule.modifyRenderShape(state, cir.getReturnValue());
		if (modified != cir.getReturnValue()) {
			cir.setReturnValue(modified);
		}
	}
}
