package net.shoreline.client.impl.module.combat;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.BlockPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author linus, zavzagali
 * @since 1.0
 */
public class SelfWebModule extends BlockPlacerModule
{
    private static SelfWebModule INSTANCE;

    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
    Config<Integer> shiftDelayConfig = register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0, 1, 5));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Renders web placements", false));
    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
    private int shiftDelay;
    private List<BlockPos> webs = new ArrayList<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();

    public SelfWebModule()
    {
        super("SelfWeb", "Automatically places a web on your own feet", ModuleCategory.COMBAT);
        INSTANCE = this;
    }

    public static SelfWebModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        fadeList.clear();
        webs.clear();
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event)
    {
        disable();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        if (!multitaskConfig.getValue() && checkMultitask())
        {
            webs.clear();
            return;
        }

        int slot = getBlockItemSlot(Blocks.COBWEB);
        if (slot == -1)
        {
            webs.clear();
            return;
        }

        if (shiftDelay < shiftDelayConfig.getValue())
        {
            shiftDelay++;
            return;
        }

        BlockPos feetPos = mc.player.getBlockPos();
        if (!mc.world.getBlockState(feetPos).isAir())
        {
            webs.clear();
            return;
        }

        webs = new ArrayList<>(List.of(feetPos));
        shiftDelay = 0;
        placeWeb(feetPos, slot);

        if (rotateConfig.getValue())
        {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (renderConfig.getValue())
        {
            RenderBuffers.preRender();
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int lineAlpha = (int) (120 * set.getValue().getFactor());
                Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
                BlockPos blockPos = set.getKey();
                double x1 = blockPos.getX();
                double y1 = blockPos.getY();
                double z1 = blockPos.getZ();
                double x2 = blockPos.getX() + 1.0;
                double y2 = blockPos.getY() + 1.0;
                double z2 = blockPos.getZ() + 1.0;
                RenderManager.renderPlane(event.getMatrices(), x1, y1, z1, x2, y2, z2, lineColor.getRGB());
                RenderManager.renderPlane(event.getMatrices(), x2, y1, z1, x1, y2, z2, lineColor.getRGB());
            }
            RenderBuffers.postRender();

            if (webs.isEmpty())
            {
                return;
            }

            for (BlockPos pos : webs)
            {
                Animation animation = new Animation(true, fadeTimeConfig.getValue());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
                e.getValue().getFactor() == 0.0);
    }

    private void placeWeb(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirectionConfig.getValue(), false, (state, angles) ->
        {
            if (rotateConfig.getValue() && state)
            {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
    }

    public boolean isPlacing()
    {
        return !webs.isEmpty();
    }
}