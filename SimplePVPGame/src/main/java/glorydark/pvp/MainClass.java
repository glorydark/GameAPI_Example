package glorydark.pvp;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.Config;
import gameapi.GameAPI;
import gameapi.arena.WorldTools;
import gameapi.listener.base.GameListenerRegistry;
import gameapi.room.Room;
import gameapi.room.RoomRule;
import gameapi.room.RoomStatus;
import gameapi.utils.PlayerTools;

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
        // 注册游戏房间监听器（只有gameapi内的事件才能用）
        GameListenerRegistry.registerEvents("PvpGame", new PvpGameListener(), this);

        // 读取地图配置
        mapConfigs = new Config(path+"/maps.yml", Config.YAML).getAll();

        // 注册指令
        this.getServer().getCommandMap().register("", new PvpCommands("pvp"));

        // 1v1匹配机制（简单版）
        Server.getInstance().getScheduler().scheduleRepeatingTask(this, new NukkitRunnable() {

            final Random random = new Random(System.currentTimeMillis()); // 随机数
            boolean isEnabled = true; // 是否继续匹配，如果加载地图出错则设置为false，即停止匹配

            @Override
            public void run() {
                while (isEnabled && queuePlayers.size() >= 2){
                    int randomId = random.nextInt(mapConfigs.keySet().size()); // [0, keySize) 获取maps.yml中随机一个地图的id
                    String mapName = new ArrayList<>(mapConfigs.keySet()).get(randomId); // 从地图处获得地图名
                    Room room = loadRoom(mapName); // 加载地图
                    if(room != null){
                        // 设定匹配成功玩家
                        Player p1 = queuePlayers.get(0);
                        Player p2 = queuePlayers.get(1);
                        room.addPlayer(p1);
                        room.addPlayer(p2);
                        PlayerTools.sendMessage(room.getPlayers(), "Preparing the match, map:"+mapName);
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
    public Room loadRoom(String map) {
        // 设置房间规则
        RoomRule roomRule = new RoomRule(0);
        roomRule.setAllowDamagePlayer(true);
        roomRule.setNoTimeLimit(true);
        // 创建房间变量，playLevel设为null是为了方便下面进行设置
        Room room = new Room("PvpGame", roomRule, null, "", 1);
        // 设置房间为临时房间，这样房间就不会自动加载地图备份了
        room.setTemporary(true);
        Map<String, Object> config = (Map<String, Object>) mapConfigs.getOrDefault(map, new HashMap<>());
        if (config.containsKey("LoadWorld")) {
            String backup = (String) config.get("LoadWorld");
            // 设置房间备份地图，此地图应该放在GameAPI/worlds下
            // room.setRoomLevelBackup(backup);
            // 此处无需设置，因为这个房间是临时房间
            String newName = room.getGameName() + "_" + backup + "_" + UUID.randomUUID();
            // 设置房间名
            room.setRoomName(newName);
            // 从备份处加载地图，此地图应该放在GameAPI/worlds下
            if (WorldTools.loadLevelFromBackUp(newName, backup)) {
                // 设置游戏世界（必须设置）
                room.setPlayLevel(Server.getInstance().getLevelByName(newName));
                this.getLogger().info("Room 【" + backup + "】 loaded！");

                // 下面是加载出生点配置，这些配置如果出错则会报错，返回null值。
                if(config.containsKey("WaitSpawn")) {
                    room.setWaitSpawn(((String) config.get("WaitSpawn")).replace(backup, newName));
                } else {
                    this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in WaitSpawn Configuration!");
                    return null;
                }

                if(config.containsKey("StartSpawn")) {
                    room.addStartSpawn(((String) config.get("StartSpawn")).replace(backup, newName));
                } else {
                    this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in StartSpawn Configuration!");
                    return null;
                }

                // 下面是加载等待时间、游戏最大时间，这些配置如果出错则会报错，返回null值。
                if(config.containsKey("WaitTime")) {
                    room.setWaitTime((int) config.get("WaitTime"));
                } else {
                    this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in WaitTime Configuration!");
                    return null;
                }

                if(config.containsKey("GameTime")) {
                    room.setGameTime((int) config.get("GameTime"));
                } else {
                    this.getLogger().error("Failed to load the room "+map+", caused by the wrong format in GameTime Configuration!");
                    return null;
                }

                // 设置房间最大最小人数
                room.setMinPlayer(2);
                room.setMaxPlayer(2);

                // 设置默认返回地点为服务器默认出生点
                room.setEndSpawn(Server.getInstance().getDefaultLevel().getSpawnLocation().getLocation());

                // 设置胜利、失败指令，自带%player%变量
                room.setWinConsoleCommands((List<String>) config.getOrDefault("WinCommands", new ArrayList<>()));
                room.setLoseConsoleCommands((List<String>) config.getOrDefault("FailedCommands", new ArrayList<>()));

                // 加载房间，到此处房间加载正式完成！
                GameAPI.loadRoom(room, RoomStatus.ROOM_STATUS_WAIT);
                this.getLogger().info("Room "+map+" loaded successfully!");
                return room;
            } else {
                this.getLogger().error("Failed to copy the map!");
            }
        } else {
            this.getLogger().error("Failed to load the room, caused by: MapNotFoundException");
        }
        return null;
    }
}