package net.dugged.csworldedit.api;

import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Function;

public final class WorldEditApiFunction implements Function<NBTTagCompound, NBTTagCompound> {
	public WorldEditApiFunction() {
	}

	@Override
	public NBTTagCompound apply(NBTTagCompound request) {
		return PasteImcProtocol.handle(request);
	}
}
