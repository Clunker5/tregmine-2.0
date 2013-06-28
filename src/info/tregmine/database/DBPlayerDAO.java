package info.tregmine.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
//import java.util.List;

import org.bukkit.entity.Player;

import info.tregmine.api.TregminePlayer;

public class DBPlayerDAO
{
    private Connection conn;

    public DBPlayerDAO(Connection conn)
    {
        this.conn = conn;
    }

    public TregminePlayer getPlayer(Player player)
    throws SQLException
    {
        return getPlayer(player.getName(), player);
    }

    public TregminePlayer getPlayer(String name)
    throws SQLException
    {
        return getPlayer(name, null);
    }

    public TregminePlayer getPlayer(String name, Player wrap)
    throws SQLException
    {
        TregminePlayer player = new TregminePlayer(wrap);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM `user` " +
                                         "WHERE player = ?");
            stmt.setString(1, player.getName());
            stmt.execute();

            rs = stmt.getResultSet();
            if (!rs.next()) {
                return null;
            }

            player.setId(rs.getInt("uid"));
            player.setPassword(rs.getString("password"));
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
                rs = null;
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
                stmt = null;
            }
        }

        try {
            stmt = conn.prepareStatement("SELECT * FROM user_settings " +
                                         "WHERE id = ?");
            stmt.setInt(1, player.getId());
            stmt.execute();

            rs = stmt.getResultSet();
            while (rs.next()) {
                String key = rs.getString("key");
                String value = rs.getString("value");
                if ("admin".equals(key)) {
                    player.setAdmin(Boolean.valueOf(value));
                } else if ("builder".equals(key)) {
                    player.setBuilder(Boolean.valueOf(value));
                } else if ("child".equals(key)) {
                    player.setChild(Boolean.valueOf(value));
                } else if ("invisible".equals(key)) {
                    player.setInvisible(Boolean.valueOf(value));
                } else if ("donator".equals(key)) {
                    player.setDonator(Boolean.valueOf(value));
                } else if ("banned".equals(key)) {
                    player.setBanned(Boolean.valueOf(value));
                } else if ("trusted".equals(key)) {
                    player.setTrusted(Boolean.valueOf(value));
                } else if ("tpblock".equals(key)) {
                    player.setTeleportShield(Boolean.valueOf(value));
                } else if ("hiddenloc".equals(key)) {
                    player.setHiddenLocation(Boolean.valueOf(value));
                } else if ("guardian".equals(key)) {
                    player.setGuardian(true);
                    player.setGuardianRank(Integer.parseInt(value));
                } else if ("password".equals(key)) {
                    player.setPassword(value);
                } else if ("keyword".equals(key)) {
                    player.setKeyword(value);
                } else if ("countryName".equals(key)) {
                    player.setCountryName(value);
                } else if ("city".equals(key)) {
                    player.setCity(value);
                } else if ("ip".equals(key)) {
                    player.setIp(value);
                } else if ("postalCode".equals(key)) {
                    player.setPostalCode(value);
                } else if ("region".equals(key)) {
                    player.setRegion(value);
                } else if ("hostName".equals(key)) {
                    player.setHostName(value);
                } else if ("color".equals(key)) {
                    player.setNameColor(value);
                } else if ("timezone".equals(key)) {
                    player.setTimezone(value);
                }
            }
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }

        return player;
    }

    public TregminePlayer createPlayer(Player wrap)
    throws SQLException
    {
        TregminePlayer player = new TregminePlayer(wrap);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "INSERT INTO user (player) VALUE (?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, player.getName());
            stmt.execute();

            stmt.executeQuery("SELECT LAST_INSERT_ID()");

            rs = stmt.getResultSet();
            if (!rs.next()) {
                throw new SQLException("Failed to get player id");
            }

            player.setId(rs.getInt(1));
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }

        return player;
    }

    public void updatePlayerPermissions(TregminePlayer player)
    throws SQLException
    {
        updateProperty(player, "admin", player.isAdmin());
        updateProperty(player, "builder", player.isBuilder());
        updateProperty(player, "child", player.isChild());
        updateProperty(player, "donator", player.isDonator());
        updateProperty(player, "banned", player.isBanned());
        updateProperty(player, "trusted", player.isTrusted());
        if (player.isGuardian()) {
            updateProperty(player,
                           "guardian",
                           String.valueOf(player.getGuardianRank()));
        }
    }

    public void updatePlayerKeyword(TregminePlayer player)
    throws SQLException
    {
        updateProperty(player, "keyword", player.getKeyword());
    }

    public void updatePlayerInfo(TregminePlayer player)
    throws SQLException
    {
        //updateProperty(player, "invisible", player.isInvisible());
        //updateProperty(player, "tpblock", player.hasTeleportShield());
        //updateProperty(player, "hiddenloc", player.hasHiddenLocation());
        updateProperty(player, "countryName", player.getCountryName());
        updateProperty(player, "city", player.getCity());
        updateProperty(player, "ip", player.getIp());
        updateProperty(player, "postalCode", player.getPostalCode());
        updateProperty(player, "region", player.getRegion());
        updateProperty(player, "hostName", player.getHostName());
        updateProperty(player, "color", player.getColor());
        updateProperty(player, "timezone", player.getTimezone());
    }

    private void updateProperty(TregminePlayer player, String key, boolean value)
    throws SQLException
    {
        updateProperty(player, key, String.valueOf(value));
    }

    private void updateProperty(TregminePlayer player, String key, String value)
    throws SQLException
    {
        if (value == null) {
            return;
        }

        PreparedStatement stmt = null;
        try {
            String sqlInsert = "REPLACE INTO user_settings (id, `key`, `value`, username) " +
                               "VALUE (?, ?, ?, ?)";
            stmt = conn.prepareStatement(sqlInsert);
            stmt.setInt(1, player.getId());
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.setString(4, player.getName());
            stmt.execute();

        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    public void updatePassword(TregminePlayer player)
    throws SQLException
    {
        PreparedStatement stmt = null;
        try {
            String sql = "UPDATE user SET password = ? WHERE uid = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, player.getPasswordHash());
            stmt.setInt(2, player.getId());
            stmt.execute();
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }
}
