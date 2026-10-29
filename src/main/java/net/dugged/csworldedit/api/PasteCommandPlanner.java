package net.dugged.csworldedit.api;

import net.dugged.csworldedit.tasks.TaskFill;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCake;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockEndRod;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PasteCommandPlanner {
	private PasteCommandPlanner() {
	}

	static Map<ChunkPos, Deque<Command>> plan(List<BlockChange> changes) {
		Map<ChunkPos, List<BlockChange>> immediate = new LinkedHashMap<>();
		Map<ChunkPos, List<BlockChange>> delayed = new LinkedHashMap<>();
		for (BlockChange change : changes) {
			ChunkPos chunk = new ChunkPos(change.pos());
			Map<ChunkPos, List<BlockChange>> target = placeLast(change.state().getBlock()) ? delayed : immediate;
			target.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(change);
		}

		Map<ChunkPos, Deque<Command>> result = new LinkedHashMap<>();
		for (Map.Entry<ChunkPos, List<BlockChange>> entry : immediate.entrySet()) {
			Deque<Command> queue = new ArrayDeque<>();
			queue.addAll(buildBatches(entry.getValue()));
			result.put(entry.getKey(), queue);
		}
		for (Map.Entry<ChunkPos, List<BlockChange>> entry : delayed.entrySet()) {
			Deque<Command> queue = result.computeIfAbsent(entry.getKey(), ignored -> new ArrayDeque<>());
			for (BlockChange change : entry.getValue()) {
				queue.add(Command.single(change.pos(), change.state()));
			}
		}
		return result;
	}

	static boolean placeLast(Block block) {
		return block instanceof BlockBed || block instanceof BlockBush || block instanceof BlockFlowerPot
				|| block instanceof BlockFire || block instanceof BlockButton || block instanceof BlockSign
				|| block instanceof BlockChorusFlower || block instanceof BlockCake || block instanceof BlockCarpet
				|| block instanceof BlockRailBase || block instanceof BlockEndRod || block instanceof BlockLever
				|| block instanceof BlockRedstoneWire || block instanceof BlockCactus || block instanceof BlockVine
				|| block instanceof BlockSnow || block instanceof BlockTorch || block instanceof BlockLadder
				|| block instanceof BlockBanner || block instanceof BlockDoor || block instanceof BlockRedstoneDiode
				|| block instanceof BlockBasePressurePlate || block instanceof BlockPistonMoving
				|| block instanceof BlockPistonExtension || block instanceof BlockReed || block instanceof BlockTripWireHook
				|| block instanceof BlockDoublePlant || block instanceof BlockMushroom;
	}

	private static List<Command> buildBatches(List<BlockChange> changes) {
		Map<IBlockState, List<BlockChange>> byState = new LinkedHashMap<>();
		for (BlockChange change : changes) {
			byState.computeIfAbsent(change.state(), ignored -> new ArrayList<>()).add(change);
		}

		List<Command> boxes = new ArrayList<>();
		for (Map.Entry<IBlockState, List<BlockChange>> entry : byState.entrySet()) {
			List<BlockChange> ops = entry.getValue();
			ops.sort(Comparator.comparingInt((BlockChange change) -> change.pos().getY())
					.thenComparingInt(change -> change.pos().getZ())
					.thenComparingInt(change -> change.pos().getX()));
			boxes.addAll(mergeAlongZ(mergeAlongY(buildXRuns(ops, entry.getKey()), entry.getKey()), entry.getKey()));
		}
		return boxes;
	}

	private static List<Command> buildXRuns(List<BlockChange> changes, IBlockState state) {
		List<Command> runs = new ArrayList<>();
		Command current = null;
		for (BlockChange change : changes) {
			BlockPos pos = change.pos();
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			if (current != null && current.y1 == y && current.z1 == z && current.x2 + 1 == x) {
				current.x2 = x;
				current.count++;
			} else {
				current = Command.box(x, y, z, x, y, z, state);
				current.count = 1;
				runs.add(current);
			}
		}
		return runs;
	}

	private static List<Command> mergeAlongY(List<Command> runs, IBlockState state) {
		Map<BatchKey, Command> latest = new HashMap<>();
		List<Command> merged = new ArrayList<>();
		for (Command run : runs) {
			BatchKey key = new BatchKey(state, run.x1, run.x2, run.z1, 0);
			Command previous = latest.get(key);
			if (previous != null && previous.y2 + 1 == run.y1
					&& volume(previous.x2 - previous.x1 + 1, run.y2 - previous.y1 + 1,
					previous.z2 - previous.z1 + 1) <= TaskFill.VANILLA_FILL_LIMIT) {
				previous.y2 = run.y2;
				previous.count += run.count;
			} else {
				Command box = Command.box(run.x1, run.y1, run.z1, run.x2, run.y2, run.z1, state);
				box.count = run.count;
				merged.add(box);
				latest.put(key, box);
			}
		}
		return merged;
	}

	private static List<Command> mergeAlongZ(List<Command> boxes, IBlockState state) {
		boxes.sort(Comparator.comparingInt((Command box) -> box.y1)
				.thenComparingInt(box -> box.y2)
				.thenComparingInt(box -> box.z1)
				.thenComparingInt(box -> box.x1));
		Map<BatchKey, Command> latest = new HashMap<>();
		List<Command> merged = new ArrayList<>();
		for (Command box : boxes) {
			BatchKey key = new BatchKey(state, box.x1, box.x2, box.y1, box.y2);
			Command previous = latest.get(key);
			if (previous != null && previous.z2 + 1 == box.z1
					&& volume(box.x2 - box.x1 + 1, box.y2 - box.y1 + 1,
					box.z2 - previous.z1 + 1) <= TaskFill.VANILLA_FILL_LIMIT) {
				previous.z2 = box.z2;
				previous.count += box.count;
			} else {
				merged.add(box);
				latest.put(key, box);
			}
		}
		return merged;
	}

	private static long volume(int x, int y, int z) {
		return (long) x * y * z;
	}

	static final class Command {
		final IBlockState state;
		int x1;
		int y1;
		int z1;
		int x2;
		int y2;
		int z2;
		int count;

		private Command(IBlockState state, int x1, int y1, int z1, int x2, int y2, int z2) {
			this.state = state;
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
		}

		static Command box(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
			return new Command(state, x1, y1, z1, x2, y2, z2);
		}

		static Command single(BlockPos pos, IBlockState state) {
			Command command = box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ(), state);
			command.count = 1;
			return command;
		}

		boolean single() {
			return x1 == x2 && y1 == y2 && z1 == z2;
		}
	}

	private record BatchKey(IBlockState state, int a, int b, int c, int d) {
	}
}
