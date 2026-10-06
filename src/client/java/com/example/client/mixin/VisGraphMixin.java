package com.example.client.mixin;

import com.example.client.module.XRayModule;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Meteor {@code VisGraphMixin} + {@code ChunkOcclusionEvent}: while X-Ray is
 * active, do not mark blocks opaque in the visibility graph so caves/ores
 * behind solid walls remain rendered.
 */
@Mixin(VisGraph.class)
public class VisGraphMixin {
	@Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
	private void rooty$xrayCancelChunkOcclusion(BlockPos pos, CallbackInfo ci) {
		if (XRayModule.isActive()) {
			ci.cancel();
		}
	}
}
