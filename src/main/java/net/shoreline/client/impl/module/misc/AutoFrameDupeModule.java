package net.shoreline.client.impl.module.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author zavzagali
 * @since 1.0
 */
public class AutoFrameDupeModule extends ToggleModule
{
    Config<Float> range = register(new NumberConfig<>("Range", "The maximum distance to interact with item frames", 1f, 5f, 7f));
    Config<Integer> turns = register(new NumberConfig<>("Turns", "How many times to rotate the item in the frame", 1, 5, 10));
    Config<Integer> ticks = register(new NumberConfig<>("Ticks", "Delay between interactions in ticks", 1, 5, 10));
    Config<Boolean> switchxd = register(new BooleanConfig("Switch", "Automatically switch to shulker boxes when needed", true));
    Config<Boolean> autoPlace = register(new BooleanConfig("AutoPlace", "Automatically place item frames if none are in range", true));

    private int timeoutTicks = 0;

    public AutoFrameDupeModule()
    {
        super("AutoFrameDupe", "Automatically places, rotates, and breaks frames for duping within range", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (mc.world == null || mc.player == null || mc.interactionManager == null) return;

        boolean foundFrame = false;

        for (Entity entity : mc.world.getEntities()) 
        {
            if (entity instanceof ItemFrameEntity frame && mc.player.distanceTo(frame) <= range.getValue()) 
            {
                foundFrame = true;
                if (timeoutTicks >= ticks.getValue()) 
                {
                    ItemStack displayedItem = frame.getHeldItemStack();
                    boolean hasItem = !displayedItem.isEmpty();
                    boolean isHolding = !mc.player.getMainHandStack().isEmpty();

                    if (switchxd.getValue() && (!isHolding || !isShulkerBox(mc.player.getMainHandStack()))) 
                    {
                        int shulkerSlot = findShulkers();
                        if (shulkerSlot != -1) 
                        {
                            mc.player.getInventory().selectedSlot = shulkerSlot;
                            isHolding = true; 
                        }
                    }

                    if (!hasItem && isHolding) 
                    {
                        mc.interactionManager.interactEntity(mc.player, frame, Hand.MAIN_HAND);
                    }

                    if (hasItem) 
                    {
                        for (int i = 0; i < turns.getValue(); i++) 
                        {
                            mc.interactionManager.interactEntity(mc.player, frame, Hand.MAIN_HAND);
                        }
                        mc.interactionManager.attackEntity(mc.player, frame);
                    }

                    timeoutTicks = 0;
                } 
                else 
                {
                    timeoutTicks++;
                }
                break; 
            }
        }

        if (!foundFrame && autoPlace.getValue()) 
        {
            if (timeoutTicks >= ticks.getValue()) 
            {
                int frameSlot = findItemFrame();
                if (frameSlot != -1) 
                {
                    BlockHitResult placementHit = findBestBlockInRange();
                    if (placementHit != null) 
                    {
                        int oldSlot = mc.player.getInventory().selectedSlot;
                        boolean swappedFromInv = false;
                        Hand handToUse = Hand.MAIN_HAND;

                        // Sol el (40) kontrolü
                        if (frameSlot == 40) 
                        {
                            handToUse = Hand.OFF_HAND;
                        }
                        // Ana envanter (9-35) silent swap
                        else if (frameSlot >= 9 && frameSlot < 36) 
                        {
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, frameSlot, oldSlot, SlotActionType.SWAP, mc.player);
                            swappedFromInv = true;
                        } 
                        // Hotbar (0-8) silent select packet
                        else if (frameSlot < 9 && frameSlot != oldSlot) 
                        {
                            mc.player.getInventory().selectedSlot = frameSlot;
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(frameSlot));
                        }

                        // Yerleştirme
                        mc.interactionManager.interactBlock(mc.player, handToUse, placementHit);
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(handToUse));

                        // Geri Alma (Silent Restore)
                        if (swappedFromInv) 
                        {
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, frameSlot, oldSlot, SlotActionType.SWAP, mc.player);
                        } 
                        else if (frameSlot < 9 && frameSlot != oldSlot) 
                        {
                            mc.player.getInventory().selectedSlot = oldSlot;
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                        }

                        timeoutTicks = 0;
                    } 
                    else 
                    {
                        timeoutTicks++;
                    }
                } 
                else 
                {
                    timeoutTicks++;
                }
            } 
            else 
            {
                timeoutTicks++;
            }
        }

    }
    private BlockHitResult findBestBlockInRange() 
    {
        BlockPos playerPos = mc.player.getBlockPos();
        int r = (int) Math.ceil(range.getValue());

        for (int x = -r; x <= r; x++) 
        {
            for (int y = -r; y <= r; y++) 
            {
                for (int z = -r; z <= r; z++) 
                {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!mc.world.getBlockState(pos).isAir()) 
                    {
                        for (Direction dir : Direction.values()) 
                        {
                            BlockPos offsetPos = pos.offset(dir);
                            if (mc.world.getBlockState(offsetPos).isAir()) 
                            {
                                double dist = mc.player.squaredDistanceTo(offsetPos.getX() + 0.5, offsetPos.getY() + 0.5, offsetPos.getZ() + 0.5);
                                if (dist <= range.getValue() * range.getValue()) 
                                {
                                    Vec3d hitVec = new Vec3d(pos.getX() + 0.5 + dir.getOffsetX() * 0.5, 
                                                             pos.getY() + 0.5 + dir.getOffsetY() * 0.5, 
                                                             pos.getZ() + 0.5 + dir.getOffsetZ() * 0.5);
                                    return new BlockHitResult(hitVec, dir, pos, false);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isShulkerBox(ItemStack stack) 
    {
        Item item = stack.getItem();
        return item == Items.SHULKER_BOX ||
                item == Items.WHITE_SHULKER_BOX ||
                item == Items.ORANGE_SHULKER_BOX ||
                item == Items.MAGENTA_SHULKER_BOX ||
                item == Items.LIGHT_BLUE_SHULKER_BOX ||
                item == Items.YELLOW_SHULKER_BOX ||
                item == Items.LIME_SHULKER_BOX ||
                item == Items.PINK_SHULKER_BOX ||
                item == Items.GRAY_SHULKER_BOX ||
                item == Items.LIGHT_GRAY_SHULKER_BOX ||
                item == Items.CYAN_SHULKER_BOX ||
                item == Items.PURPLE_SHULKER_BOX ||
                item == Items.BLUE_SHULKER_BOX ||
                item == Items.BROWN_SHULKER_BOX ||
                item == Items.GREEN_SHULKER_BOX ||
                item == Items.RED_SHULKER_BOX ||
                item == Items.BLACK_SHULKER_BOX;
    }

    private int findShulkers() 
    {
        for (int i = 0; i < 9; i++) 
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isShulkerBox(stack)) 
            {
                return i;
            }
        }
        return -1;
    }

    private int findItemFrame() 
    {   
        for (int i = 0; i < mc.player.getInventory().size(); i++) 
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ITEM_FRAME || stack.getItem() == Items.GLOW_ITEM_FRAME) 
            {
                return i;
            }
        }
        return -1;
    }
}

