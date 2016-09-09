package szewek.mcflux.tileentities;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import szewek.mcflux.MCFlux;
import szewek.mcflux.blocks.BlockEnergyMachine;
import szewek.mcflux.network.UpdateMessageClient;
import szewek.mcflux.util.TransferType;

public abstract class TileEntityEnergyMachine extends TileEntity implements ITickable {
	TransferType[] sideTransfer = new TransferType[]{TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE};
	long[] sideValues = new long[]{0, 0, 0, 0, 0, 0};
	private IBlockState cachedState;
	private boolean refresh;

	public TileEntityEnergyMachine(IBlockState ibs) {
		super();
		cachedState = ibs;
	}

	public IBlockState getCachedState() {
		return cachedState;
	}

	@Override public void onLoad() {
		if (worldObj.isRemote)
			MCFlux.SNW.sendToServer(new UpdateMessageClient(pos));
	}

	@Override
	public void update() {
		if (refresh)
			worldObj.setBlockState(pos, cachedState, 3);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		int[] sides = compound.getIntArray("sides");
		if (sides.length != 6) return;
		TransferType[] tt = TransferType.values();
		IBlockState oldState = cachedState;
		for (int i = 0; i < 6; i++) {
			sideTransfer[i] = tt[sides[i]];
			cachedState = cachedState.withProperty(BlockEnergyMachine.sideFromId(i), sides[i]);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		int[] sides = new int[6];
		for (int i = 0; i < 6; i++) {
			sides[i] = sideTransfer[i].ord;
		}
		compound.setIntArray("sides", sides);
		return compound;
	}

	@Override
	public boolean shouldRefresh(World w, BlockPos pos, IBlockState obs, IBlockState nbs) {
		return obs.getBlock() != nbs.getBlock() || obs.getValue(BlockEnergyMachine.VARIANT) != nbs.getValue(BlockEnergyMachine.VARIANT);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, getBlockMetadata(), writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	public void switchSideTransfer(EnumFacing f) {
		int s = f.getIndex();
		int v = (sideTransfer[s].ord + 1) % 3;
		sideTransfer[s] = TransferType.values()[v];
		cachedState = cachedState.withProperty(BlockEnergyMachine.sideFromId(s), v);
		worldObj.setBlockState(pos, cachedState, 3);
		markDirty();
	}

	public long getTransferSide(EnumFacing f) {
		return sideValues[f.getIndex()];
	}

	public TransferType[] getAllTransferSides() {
		return sideTransfer;
	}

	public void updateTransferSides(TransferType[] tts) {
		for (int i = 0; i < 6; i++) {
			sideTransfer[i] = tts[i];
			cachedState = cachedState.withProperty(BlockEnergyMachine.sideFromId(i), tts[i].ord);
		}
		refresh = true;
	}
}
