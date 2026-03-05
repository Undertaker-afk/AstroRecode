package dev.recode.astro.module.modules.combat;

import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.event.events.AttackEntityEvent;
import dev.recode.astro.api.event.orbit.EventHandler;
import dev.recode.astro.module.Category;
import dev.recode.astro.module.Module;
import dev.recode.astro.module.settings.BooleanSetting;
import dev.recode.astro.module.settings.RangeSliderSetting;
import dev.recode.astro.api.config.FriendCFG;
import dev.recode.astro.api.event.events.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

// TODO; add a proper target system

public class TriggerBotModule extends Module {
    private static TriggerBotModule instance;
    private static boolean enabled = false;
    private static long nextAttack = 0;
    private static long fallStart = 0;
    private static boolean wasFalling = false;
    private static long targetLockTime = 0;
    private static Entity lastSeenTarget = null;
    private static final Random random = new Random();

    private final RangeSliderSetting delay;
    private final RangeSliderSetting delayAxe;
    private final BooleanSetting whileAscending;
    // private final BooleanSetting stickyTarget;
    private final BooleanSetting weaponOnly;
    private final BooleanSetting workInScreens;

    public TriggerBotModule() {
        super("TriggerBot", Category.COMBAT);
        setDescription("hits entities when your crosshair is on them");

        delay = (RangeSliderSetting) new RangeSliderSetting("delay", 500, 540, 0, 1000)
                .setDescription("delay for hitting with everything except for with axe");

        delayAxe = (RangeSliderSetting) new RangeSliderSetting("delay axe", 810, 840, 0, 1000)
                .setDescription("delay for hitting with axe");

        whileAscending = (BooleanSetting) new BooleanSetting("wait while ascending", true)
                .setDescription("waits hitting until you're done going upwards so you can land crits");

//        stickyTarget = (BooleanSetting) new BooleanSetting("sticky target", false)
//                .setDescription("locks onto target (very good for FFA)");

        weaponOnly = (BooleanSetting) new BooleanSetting("weapon only", true)
                .setDescription("only attack when holding a weapon");

        workInScreens = (BooleanSetting) new BooleanSetting("work in screens", false)
                .setDescription("allows you to hit while in gui's");

        addSetting(delay);
        addSetting(delayAxe);
        addSetting(whileAscending);
        //    addSetting(stickyTarget);
        addSetting(weaponOnly);
        addSetting(workInScreens);


        instance = this;

    }

    @Override
    public void onEnable() {
        enabled = true;
        targetLockTime = 0;
        lastSeenTarget = null;
        OrbitManager.EVENT_BUS.subscribe(this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        enabled = false;
        targetLockTime = 0;
        lastSeenTarget = null;
        OrbitManager.EVENT_BUS.unsubscribe(this);
        super.onDisable();
    }

    private void onTick() {
        if (shouldTriggerAttack()) {
            Entity target = getTargetEntity();
            if (target != null && Minecraft.getInstance().player != null && Minecraft.getInstance().gameMode != null) {
                Minecraft.getInstance().gameMode.attack(Minecraft.getInstance().player, target);
                Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }


    @EventHandler
    public void onClientTick(ClientTickEvent event) {
        onTick();
    }

    @EventHandler
    public void onAttackEntity(AttackEntityEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Entity target = event.target;

        // Cancel attack if player is using an item (eating, drinking, blocking, etc.)
        if (client.player.isUsingItem()) {
            event.setCancelled(true);
            return;
        }

        // Cancel attack if target is a friend
        String targetName = getEntityName(target);
        if (targetName != null && FriendCFG.isFriend(targetName)) {
            event.setCancelled(true);
        }
    }


    private static boolean shouldTriggerAttack() {
        Minecraft client = Minecraft.getInstance();

        if (!enabled || client.player == null || client.level == null)
            return false;

        if (System.currentTimeMillis() < nextAttack)
            return false;

        // gui check
        if (client.screen != null && (instance == null || !instance.workInScreens.getValue()))
            return false;

        // Pause when using items
        if (client.player.isUsingItem())
            return false;

        // weapon check
        if (instance != null && instance.weaponOnly.getValue()) {
            if (!isHoldingWeapon())
                return false;
        }

        if (!canPlayerAttack())
            return false;

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY)
            return false;

        Entity target = ((EntityHitResult) hit).getEntity();

        if (target == client.player || !(target instanceof LivingEntity))
            return false;

        // Friend check
        String targetName = getEntityName(target);
        if (targetName != null && FriendCFG.isFriend(targetName))
            return false;

//        if (instance != null && instance.stickyTarget.getValue()) {
//            if (lastSeenTarget != null && target != lastSeenTarget)
//                return false;
//        }

        if (target != lastSeenTarget) {
            lastSeenTarget = target;
            targetLockTime = System.currentTimeMillis();
            return false;
        }

        // 10ms but up to 20ms delay before hitting when you look at their hitbox to bypass stray reach and hitbox check
        long timeSinceTargetLock = System.currentTimeMillis() - targetLockTime;
        long targetDelay = 10 + random.nextInt(10);
        if (timeSinceTargetLock < targetDelay)
            return false;

        nextAttack = System.currentTimeMillis() + getRandomDelay();
        targetLockTime = System.currentTimeMillis();
        return true;
    }

    private static String getEntityName(Entity entity) {
        if (entity == null) return null;
        return entity.getName().getString();
    }

    private static boolean isHoldingWeapon() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return false;

        ItemStack mainHandStack = client.player.getMainHandItem();
        if (mainHandStack.isEmpty())
            return false;

        Item item = mainHandStack.getItem();

        // weapon list
        String itemName = item.toString().toLowerCase();
        return itemName.contains("sword") ||
                itemName.contains("axe") ||
                itemName.contains("spear") ||
                itemName.contains("mace");
    }

    private static boolean isHoldingAxe() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return false;

        ItemStack mainHandStack = client.player.getMainHandItem();
        if (mainHandStack.isEmpty())
            return false;

        Item item = mainHandStack.getItem();
        String itemName = item.toString().toLowerCase();
        return itemName.contains("axe");
    }

    private static boolean canPlayerAttack() {
        Minecraft client = Minecraft.getInstance();
        if (instance != null && instance.whileAscending.getValue()) {
            double velocityY = client.player.getDeltaMovement().y;

            if (velocityY < 0 && !wasFalling) {
                fallStart = System.currentTimeMillis();
                wasFalling = true;
            } else if (velocityY >= 0) {
                wasFalling = false;
            }

            // fall delay, even though you're falling it wont crit (50-60ms)
            long fallDelay = 50 + random.nextInt(10);
            if (velocityY >= 0 || !wasFalling || (System.currentTimeMillis() - fallStart) < fallDelay)
                return false;
        }

        return true;
    }

    private static Entity getTargetEntity() {
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
        return (hit != null && hit.getType() == HitResult.Type.ENTITY)
                ? ((EntityHitResult) hit).getEntity() : null;
    }

    private static long getRandomDelay() {
        if (instance == null)
            return 500;

        // Use axe delays if holding an axe
        if (isHoldingAxe()) {
            return (long) instance.delayAxe.getRandomDouble();
        } else {
            return (long) instance.delay.getRandomDouble();
        }
    }
}