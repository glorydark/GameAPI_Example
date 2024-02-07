package glorydark.pvp;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import gameapi.manager.RoomManager;
import gameapi.room.Room;

public class PvpCommands extends Command {

    public PvpCommands(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (strings.length > 0) {
            switch (strings[0]) {
                case "matching":
                    if (commandSender.isPlayer() && MainClass.queuePlayers.remove((Player) commandSender)) {
                        commandSender.sendMessage("You left the queue of matching!");
                    } else {
                        commandSender.sendMessage("Finding an available match for you...");
                        MainClass.queuePlayers.add((Player) commandSender);
                    }
                    break;
                case "watch":
                    if (commandSender.isPlayer()) {
                        Room room = RoomManager.getRoom("PvpSimple", strings[1]);
                        if (room != null) {
                            if (room.getSpectators().contains((Player) commandSender)) {
                                room.removeSpectator((Player) commandSender);
                                commandSender.sendMessage("Quit spectating!");
                            } else {
                                room.processJoinSpectator((Player) commandSender);
                                commandSender.sendMessage("You're a spectator now!");
                            }
                        } else {
                            commandSender.sendMessage("Unable to find an available match for you！");
                        }
                    }
                    break;
                case "help":
                    commandSender.sendMessage(""); // Write your tips
                    break;
            }
        }
        return true;
    }
}
