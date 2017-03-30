package info.tregmine.commands;

import info.tregmine.Tregmine; import info.tregmine.api.GenericPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.List;

public class SendToCommand extends AbstractCommand {
    public SendToCommand(Tregmine tregmine) {
        super(tregmine, "sendto");
    }

    @Override
    public boolean handlePlayer(GenericPlayer player, String[] args) {
        if (args.length != 2) {
            return false;
        }
        if (!player.getRank().canSendPeopleToOtherWorlds()) {
            return true;
        }

        List<GenericPlayer> candidates = tregmine.matchPlayer(args[0]);
        if (candidates.size() != 1) {
            // TODO: List users
            return true;
        }

        GenericPlayer victim = candidates.get(0);
        Server server = tregmine.getServer();
        World world = server.getWorld(args[1]);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "That world does not exist.");
            return true;
        }

        Location cpspawn = world.getSpawnLocation();
        victim.teleportWithHorse(cpspawn);

        return true;
    }
}
