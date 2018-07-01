package eu.icrafted.statistics;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.GameReloadEvent;

import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.type.Include;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;
import org.spongepowered.api.service.sql.SqlService;

import com.google.inject.Inject;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.IntStream;

import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;

import java.util.concurrent.TimeUnit;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "statistics", name = "iCrafted Statistics Plugin", version = "0.0.1", description = "Statistics plugin for player and server information")
public class Statistics {
    private static List<String> cachedPlayers = new ArrayList<>();
    private static HashMap<String, Session> playerSessions = new HashMap<>();

    private int serverId;

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defConfig;

    private SqlService sql;
    private Connection conn;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private CommentedConfigurationNode config;

    // scheduler
    private Task task;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        try {
            logger.info("Starting, iCrafted statistics...");

            // load configuration
            initConfig();

            // open the SQL connection
            initSqlConnection();

            // load scheduler
            initScheduler();

            // get the server id
            serverId = getServerID(config.getNode("general", "server").getString());
            executeSql("UPDATE player SET isonline=0, serverid=NULL WHERE serverid=" + serverId);

            logger.info("-> Statistics module loaded");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        logger.info("Stopping, iCrafted statistics...");

        // handle the store of the sessions
        for(Session s:playerSessions.values()) {
            savePlayerStatistics(s.getUUID());
            executeSql("UPDATE player SET isonline=0, serverid=NULL WHERE id='" + s.getUUID() + "'");
        }

        // close the connection to the database server
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                conn = null;
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Listener
    public void onReloadPlugins(GameReloadEvent event)
    {
        try {
            // reload configuration
            initConfig();

            // open the SQL connection
            initSqlConnection();

            // load scheduler
            initScheduler();

            // get the server id
            serverId = getServerID(config.getNode("general", "server").getString());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event)
    {
        logger.info("Player joined: " + event.getTargetEntity().getName() + " [" + event.getTargetEntity().getUniqueId() + "]");

        try {
            // try get user from database
            ResultSet results = querySql("SELECT id FROM player WHERE id='" + event.getTargetEntity().getUniqueId().toString() + "'");
            if (results != null && results.first()) {
                executeSql("UPDATE player SET isonline=1, serverid=" + serverId + " WHERE id='" + event.getTargetEntity().getUniqueId().toString() + "'");
            } else {
                // add player to database
                executeSql("INSERT INTO player(name, id, isonline, serverid) VALUES('" + event.getTargetEntity().getName() + "', '" + event.getTargetEntity().getUniqueId().toString() + "', 1, " + serverId + ")");
            }

            // create player session
            Session session = new Session();
            executeSql("INSERT INTO statistic_player (playerid, serverid) VALUES('" + event.getTargetEntity().getUniqueId() + "', " + serverId + ")");
            ResultSet result = querySql("SELECT LAST_INSERT_ID()");
            if(result != null && result.first()) {
                session.setSessionID(result.getLong(1));
            }

            session.setName(event.getTargetEntity().getName());
            session.setUUID(event.getTargetEntity().getUniqueId().toString());
            session.setStartTime(System.currentTimeMillis());

            playerSessions.put(event.getTargetEntity().getUniqueId().toString(), session);
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Listener
    public void onAttackEntity(AttackEntityEvent event, @First Player player)
    {
        if(playerSessions.containsKey(player.getUniqueId().toString())) {
            //logger.info("Attack " + event.getFinalOutputDamage() + " - " + event.getTargetEntity().getType().getName() + " - " + player.getUniqueId());

            Session session = playerSessions.get(player.getUniqueId().toString());
            session.setDamagedealt(session.getDamagedealt() + event.getFinalOutputDamage());

            if(session.AttackedEntities.containsKey(event.getTargetEntity().getType().getName())) {
                session.AttackedEntities.put(event.getTargetEntity().getType().getName(), session.AttackedEntities.get(event.getTargetEntity().getType().getName()) + event.getFinalOutputDamage());
            } else {
                session.AttackedEntities.put(event.getTargetEntity().getType().getName(), event.getFinalOutputDamage());
            }
        }
    }

    @Listener
    public void onDamageEntityEvent(DamageEntityEvent event, @First Player player) {
        // do something
        //logger.info(event.getTargetEntity().getType().getName() + " - " + event.toString() + " - " + event.getBaseDamage() + " - " + event.getFinalDamage() + " - " + event.willCauseDeath() + " - " + event.getTargetEntity().getUniqueId());
        //logger.info(player.getName() + " is attacked? " + (player.getUniqueId().equals(event.getTargetEntity().getUniqueId().toString()) ? "yes" : "no"));

        if(playerSessions.containsKey(event.getTargetEntity().getUniqueId().toString())) {
            Session session = playerSessions.get(event.getTargetEntity().getUniqueId().toString());
            session.setDamagetaken(session.getDamagetaken() + event.getFinalDamage());
        }
    }

    @Listener
    public void onDestructEntityEvent(DestructEntityEvent event, @First Player player) {
        //logger.info(event.getCause().toString());
        if(player.getUniqueId() == event.getTargetEntity().getUniqueId() && playerSessions.containsKey(event.getTargetEntity().getUniqueId())) {
            //logger.info("Player died: " + player.getName());

            Session session = playerSessions.get(player.getUniqueId().toString());
            session.setDeads(session.getDeads() + 1);
        }
    }


    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event)
    {
        String playerID = event.getTargetEntity().getUniqueId().toString();

        savePlayerStatistics(playerID);

        playerSessions.remove(playerID);
        executeSql("UPDATE player SET isonline=0, serverid=NULL WHERE id='" + event.getTargetEntity().getUniqueId().toString() + "'");

        //logger.info("Player left: " + event.getTargetEntity().getName() + " [" + event.getTargetEntity().getUniqueId() + "]");
    }

    private void savePlayerStatistics(String playerID)
    {
        // get the session
        Session session = playerSessions.get(playerID);
        long playTime = (System.currentTimeMillis() - session.getStartTime());

        // jsonize the attack log
        Gson gson = new Gson();
        String attackLog = gson.toJson(session.AttackedEntities);

        // update the session play time in the database
        executeSql("UPDATE statistic_player SET playtime=" + playTime + ", deads=" + session.getDeads() + ", damagetaken=" + session.getDamagetaken() + ", damagedealt=" + session.getDamagedealt() + ", attacklog='" + attackLog + "' WHERE id=" + session.getSessionID());
    }

    private static int LastOnlinePlayers = -1;
    private static double LastTps = -1;

    private void initScheduler()
    {
        logger.info("Reloading tasks...");
        if (task != null){
            task.cancel();
            logger.info("-> Task stoped");
        }

        // run every x seconds
        int interval = config.getNode("general", "interval").getInt();

        // create schedule task
        task = game.getScheduler().createTaskBuilder().interval(interval, TimeUnit.SECONDS).execute(t -> {
            // store the session information
            for(Player p:game.getServer().getOnlinePlayers()) {
                savePlayerStatistics(p.getUniqueId().toString());
            }

            /*List<String> processedPlayers = new ArrayList<>();

            for(Player p:game.getServer().getOnlinePlayers()) {
                logger.info("> " + p.getName());
                String playerID = "";

                if(cachedPlayers.contains(p.getUniqueId().toString())) {
                    playerID = p.getUniqueId().toString();
                } else {
                    try {
                        // try get user from database
                        ResultSet results = querySql("SELECT id FROM player WHERE id='" + p.getUniqueId().toString() + "'");
                        if (results != null && results.first()) {
                            playerID = results.getString(1);
                            cachedPlayers.add(p.getUniqueId().toString());
                        } else {
                            // add player to database
                            executeSql("INSERT INTO player(name, id, isonline) VALUES('" + p.getName() + "', '" + p.getUniqueId().toString() + "', 1)");
                        }
                    } catch(SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                processedPlayers.add(playerID);
            }

            // remove all players which are not online
            if(processedPlayers.size() > 0) {
                executeSql("UPDATE player SET isonline=0 WHERE id NOT IN('" + String.join("','", processedPlayers) + "')");
            }*/

            // check if something changed
            if(config.getNode("statistics", "store-always").getBoolean() || (LastOnlinePlayers != game.getServer().getOnlinePlayers().size() || LastTps != game.getServer().getTicksPerSecond())) {
                LastTps = game.getServer().getTicksPerSecond();
                LastOnlinePlayers = game.getServer().getOnlinePlayers().size();

                // write statistics
                executeSql("INSERT INTO statistic_server(serverid, tps, onlineplayers, timestamp) VALUES('" + serverId + "', " + LastTps + ", " + LastOnlinePlayers + ", NOW())");
            }

            //logger.info("Online players: " + game.getServer().getOnlinePlayers().size());
            //logger.info("Ticks per seconds: " + game.getServer().getTicksPerSecond());

            /*for(World w:game.getServer().getWorlds()) {
                logger.info("> World: " + w.getName());
                // is array
                //logger.info(" - Loaded chunks: " + w.getLoadedChunks());
                logger.info(" - Dimension: " + w.getDimension().getType().getName());
                logger.info(" - Weather: " + w.getWeather().getName());

            }*/
        }).submit(this);
    }

    private void initConfig()
    {
        logger.info("-> Config module");
        try {
            // check if configuration exists else create it
            Files.createDirectories(configDir);
            if(!defConfig.exists()) {
                logger.info("Creating configuration file... :)");
                defConfig.createNewFile();
            }

            // get the configuration manager
            configManager = HoconConfigurationLoader.builder().setFile(defConfig).build();
            config = configManager.load();

            // build configuration
            config.getNode("general", "interval").setValue(config.getNode("general", "interval").getInt(15));
            config.getNode("general", "server").setValue(config.getNode("general", "server").getString(""));

            config.getNode("statistics", "store-always").setValue(config.getNode("general", "store-always").getBoolean(false));

            config.getNode("database", "server").setValue(config.getNode("database", "server").getString("localhost"));
            config.getNode("database", "username").setValue(config.getNode("database", "username").getString("root"));
            config.getNode("database", "password").setValue(config.getNode("database", "password").getString(""));
            config.getNode("database", "database").setValue(config.getNode("database", "database").getString(""));

            // store the configuration
            configManager.save(config);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private int getServerID(String identifier) throws Exception
    {
        if(identifier.length() < 1) {
            throw new Exception("No server name is given.");
        }

        try {
            // check if the server exists
            ResultSet result = querySql("SELECT id FROM server WHERE identifier='" + identifier + "'");
            if (result != null && result.first()) {
                return result.getInt(1);
            } else {
                executeSql("INSERT INTO server(identifier) VALUES('" + identifier + "')");
                result = querySql("SELECT LAST_INSERT_ID()");
                if(result != null && result.first()) {
                    return result.getInt(1);
                }
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
        }

        return -1;
    }

    private void initSqlConnection()
    {
        if(sql == null) {
            sql = Sponge.getServiceManager().provide(SqlService.class).get();
        }

        try {
            conn = sql.getDataSource("jdbc:mysql://" + config.getNode("database", "username").getString() + ":" + config.getNode("database", "password").getString() + "@" + config.getNode("database", "server").getString() + "/" + config.getNode("database", "database").getString()).getConnection();

            // validate the table setup
            /*ResultSet results = querySql("SHOW TABLES");
            while(results != null && results.next()) {
                logger.info("- table found: " + results.getString(1));
            }*/
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    private ResultSet querySql(String query)
    {
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet results = stmt.executeQuery();
            stmt = null;

            return results;
        } catch(SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private void executeSql(String query)
    {
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            boolean result = stmt.execute();
            stmt = null;
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }
}