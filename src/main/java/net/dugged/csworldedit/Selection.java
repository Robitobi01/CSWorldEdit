package net.dugged.csworldedit;

import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.HashMap;

public class Selection {

	private final HashMap<Position, BlockPos> positions = new HashMap<>();
	private final SelectionType type;

	public Selection(SelectionType type) {
		this.type = type;
	}

	public static SelectionType getTypeForMaterial(Item.ToolMaterial material) {
		switch (material) {
			case WOOD:
				return SelectionType.WOOD;
			case STONE:
				return SelectionType.STONE;
			case IRON:
				return SelectionType.IRON;
			case DIAMOND:
				return SelectionType.DIAMOND;
			case GOLD:
				return SelectionType.GOLD;
		}
		return null;
	}

	public BlockPos getPos(Position pos) {
		return positions.getOrDefault(pos, null);
	}

	public void setPos(Position pos, BlockPos blockPos) {
		if (blockPos != null) {
			int cappedY = Math.max(0, Math.min(blockPos.getY(), 255));
			blockPos = new BlockPos(blockPos.getX(), cappedY, blockPos.getZ());
		}
		positions.put(pos, blockPos);
		Manager.selections.put(type, this);
	}


	public boolean isCompleted() {
		return positions.containsKey(Position.A) && positions.get(Position.A) != null && positions.containsKey(Position.B) && positions.get(Position.B) != null;
	}

	public AxisAlignedBB getBB() {
		return new AxisAlignedBB(positions.get(Position.A), positions.get(Position.B));
	}

	public BlockPos minPos() {
		return new BlockPos(getBB().minX, getBB().minY, getBB().minZ);
	}

	public BlockPos maxPos() {
		return new BlockPos(getBB().maxX, getBB().maxY, getBB().maxZ);
	}

	public int widthX() {
		return maxPos().getX() - minPos().getX() + 1;
	}

	public int widthY() {
		return maxPos().getY() - minPos().getY() + 1;
	}

	public int widthZ() {
		return maxPos().getZ() - minPos().getZ() + 1;
	}

	public long volume() {
		return (long) widthX() * widthY() * widthZ();
	}

	public boolean isOneByOne() {
		return widthX() == 1 && widthY() == 1 && widthZ() == 1;
	}

	public Selection copy() {
		Selection copy = new Selection(type);
		copy.positions.putAll(positions);
		return copy;
	}

	public enum SelectionType {
		WOOD(134, 101, 38), STONE(154, 154, 154), IRON(255, 255, 255), DIAMOND(51, 235, 203), GOLD(234, 238, 87);

		private final Color color;

		SelectionType(int r, int g, int b) {
			this.color = new Color(r, g, b);
		}

		public Color getColor() {
			return color;
		}
	}

	public enum Position {
		A(255, 0, 0), B(0, 0, 255);

		private final Color color;

		Position(int r, int g, int b) {
			this.color = new Color(r, g, b);
		}

		public Color getColor() {
			return color;
		}
	}
}
