package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public class WorldCommand extends AbstractCommand {
    private Tregmine t;

    public WorldCommand(Tregmine t) {
        super(t, "world");
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args) {
        List<?> allowedWorlds = t.getConfig().getList("worlds.special.worlds");
        if (allowedWorlds.contains(args[0].toLowerCase())) {

        } else {
            player.sendMessage(
                    new TextComponent(ChatColor.RED + "You cannot go to that world with the autoswitcher."));
        }
        return true;
    }
}
