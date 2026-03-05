package dev.recode.astro.module.modules.combat;

import dev.recode.astro.api.config.FriendCFG;
import dev.recode.astro.api.utils.Globals;
import dev.recode.astro.module.Category;
import dev.recode.astro.module.Module;
import dev.recode.astro.module.settings.RangeSliderSetting;
import dev.recode.astro.module.settings.SliderSetting;
import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.event.events.ClientTickEvent;
import dev.recode.astro.api.event.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Random;

public class AimAssistModule extends Module implements Globals {

    private final RangeSliderSetting horizontalSpeed;
    private final RangeSliderSetting verticalSpeed;
    private final SliderSetting maxDistance;
    private final SliderSetting maxFov;

    private final Random random = new Random();
    private float yawSpeed;
    private float pitchSpeed;
    private Vec3 aimOffset = Vec3.ZERO;
    private int randomizationTicks;

    public AimAssistModule() {
        super("AimAssist", Category.COMBAT);
        setDescription("aims at players for you");

        horizontalSpeed = new RangeSliderSetting("Horizontal speed", 3.0, 5.0, 0.5, 20.0);
        horizontalSpeed.setDescription("speed for aiming");

        verticalSpeed = new RangeSliderSetting("Vertical speed", 2.5, 4.5, 0.5, 20.0);
        verticalSpeed.setDescription("speed for aiming upwards and downwards");

        maxDistance = new SliderSetting("Max distance", 4.5, 1.0, 8.0);
        maxDistance.setDescription("The maximum reach the assist will lock onto.");

        maxFov = new SliderSetting("Max FOV", 90.0, 10.0, 360.0);
        maxFov.setDescription("The field of view range for target detection.");

        addSetting(horizontalSpeed);
        addSetting(verticalSpeed);
        addSetting(maxDistance);
        addSetting(maxFov);

    }

    @Override
    public void onEnable() {
        OrbitManager.EVENT_BUS.subscribe(this);
        randomize();
    }

    @Override
    public void onDisable() {
        OrbitManager.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler
    public void onTickEvent(ClientTickEvent event) {
        onTick(Minecraft.getInstance());
    }

    private void onTick(Minecraft mc) {
        if (!isEnabled() || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        updateRandomization();

        Player player = mc.player;
        Player target = findTarget(player);

        if (target == null) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 targetPos = getTargetPos(target);

        Vec3 diff = targetPos.subtract(eyePos);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float targetYaw = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F));
        float targetPitch = Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diff.y, distXZ))));

        float yawDiff = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDiff = Mth.wrapDegrees(targetPitch - player.getXRot());

        float yawStep = (yawDiff / 4.0F) * (yawSpeed / 5.0F);
        float pitchStep = (pitchDiff / 4.0F) * (pitchSpeed / 5.0F);

        double sensitivity = mc.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6F + 0.2F);
        float gcd = f * f * f * 1.2F;

        if (Math.abs(yawDiff) > 0.05) {
            float finalYawStep = Math.round(yawStep / gcd) * gcd;
            player.setYRot(player.getYRot() + finalYawStep);
        }

        if (Math.abs(pitchDiff) > 0.05) {
            float finalPitchStep = Math.round(pitchStep / gcd) * gcd;
            player.setXRot(Mth.clamp(player.getXRot() + finalPitchStep, -90.0F, 90.0F));
        }

        player.yHeadRot = player.getYRot();
        player.yBodyRot = player.getYRot();
    }

    private Player findTarget(Player player) {
        return player.level().players().stream()
                .filter(p -> p != player && p.isAlive() && !p.isSpectator())
                .filter(p -> !FriendCFG.isFriend(p.getName().getString()))
                .filter(p -> player.distanceTo(p) <= maxDistance.getDoubleValue())
                .filter(p -> isInFov(player, p))
                .min(Comparator.comparingDouble(player::distanceTo))
                .orElse(null);
    }

    private boolean isInFov(Player player, Player target) {
        if (maxFov.getFloatValue() >= 360.0F) return true;
        Vec3 diff = target.position().subtract(player.getEyePosition());
        float yawTo = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float fovDiff = Math.abs(Mth.wrapDegrees(yawTo - player.getYRot()));
        return fovDiff <= maxFov.getFloatValue() / 2F;
    }

    private Vec3 getTargetPos(Player target) {
        AABB bb = target.getBoundingBox();
        return new Vec3(
                (bb.minX + bb.maxX) * 0.5 + aimOffset.x,
                (bb.minY + bb.maxY) * 0.5 + 0.2 + aimOffset.y,
                (bb.minZ + bb.maxZ) * 0.5 + aimOffset.z
        );
    }

    private void updateRandomization() {
        if (--randomizationTicks <= 0) randomize();
    }

    private void randomize() {
        yawSpeed = (float) horizontalSpeed.getRandomDouble();
        pitchSpeed = (float) verticalSpeed.getRandomDouble();
        aimOffset = new Vec3(random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1), random.nextDouble(-0.1, 0.1));
        randomizationTicks = random.nextInt(15, 45);
    }
}
