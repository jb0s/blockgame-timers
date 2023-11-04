package dev.jb0s.clockgame;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CommandTimer {
    private final ClockgamePlugin plugin;
    private final YamlConfiguration cache;
    private final HashMap<String, Long> timerMap = new HashMap<>();

    public CommandTimer(ClockgamePlugin plugin, YamlConfiguration cache) {
        this.plugin = plugin;
        this.cache = cache;

        // Populate timer list, load timer value from cache if possible
        Set<String> timers = plugin.getConfig().getConfigurationSection("timers").getKeys(false);
        for (String timer : timers) {
            if(cache.contains(timer)) {
                timerMap.put(timer, cache.getLong(timer));
                continue;
            }

            timerMap.put(timer, 0L);
        }

        // Schedule tick events
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0L, 1L);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::saveCache, 0L, 60L);
    }

    public void tick() {
        FileConfiguration config = plugin.getConfig();

        for (String key : timerMap.keySet()) {
            long val = timerMap.get(key);
            String intervalPath = String.format("timers.%s.interval", key);

            // Check if timer value has surpassed the interval
            if(val >= config.getLong(intervalPath)) {
                String commandsPath = String.format("timers.%s.commands", key);
                List<String> commands = config.getStringList(commandsPath);
                ConsoleCommandSender console = plugin.getServer().getConsoleSender();

                // The timer has surpassed the interval, so execute this timer's commands
                for(int i = 0; i < commands.size(); i++) {
                    String cmd = commands.get(i);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        Bukkit.dispatchCommand(console, cmd);
                    }, i);
                }

                // Set timer value to -1 (It will tick up to 0 immediately below this statement)
                val = -1;
            }

            // Update timer value in the map
            timerMap.replace(key, val + 1);
        }
    }

    /**
     * Saves all current timer values to cache.yml
     */
    public void saveCache() {
        try {
            File file = new File(plugin.getDataFolder(), "cache.yml");

            for (String timer : timerMap.keySet()) {
                cache.set(timer, timerMap.get(timer));
            }

            cache.save(file);
        }
        catch (Exception e) {
            // todo handle
        }
    }
}
