package glorydark.pvp;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.Config;
import gameapi.arena.Arena;
import gameapi.listener.base.GameListenerRegistry;
import gameapi.room.Room;
import gameapi.room.RoomRule;
import gameapi.room.RoomStatus;
import gameapi.utils.SmartTools;

import java.util.*;

public class MainClass extends PluginBase {

    public static String path;

    public static Plugin plugin;

    public static List<Player> queuePlayers = new ArrayList<>();

    Map<String, Object> mapConfigs = new HashMap<>();

    @Override
    public void onLoad() {
        this.getLogger().info("PvpGame onLoad");
    }

    @Override
    public void onEnable() {
        plugin = this;
        path = this.getDataFolder().getPath();
        this.saveResource("maps.yml", false);
        GameListenerRegistry.registerEvents("PvpGame", new PvpGameListener(), this);
        mapConfigs = new Config(path+"/maps.yml", Config.YAML).getAll();
        this.getServer().getCommandMap().register("", new PvpCommands("pvp"));
        Server.getInstance().getScheduler().scheduleRepeatingTask(this, new NukkitRunnable() {

            final Random random = new Random(System.currentTimeMillis());
            boolean isEnabled = true;

            @Override
            public void run() {
                while (isEnabled && queuePlayers.size() >= 2){
                    int randomId = random.nextInt(mapConfigs.keySet().size()); // [0, keySize)
                    String mapName = new ArrayList<>(mapConfigs.keySet()).get(randomId);
                    Room room = loadRoom(mapName);
                    if(room != null){
                        Player p1 = queuePlayers.get(0);
                        Player p2 = queuePlayers.get(1);
                        room.addPlayer(p1);
                        room.addPlayer(p2);
                        SmartTools.sendMessage(room.getPlayers(), "Preparing the match, map:"+mapName);
                        queuePlayers.remove(p1);
                        queuePlayers.remove(p2);
                    }else{
                        plugin.getLogger().warning("Unable to load the map: "+mapName);
                        isEnabled = false;
                    }
                }
            }

        }, 20);
        this.getLogger().info("PvpGame onEnabled");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("PvpGame onDisabled");
    }

    /**
     * This method is to load a room, either temporary or normal.
     *
     * @param map mapName
     * @return room
     */
    public Room loadRoom(String map){
        RoomRule roomRule = new RoomRule(0);
        roomRule.setAllowDamagePlayer(true);
        roomRule.setNoTimeLimit(true);
        Room room = new Room("PvpGame", roomRule, "", 1);
        room.setTemporary(true);
        Map<String, Object> config = (Map<String, Object>) mapConfigs.getOrDefault(map, new HashMap<>());
        if (config.containsKey("LoadWorld")) {
            String backup = (String) config.get("LoadWorld");
            room.setRoomLevelBackup(backup);
            String newName = room.getGameName() + "_" + backup + "_" + UUID.randomUUID();
            room.setRoomName(newName);
            if (Arena.copyWorldAndLoad(newName, backup)) {
                room.setPlayLevel(Server.getInstance().getLevelByName(newName));
                if (Server.getInstance().isLevelLoaded(newName)) {
                    Server.getInstance().getLevelByName(newName).setAutoSave(false);
                    this.getLogger().info("Room 【" + backup + "】 loaded！");

                    if(config.containsKey("WaitSpawn")){
                        room.setWaitSpawn(((String) config.get("WaitSpawn")).replace(backup, newName));
                    }else{
                        this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in WaitSpawn Configuration!");
                        return null;
                    }

                    if(config.containsKey("StartSpawn")){
                        room.addStartSpawn(((String) config.get("StartSpawn")).replace(backup, newName));
                    }else{
                        this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in StartSpawn Configuration!");
                        return null;
                    }

                    if(config.containsKey("WaitTime")){
                        room.setWaitTime((int) config.get("WaitTime"));
                    }else{
                        this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in WaitTime Configuration!");
                        return null;
                    }

                    if(config.containsKey("GameTime")){
                        room.setGameTime((int) config.get("GameTime"));
                    }else{
                        this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in GameTime Configuration!");
                        return null;
                    }
                    room.setMinPlayer(2);
                    room.setMaxPlayer(2);
                    room.setEndSpawn(Server.getInstance().getDefaultLevel().getSpawnLocation().getLocation());
                    Room.loadRoom(room);
                    room.setRoomStatus(RoomStatus.ROOM_STATUS_WAIT);
                    room.setWinConsoleCommands((List<String>) config.getOrDefault("WinCommands", new ArrayList<>()));
                    room.setLoseConsoleCommands((List<String>) config.getOrDefault("FailedCommands", new ArrayList<>()));
                    this.getLogger().info("Room "+map+" loaded successfully!");
                    return room;
                } else {
                    this.getLogger().error("Failed to load the map: " + backup);
                }
            } else {
                this.getLogger().error("Failed to copy the map!");
            }
        } else {
            this.getLogger().error("Failed to load the room, caused by: MapNotFoundException");
        }
        return null;
    }
}