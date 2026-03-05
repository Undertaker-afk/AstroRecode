package dev.recode.astro.module.modules.combat;

import dev.recode.astro.api.utils.InventoryUtil;
import dev.recode.astro.module.Category;
import dev.recode.astro.module.Module;
import dev.recode.astro.module.settings.RangeSliderSetting;
import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.event.events.ClientTickEvent;
import dev.recode.astro.api.event.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ShieldStunModule extends Module {
    private final RangeSliderSetting switchDelay;
    private final RangeSliderSetting hitDelay;
    private final RangeSliderSetting switchBackDelay;

    private int step = 0;
    private long time = 0;
    private int previousSlot = -1;
    private Player target = null;

    public ShieldStunModule() {
        super("ShieldStun", Category.COMBAT);
        setDescription("auto stuns/breaks enemy shields");

        switchDelay = new RangeSliderSetting("switch delay", 10, 50, 0, 250);
        switchDelay.setDescription("Delay for switching to an axe.");

        hitDelay = new RangeSliderSetting("hit delay", 10, 120, 0, 250);
        hitDelay.setDescription("Delay for hitting after switching to an axe.");

        switchBackDelay = new RangeSliderSetting("switch back delay", 50, 70, 0, 100);
        switchBackDelay.setDescription("delay for switching back to your previous held item");

        addSetting(switchDelay);
        addSetting(hitDelay);
        addSetting(switchBackDelay);

    }

    @Override
    public void onEnable() {
        OrbitManager.EVENT_BUS.subscribe(this);
        reset();
    }

    @Override
    public void onDisable() {
        OrbitManager.EVENT_BUS.unsubscribe(this);
        reset();
    }

    @EventHandler
    public void onClientTick(ClientTickEvent event) {
        onTick(Minecraft.getInstance());
    }

    private void onTick(Minecraft client) {
        if (!canRun(client)) return;

        switch (step) {
            case 0: target1(client); break;
            case 1: slot2(client); break;
            case 2: attack3(client); break;
            case 3: slotback4(client); break;
        }
    }

    private boolean canRun(Minecraft client) {
        return isEnabled() && client.player != null && client.level != null &&
                client.gameMode != null && client.screen == null;
    }

    private void target1(Minecraft client) {
        if (client.player.isUsingItem()) return;

        HitResult hitResult = client.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult hit = (EntityHitResult) hitResult;
        if (!(hit.getEntity() instanceof Player player)) return;

        if (!player.isHolding(Items.SHIELD) || !player.isBlocking()) return;

        target = player;
        previousSlot = InventoryUtil.getSelectedSlot(client);
        step = 1;
        time = System.currentTimeMillis();
    }

    private void slot2(Minecraft client) {
        if (!isTargetValid(client)) return;
        if (!hasDelayPassed((long) switchDelay.getRandomDouble())) return;

        if (InventoryUtil.findAndSelectItem(client, AxeItem.class)) {
            step = 2;
            time = System.currentTimeMillis();
        } else {
            reset();
        }
    }

    private void attack3(Minecraft client) {
        if (!isTargetValid(client)) return;
        if (!hasDelayPassed((long) hitDelay.getRandomDouble())) return;

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        step = 3;
        time = System.currentTimeMillis();
    }

    private void slotback4(Minecraft client) {
        if (!hasDelayPassed((long) switchBackDelay.getRandomDouble())) return;

        if (previousSlot != -1 && previousSlot != InventoryUtil.getSelectedSlot(client)) {
            InventoryUtil.setSelectedSlot(client, previousSlot);
        }
        reset();
    }

    private boolean isTargetValid(Minecraft client) {
        if (target == null || target.isRemoved()) {
            reset();
            return false;
        }

        if (!target.isHolding(Items.SHIELD) || !target.isBlocking()) {
            reset();
            return false;
        }

        HitResult hitResult = client.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            reset();
            return false;
        }

        EntityHitResult hit = (EntityHitResult) hitResult;
        if (hit.getEntity() != target) {
            reset();
            return false;
        }

        return true;
    }

    private boolean hasDelayPassed(long delay) {
        return System.currentTimeMillis() - time >= delay;
    }

    private void reset() {
        step = 0;
        time = 0;
        previousSlot = -1;
        target = null;
    }
}
