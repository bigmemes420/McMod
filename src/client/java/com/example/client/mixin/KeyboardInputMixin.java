package com.example.client.mixin;

import com.example.client.module.InventoryMoveModule;
import com.example.client.module.SneakModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory Move: rebuild WASD from physical keys while a container is open.
 * Sneak module: force the shift bit (Legit / Cheat) after input is built.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Shadow
	@Final
	private Options options;

	@Inject(method = "tick", at = @At("TAIL"))
	private void rooty$inventoryMoveAndSneak(CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (InventoryMoveModule.shouldPassMovement(client)) {
			boolean forward = isPhysicallyDown(this.options.keyUp);
			boolean backward = isPhysicallyDown(this.options.keyDown);
			boolean left = isPhysicallyDown(this.options.keyLeft);
			boolean right = isPhysicallyDown(this.options.keyRight);
			boolean jump = isPhysicallyDown(this.options.keyJump);
			boolean shift = isPhysicallyDown(this.options.keyShift);
			boolean sprint = isPhysicallyDown(this.options.keySprint);
			this.keyPresses = new Input(forward, backward, left, right, jump, shift, sprint);
			float impulseY = impulse(forward, backward);
			float impulseX = impulse(left, right);
			this.moveVector = new Vec2(impulseX, impulseY).normalized();
		}
		SneakModule.applyInput(client);
	}

	private static float impulse(boolean positive, boolean negative) {
		if (positive == negative) {
			return 0.0F;
		}
		return positive ? 1.0F : -1.0F;
	}

	private static boolean isPhysicallyDown(net.minecraft.client.KeyMapping mapping) {
		InputConstants.Key key = ((KeyMappingAccessor) mapping).rooty$getKey();
		if (key.getType() != InputConstants.Type.KEYBOARD) {
			return false;
		}
		return InputConstants.isKeyDown(key.getValue());
	}
}
