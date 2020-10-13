package live.shino.tpa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class Tpa extends JavaPlugin {

    private final Map<String, String> requests = new HashMap<>();
    private final Map<String, BukkitTask> timeouts = new HashMap<>();

    private static final class TpaListener implements Listener {
        public Tpa plugin;

        public TpaListener(Tpa plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            String target = plugin.requests.get(event.getPlayer().getName());
            plugin.removeRequest(event.getPlayer().getName());
            if (target != null) {
                Player targetPlayer = plugin.getServer().getPlayer(target);
                if (targetPlayer != null)
                    targetPlayer.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The teleport request from " + event.getPlayer().getName() + " was cancelled because they logged out.");
            }
            for (String source : plugin.requests.keySet()) {
                target = plugin.requests.get(source);
                if (plugin.requests.get(source).equals(event.getPlayer().getName())) {
                    plugin.removeRequest(source);
                    Player sourcePlayer = plugin.getServer().getPlayer(target);
                    if (sourcePlayer != null)
                        sourcePlayer.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your request to " + event.getPlayer().getName() + " was cancelled because they logged out.");
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new TpaListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        requests.clear();
    }

    private void removeRequest(String source) {
        if (this.timeouts.containsKey(source) && !this.timeouts.get(source).isCancelled())
            this.timeouts.get(source).cancel();
        this.timeouts.remove(source);
        this.requests.remove(source);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by players.");
        }
        else if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length != 1) return false;
            Player targetPlayer = getServer().getPlayer(args[0]);
            if (targetPlayer != null && targetPlayer != sender) {
                targetPlayer.sendMessage(
                            ChatColor.ITALIC + ""
                        +   ChatColor.GOLD + sender.getName() + " is requesting to teleport to you. To accept, type "
                        +   ChatColor.RED + "/tpaccept " + sender.getName()
                        +   ChatColor.GOLD + " within 30 seconds or type "
                        +   ChatColor.RED + "/tpdeny " + sender.getName()
                        +   ChatColor.GOLD + " to deny it.");
                sender.sendMessage(
                            ChatColor.ITALIC + ""
                        +   ChatColor.GOLD + "Sent a teleport request to " + targetPlayer.getName() + ". They have 30 seconds to accept it.");
                requests.put(sender.getName(), targetPlayer.getName());
                timeouts.put(sender.getName(), getServer().getScheduler().runTaskLater(this, () -> {
                    try {
                        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your teleport request to " + targetPlayer.getName() + " expired.");
                        targetPlayer.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The teleport request from " + sender.getName() + " expired.");
                    } catch (Exception ignored) { }
                    removeRequest(sender.getName());
                }, 20 * 30));
            }
            else if (targetPlayer == sender) {
                sender.sendMessage(ChatColor.RED + "Woooosh! You teleported to yourself.");
            }
            else {
                sender.sendMessage(ChatColor.RED + "Could not find that player.");
            }
        }
        else if (command.getName().equalsIgnoreCase("tpcancel")) {
            if (requests.containsKey(sender.getName())) {
                String target = requests.get(sender.getName());

                removeRequest(sender.getName());

                sender.sendMessage(ChatColor.GOLD + "Removed your request to " + ChatColor.GREEN + target + ChatColor.GOLD + ".");
                Player targetPlayer = getServer().getPlayer(target);
                if (targetPlayer != null)
                    targetPlayer.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + sender.getName() + " cancelled their teleport request to you.");
            }
            else {
                sender.sendMessage(ChatColor.RED + "You have no outgoing teleport requests.");
            }
        }
        else if (command.getName().equalsIgnoreCase("tpaccept")) {
            List<String> sources = new ArrayList<>();
            if (args.length == 0) {
                for (String source : requests.keySet()) {
                    if (requests.get(source).equals(sender.getName()))
                        sources.add(source);
                }
            }
            else {
                for (String arg : args) {
                    if (!requests.containsKey(arg) || !requests.get(arg).equals(sender.getName())) {
                        sender.sendMessage(ChatColor.RED + "Could not find a request from '" + arg + "'.");
                        return true;
                    }
                    sources.add(arg);
                }
            }
            if (sources.size() == 0) {
                sender.sendMessage(ChatColor.GOLD + "Nothing to accept!");
            }
            else {
                for (String source : sources) {
                    Player sourcePlayer = getServer().getPlayer(source);

                    removeRequest(source);

                    if (sourcePlayer != null) {
                        sourcePlayer.teleport((Player) sender);
                        sourcePlayer.sendMessage(ChatColor.BLUE + sender.getName() + " accepted your teleport request.");
                        sender.sendMessage(ChatColor.BLUE + "Teleported " + source + " to you.");
                    }
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("tpdeny")) {
            List<String> sources = new ArrayList<>();
            if (args.length == 0) {
                for (String source : requests.keySet()) {
                    if (requests.get(source).equals(sender.getName()))
                        sources.add(source);
                }
            }
            else {
                for (String arg : args) {
                    if (!requests.containsKey(arg) || !requests.get(arg).equals(sender.getName())) {
                        sender.sendMessage(ChatColor.RED + "Could not find a request from '" + arg + "'.");
                        return true;
                    }
                    sources.add(arg);
                }
            }
            if (sources.size() == 0) {
                sender.sendMessage(ChatColor.GOLD + "Nothing to deny!");
            }
            else {
                for (String source : sources) {
                    Player sourcePlayer = getServer().getPlayer(source);

                    removeRequest(source);

                    if (sourcePlayer != null) {
                        sourcePlayer.sendMessage(ChatColor.RED + sender.getName() + " denied your teleport request.");
                        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Denied teleport request from " + source + ".");
                    }
                }
            }
        }
        else {
            return false;
        }
        return true;
    }
}
