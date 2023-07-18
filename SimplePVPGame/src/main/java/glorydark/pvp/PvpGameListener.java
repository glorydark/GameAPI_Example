package glorydark.pvp;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import gameapi.event.player.RoomPlayerDeathEvent;
import gameapi.event.player.RoomPlayerLeaveEvent;
import gameapi.event.room.RoomGameStartEvent;
import gameapi.listener.base.annotations.GameEventHandler;
import gameapi.listener.base.interfaces.GameListener;
import gameapi.room.Room;
import gameapi.room.RoomStatus;
import gameapi.utils.PlayerTools;

public class PvpGameListener implements GameListener {

    @GameEventHandler
    public void RoomGameStartEvent(RoomGameStartEvent event){
        for (Player player : event.getRoom().getPlayers()) {
            Item sword = Item.get(ItemID.IRON_SWORD);
            Item wool = Block.get(BlockID.WOOL, 2).toItem();
            wool.setCount(64);
            player.getInventory().addItem(sword, wool);
        }
    }

    @GameEventHandler
    public void RoomPlayerDeathEvent(RoomPlayerDeathEvent event){
        Room room = event.getRoom();
        Player dead = event.getPlayer();
        if(event.getLastDamageSource() == null){
            room.sendMessageToAll(event.getPlayer().getName()+" tumble to death.");
            PlayerTools.sendMessage(room.getSpectators(), event.getPlayer().getName()+" tumble to death.");
            Player win = null;
            for(Player player: event.getRoom().getPlayers()){
                if(player != dead){
                    win = player;
                }
            }
            if(win != null) {
                room.sendMessageToAll(win + " won the trophy!");
            }
        }else{
            room.sendMessageToAll(event.getPlayer().getName()+" was killed by "+event.getLastDamageSource().getName());
            room.sendMessageToAll(event.getLastDamageSource().getName() + " is the last player standing!");
        }
        room.setRoomStatus(RoomStatus.ROOM_STATUS_GameEnd);
    }

    @GameEventHandler
    public void RoomPlayerLeaveEvent(RoomPlayerLeaveEvent event){
        Room room = event.getRoom();
        if(room.getPlayers().size() > 0 && room.getRoomStatus() == RoomStatus.ROOM_STATUS_GameStart){
            Player winner = room.getPlayers().get(0);
            winner.sendMessage("The opponent had left just now. You win!");
            room.setRoomStatus(RoomStatus.ROOM_STATUS_GameEnd);
        }
    }

}
