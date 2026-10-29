package net.dugged.csworldedit.mixins;

import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.clientcommands.ClientCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value =
			"RETURN"))
	public void onWorldLoad(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
		if (!Configuration.general.enabled) {
			return;
		}
		ClientCommandHandler.updatePermissions();
	}
}
