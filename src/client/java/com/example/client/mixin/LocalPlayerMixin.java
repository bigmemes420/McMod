package com.example.client.mixin;

import com.example.client.module.NoSlowModule;
import com.example.client.module.SneakModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * No Slow: skip the item-use speed multiplier inside {@code modifyInput}.
 * Sneak Cheat: rewrite the input snapshot used by {@code sendChanges} so the
 * server sees shift without forcing client sneak pose/movement.
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

	@Redirect(
			method = "sendChanges",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;",
					opcode = Opcodes.GETFIELD
			)
	)
	private Input rooty$packetSneak(ClientInput input) {
		return SneakModule.maybePacketSneak(input.keyPresses);
	}
}
