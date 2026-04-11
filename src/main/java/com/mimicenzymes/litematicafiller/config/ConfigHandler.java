package com.mimicenzymes.litematicafiller.config;

import com.mimicenzymes.litematicafiller.input.InputHandler;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigHandler implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = "litematica_container_filler.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void load() {
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);
        if (file.exists() && file.canRead()) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    JsonObject root = element.getAsJsonObject();
                    ConfigUtils.readConfigBase(root, "Features", Configs.OPTIONS);
                    ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse config file: " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void save() {
        File dir = FabricLoader.getInstance().getConfigDir().toFile();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Features", Configs.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

            File file = new File(dir, CONFIG_FILE_NAME);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                System.err.println("Failed to save config file: " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }

        InputHandler.getInstance().addKeysToMap(InputEventHandler.getKeybindManager());
    }
}