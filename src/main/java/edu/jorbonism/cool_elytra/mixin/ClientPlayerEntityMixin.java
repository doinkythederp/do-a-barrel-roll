package edu.jorbonism.cool_elytra.mixin;

import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import edu.jorbonism.cool_elytra.CoolElytraClient;
import edu.jorbonism.cool_elytra.config.CoolElytraConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.math.Vec3d;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

	public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile, PlayerPublicKey publicKey) { super(world, profile, publicKey); }

	@Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/ClientPlayNetworkHandler;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;ZZ)V")
	public void init(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, boolean lastSneaking, boolean lastSprinting, CallbackInfo ci) {
		CoolElytraClient.left = CoolElytraClient.getAssumedLeft(this.getYaw());
	}

	public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
		Vec3d facing = this.getRotationVecClient();

        // set left vector to the assumed upright left if not in flight in mode 2
		if (!this.isFallFlying() || CoolElytraConfig.mode != 2) {
			CoolElytraClient.left = CoolElytraClient.getAssumedLeft(this.getYaw());
            if (CoolElytraConfig.mode == 1) {
                CoolElytraClient.left = CoolElytraClient.rotateAxisAngle(CoolElytraClient.left, facing, CoolElytraClient.lastRollAngle * CoolElytraClient.TORAD);
            }
			super.changeLookDirection(cursorDeltaX, cursorDeltaY);
			return;
		}


		CoolElytraClient.left = CoolElytraClient.left.subtract(facing.multiply(CoolElytraClient.left.dotProduct(facing))).normalize();

		// pitch
		facing = CoolElytraClient.rotateAxisAngle(facing, CoolElytraClient.left, -0.15 * cursorDeltaY * CoolElytraClient.TORAD);
		
		if (this.isSneaking()) {
			// yaw
			Vec3d up = facing.crossProduct(CoolElytraClient.left);
			facing = CoolElytraClient.rotateAxisAngle(facing, up, 0.15 * cursorDeltaX * CoolElytraClient.TORAD);
			CoolElytraClient.left = CoolElytraClient.rotateAxisAngle(CoolElytraClient.left, up, 0.15 * cursorDeltaX * CoolElytraClient.TORAD);
		} else {
			// roll
			CoolElytraClient.left = CoolElytraClient.rotateAxisAngle(CoolElytraClient.left, facing, 0.15 * cursorDeltaX * CoolElytraClient.TORAD);
		}

		
		double deltaY = -Math.asin(facing.getY()) * CoolElytraClient.TODEG - this.getPitch();
		double deltaX = -Math.atan2(facing.getX(), facing.getZ()) * CoolElytraClient.TODEG - this.getYaw();

		super.changeLookDirection(deltaX / 0.15, deltaY / 0.15);
    }
}