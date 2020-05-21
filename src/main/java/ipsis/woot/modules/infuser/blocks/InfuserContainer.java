package ipsis.woot.modules.infuser.blocks;

import ipsis.woot.Woot;
import ipsis.woot.crafting.InfuserRecipe;
import ipsis.woot.fluilds.network.TankPacket;
import ipsis.woot.modules.infuser.InfuserSetup;
import ipsis.woot.setup.NetworkChannel;
import ipsis.woot.util.TankPacketHandler;
import ipsis.woot.util.WootContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class InfuserContainer extends WootContainer implements TankPacketHandler {

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
                    addSlot(new SlotItemHandler(iItemHandler, 2, 118, 40){
                        @Override
                        public boolean isItemValid(@Nonnull ItemStack stack) {
                            // Cannot put anything in here as it is an output
                           return false;
                        }
                    });
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


    private int progress = 0;
    private FluidStack inputFluid = FluidStack.EMPTY;
    private int energy = 0;

    @OnlyIn(Dist.CLIENT)
    public int getProgress() { return this.progress; }
    @OnlyIn(Dist.CLIENT)
    public int getEnergy() { return this.energy; }
    @OnlyIn(Dist.CLIENT)
    public FluidStack getInputFluid() { return inputFluid; }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        try {
            List<IContainerListener> iContainerListeners =
                    (List<IContainerListener>) ObfuscationReflectionHelper.getPrivateValue(Container.class, this, "listeners");

            if (!inputFluid.isFluidStackIdentical(tileEntity.getTankFluid())) {
                inputFluid = tileEntity.getTankFluid().copy();
                TankPacket tankPacket = new TankPacket(0, inputFluid);
                for (IContainerListener l : iContainerListeners) {
                    NetworkChannel.channel.sendTo(tankPacket, ((ServerPlayerEntity) l).connection.netManager,
                            NetworkDirection.PLAY_TO_CLIENT);
                }
            }
        } catch (Throwable e) {
            Woot.setup.getLogger().error("Reflection of container listener failed");
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return isWithinUsableDistance(IWorldPosCallable.of(tileEntity.getWorld(), tileEntity.getPos()),
                playerIn, InfuserSetup.INFUSER_BLOCK.get());
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerEntity, int index) {

        // 0,1,2 input/output slots
        // 3 -> 29 player
        // 30 -> 38 hotbar

        // NB: mergeItemStack minIndex(inclusive), maxIndex(exclusive)
        final int LAST_MACHINE_SLOT = 2;
        final int FIRST_PLAYER_SLOT = 3;
        final int LAST_PLAYER_SLOT = FIRST_PLAYER_SLOT + 27 - 1;
        final int FIRST_HOTBAR_SLOT = LAST_PLAYER_SLOT + 1;
        final int LAST_HOTBAR_SLOT = FIRST_HOTBAR_SLOT + 9 - 1;

        // Straight from McJty's YouTubeModdingTutorial
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemStack = stack.copy();
            if (index <= LAST_MACHINE_SLOT) {
                // Machine -> Player/Hotbar
                if (!this.mergeItemStack(stack, FIRST_PLAYER_SLOT, LAST_HOTBAR_SLOT + 1, true))
                    return ItemStack.EMPTY;
                slot.onSlotChange(stack, itemStack);
            } else {
                if (InfuserRecipe.isValidInput(itemStack)) {
                    // Player -> innput
                    if (!this.mergeItemStack(stack, 0, 1, false))
                        return ItemStack.EMPTY;
                } else if (InfuserRecipe.isValidAugment(itemStack)) {
                    // Player -> augment
                    if (!this.mergeItemStack(stack, 1, 2, false))
                        return ItemStack.EMPTY;
                }
                // No player to machine supported
                if (index >= FIRST_PLAYER_SLOT && index <= LAST_PLAYER_SLOT) {
                    // Player -> Hotbar
                    if (!this.mergeItemStack(stack, FIRST_HOTBAR_SLOT, LAST_HOTBAR_SLOT + 1, false))
                        return ItemStack.EMPTY;
                } else if (index <= LAST_HOTBAR_SLOT && !this.mergeItemStack(stack, FIRST_PLAYER_SLOT, LAST_PLAYER_SLOT + 1, false)) {
                    // Hotbar -> Player
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty())
                slot.putStack(ItemStack.EMPTY);
            else
                slot.onSlotChanged();

            if (stack.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;

            slot.onTake(playerEntity, stack);
        }

        return itemStack;
    }

    public void addListeners() {
        addIntegerListener(new IntReferenceHolder() {
            @Override
            public int get() { return tileEntity.getEnergy(); }

            @Override
            public void set(int i) { energy = i; }
        });

        addIntegerListener(new IntReferenceHolder() {
            @Override
            public int get() { return tileEntity.getProgress(); }

            @Override
            public void set(int i) { progress = i; }
        });
    }

    @Override
    public void handlePacket(TankPacket packet) {
        if (packet.tankId == 0)
            inputFluid = packet.fluidStack;
    }
}
