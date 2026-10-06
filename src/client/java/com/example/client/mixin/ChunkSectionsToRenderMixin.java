package com.example.client.mixin;

import com.example.client.module.XRayModule;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remesh-free X-Ray: skip OPAQUE terrain (SOLID + CUTOUT) each frame while
 * active. Opacity / selection never touch the section compile queue.
 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {
	@Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
	private void rooty$xraySkipOpaque(
			ChunkSectionLayerGroup group,
			RenderPass renderPass,
			GpuSampler sampler,
			GpuTextureView atlas,
			boolean renderWireframeTerrain,
			CallbackInfo ci
	) {
		if (group == ChunkSectionLayerGroup.OPAQUE && XRayModule.shouldHideOpaqueTerrain()) {
			ci.cancel();
		}
	}
}
