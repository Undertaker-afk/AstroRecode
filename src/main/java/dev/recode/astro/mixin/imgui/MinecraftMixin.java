package dev.recode.astro.mixin.imgui;

import com.mojang.blaze3d.platform.Window;
import dev.recode.astro.AstroRecodeClient;
import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.event.events.ClientTickEvent;
import dev.recode.astro.api.imgui.ImGuiImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    private Window window;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initImGui(GameConfig args, CallbackInfo ci) {
        ImGuiImpl.create(window.handle());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void astroClientTick(CallbackInfo ci) {
        OrbitManager.EVENT_BUS.post(new ClientTickEvent());

        AstroRecodeClient client = AstroRecodeClient.getInstance();
        if (client != null) {
            client.onClientTick((Minecraft) (Object) this);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void closeImGui(CallbackInfo ci) {
        ImGuiImpl.dispose();
    }
}
