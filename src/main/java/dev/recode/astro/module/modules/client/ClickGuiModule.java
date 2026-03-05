package dev.recode.astro.module.modules.client;

import dev.recode.astro.module.Category;
import dev.recode.astro.module.Module;
import dev.recode.astro.module.settings.ColorSetting;
import dev.recode.astro.screens.ClickGUIScreen1;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import dev.recode.astro.OrbitManager;
import dev.recode.astro.api.event.events.ClientTickEvent;
import dev.recode.astro.api.event.orbit.EventHandler;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClickGuiModule extends Module {

    public final ColorSetting primaryColor;
    public final ColorSetting secondaryColor;
    public final ColorSetting backgroundColor;

    private static final int MODULE_COLOR = 0xFF6969FF;

    public ClickGuiModule() {
        super("ClickGUI", Category.CLIENT);
        setDescription("shows this menu");


        primaryColor = new ColorSetting("Primary", 0xFF6969FF, MODULE_COLOR);
        primaryColor.setDescription("Primary/main color");

        secondaryColor = new ColorSetting("Secondary", 0xFF303030, MODULE_COLOR);
        secondaryColor.setDescription("Secondary color");

        backgroundColor = new ColorSetting("Background", 0xFF202020, MODULE_COLOR);
        backgroundColor.setDescription("Background color");

        addSetting(primaryColor);
        addSetting(secondaryColor);
        addSetting(backgroundColor);
    }

    @Override
    public void onEnable() {

        if (Minecraft.getInstance().player == null) {
            setEnabled(false);
            return;
        }

        ClickGUIScreen1 guiScreen = new ClickGUIScreen1();

        OrbitManager.EVENT_BUS.subscribe(this);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(guiScreen));
    }

    @Override
    public void onDisable() {
        OrbitManager.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler
    public void onClientTick(ClientTickEvent event) {
        if (!isEnabled()) return;
        if (!(Minecraft.getInstance().screen instanceof ClickGUIScreen1)) {
            setEnabled(false);
        }
    }

}
