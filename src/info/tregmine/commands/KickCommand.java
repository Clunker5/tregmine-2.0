package info.tregmine.commands;

import java.util.List;

import static org.bukkit.ChatColor.*;
import org.bukkit.Server;
import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import info.tregmine.api.PlayerReport;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IPlayerReportDAO;
import net.md_5.bungee.api.chat.TextComponent;

public class KickCommand extends AbstractCommand
{
	private Tregmine plugin;
    public KickCommand(Tregmine tregmine)
    {
        super(tregmine, "kick");
        plugin = tregmine;
    }

    private String argsToMessage(String[] args)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 1; i < args.length; ++i) {
            buf.append(" ");
            buf.append(args[i]);
        }

        return buf.toString();
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args)
    {
        if (!player.getRank().canKick()) {
            return false;
        }

        if (args.length < 2) {
            player.sendStringMessage(DARK_AQUA + "/kick <player> <message>");
            return true;
        }

        Server server = player.getServer();
        String pattern = args[0];
        String message = argsToMessage(args);

        List<TregminePlayer> candidates = tregmine.matchPlayer(pattern);
        if (candidates.size() != 1) {
            // TODO: error message
            return true;
        }

        TregminePlayer victim = candidates.get(0);
        plugin.broadcast(new TextComponent(player.getChatName() + "" + AQUA + " kicked "
                + victim.getChatName() + AQUA + ": " + message));
        LOGGER.info(victim.getName() + " kicked by " + player.getName());
        victim.kickPlayer(plugin, "kicked by " + player.getName() + ": " + message);

        try (IContext ctx = tregmine.createContext()) {
            PlayerReport report = new PlayerReport();
            report.setSubjectId(victim.getId());
            report.setIssuerId(player.getId());
            report.setAction(PlayerReport.Action.KICK);
            report.setMessage(message);

            IPlayerReportDAO reportDAO = ctx.getPlayerReportDAO();
            reportDAO.insertReport(report);
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public boolean handleOther(Server server, String[] args)
    {
        if (args.length == 0) {
            return false;
        }

        String pattern = args[0];

        List<TregminePlayer> candidates = tregmine.matchPlayer(pattern);
        if (candidates.size() != 1) {
            // TODO: error message
            return true;
        }

        TregminePlayer victim = candidates.get(0);

        server.broadcastMessage("GOD kicked " + victim.getChatName() + ".");
        victim.kickPlayer(plugin, "kicked by GOD.");

        return true;
    }
}
