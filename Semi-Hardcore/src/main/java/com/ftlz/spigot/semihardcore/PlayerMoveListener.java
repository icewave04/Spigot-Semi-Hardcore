package com.ftlz.spigot.semihardcore;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.md_5.bungee.api.ChatColor;
 
public class PlayerMoveListener implements Listener
{
    //private JSONArray _deathData;
    private HashMap<String, DeathData> _deathLocations;
    private HashMap<String, Timer> _deathTimers;

    private App _app;
    private final String DeathLocationsFilename = "SH-DeathLocations.json";

    public PlayerMoveListener(App app)
    {
        _app = app;
        _deathLocations = new HashMap<String, DeathData>();
        _deathTimers = new HashMap<String, Timer>();
        loadData();
    }

    @EventHandler
    public void onEntityDeath(PlayerDeathEvent event)
    {
        _app.getLogger().info("onEntityDeath");

        Player player = event.getEntity();

        int deathDuration = _app.getConfig().getInt("death-duration", 21600);

        _deathLocations.put(player.getName(), new DeathData(player, deathDuration));

        startTimer(player.getName(), deathDuration);

        saveData();
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        _app.getLogger().info("onPlayerRespawn");

        Player player = event.getPlayer();

        _app.getServer().getScheduler().scheduleSyncDelayedTask(_app, new Runnable() {
            public void run() {                

                if (player.getGameMode() == GameMode.SPECTATOR)
                {
                    if (_deathLocations.containsKey(player.getName()))
                    {
                        DeathData deathData = _deathLocations.get(player.getName());
                        _app.getLogger().info("containsKey");
                        player.teleport(deathData.getDeathLocation());
                        player.setFlySpeed(0);
                        player.setWalkSpeed(0);
                        _app.getLogger().info("deathTime: " + deathData.getDeathTime());
                        _app.getLogger().info("getRespawnTime: " + deathData.getRespawnTime());
                        int currentTime = (int)(System.currentTimeMillis() / 1000L);
                        int secondsUntilRespawn = deathData.getRespawnTime() - currentTime;
                        if (secondsUntilRespawn > 0)
                        {
                            String respawnTime = secondsToDisplay( secondsUntilRespawn);
                            player.sendMessage("" + ChatColor.RED + "" + ChatColor.BOLD + "NOTE:" + ChatColor.RESET + "" + ChatColor.RED + " You will respawn in " + respawnTime + ".");
                        }
                        else
                        {
                            respawnPlayer(player);
                        }
                    }
                    else
                    {
                        _app.getLogger().info("doesnt contain key");
                        player.teleport(player.getBedSpawnLocation());
                        player.sendMessage("" + ChatColor.RED + "" + ChatColor.BOLD + "NOTE:" + ChatColor.RESET + "" + ChatColor.RED + " Something broke, fetch an OP to make you no longer a ghost.");

                    }
                }
                else
                {

                }
            }
        }, 2l);

       
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        _app.getLogger().info("onPlayerQuitEvent");

        String playerName = event.getPlayer().getName();
        Timer timer = _deathTimers.get(playerName);
        if (timer != null)
        {
            timer.cancel();
            timer = null;

            _deathTimers.remove(playerName);
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event)
    {
        _app.getLogger().info("onPlayerJoinEvent");

        Player player = event.getPlayer();
        player.sendMessage("" + ChatColor.RED + "" + ChatColor.BOLD + "NOTE:" + ChatColor.RESET + "" + ChatColor.RED + " A death will result in you being a ghost for " + displaySeconds + ".");


        if (player.getGameMode() == GameMode.SPECTATOR)
        {
            if (_deathLocations.containsKey(player.getName()))
            {
                DeathData deathData = _deathLocations.get(player.getName());
                
            }
            else
            {
                respawnPlayer(player);
            }
            
            int deathDuration = _app.getConfig().getInt("death-duration", 21600);
            String displaySeconds = secondsToDisplay(deathDuration);

        }

    }
    
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event)
    {
        if (event.getCause() == TeleportCause.SPECTATE)
        {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Sorry, but I can't let you do that!");
        }
    }

    private void displayRespawnCountdown(Player player)
    {
        if (_deathLocations.containsKey(player.getName()) && player.isOnline())
        {

        }
    }

    private String secondsToDisplay(int seconds)
    { 
        int remainingSeconds = seconds;

        int minutes = remainingSeconds / 60;
        remainingSeconds -= (minutes * 60);

        int hours = minutes / 60;
        minutes -= (hours * 60);

        String output = "";

        if (hours > 0)
        {
            output += "" + hours + ((hours == 1) ? " hour" : " hours");
        }

        if (minutes > 0)
        {
            if (output != "")
            {
                output += ", "; 
            }
            output += "" + minutes + ((minutes == 1) ? " minute" : " minutes");
        }

        if (remainingSeconds > 0)
        {
            if (output != "")
            {
                output += ", "; 
            }
            output += "" + remainingSeconds + ((remainingSeconds == 1) ? " second" : " seconds");
        }

        return output;
    }

    private void loadData()
    {
        _app.getLogger().info("LoadData");
        FileReader fileReader = null;
        try
        {
            File file = new File(DeathLocationsFilename);
            if (file.exists())
            {
                if (file.canRead())
                {
                    final GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.registerTypeAdapter(Location.class, new LocationAdapter());
                    gsonBuilder.setPrettyPrinting();        
                    final Gson gson = gsonBuilder.create();
                   
                    fileReader = new FileReader(file);                   
                    _deathLocations = gson.fromJson(fileReader, _deathLocations.getClass());
                }
                else
                {
                    throw new Exception("Config exists but we can't read it.");
                }
            }  
        }
        catch (Exception err)
        {
            _app.getLogger().info("ERROR (LoadData): " + err.getMessage());
            _deathLocations = new HashMap<String, DeathData>();
        }
        finally
        {
            if (fileReader != null)
            {
                try
                {
                    fileReader.close();
                }
                catch (Exception err)
                {
                    _app.getLogger().info("ERROR (LoadData): " + err.getMessage());
                }
                fileReader = null;
            }
        }
    }

    private void saveData()
    {
        _app.getLogger().info("SaveData");

        FileWriter fileWriter = null;
        try
        {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Location.class, new LocationAdapter());
            gsonBuilder.setPrettyPrinting();        
            final Gson gson = gsonBuilder.create();
            String jsonData = gson.toJson(_deathLocations);

            fileWriter = new FileWriter(DeathLocationsFilename);    
            fileWriter.write(jsonData);
        }
        catch (Exception err)
        {
            _app.getLogger().info("ERROR (SaveData): " + err.getMessage());
        }
        finally
        {
            if (fileWriter != null)
            {
                try
                {
                    fileWriter.close();
                }
                catch (Exception err)
                {
                    _app.getLogger().info("ERROR (SaveData): " + err.getMessage());
                }

                fileWriter = null;
            } 
        }
    }

    private void cancelTimer(String playerName)
    {
        if (_deathTimers.containsKey(playerName))
        {
            Timer timer = _deathTimers.get(playerName);
            if (timer != null)
            {
                timer.cancel();
                timer = null;
            }
            _deathTimers.remove(playerName);
        }
    }

    private void startTimer(String playerName, int seconds)
    {
        // If it exists, kill it.
        cancelTimer(playerName);

        TimerTask task = new TimerTask()
        {
            public void run()
            {
                _app.getLogger().info("TimerTaskFired");
                String playerName = Thread.currentThread().getName().replace("-DeathTimer", "");
                _app.getLogger().info(playerName);
                _deathTimers.remove(playerName);
                Player player = _app.getServer().getPlayer(playerName);
                respawnPlayer(player);
                
            }
        };
        Timer timer = new Timer(playerName+"-DeathTimer");
        timer.schedule(task, seconds * 1000L);
        _deathTimers.put(playerName, timer);
    }

    private void respawnPlayer(Player player)
    {
        _app.getLogger().info("RespawnPlayer");

        if (player == null || player.isOnline() == false)
        {
            return;
        }

        if (player.getGameMode() != GameMode.SPECTATOR)
        {
            return;
        }

        _app.getServer().getScheduler().scheduleSyncDelayedTask(_app, new Runnable() {
            public void run() {        
                
                Location spawnLocation = player.getBedSpawnLocation();

                if (spawnLocation == null)
                {
                    spawnLocation = _app.getServer().getWorlds().get(0).getSpawnLocation();
                }

                player.teleport(spawnLocation);
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);            
                player.spigot().respawn();
                player.setGameMode(GameMode.SURVIVAL);
            }
        }, 2l);
    }

    
}