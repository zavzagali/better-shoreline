package net.shoreline.client.impl.module.combat;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author zavzagali
 * @since 1.0
 */
public class AutoElytraModule extends ToggleModule
{
    private final Config<Float> minDurabilityConfig = register(new NumberConfig<>("MinDurability", "Durability percent to replace elytra", 5.0f, 0.0f, 100.0f, NumberDisplay.PERCENT));
    private final Config<Boolean> inventoryConfig = register(new BooleanConfig("AllowInventory", "Allows elytra to be swapped while in the inventory menu", false));

    public AutoElytraModule()
    {
        super("AutoElytra", "Automatically replaces low durability elytras", ModuleCategory.COMBAT);
    }

    @EventListener
    public void onTick(PlayerTickEvent event)
    {
        if (mc.player == null) return;

        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen && inventoryConfig.getValue()))
        {
            return;
        }

        ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
        if (chestStack.isEmpty() || chestStack.getItem() != Items.ELYTRA)
        {
            return;
        }

        float currentDurability = getDurabilityPercent(chestStack);
        if (currentDurability >= minDurabilityConfig.getValue())
        {
            return;
        }

        int bestSlot = -1;
        float maxDurability = currentDurability;

        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != Items.ELYTRA)
            {
                continue;
            }

            float dura = getDurabilityPercent(stack);
            if (dura > maxDurability)
            {
                maxDurability = dura;
                bestSlot = i;
            }
        }

        if (bestSlot != -1)
        {
            swapArmor(2, bestSlot);
        }
    }

    private float getDurabilityPercent(ItemStack stack)
    {
        if (stack.getMaxDamage() <= 0) return 100.0f;
        return ((stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage()) * 100.0f;
    }

    private void swapArmor(int armorSlot, int slot)
    {
        ItemStack stack = mc.player.getInventory().getArmorStack(armorSlot);
        int targetSlot = 8 - armorSlot;
        int inventorySlot = slot < 9 ? slot + 36 : slot;

        Managers.INVENTORY.pickupSlot(inventorySlot);
        Managers.INVENTORY.pickupSlot(targetSlot);
        if (!stack.isEmpty())
        {
            Managers.INVENTORY.pickupSlot(inventorySlot);
        }
    }
}