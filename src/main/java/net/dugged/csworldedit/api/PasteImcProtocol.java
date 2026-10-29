package net.dugged.csworldedit.api;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

final class PasteImcProtocol {
	private static final int TAG_COMPOUND = 10;

	private PasteImcProtocol() {
	}

	static NBTTagCompound handle(NBTTagCompound request) {
		if (request == null) {
			return error("BAD_REQUEST", "Missing request");
		}
		if (!request.hasKey("version") || request.getInteger("version") != PasteApi.VERSION) {
			return error("BAD_VERSION", "Unsupported paste API version");
		}
		if (!request.hasKey("op")) {
			return error("BAD_REQUEST", "Missing operation");
		}
		Minecraft mc = Minecraft.getMinecraft();
		String op = request.getString("op");
		try {
			if ("hello".equals(op)) {
				return hello(mc);
			}
			if (mc == null || !mc.isCallingFromMinecraftThread()) {
				return error("THREAD", "Paste API requests must run on the client thread");
			}
			if ("begin".equals(op)) {
				return begin(mc, request);
			}
			PasteSession session = session(request);
			if (session == null) {
				return error("NOT_FOUND", "Unknown paste session");
			}
			if ("batch".equals(op)) {
				return batch(session, request);
			}
			if ("commit".equals(op)) {
				boolean committed = session.commit();
				NBTTagCompound response = base();
				response.setBoolean("ok", committed);
				response.setString("code", committed ? "OK" : "CLOSED");
				fillProgress(response, session.progress());
				return response;
			}
			if ("cancel".equals(op)) {
				CancelReason reason = parseReason(request.getString("reason"));
				boolean cancelled = PasteApi.service().cancel(session.id(), reason);
				NBTTagCompound response = base();
				response.setBoolean("ok", cancelled);
				response.setString("code", cancelled ? "OK" : "CLOSED");
				fillProgress(response, session.progress());
				return response;
			}
			if ("progress".equals(op)) {
				NBTTagCompound response = base();
				response.setBoolean("ok", true);
				response.setString("code", "OK");
				fillProgress(response, session.progress());
				return response;
			}
			return error("BAD_OP", "Unknown operation");
		} catch (IllegalArgumentException exception) {
			return error("INVALID", exception.getMessage());
		} catch (IllegalStateException exception) {
			return error("CLOSED", exception.getMessage());
		} catch (Throwable throwable) {
			return error("INTERNAL", throwable.getClass().getSimpleName());
		}
	}

	private static NBTTagCompound hello(Minecraft mc) {
		NBTTagCompound response = base();
		response.setBoolean("ok", true);
		response.setString("code", "OK");
		response.setInteger("maxBatch", PasteApi.MAX_BATCH_CHANGES);
		response.setInteger("maxQueued", PasteApi.MAX_QUEUED_CHANGES);
		response.setBoolean("sp", mc != null && mc.world != null
				&& net.dugged.csworldedit.sp.SPEditBackend.isAvailable(mc.world));
		response.setBoolean("mp", true);
		return response;
	}

	private static NBTTagCompound begin(Minecraft mc, NBTTagCompound request) {
		if (mc.world == null || mc.player == null) {
			return error("NO_WORLD", "No client world");
		}
		if (request.hasKey("dimension")
				&& request.getInteger("dimension") != mc.world.provider.getDimension()) {
			return error("DIMENSION", "Dimension does not match the client world");
		}
		PasteOptions options = new PasteOptions(request.getBoolean("pasteAir"),
				request.hasKey("maxQueued") ? request.getInteger("maxQueued") : PasteApi.MAX_QUEUED_CHANGES);
		PasteSession session = PasteApi.service().begin(options);
		if (session == null) {
			return error("CAPACITY", "Too many active paste sessions");
		}
		NBTTagCompound response = base();
		response.setBoolean("ok", true);
		response.setString("code", "OK");
		response.setString("session", session.id());
		response.setString("undo", session.id());
		fillProgress(response, session.progress());
		return response;
	}

	private static NBTTagCompound batch(PasteSession session, NBTTagCompound request) {
		if (!request.hasKey("changes")) {
			return error("BAD_REQUEST", "Missing changes");
		}
		NBTTagList list = request.getTagList("changes", TAG_COMPOUND);
		if (list.tagCount() == 0 || list.tagCount() > PasteApi.MAX_BATCH_CHANGES) {
			return error("BAD_REQUEST", "Invalid batch size");
		}
		List<BlockChange> changes = new ArrayList<>(list.tagCount());
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound entry = list.getCompoundTagAt(i);
			if (!entry.hasKey("x") || !entry.hasKey("y") || !entry.hasKey("z")
					|| !entry.hasKey("state") || !entry.hasKey("meta")) {
				return error("BAD_REQUEST", "Invalid block change");
			}
			int x = entry.getInteger("x");
			int y = entry.getInteger("y");
			int z = entry.getInteger("z");
			if (Math.abs((long) x) > 30000000L || Math.abs((long) z) > 30000000L) {
				return error("INVALID_POSITION", "Block position out of range");
			}
			ResourceLocation name = new ResourceLocation(entry.getString("state"));
			Block block = Block.REGISTRY.getObject(name);
			if (block == null) {
				return error("INVALID_BLOCK", "Unknown block " + name);
			}
			IBlockState state;
			try {
				state = block.getStateFromMeta(entry.getInteger("meta"));
			} catch (RuntimeException exception) {
				return error("INVALID_BLOCK", "Invalid block metadata for " + name);
			}
			NBTTagCompound tileEntity = entry.hasKey("nbt", TAG_COMPOUND) ? entry.getCompoundTag("nbt").copy() : null;
			changes.add(new BlockChange(new BlockPos(x, y, z), state, tileEntity));
		}
		SubmitResult result = session.submit(changes);
		NBTTagCompound response = base();
		response.setBoolean("ok", result == SubmitResult.ACCEPTED);
		response.setString("code", result.name());
		fillProgress(response, session.progress());
		return response;
	}

	private static PasteSession session(NBTTagCompound request) {
		if (!request.hasKey("session")) {
			return null;
		}
		return PasteApi.service().session(request.getString("session"));
	}

	private static CancelReason parseReason(String reason) {
		try {
			return CancelReason.valueOf(reason);
		} catch (IllegalArgumentException exception) {
			return CancelReason.USER;
		}
	}

	private static NBTTagCompound base() {
		NBTTagCompound response = new NBTTagCompound();
		response.setInteger("version", PasteApi.VERSION);
		response.setString("schema", PasteApi.SCHEMA);
		return response;
	}

	private static NBTTagCompound error(String code, String message) {
		NBTTagCompound response = base();
		response.setBoolean("ok", false);
		response.setString("code", code);
		response.setString("message", message == null ? code : message);
		return response;
	}

	private static void fillProgress(NBTTagCompound response, PasteProgress progress) {
		response.setString("session", progress.sessionId());
		response.setString("undo", progress.sessionId());
		response.setString("state", progress.state().name());
		response.setLong("processed", progress.processed());
		response.setLong("total", progress.total());
		response.setInteger("queued", progress.queuedChanges());
		response.setInteger("nbtOmitted", progress.nbtOmitted());
		response.setString("cancel", progress.cancelReason() == null ? "" : progress.cancelReason().name());
	}
}
