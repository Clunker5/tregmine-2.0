package info.tregmine.web;

import info.tregmine.Tregmine;
import info.tregmine.WebHandler;
import info.tregmine.WebServer;
import info.tregmine.api.GenericPlayer;
import info.tregmine.api.PlayerReport;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IPlayerReportDAO;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AuthAction implements WebHandler.Action {
    private int playerId;
    private boolean found = false;
    private String token = null;
    public AuthAction(int playerId) {
        this.playerId = playerId;
    }

    private boolean checkForBan(Tregmine tregmine, GenericPlayer player) {
        try (IContext ctx = tregmine.createContext()) {
            IPlayerReportDAO reportDAO = ctx.getPlayerReportDAO();
            List<PlayerReport> reports = reportDAO.getReportsBySubject(player);
            for (PlayerReport report : reports) {
                Date validUntil = report.getValidUntil();
                if (validUntil == null) {
                    continue;
                }
                if (validUntil.getTime() < System.currentTimeMillis()) {
                    continue;
                }

                if (report.getAction() == PlayerReport.Action.BAN) {
                    return true;
                }
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    @Override
    public void generateResponse(PrintWriter writer) throws WebHandler.WebException {
        try {
            JSONWriter json = new JSONWriter(writer);

            if (found) {
                json.object().key("found").value(found).key("token").value(token).endObject();
            } else {
                json.object().key("found").value(found).endObject();
            }

            writer.close();
        } catch (JSONException e) {
            throw new WebHandler.WebException(e);
        }
    }

    private String generateToken() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 32; i++) {
            buffer.append(String.format("%02X", rand.nextInt(0xFF)));
        }

        return buffer.toString();
    }

    @Override
    public void queryGameState(Tregmine tregmine) {
        WebServer server = tregmine.getWebServer();
        Map<String, GenericPlayer> authTokens = server.getAuthTokens();

        // look for existing tokens
        for (Map.Entry<String, GenericPlayer> entry : authTokens.entrySet()) {
            String currentToken = entry.getKey();
            GenericPlayer currentPlayer = entry.getValue();
            if (currentPlayer.getId() == playerId) {
                if (!checkForBan(tregmine, currentPlayer)) {
                    token = currentToken;
                    found = true;
                    Tregmine.LOGGER.info("Restoring token " + token + " to " + currentPlayer.getRealName());
                } else {
                    token = null;
                    found = false;
                    Tregmine.LOGGER
                            .info("Refused to restore token for " + currentPlayer.getRealName() + " due to ban.");
                    authTokens.remove(currentToken);
                }
                return;
            }
        }

        // otherwise, retrieve player from db...
        GenericPlayer player = tregmine.getPlayerOffline(playerId);
        if (player == null) {
            found = false;
            return;
        }

        if (!checkForBan(tregmine, player)) {
            // and generate a new token
            token = generateToken();
            authTokens.put(token, player);
            found = true;

            Tregmine.LOGGER.info("Assigned token " + token + " to " + player.getRealName());
        } else {
            token = null;
            found = false;

            Tregmine.LOGGER.info("Denied token for " + player.getRealName() + " due to ban.");
        }
    }

    public static class Factory implements WebHandler.ActionFactory {
        public Factory() {
        }

        @Override
        public WebHandler.Action createAction(Request request) throws WebHandler.WebException {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                return new AuthAction(id);
            } catch (NullPointerException e) {
                throw new WebHandler.WebException(e);
            } catch (NumberFormatException e) {
                throw new WebHandler.WebException(e);
            }
        }

        @Override
        public String getName() {
            return "/auth";
        }
    }
}
