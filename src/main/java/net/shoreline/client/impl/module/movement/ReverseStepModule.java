package net.shoreline.client.impl.module.movement;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author zavzagali
 * @since 1.0
 */
public class ReverseStepModule extends ToggleModule
{
    //
    Config<Float> speedConfig = register(new NumberConfig<>("Speed", "The speed at which to fall", 0.1f, 1.0f, 10.0f));
    Config<Float> heightConfig = register(new NumberConfig<>("Height", "The maximum fall distance to reverse step", 1.0f, 2.5f, 10.0f));

    /**
     *
     */
    public ReverseStepModule()
    {
        super("ReverseStep", "Pulls the player down fast when stepping off blocks",
                ModuleCategory.MOVEMENT);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }

        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isFallFlying() || mc.player.isClimbing())
        {
            return;
        }

        if (mc.options.jumpKey.isPressed())
        {
            return;
        }

        if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance <= heightConfig.getValue())
        {
            mc.player.setVelocity(mc.player.getVelocity().x, -speedConfig.getValue(), mc.player.getVelocity().z);
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket)
        {
            disable();
        }
    }
}