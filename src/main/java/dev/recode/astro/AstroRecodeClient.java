package dev.recode.astro;

import dev.recode.astro.module.KeybindMode;
import dev.recode.astro.module.Module;
import dev.recode.astro.module.ModuleManager;
import dev.recode.astro.module.Setting;
import dev.recode.astro.module.settings.KeybindSetting;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class AstroRecodeClient implements ClientModInitializer {
    private static AstroRecodeClient instance;
    private final boolean[] keyState = new boolean[512];
    private KeybindSetting activeBinding;

    @Override
    public void onInitializeClient() {
        instance = this;
    }

    public void onClientTick(Minecraft mc) {
        if (mc == null || mc.getWindow() == null) return;
        long window = mc.getWindow().handle();

        if (activeBinding != null) {
            handleCapture(window);
            return;
        }

        for (Module m : ModuleManager.getInstance().getModules()) {
            for (Setting s : m.getSettings()) {
                if (s instanceof KeybindSetting k) handleKeybind(window, m, k);
            }
        }
    }

    private void handleKeybind(long window, Module m, KeybindSetting k) {
        int key = k.getKey();
        if (key <= 0 || key >= keyState.length) return;

        boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;

        if (pressed && !keyState[key]) {
            keyState[key] = true;
            if (k.getMode() == KeybindMode.TOGGLE) m.toggle();
            else m.setEnabled(true);
        } else if (!pressed && keyState[key]) {
            keyState[key] = false;
            if (k.getMode() == KeybindMode.HOLD) m.setEnabled(false);
        }
    }

    private void handleCapture(long window) {
        for (int key = 32; key < keyState.length; key++) {
            if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS && !keyState[key]) {
                activeBinding.setKey(key);
                activeBinding.stopBinding();
                activeBinding = null;
                keyState[key] = true;
                return;
            }
        }
    }

    public static AstroRecodeClient getInstance() {
        return instance;
    }

    public void startBinding(KeybindSetting k) {
        activeBinding = k;
    }

    public void stopBinding() {
        activeBinding = null;
    }
}
