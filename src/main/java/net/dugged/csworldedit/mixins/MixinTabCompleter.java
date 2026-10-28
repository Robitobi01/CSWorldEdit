package net.dugged.csworldedit.mixins;


import com.google.common.collect.ObjectArrays;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.clientcommands.ClientCommandHandler;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabCompleter.class)
public abstract class MixinTabCompleter {

	@ModifyArg(method = "complete", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;" +
			"writeText(Ljava/lang/String;)V"))
	private String stripFormattingCodes(String text) {
		return TextFormatting.getTextWithoutFormattingCodes(text);
	}

	@Inject(method = "requestCompletions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network" +
			"/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V"))
	private void addAutoComplete(String prefix, CallbackInfo ci) {
		if (!Configuration.enabled) {
			return;
		}
		ClientCommandHandler.getInstance().autoComplete(prefix);
	}

	@ModifyVariable(method = "setCompletions", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", remap =
			false), argsOnly = true)
	private String[] setLatestAutoComplete(String... completions) {
		if (!Configuration.enabled) {
			return completions;
		}
		final String[] complete = ClientCommandHandler.getInstance().latestAutoComplete;
		if (complete != null) {
			return ObjectArrays.concat(complete, completions, String.class);
		}
		return completions;
	}

	@ModifyExpressionValue(method = "setCompletions", at = @At(value = "INVOKE", target =
			"Lorg/apache/commons/lang3/StringUtils;" + "getCommonPrefix([Ljava/lang/String;)Ljava/lang/String;",
			remap = false))
	private String adjustedGetCommonPrefix(String string) {
		return TextFormatting.getTextWithoutFormattingCodes(string);
	}
}