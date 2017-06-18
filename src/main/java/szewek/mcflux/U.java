package szewek.mcflux;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import szewek.mcflux.api.ex.IEnergy;
import szewek.mcflux.api.fe.Flavored;
import szewek.mcflux.api.fe.FlavoredMutable;
import szewek.mcflux.api.fe.IFlavorEnergy;
import szewek.mcflux.util.MCFluxReport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static net.minecraft.util.EnumFacing.*;

public final class U {
	public static final EnumFacing[] FANCY_FACING = new EnumFacing[] {DOWN, UP, NORTH, SOUTH, WEST, EAST, null};

	public static String formatMF(IEnergy ie) {
		return ie.getEnergy() + " / " + ie.getEnergyCapacity() + " MF";
	}

	public static long transferEnergy(IEnergy from, IEnergy to, final long amount) {
		if (from != null && to != null && from.canOutputEnergy() && to.canInputEnergy()) {
			long r = to.inputEnergy(from.outputEnergy(amount, true), true);
			if (r > 0)
				return to.inputEnergy(from.outputEnergy(r, false), false);
		}
		return 0;
	}

	public static long transferFlavorEnergy(IFlavorEnergy from, IFlavorEnergy to, Flavored fl, final long amount) {
		if (from != null && to != null && from.canOutputFlavorEnergy(fl) && to.canInputFlavorEnergy(fl)) {
			FlavoredMutable fli = new FlavoredMutable(fl, amount);
			long r = from.outputFlavorEnergy(fli.toImmutable(), true);
			fli.setAmount(r);
			r = to.inputFlavorEnergy(fli.toImmutable(), true);
			if (r > 0) {
				fli.setAmount(r);
				r = from.outputFlavorEnergy(fli.toImmutable(), false);
				fli.setAmount(r);
				return to.inputFlavorEnergy(fli.toImmutable(), false);
			}
		}
		return 0;
	}

	@SideOnly(Side.CLIENT)
	public static void registerItemModels(Item... items) {
			ItemModelMesher imm = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
		for (Item i : items) {
			ModelResourceLocation mrl = new ModelResourceLocation(i.getRegistryName(), "inventory");
			imm.register(i, 0, mrl);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerItemMultiModels(Item item, int m) {
		net.minecraft.util.ResourceLocation rl = item.getRegistryName();
		final String dom = rl.getResourceDomain();
		for (int i = 0; i < m; i++) {
			ItemStack is = new ItemStack(item, 1, i);
			ModelResourceLocation mrl = new ModelResourceLocation(dom + ':' + item.getUnlocalizedName(is).substring(5), "inventory");
			net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(item, i, mrl);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] makeFilledArray(T[] t, @Nonnull T fill) {
		for (int i = 0, l = t.length; i < l; i++)
			t[i] = fill;
		return t;
	}

	@Nullable
	public static Class<?> getClassSafely(String name) {
		Class<?> c = null;
		try {
			c = Class.forName(name);
		} catch (ClassNotFoundException e) {
			MCFluxReport.sendException(e, "Getting class");
		}
		return c;
	}

	@Nullable
	public static Method getMethodSafely(Class<?> cl, String name, Class<?>... cargs) {
		Method m = null;
		try {
			m = cl.getDeclaredMethod(name, cargs);
		} catch (Exception e) {
			MCFluxReport.sendException(e, "Getting method");
		}
		return m;
	}

	private U() {}
}
