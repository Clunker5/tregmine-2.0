package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import static org.bukkit.ChatColor.WHITE;

public abstract class NotifyCommand extends AbstractCommand {
    public NotifyCommand(Tregmine tregmine, String command) {
        super(tregmine, command);
    }

    private String argsToMessage(String[] args) {
        StringBuffer buf = new StringBuffer();
        buf.append(args[0]);
        for (int i = 1; i < args.length; ++i) {
            buf.append(" ");
            buf.append(args[i]);
        }

        return buf.toString();
    }

    protected abstract ChatColor getColor();

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String msg = argsToMessage(args);
        if (player.getRank().canUseChatColors()) {
            msg = ChatColor.translateAlternateColorCodes('#', msg);
        }

        // Don't send it twice
        if (!isTarget(player)) {
            player.sendMessage(new TextComponent(getColor() + " + "), player.decideVS(player),
                    new TextComponent(" " + WHITE + msg));
        }

        for (TregminePlayer to : tregmine.getOnlinePlayers()) {
            if (!isTarget(to)) {
                continue;
            }
            to.sendMessage(new TextComponent(getColor() + " + "), player.decideVS(to),
                    new TextComponent(" " + WHITE + msg));
        }

        return true;
    }

    protected abstract boolean isTarget(TregminePlayer player);
}
