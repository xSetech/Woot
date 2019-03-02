package ipsis.woot.factory;

import ipsis.woot.debug.IWootDebug;
import ipsis.woot.factory.layout.FactoryBlock;
import ipsis.woot.factory.layout.IFactoryBlockProvider;
import ipsis.woot.util.WootBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemUseContext;

import java.util.List;

public class BlockExport extends WootBlock implements IWootDebug, IFactoryBlockProvider {

    public static final String BASENAME = "factory_export";
    public BlockExport() {
        super(Properties.create(Material.ROCK), BASENAME);
    }

    /**
     * IWootDebug
     */
    @Override
    public List<String> getDebugText(List<String> debug, ItemUseContext itemUseContext) {
        debug.add("====> BlockExport");
        return debug;
    }

    /**
     * IFactoryBlockProvider
     */
    @Override
    public FactoryBlock getFactoryBlock() {
        return FactoryBlock.EXPORT;
    }
}
