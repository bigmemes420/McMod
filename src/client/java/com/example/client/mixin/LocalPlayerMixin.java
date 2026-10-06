package com.example.client.mixin;

import com.example.client.module.NoSlowModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * No Slow: skip the item-use speed multiplier inside {@code modifyInput}.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Redirect(
			method = "modifyInput",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
			)
	)
	private boolean rooty$noSlowItemUse(LocalPlayer self) {
		if (NoSlowModule.shouldCancelItemSlowdown()) {
			return false;
		}
		return self.isUsingItem();
	}
}
