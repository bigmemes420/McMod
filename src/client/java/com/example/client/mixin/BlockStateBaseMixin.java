package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * X-Ray (classic Fabric / Meteor-style for 26.3):
 * <ul>
 *   <li>Hide non-selected at opacity 0 via {@link RenderShape#INVISIBLE}.</li>
 *   <li>{@code canOcclude=false} + empty face occlusion on non-selected so
 *       selected ores mesh every face through solid walls.</li>
 * </ul>
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

	@Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayCanOcclude(CallbackInfoReturnable<Boolean> cir) {
		BlockState state = (BlockState) (Object) this;
		if (XRayModule.shouldDisableOcclusion(state)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayFaceOcclusion(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
		BlockState state = (BlockState) (Object) this;
		if (XRayModule.shouldDisableOcclusion(state)) {
			cir.setReturnValue(Shapes.empty());
		}
	}

	@Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayOcclusionShape(CallbackInfoReturnable<VoxelShape> cir) {
		BlockState state = (BlockState) (Object) this;
		if (XRayModule.shouldDisableOcclusion(state)) {
			cir.setReturnValue(Shapes.empty());
		}
	}
}
