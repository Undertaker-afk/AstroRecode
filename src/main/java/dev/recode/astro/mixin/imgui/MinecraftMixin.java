package dev.recode.astro.mixin.imgui;

import com.mojang.blaze3d.platform.Window;
import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.config.ConfigCFG;
import dev.recode.astro.api.event.events.ClientTickEvent;
import dev.recode.astro.api.imgui.ImGuiImpl;
import dev.recode.astro.module.ModuleManager;
import dev.recode.astro.module.modules.client.ClickGuiModule;
import dev.recode.astro.screens.ClickGUIScreen1;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jetbrains.annotations.Nullable;
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

    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initImGui(GameConfig args, CallbackInfo ci) {
        ImGuiImpl.create(window.handle());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void astroClientTick(CallbackInfo ci) {
        if (OrbitManager.EVENT_BUS.isListening(ClientTickEvent.class)) {
            OrbitManager.EVENT_BUS.post(ClientTickEvent.INSTANCE);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(@Nullable Screen newScreen, CallbackInfo ci) {
        if (screen instanceof ClickGUIScreen1 && !(newScreen instanceof ClickGUIScreen1)) {
            ClickGuiModule module = ModuleManager.getInstance().getModuleByClass(ClickGuiModule.class);
            if (module != null && module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
        ConfigCFG.saveLatestConfig();
        ImGuiImpl.dispose();
    }
}
