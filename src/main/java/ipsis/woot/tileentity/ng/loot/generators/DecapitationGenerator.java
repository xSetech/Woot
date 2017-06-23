package ipsis.woot.tileentity.ng.loot.generators;

import ipsis.woot.oss.LogHelper;
import ipsis.woot.tileentity.ng.IFarmSetup;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class DecapitationGenerator implements ILootGenerator {

    /**
     * ILootGenerator
     */
    public void generate(@Nonnull List<IFluidHandler> fluidHandlerList, @Nonnull List<IItemHandler> itemHandlerList, @Nonnull IFarmSetup farmSetup) {

        LogHelper.info("Generating mob skulls into chest");
    }
}
