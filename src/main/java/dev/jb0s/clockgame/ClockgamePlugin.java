package dev.jb0s.clockgame;

import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ClockgamePlugin extends JavaPlugin {
    private CommandTimer commandTimer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        YamlConfiguration cache = createClockCache();
        commandTimer = new CommandTimer(this, cache);
    }

    @Override
    public void onDisable() {
        commandTimer.saveCache();
    }

    @SneakyThrows
    private YamlConfiguration createClockCache() {
        File file = new File(getDataFolder(), "cache.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.save(file);
        return config;
    }
}
