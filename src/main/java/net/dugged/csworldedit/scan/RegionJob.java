package net.dugged.csworldedit.scan;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicBoolean;

final class RegionJob extends ClientJob {

	private final SnapshotManager manager;
	private final BlockReader reader;
	private final BedColorReader bedColorReader;
	private final BlockSnapshot.Builder builder;
	private final RegionAnalysis analysis;
	private final int sizeX;
	private final int sizeZ;
	private final AtomicBoolean retentionReleased = new AtomicBoolean();
	private int cursor;
	private boolean dispatched;

	RegionJob(SnapshotManager manager, World world, BlockReader reader, BedColorReader bedColorReader,
			  BlockSnapshot.Builder builder, RegionAnalysis analysis) {
		super(world);
		this.manager = manager;
		this.reader = reader;
		this.bedColorReader = bedColorReader;
		this.builder = builder;
		this.analysis = analysis;
		this.sizeX = builder.sizeX();
		this.sizeZ = builder.sizeZ();
	}

	@Override
	public int step(int budget) {
		if (budget <= 0 || isCancelled()) {
			return 0;
		}
		int remaining = builder.length() - cursor;
		int count = Math.min(budget, remaining);
		int minX = builder.minX();
		int minY = builder.minY();
		int minZ = builder.minZ();
		for (int i = 0; i < count; i++) {
			int index = cursor + i;
			int local = index;
			int x = minX + local % sizeX;
			local /= sizeX;
			int z = minZ + local % sizeZ;
			int y = minY + local / sizeZ;
			IBlockState state = reader.getBlock(x, y, z);
			builder.setIndex(index, state);
			if (bedColorReader != null) {
				int bedColor = -1;
				if (state != null && state.getBlock() instanceof BlockBed
						&& state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
					bedColor = bedColorReader.getBedColorValue(x, y, z);
				}
				builder.setBedColorValue(index, bedColor);
			}
		}
		cursor += count;
		if (cursor >= builder.length()) {
			markFinished();
			dispatch();
		}
		return count;
	}

	private void dispatch() {
		if (dispatched) {
			return;
		}
		dispatched = true;
		manager.dispatch(this);
	}

	void runAnalysis() {
		if (isCancelled()) {
			releaseRetention();
			return;
		}
		BlockSnapshot snapshot = builder.build();
		ScanContext context = new ScanContext() {
			@Override
			public boolean isCancelled() {
				return RegionJob.this.isCancelled();
			}

			@Override
			public void postClient(ClientAction action) {
				manager.postClient(RegionJob.this, action);
			}
		};
		try {
			analysis.analyze(snapshot, context);
		} catch (Throwable t) {
			SnapshotManager.LOGGER.error("Region analysis failed", t);
		} finally {
			releaseRetention();
		}
	}

	void releaseRetention() {
		if (retentionReleased.compareAndSet(false, true)) {
			manager.releaseVolume(builder.length());
		}
	}
}
