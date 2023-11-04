package dev.jb0s.clockgame;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class ClockgamePlugin extends JavaPlugin {
    private FileConfiguration config;

    @Getter
    private final HashMap<String, LocalTime> timerMap = new HashMap<>();

    // Config path flags
    private final String TIMER_TIMESTAMP_PATH = "timers.%s.timestamp";
    private final String TIMER_COMMANDS_PATH = "timers.%s.commands";
    private final String TIMER_ONETICK_PATH = "timers.%s.oneTick";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        // Register commands
        Objects.requireNonNull(getCommand("reloadtimers")).setExecutor(new ReloadCommand(this));

        // Populate timer list
        loadTimers();

        // Schedule tick events
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 0L, 1L);
    }

    /**
     * (Re)loads the timer data from config.yml
     */
    public void loadTimers() {
        timerMap.clear();

        ConfigurationSection timerSection = config.getConfigurationSection("timers");
        if(timerSection == null) {
            getLogger().severe("Could not load timers from config.yml! Make sure it is properly formatted.");
            return;
        }

        for (String key : timerSection.getKeys(false)) {

            // Java will shit itself if a HashMap has 2 entries with the same name
            if(timerMap.containsKey(key)) {
                getLogger().warning(String.format("Could not load timer %s due to a duplicate timer name.", key));
                continue;
            }

            // No duplicates, attempt to parse the timestamp and put it in our HashMap
            String path = String.format(TIMER_TIMESTAMP_PATH, key);
            String value = config.getString(path);
            if(value != null) {
                LocalTime time = LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
                timerMap.put(key, time);
            }
            else {
                // Failed to parse the timestamp, skill issue
                getLogger().warning(String.format("Could not load timer %s due to a malformed timestamp value.", key));
            }
        }
    }

    private void tick() {
        for (String key : timerMap.keySet()) {
            LocalTime time = timerMap.get(key);
            LocalTime now = LocalTime.now();

            // The time is now.
            if(time == now) {
                String commandsPath = String.format(TIMER_COMMANDS_PATH, key);
                String onetickPath = String.format(TIMER_ONETICK_PATH, key);

                List<String> commands = config.getStringList(commandsPath);
                boolean oneTick = config.getBoolean(onetickPath);

                // Execute all commands
                ConsoleCommandSender console = getServer().getConsoleSender();
                for(int i = 0; i < commands.size(); i++) {
                    String cmd = commands.get(i);

                    // Instant if oneTick is enabled. Otherwise, wait a tick per command.
                    if(oneTick) {
                        Bukkit.dispatchCommand(console, cmd);
                    }
                    else {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.dispatchCommand(console, cmd), i);
                    }
                }
            }
        }
    }
}
