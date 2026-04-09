package com.mimicenzymes.litematicafiller.mixin;

import com.mimicenzymes.litematicafiller.config.GuiConfigs;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(value = GuiMainMenu.class, remap = false)
public abstract class GuiMainMenuMixin extends GuiBase {

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void onInitGuiAddFillerButton(CallbackInfo ci) {

        int targetX = 214;
        int targetY = 104;
        int btnWidth = 98;

        try {
            List<?> buttons = null;
            for (Field f : GuiBase.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType()) && f.getName().toLowerCase().contains("button")) {
                    f.setAccessible(true);
                    buttons = (List<?>) f.get(this);
                    break;
                }
            }

            if (buttons != null && !buttons.isEmpty()) {
                int maxX = -1;

                for (Object btnObj : buttons) {
                    int btnY = (int) btnObj.getClass().getMethod("getY").invoke(btnObj);
                    int btnX = (int) btnObj.getClass().getMethod("getX").invoke(btnObj);
                    if (btnY >= 80 && btnX > maxX) {
                        maxX = btnX;
                    }
                }

                int minY = Integer.MAX_VALUE;
                Object anchorBtn = null;

                for (Object btnObj : buttons) {
                    int btnX = (int) btnObj.getClass().getMethod("getX").invoke(btnObj);
                    int btnY = (int) btnObj.getClass().getMethod("getY").invoke(btnObj);

                    if (btnY >= 80 && Math.abs(btnX - maxX) < 10) {
                        if (btnY < minY) {
                            minY = btnY;
                            anchorBtn = btnObj;
                        }
                    }
                }

                if (anchorBtn != null) {
                    int anchorX = (int) anchorBtn.getClass().getMethod("getX").invoke(anchorBtn);
                    int anchorY = (int) anchorBtn.getClass().getMethod("getY").invoke(anchorBtn);
                    int anchorWidth = (int) anchorBtn.getClass().getMethod("getWidth").invoke(anchorBtn);

                    targetX = anchorX + anchorWidth + 4;
                    targetY = anchorY;
                    btnWidth = anchorWidth;
                }

                boolean isOccupied = true;
                while (isOccupied) {
                    isOccupied = false;
                    for (Object btnObj : buttons) {
                        int btnY = (int) btnObj.getClass().getMethod("getY").invoke(btnObj);
                        int btnX = (int) btnObj.getClass().getMethod("getX").invoke(btnObj);

                        if (Math.abs(btnX - targetX) < 5 && Math.abs(btnY - targetY) < 5) {
                            isOccupied = true;
                            targetY += 24;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        String btnText = StringUtils.translate("litematica_container_filler.gui.title.configs");
        ButtonGeneric configBtn = new ButtonGeneric(targetX, targetY, btnWidth, 20, btnText);

        this.addButton(configBtn, (btn, mouseButton) -> {
            GuiBase.openGui(new GuiConfigs(this));
        });
    }
}