package dev.jb0s.clockgame;

import dev.jb0s.clockgame.command.ClockgameCommand;
import dev.jb0s.clockgame.command.ClockgameTabComplete;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class ClockgamePlugin extends JavaPlugin {
    private FileConfiguration config;
    private final HashMap<String, LocalTime> timerMap = new HashMap<>();
    private final ArrayList<String> timersOnCooldown = new ArrayList<>();

    // Config path flags
    private final String TIMER_TIMESTAMP_PATH = "timers.%s.timestamp";
    private final String TIMER_COMMANDS_PATH = "timers.%s.commands";
    private final String TIMER_ONETICK_PATH = "timers.%s.oneTick";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        // Register commands
        PluginCommand cmd = getCommand("clockgame");
        if(cmd != null) {
            cmd.setExecutor(new ClockgameCommand(this));
            cmd.setTabCompleter(new ClockgameTabComplete());
        }

        // Populate timer list
        loadTimers();

        // Schedule tick events
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 0L, 1L);
    }

    /**
     * Simple function to (re)load the config.
     * I put it in a method to prevent duplicated code.
     */
    private void loadConfig() {
        reloadConfig();
        config = getConfig();
    }

    /**
     * (Re)loads the timer data from config.yml
     */
    public void loadTimers() {
        loadConfig();
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

        // Log success
        getLogger().info(String.format("Loaded %d timers.", getNumTimers()));
    }

    /**
     * Calculates the total amount of timers.
     * @return Total amount of timers.
     */
    public int getNumTimers() {
        return timerMap.size();
    }

    private void tick() {
        for (String key : timerMap.keySet()) {
            LocalTime time = timerMap.get(key);
            LocalTime now = LocalTime.now();

            // The time is now.
            if(now.toSecondOfDay() == time.toSecondOfDay()) {

                // If this timer has already executed in a previous tick this second, stop now
                if(timersOnCooldown.contains(key)) {
                    continue;
                }

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

                    // Put the timer on cooldown so that it cannot execute any further while the seconds still match.
                    timersOnCooldown.add(key);
                }
            }
            else {
                // We can remove this from cooldown now, the seconds no longer match.
                timersOnCooldown.remove(key);
            }
        }
    }
}
