package ipsis.woot.modules.infuser.blocks;

import ipsis.woot.Woot;
import ipsis.woot.fluilds.network.FluidStackPacket;
import ipsis.woot.modules.infuser.InfuserSetup;
import ipsis.woot.setup.NetworkChannel;
import ipsis.woot.util.FluidStackPacketHandler;
import ipsis.woot.util.WootContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.lang.reflect.Field;
import java.util.List;

public class InfuserContainer extends WootContainer implements FluidStackPacketHandler {

    public InfuserTileEntity tileEntity;

    public InfuserContainer(int windowId, World world, BlockPos pos, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        super(InfuserSetup.INFUSER_BLOCK_CONTAINER.get(), windowId);
        tileEntity = (InfuserTileEntity) world.getTileEntity(pos);

        addOwnSlots();
        addPlayerSlots(playerInventory);
        addListeners();
    }

    private void addOwnSlots() {
        tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent((
                iItemHandler -> {
                    addSlot(new SlotItemHandler(iItemHandler, 0, 46, 40));
                    addSlot(new SlotItemHandler(iItemHandler, 1, 64, 40));
                    addSlot(new SlotItemHandler(iItemHandler, 2, 118, 40));
                }
        ));
    }

    private void addPlayerSlots(IInventory playerInventory) {
        // Slots for the main inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 10 + col * 18;
                int y = row * 18 + 95;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        for (int row = 0; row < 9; ++row) {
            int x = 10 + row * 18;
            this.addSlot(new Slot(playerInventory, row, x, 153));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        try {
            List<IContainerListener> iContainerListeners =
                    (List<IContainerListener>) ObfuscationReflectionHelper.getPrivateValue(Container.class, this, "listeners");

            // TODO check for actual changes
            for (IContainerListener l : iContainerListeners) {
                NetworkChannel.channel.sendTo(tileEntity.getFluidStackPacket(), ((ServerPlayerEntity)l).connection.netManager,
                        NetworkDirection.PLAY_TO_CLIENT);
            }
        } catch (Throwable e) {
            Woot.LOGGER.error("Reflection of container listener failed");
        }
}

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return isWithinUsableDistance(IWorldPosCallable.of(tileEntity.getWorld(), tileEntity.getPos()),
                playerIn, InfuserSetup.INFUSER_BLOCK.get());
    }

    public InfuserTileEntity getTileEntity() { return tileEntity; }


    public void addListeners() {
        addIntegerListener(new IntReferenceHolder() {
            @Override
            public int get() { return tileEntity.getEnergy(); }

            @Override
            public void set(int i) { tileEntity.setEnergy(i); }
        });
    }

    @Override
    public void handlePacket(FluidStackPacket packet) {
        if (packet.fluidStackList.isEmpty())
            return;
        tileEntity.setTankFluid(packet.fluidStackList.get(0));
    }
}
