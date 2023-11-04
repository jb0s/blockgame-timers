package dev.jb0s.clockgame;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final ClockgamePlugin plugin;

    public ReloadCommand(ClockgamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;

        // Only server operators may execute this command
        if(!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");

            // funny secret
            if(!player.getName().equals("PirateSoftware")) {
                player.sendMessage(ChatColor.RED + "You're a goblin.");
            }

            return true;
        }

        // Player passed server operator checks, reload
        plugin.loadTimers();
        player.sendMessage(String.format(ChatColor.GREEN + "Reloaded %d timer(s).", plugin.getTimerMap().size()));
        return true;
    }
}
