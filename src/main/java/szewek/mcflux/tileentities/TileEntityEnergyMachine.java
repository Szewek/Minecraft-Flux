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
import szewek.fl.energy.Battery;
import szewek.fl.energy.IEnergy;
import szewek.mcflux.MCFluxResources;
import szewek.mcflux.api.MCFluxAPI;
import szewek.mcflux.blocks.BlockEnergyMachine;
import szewek.mcflux.blocks.BlockSided;
import szewek.mcflux.config.MCFluxConfig;
import szewek.mcflux.network.MCFluxNetwork;
import szewek.mcflux.network.Msg;
import szewek.mcflux.util.TransferType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntBinaryOperator;

import static szewek.mcflux.config.MCFluxConfig.CHUNK_CHARGER_TRANS;

public final class TileEntityEnergyMachine extends TileEntityWCEAware implements ITickable {
	private boolean oddTick = true, clientUpdate = true, serverUpdate = false;
	private final TransferType[] sideTransfer = new TransferType[]{TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE, TransferType.NONE};
	private final long[] sideValues = new long[]{0, 0, 0, 0, 0, 0};
	private IBlockState cachedState = MCFluxResources.SIDED.getDefaultState();
	private IntBinaryOperator module;
	private int moduleId;

	public IBlockState getCachedState() {
		return cachedState;
	}

	public void setModuleId(int id) {
		moduleId = id;
		module = getModule(id);
	}

	public int getModuleId() {
		return moduleId;
	}

	@Nullable
	private IntBinaryOperator getModule(int i) {
		switch (i) {
			case 0: return this::moduleEnergyDistributor;
			case 1: return this::moduleChunkCharger;
		}
		return null;
	}

	@Override public void onLoad() {
		if (world.isRemote) {
			MCFluxNetwork.toServer(Msg.update(pos, null));
			clientUpdate = false;
		}
	}

	@Override
	public void updateTile() {
		if (world.isRemote && clientUpdate) {
			MCFluxNetwork.toServer(Msg.update(pos, null));
			clientUpdate = false;
		} else if (!world.isRemote && serverUpdate) {
			MCFluxNetwork.toDimension(Msg.update(pos, sideTransfer), world.provider.getDimension());
			serverUpdate = false;
		}
		if (!world.isRemote && bat != null) {
			int i = oddTick ? 0 : 3, m = i + 3;
			for (int j = i; j < m; j++)
				sideValues[j] = 0;
			if (module != null)
				module.applyAsInt(i, m);
		}
		oddTick = !oddTick;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		moduleId = nbt.getInteger("module");
		int[] sides = nbt.getIntArray("sides");
		if (sides.length != 6) return;
		TransferType[] tt = TransferType.values();
		for (int i = 0; i < 6; i++) {
			sideTransfer[i] = tt[sides[i]];
			cachedState = cachedState.withProperty(BlockSided.sideFromId(i), sides[i]);
		}
		module = getModule(moduleId);
		serverUpdate = true;
	}

	@Override
	@Nonnull
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		int[] sides = new int[6];
		for (int i = 0; i < 6; i++) {
			sides[i] = sideTransfer[i].ord;
		}
		nbt.setIntArray("sides", sides);
		nbt.setInteger("module", moduleId);
		return nbt;
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

	@Override public boolean hasFastRenderer() {
		return true;
	}

	public void switchSideTransfer(EnumFacing f) {
		int s = f.getIndex();
		int v = (sideTransfer[s].ord + 1) % 3;
		sideTransfer[s] = TransferType.values()[v];
		cachedState = cachedState.withProperty(BlockSided.sideFromId(s), v);
		MCFluxNetwork.toDimension(Msg.update(pos, sideTransfer), world.provider.getDimension());
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
			cachedState = cachedState.withProperty(BlockSided.sideFromId(i), (int) tts[i].ord);
		}
	}

	private int moduleEnergyDistributor(int i, int m) {
		for (; i < m; i++) {
			TransferType tt = sideTransfer[i];
			if (tt == TransferType.NONE)
				continue;
			EnumFacing f = EnumFacing.VALUES[i];
			TileEntity te = world.getTileEntity(pos.offset(f, 1));
			if (te == null)
				continue;
			f = f.getOpposite();
			IEnergy ea = MCFluxAPI.getEnergySafely(te, f);
			if (ea == null)
				continue;
			switch (tt) {
				case INPUT:
					sideValues[i] = ea.to(bat, MCFluxConfig.ENERGY_DIST_TRANS * 2) / 2;
					break;
				case OUTPUT:
					sideValues[i] = bat.to(ea, MCFluxConfig.ENERGY_DIST_TRANS * 2) / 2;
					break;
			}
		}
		return 0;
	}

	private int moduleChunkCharger(int i, int m) {
		for (; i < m; i++) {
			TransferType tt = sideTransfer[i];
			if (tt == TransferType.NONE)
				continue;
			EnumFacing f = EnumFacing.VALUES[i];
			BlockPos bpc = pos.offset(f, 16);
			Battery ebc = wce.getEnergyChunk(bpc.getX(), bpc.getY(), bpc.getZ());
			if (ebc == null)
				continue;
			switch (tt) {
				case INPUT:
					sideValues[i] = ebc.to(bat, CHUNK_CHARGER_TRANS * 2) / 2;
					break;
				case OUTPUT:
					sideValues[i] = bat.to(ebc, CHUNK_CHARGER_TRANS * 2) / 2;
					break;
				default:
			}
		}
		return 0;
	}
}
