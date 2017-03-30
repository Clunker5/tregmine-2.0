package info.tregmine;

import info.tregmine.api.GenericPlayer;
import info.tregmine.web.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class WebServer implements Runnable {
    private Server webServer;
    private WebHandler webHandler;
    private ChatHandler chatHandler;
    private Map<String, GenericPlayer> authTokens;
    private BlockingQueue<ChatAction> messageQueue;
    private Thread thread = null;
    private boolean running = true;

    public WebServer(Tregmine tregmine) {
        thread = new Thread(this);

        authTokens = new HashMap<>();
        messageQueue = new LinkedBlockingQueue<>();

        PluginManager pluginMgm = tregmine.getServer().getPluginManager();

        try {
            FileConfiguration config = tregmine.getConfig();
            String apiKey = config.getString("api.signing-key", "");
            int apiPort = config.getInt("api.port", 9192);

            HandlerList handlers = new HandlerList();

            // Start chat handler and bind to /chat
            chatHandler = new ChatHandler(tregmine, pluginMgm);
            pluginMgm.registerEvents(chatHandler, tregmine);

            ContextHandler context = new ContextHandler();
            context.setContextPath("/chat");
            context.setHandler(chatHandler);
            handlers.addHandler(context);

            // Start web handler and bind to rest of paths
            webHandler = new WebHandler(tregmine, pluginMgm, apiKey);
            pluginMgm.registerEvents(webHandler, tregmine);

            webHandler.addAction(new AuthAction.Factory());
            webHandler.addAction(new PlayerKickAction.Factory());
            webHandler.addAction(new PlayerListAction.Factory());
            webHandler.addAction(new PushNotificationAction.Factory());
            webHandler.addAction(new PlayerReloadInventoryAction.Factory());
            webHandler.addAction(new QueryLogAction.Factory());
            webHandler.addAction(new VersionAction.Factory());

            handlers.addHandler(webHandler);

            // Disable jetty logging
            org.eclipse.jetty.util.log.Log.setLog(new DummyLogger());

            // Start server at apiPort
            webServer = new Server();

            ServerConnector connector = new ServerConnector(webServer);
            connector.setPort(apiPort);
            connector.setHost("localhost");
            connector.setReuseAddress(true);
            connector.setSoLingerTime(-1);
            webServer.addConnector(connector);
            webServer.setHandler(handlers);
        } catch (Exception e) {
            Tregmine.LOGGER.log(Level.WARNING, "Failed to start web server!", e);
        }
    }

    public void executeChatAction(ChatAction msg) {
        messageQueue.offer(msg);
    }

    public Map<String, GenericPlayer> getAuthTokens() {
        return authTokens;
    }

    public ChatHandler getChatHandler() {
        return chatHandler;
    }

    public WebHandler getWebHandler() {
        return webHandler;
    }

    public boolean isPlayerOnWeb(GenericPlayer player) {
        return chatHandler.isOnline(player);
    }

    @Override
    public void run() {
        try {
            webServer.start();
            Tregmine.LOGGER.info("Web server started.");
        } catch (Exception e) {
            Tregmine.LOGGER.log(Level.WARNING, "Failed to start web server!", e);
        }

        while (running) {
            try {
                ChatAction msg = messageQueue.take();
                msg.execute(chatHandler);
            } catch (InterruptedException e) {
            } catch (Exception e) {
                Tregmine.LOGGER.log(Level.WARNING, "Exception in WebServer message loop.", e);
            }
        }

        try {
            webServer.stop();
            webServer.join();
        } catch (Exception e) {
            Tregmine.LOGGER.log(Level.WARNING, "Failed to stop web server!", e);
        }
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        running = false;

        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ChatAction {
        void execute(ChatHandler handler);
    }

    public static class ChatMessage implements ChatAction {
        private GenericPlayer sender;
        private String channel;
        private String text;

        public ChatMessage(GenericPlayer sender, String channel, String text) {
            this.sender = sender;
            this.channel = channel;
            this.text = text;
        }

        @Override
        public void execute(ChatHandler handler) {
            handler.broadcastToWeb(sender, channel, text);
        }

        public String getChannel() {
            return channel;
        }

        public GenericPlayer getSender() {
            return sender;
        }

        public String getText() {
            return text;
        }
    }

    private static class DummyLogger implements org.eclipse.jetty.util.log.Logger {
        @Override
        public void debug(String msg, Object... args) {
        }

        @Override
        public void debug(String msg, Throwable thrown) {
        }

        @Override
        public void debug(Throwable thrown) {
        }

        @Override
        public org.eclipse.jetty.util.log.Logger getLogger(String name) {
            return this;
        }

        @Override
        public String getName() {
            return "DummyLogger";
        }

        @Override
        public void ignore(Throwable ignored) {
        }

        @Override
        public void info(String msg, Object... args) {
        }

        @Override
        public void info(String msg, Throwable thrown) {
        }

        @Override
        public void info(Throwable thrown) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void setDebugEnabled(boolean enabled) {
        }

        @Override
        public void warn(String msg, Object... args) {
        }

        @Override
        public void warn(String msg, Throwable thrown) {
        }

        @Override
        public void warn(Throwable thrown) {
        }

        @Override
        public void debug(String msg, long value) {

        }
    }

    public static class KickAction implements ChatAction {
        private GenericPlayer sender;
        private GenericPlayer victim;
        private String message;

        public KickAction(GenericPlayer sender, GenericPlayer victim, String message) {
            this.sender = sender;
            this.victim = victim;
            this.message = message;
        }

        @Override
        public void execute(ChatHandler handler) {
            handler.kickPlayer(sender, victim, message);
        }
    }
}
