package szewek.mcflux.wrapper.immersiveflux;

import blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxContainerItem;
import net.minecraft.item.ItemStack;
import szewek.mcflux.util.EnergyCapable;
import szewek.mcflux.wrapper.EnergyType;

final class IFItemContainerWrapper extends EnergyCapable implements EnergyType.Converter {
	private final IFluxContainerItem item;
	private final ItemStack stack;

	IFItemContainerWrapper(IFluxContainerItem it, ItemStack is) {
		item = it;
		stack = is;
	}

	@Override
	public long getEnergy() {
		return item.getEnergyStored(stack);
	}

	@Override
	public long getEnergyCapacity() {
		return item.getMaxEnergyStored(stack);
	}

	@Override public boolean canInputEnergy() {
		return true;
	}

	@Override public boolean canOutputEnergy() {
		return item.getEnergyStored(stack) > 0;
	}

	@Override
	public long inputEnergy(long amount, boolean sim) {
		return item.receiveEnergy(stack, amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount, sim);
	}

	@Override
	public long outputEnergy(long amount, boolean sim) {
		return item.extractEnergy(stack, amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount, sim);
	}

	@Override public boolean hasNoEnergy() {
		return item.getEnergyStored(stack) == 0;
	}

	@Override public boolean hasFullEnergy() {
		return item.getEnergyStored(stack) == item.getMaxEnergyStored(stack);
	}

	@Override public EnergyType getEnergyType() {
		return EnergyType.IF;
	}
}
