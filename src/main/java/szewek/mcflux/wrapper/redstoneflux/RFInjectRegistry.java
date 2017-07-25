package szewek.mcflux.wrapper.redstoneflux;

import cofh.redstoneflux.api.IEnergyContainerItem;
import cofh.redstoneflux.api.IEnergyHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import szewek.mcflux.util.IInjectRegistry;
import szewek.mcflux.util.InjectCond;
import szewek.mcflux.util.InjectRegistry;
import szewek.mcflux.wrapper.EnergyType;
import szewek.mcflux.wrapper.InjectCollector;
import szewek.mcflux.wrapper.InjectWrappers;
import szewek.mcflux.wrapper.WrapperRegistry;

@InjectRegistry(requires = InjectCond.MOD, args = {"redstoneflux"})
public final class RFInjectRegistry implements IInjectRegistry {
	@Override
	public void registerInjects() {
		final InjectCollector ic = InjectWrappers.getCollector();
		ic.addTileWrapperInject(RFInjectRegistry::wrapRFTile);
		ic.addItemWrapperInject(RFInjectRegistry::wrapRFItem);
	}
	private static void wrapRFTile(TileEntity te, WrapperRegistry reg) {
		if (te instanceof IEnergyHandler) {
			reg.add(EnergyType.RF, new RFTileCapabilityProvider((IEnergyHandler) te));
		}

	}
	
	private static void wrapRFItem(ItemStack is, WrapperRegistry reg) {
		Item it = is.getItem();
		if (it instanceof IEnergyContainerItem) {
			reg.add(EnergyType.RF, new RFItemContainerWrapper((IEnergyContainerItem) it, is));
		}
	}
}
