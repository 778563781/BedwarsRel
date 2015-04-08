package io.github.yannici.bedwarsreloaded;

import io.github.yannici.bedwarsreloaded.Commands.BaseCommand;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BedwarsCommandExecutor implements CommandExecutor {

    private Main plugin = null;

    public BedwarsCommandExecutor(Main plugin) {
        super();

        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if(!cmd.getName().equalsIgnoreCase("bw")) {
            return false;
        }
        
        if(args.length < 1) {
            return false;
        }

        String command = args[0].toString();
        ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(args));
        arguments.remove(0);

        for(BaseCommand bCommand : this.plugin.getCommands()) {
            if(bCommand.getCommand().equalsIgnoreCase(command)) {
                if(bCommand.getArguments().length > arguments.size()) {
                    sender.sendMessage(ChatWriter.pluginMessage(ChatColor.RED + Main._l("errors.argumentslength")));
                    return false;
                }

                return bCommand.execute(sender, arguments);
            }
        }

        return false;
    }

}
