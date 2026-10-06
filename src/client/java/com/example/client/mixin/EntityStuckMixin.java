package com.example.client.mixin;

import com.example.client.module.NoSlowModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No Slow: cancel cobweb / berry-bush stuck speed on the local player.
 */
@Mixin(Entity.class)
public class EntityStuckMixin {
	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
	private void rooty$noSlowStuck(BlockState state, Vec3 multiplier, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof LocalPlayer && NoSlowModule.shouldCancelStuckInBlock(self, state, multiplier)) {
			ci.cancel();
		}
	}
}
