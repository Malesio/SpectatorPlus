package com.pgcraft.spectatorplus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.pgcraft.spectatorplus.arenas.Arena;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused","deprecation"})
public class SpectateCommand implements CommandExecutor {

	private SpectatorPlus p = null;
	private HashMap<String, String> commands = new HashMap<String, String>();

//ender.sendMessage(ChatColor.RED + "/spec <on/off> [player]" + ChatColor.GOLD + ": Enables/disables spectator mode [for a certain player]");
//
//	sender.sendMessage(ChatColor.RED + "/spec arena <" + playerOnly + "add <name>/lobby <name>" + ChatColor.RED + "/remove <name>/reset/list>" + ChatColor.GOLD + ": Manages arenas");
//	sender.sendMessage(ChatColor.RED + playerOnly + "/spec lobby <set/del>" + ChatColor.GOLD + playerOnly + ": Adds/deletes the spectator lobby");		
//	sender.sendMessage(ChatColor.RED + "/spec mode <world/any/arena>" + ChatColor.GOLD + ": Sets who players can teleport to");
//
//	sender.sendMessage(ChatColor.RED + playerOnly + "/spec player <player>" + ChatColor.GOLD + playerOnly + ": Teleports the sender (spectator only) to <player>");
//
//	sender.sendMessage(ChatColor.RED + "/spec say <message>" + ChatColor.GOLD + ": Sends a message to spectator chat");
//
//	sender.sendMessage(ChatColor.RED + "/spec config" + ChatColor.GOLD + ": Edit configuration from ingame");
//	sender.sendMessage(ChatColor.RED + "/spec reload" + ChatColor.GOLD + ": Reloads configuration");
//	
//	sender.sendMessage(ChatColor.RED + "/spec hide [player]" + ChatColor.GOLD + ": Toggles whether you are shown in the spectator GUI");
	public SpectateCommand(SpectatorPlus p) {
		this.p = p;

		commands.put("on","&7/&coff &7[&dtarget&7]#Enable or disable spectate mode [for target]");
		commands.put("off", "@on");
		commands.put("arena", "#Manage, set up and remove arenas");
		commands.put("lobby", " &7<&5set&7/&5del&7>#Set or delete the spectator lobby point");
		commands.put("player", " &7<&5target&7>#Teleports you to the specified target");
		commands.put("p", "@player");
		commands.put("reload", "#Reloads this plugin's configuration");
		commands.put("mode", " &7<&5any&7/&5arena&7/&5world&7>#Change who spectators can teleport to");
		commands.put("say", " &7<&5message&7>#Broadcasts a message to spectator chat");
		commands.put("config", "#Edit plugin configuration from ingame");
		commands.put("hide", " &7[&dplayer&7]#Hide [player] from the spectator GUI");
		commands.put("b", "#Go back to normal spectate mode from no-clip mode"); // Temporary workaround
	}


	/**
	 * Handles a command.
	 * 
	 * @param sender The sender
	 * @param command The executed command
	 * @param label The alias used for this command
	 * @param args The arguments given to the command
	 * 
	 * @author Amaury Carrade
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("spectate") && !command.getName().equalsIgnoreCase("spec")) {
			return false;
		}

		if(args.length == 0 || args[0].equalsIgnoreCase("help")) {
			help(sender);
			return true;
		}

		String subcommandName = args[0].toLowerCase();

		// First: subcommand existence.
		if(!this.commands.containsKey(subcommandName)) {
			sender.sendMessage(SpectatorPlus.prefix+ChatColor.DARK_RED+"Invalid command. Use "+ChatColor.RED+"/spec"+ChatColor.DARK_RED+" for a list of commands.");
			return true;
		}

		// Second: is the sender allowed?
		if(!isAllowed(sender, args)) {
			unauthorized(sender, command, args);
			return true;
		}

		// Third: instantiation
		try {
			Class<? extends SpectateCommand> cl = this.getClass();
			Class[] parametersTypes = new Class[]{CommandSender.class, Command.class, String.class, String[].class};

			Method doMethod = cl.getDeclaredMethod("do" + WordUtils.capitalize(subcommandName), parametersTypes);

			doMethod.invoke(this, new Object[]{sender, command, label, args});

			return true;

		} catch (NoSuchMethodException e) {
			// Unknown method => unknown subcommand.
			sender.sendMessage(SpectatorPlus.prefix+ChatColor.DARK_RED+"Invalid command. Use "+ChatColor.RED+"/spec"+ChatColor.DARK_RED+" for a list of commands.");
			return true;

		} catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			sender.sendMessage(SpectatorPlus.prefix + ChatColor.DARK_RED + "An error occured, see console for details. This is probably a bug, please report it!");
			e.printStackTrace();
			return true; // An error message has been printed, so command was technically handled.
		}
	}

	/**
	 * Gets the message for a command.<br>
	 * '#' splits the usage from the description.<br>
	 * '&' can be used to parse colour codes.
	 * @param cmd a command. Get it from the commands hashmap.
	 * @return The formatted result message ready for printing to the player.
	 */
	private String parseHelpMsg(String cmd) {
		String msg=commands.get(cmd);
		msg=ChatColor.translateAlternateColorCodes('&', msg);
		String[] msgArr=msg.split("#", 2);
		if(msgArr.length<2) {
			Bukkit.getLogger().log(Level.WARNING, "Problem showing help for the command "+cmd+"! Please report this!");
		}
		msg=ChatColor.DARK_AQUA+"/spec "+ChatColor.RED+cmd+msgArr[0]+ChatColor.GOLD+": "+msgArr[1];
		return msg;
	}
	/**
	 * An '@' followed by a word means the command is an alias of that 'word'.
	 * @param msg a command's help message. Get it from the commands hashmap.
	 * @return boolean indicating if the input msg is an alias command or not.
	 */
	private boolean isHelpMsgAlias(String cmd) {
		String msg=commands.get(cmd);
		return (msg.charAt(0) == '@');
	}
	
	/**
	 * Prints the plugin main help page.
	 * 
	 * @param sender The help will be displayer for this sender.
	 */
	private void help(CommandSender sender) {

		String playerOnly = "";
		if(!(sender instanceof Player)) {
			playerOnly = ChatColor.STRIKETHROUGH.toString();
		}
		
		ArrayList<String> allowedCmds = new ArrayList<String>();
		for(String cmd : commands.keySet()) {
			if(isAllowed(sender, cmd.split(" "))) {
				allowedCmds.add(cmd); // Add all allowed cmds to an ArrayList
			}
		}
		
		sender.sendMessage(""
				+ ChatColor.DARK_RED+ChatColor.BOLD+"Spectator"+ChatColor.RED+ChatColor.BOLD+"Plus"
				+ ChatColor.GOLD+ChatColor.BOLD+" Help");
		
		for (String cmd : allowedCmds) {
			if (!isHelpMsgAlias(cmd)) sender.sendMessage(parseHelpMsg(cmd));
		}
		
		if(allowedCmds.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "Aww, there are no commands you can use :(");
		}
//		sender.sendMessage(ChatColor.RED + "/spec <on/off> [player]" + ChatColor.GOLD + ": Enables/disables spectator mode [for a certain player]");
//
//		sender.sendMessage(ChatColor.RED + "/spec arena <" + playerOnly + "add <name>/lobby <name>" + ChatColor.RED + "/remove <name>/reset/list>" + ChatColor.GOLD + ": Manages arenas");
//		sender.sendMessage(ChatColor.RED + playerOnly + "/spec lobby <set/del>" + ChatColor.GOLD + playerOnly + ": Adds/deletes the spectator lobby");		
//		sender.sendMessage(ChatColor.RED + "/spec mode <world/any/arena>" + ChatColor.GOLD + ": Sets who players can teleport to");
//
//		sender.sendMessage(ChatColor.RED + playerOnly + "/spec player <player>" + ChatColor.GOLD + playerOnly + ": Teleports the sender (spectator only) to <player>");
//
//		sender.sendMessage(ChatColor.RED + "/spec say <message>" + ChatColor.GOLD + ": Sends a message to spectator chat");
//
//		sender.sendMessage(ChatColor.RED + "/spec config" + ChatColor.GOLD + ": Edit configuration from ingame");
//		sender.sendMessage(ChatColor.RED + "/spec reload" + ChatColor.GOLD + ": Reloads configuration");
//		
//		sender.sendMessage(ChatColor.RED + "/spec hide [player]" + ChatColor.GOLD + ": Toggles whether you are shown in the spectator GUI");

//		if (!(sender instanceof Player)) {
//			sender.sendMessage(ChatColor.DARK_AQUA + "Strikethrough commands can only be executed as a player.");
//		}
	}

	/**
	 * This method checks if an user is allowed to send a command.
	 * 
	 * @param sender
	 * @param subcommand
	 * @param args
	 * 
	 * @return boolean The allowance status.
	 */
	private boolean isAllowed(CommandSender sender, String[] args) {

		// The console is always allowed
		if(!(sender instanceof Player)) {
			return true;
		}

		else {

			if(sender.isOp()) {
				return true;
			}

			if(args.length == 0) { // Help
				return true;
			}

			// Centralized way to manage permissions
			String permission = null;

			switch(args[0]) {
			case "on":
			case "off":
				permission = (args.length >= 2) ? "spectate.use.others" : "spectate.use."+args[0];
				break;

			case "arena":
			case "lobby":			
			case "reload":
			case "config":
			case "mode":
			case "say":
				permission = "spectate.admin." + args[0];
				break;
				
			case "hide":
				permission = (args.length >= 2) ? "spectate.admin.hide.others" : "spectate.admin.hide.self";
				break;
				
			case "player":
			case "p":
			case "b":
				permission = "spectate.use";
				break;
				
			default:
				permission = "spectate"; // Should never happens. But, just in case...
				break;
			}

			return ((Player) sender).hasPermission(permission);
		}
	}

	/**
	 * This method sends a message to a player who try to use a command without the permission.
	 * 
	 * @param sender
	 * @param command
	 * @param args
	 */
	private void unauthorized(CommandSender sender, Command command, String[] args) {
		if(args.length == 0) {
			return; // will never happens, but just in case of a mistake...
		}

		String message = null;
		String word = null;
		switch(args[0]) {
		case "on":
			word="enable";
		case "off":
			if(word==null)word="disable";
			if(args.length >= 2) {
				message = "You can't change the spectate mode of others!";
			}
			else {
				message = "You can't "+word+" your spectate mode!";
			}
			break;

		case "arena":
			message = "You can't manage arenas!";
			break;

		case "lobby":
			message = "You can't manage the global lobby.";
			break;

		case "reload":
			message = "You can't reload the configuration.";
			break;

		case "config":
			message = "You can't edit the configuration.";
			break;

		case "mode":
			message = "You can't change the plugin mode.";
			break;
			
		case "say":
			message = "You can't broadcast a message to the spectators' chat.";
			break;
		
		case "hide":
			message = "You can't toggle hide mode!";
			break;
		}

		sender.sendMessage(SpectatorPlus.prefix + ChatColor.DARK_RED + message);
	}


	/**
	 * This command enables the spectator mode on someone.<br>
	 * Usage: /spec on [player]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doOn(CommandSender sender, Command command, String label, String[] args) {

		if (args.length == 1) { // /spec on
			if(sender instanceof Player) {
				p.enableSpectate((Player) sender, sender);
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec on <player>");
			}
		}

		else { // /spec on <player>
			Player player = p.getServer().getPlayer(args[1]);
			if (player != null) {
				p.enableSpectate(player, sender);
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online!");
			}
		}
	}

	/**
	 * This command disables the spectator mode on someone.<br>
	 * Usage: /spec off [player]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doOff(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) { // /spec off
			if(sender instanceof Player) {
				p.disableSpectate((Player) sender, sender);
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec off <player>");
			}
		}

		else { // /spec off <player>
			Player player = p.getServer().getPlayer(args[1]);
			if (player != null) {
				p.disableSpectate(player, sender);
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online!");
			}
		}
	}

	/**
	 * Reloads the config from the files.<br>
	 * Usage: /spec reload
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doReload(CommandSender sender, Command command, String label, String[] args) {
		p.reloadConfig(true);

		sender.sendMessage(SpectatorPlus.prefix + "Config reloaded!");
	}

	/**
	 * Edits the config from ingame.<br>
	 * Usage: /spec config &lt;toggle> &lt;value> [temp=false]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doConfig(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 2) { // /spec config <key>
			Toggle toggle = Toggle.fromPath(args[1]);
			if(toggle != null) {
				sender.sendMessage(SpectatorPlus.prefix + "About the toggle " + ChatColor.RED + args[1]);
				sender.sendMessage(ChatColor.AQUA + toggle.getDescription());
				sender.sendMessage(ChatColor.GOLD + "Value: " + ChatColor.RED + p.toggles.get(toggle).toString());
				sender.sendMessage(ChatColor.GOLD + "Default value: " + toggle.getDefaultValue().toString());
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix+ChatColor.DARK_RED+"Toggle "+ChatColor.RED+args[1]+ChatColor.DARK_RED+" doesn't exist!");
			}
		} else if(args.length >= 3) { // /spec config <key> <value> [temp]
			Toggle entry = Toggle.fromPath(args[1]);
			boolean temp = (args.length > 3 && args[3] != null && args[3].equalsIgnoreCase("temp")) ? true : false;
			String displayValue;
			String displayTemp = (temp) ? " until next reload" : "";
			
			if (entry != null) {
				if(entry.getDataType() == Boolean.class) {
					Boolean value = p.parseBoolean(args[2]);
					p.toggles.set(entry, value, temp);
					
					if(value != null) {
						displayValue = value.toString();
					}
					else {
						displayValue = entry.getDefaultValue().toString();
					}
				}
				
				else if(entry.getDataType() == Material.class) {
					Material value = Material.matchMaterial(args[2]);
					p.toggles.set(entry, value, temp);
					
					if(value != null) {
						displayValue = value.toString();
					}
					else {
						displayValue = entry.getDefaultValue().toString();
					}
				}
				
				else {
					sender.sendMessage(SpectatorPlus.prefix + ChatColor.DARK_RED + "You cannot edit the value of "+ChatColor.RED + entry.getPath() + ChatColor.DARK_RED+" (type " + ChatColor.RED + entry.getDataType().getSimpleName() + ChatColor.DARK_RED + ") from the game currently.");
					return;
				}
				
				sender.sendMessage(SpectatorPlus.prefix+"Set "+ChatColor.RED+entry.getPath()+ChatColor.GOLD+" to "+ChatColor.RED+displayValue+ChatColor.GOLD+displayTemp);
			
			} else {
				sender.sendMessage(SpectatorPlus.prefix+ChatColor.DARK_RED+"Toggle "+ChatColor.RED+args[1]+ChatColor.DARK_RED+" doesn't exist!");
			}
		} else {
			sender.sendMessage(SpectatorPlus.prefix+"Usage: "+ChatColor.RED+"/spec config <toggle> [value] [temp]");
		}
	}

	/**
	 * Teleports a spectator to a player, just like picking a head in the teleportation GUI.<br>
	 * Usage: /spec player &lt;playerName>
	 * <p>
	 * <i>(TODO: add argument to allow the console to teleport spectators to players.)</i>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doPlayer(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) { // A player...
			if (p.getPlayerData((Player) sender).isSpectating()) { // ...who is spectating...
				if (args.length > 1) { // ... and specified a name...
					Player target = p.getServer().getPlayer(args[1]);

					if (target != null && !p.getPlayerData(target).isSpectating()) { // ... of an online player
						p.choosePlayer((Player) sender, p.getServer().getPlayer(args[1]));
					}
					else {
						sender.sendMessage(SpectatorPlus.prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online or is spectating!");
					}

				} else {
					sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec p <player>");
				}
			} else {
				sender.sendMessage(SpectatorPlus.prefix + "You aren't spectating!");
			}
		} else {
			sender.sendMessage(SpectatorPlus.prefix + "Cannot be executed from the console!");
		}
	}

	/**
	 * Teleports a spectator to a player, just like picking a head in the teleportation GUI.<br>
	 * Usage: /spec p &lt;playerName>
	 * <p>
	 * Alias of /spec player.
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doP(CommandSender sender, Command command, String label, String[] args) {
		doPlayer(sender, command, label, args);
	}

	/**
	 * This command can set or unset the main lobby.<br>
	 * Usage: /spec lobby &lt;set|del|delete>
	 * 
	 * This cannot be executed from the console.
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doLobby(CommandSender sender, Command command, String label, String[] args) {
		boolean isEmptyCommand = false;
		String subcommand = null;

		if(!(sender instanceof Player)) {
			sender.sendMessage(SpectatorPlus.prefix + "Cannot be executed from the console!");
			return;
		}

		if(args.length == 1) { // /spec lobby
			isEmptyCommand = true;
		} else {
			subcommand = args[1];
		}

		// /spec lobby set
		if(!isEmptyCommand && subcommand.equalsIgnoreCase("set")) {
			Location where = ((Player) sender).getLocation();

			p.setup.getConfig().set("xPos", Math.floor(where.getX())+0.5);
			p.setup.getConfig().set("yPos", Math.floor(where.getY()));
			p.setup.getConfig().set("zPos", Math.floor(where.getZ())+0.5);
			p.setup.getConfig().set("world", where.getWorld().getName());
			p.setup.getConfig().set("active", true);
			p.setup.saveConfig();

			sender.sendMessage(SpectatorPlus.prefix + "Global spectator lobby location set!");
		}

		// /spec lobby del|delete
		else if(!isEmptyCommand && (subcommand.equalsIgnoreCase("del") || subcommand.equalsIgnoreCase("delete"))) {
			p.setup.getConfig().set("xPos", 0);
			p.setup.getConfig().set("yPos", 0);
			p.setup.getConfig().set("zPos", 0);
			p.setup.getConfig().set("world", null);
			p.setup.getConfig().set("active", false);
			p.setup.saveConfig();

			sender.sendMessage(SpectatorPlus.prefix + "Global spectator lobby location removed! Using "+ChatColor.WHITE+"/spawn"+ChatColor.GOLD+" instead.");
		}

		else {
			sender.sendMessage(SpectatorPlus.prefix + "Usage: " + ChatColor.RED + "/spec lobby <set/del[ete]>");
		}
	}

	/**
	 * This command changes the current mode:<br>
	 *  - any: teleportation to any player;<br>
	 *  - arena: teleportation to the players inside the current arena. Players outside an arena are unreachable.
	 * <p>
	 * Usage: /spec mode &lt;world|any|arena>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doMode(CommandSender sender, Command command, String label, String[] args) {

		if(args.length == 1) { // /spec mode
			sender.sendMessage(SpectatorPlus.prefix + "Usage: " + ChatColor.RED + "/spec mode <world/arena/any>");
		}

		else { // /spec mode <?>
			String mode = args[1];

			try {
			p.setSpectatorMode(SpectatorMode.fromString(mode));

			sender.sendMessage(SpectatorPlus.prefix + "Mode set to " + ChatColor.RED + mode.toLowerCase());
			if(p.mode == SpectatorMode.ARENA) {
				sender.sendMessage(SpectatorPlus.prefix + "Only players in arena regions can be teleported to by spectators.");
			}
			
			} catch (IllegalArgumentException e) {
				sender.sendMessage(SpectatorPlus.prefix + "The mode can be \"world\", \"arena\" or \"any\".");
			}
		}
	}


	/**
	 * This command manages the arenas.<br>
	 * Usage: /spec arena &lt;add &lt;name> | remove &lt;name> | lobby &lt;name> | reset | list>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doArena(CommandSender sender, Command command, String label, String[] args) {
		boolean isEmptyCommand = false;
		String subcommand = null;

		if(args.length == 1) { // /spec arena
			isEmptyCommand = true;
		} else {
			subcommand = args[1];
		}

		if(!isEmptyCommand && subcommand.equalsIgnoreCase("add")) { // /spec arena add ...

			if(!(sender instanceof Player)) {
				sender.sendMessage(SpectatorPlus.prefix + "Cannot be executed from the console!");
				return;
			}

			if(args.length == 2) { // /spec arena add
				sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec arena add <name>");
			}
			else { // /spec arena add <?>
				p.getPlayerData((Player) sender).setArenaName(args[2]);
				sender.sendMessage(SpectatorPlus.prefix + "Punch point " + ChatColor.RED + "#1" + ChatColor.GOLD + " - a corner of the arena");
				p.getPlayerData((Player) sender).setSetup(1);
			}

		}

		else if(!isEmptyCommand && subcommand.equalsIgnoreCase("remove")) { // spec arena remove ...

			if(args.length == 2) { // /spec arena remove
				sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec arena remove <name>");
			}
			else { // /spec arena remove <?>
				if(p.removeArena(args[2])) {
					sender.sendMessage(SpectatorPlus.prefix + "Arena " + ChatColor.RED + args[2] + ChatColor.GOLD + " removed.");
				}
				else {
					sender.sendMessage(SpectatorPlus.prefix + "The arena " + ChatColor.RED + args[2] + ChatColor.GOLD + " does not exist!");
				}
			}

		}

		else if(!isEmptyCommand && subcommand.equalsIgnoreCase("list")) { // /spec arena list

			sender.sendMessage(ChatColor.GOLD + "          ~~ " + ChatColor.RED + "Arenas" + ChatColor.GOLD + " ~~          ");

			for(Arena arena : p.arenasManager.getArenas()) {
				// Only print enabled arenas.
				if (arena.isEnabled()) {
					String arenaDescription = ChatColor.RED + arena.getName();
					if(arena.getLobby() != null) {
						arenaDescription += ChatColor.GOLD + " - Lobby: " + arena.getLobby().getBlockX() + ";" + arena.getLobby().getBlockY() + ";" + arena.getLobby().getBlockZ();  
					}
					else {
						arenaDescription += ChatColor.GOLD + " - Lobby not configured";
					}
					sender.sendMessage(arenaDescription);
				}
			}

		}

		else if(!isEmptyCommand && subcommand.equalsIgnoreCase("lobby")) { // /spec arena lobby

			if(!(sender instanceof Player)) {
				sender.sendMessage(SpectatorPlus.prefix + "Cannot be executed from the console!");
				return;
			}

			if(args.length < 3) {
				sender.sendMessage(SpectatorPlus.prefix + "Usage: "+ChatColor.RED+"/spec arena lobby <name>");
				return;
			}
			
			Arena arena = p.arenasManager.getArena(args[2]);
			if(arena != null) {
				arena.setLobby(((Player) sender).getLocation());
				p.arenasManager.save();

				sender.sendMessage(SpectatorPlus.prefix + "Arena " + ChatColor.RED + args[2] + ChatColor.GOLD + "'s lobby location set to your location");
			}
			else {
				sender.sendMessage(SpectatorPlus.prefix + "Arena " + ChatColor.RED + args[2] + ChatColor.GOLD + " doesn't exist!");
			}

		}

		else if(!isEmptyCommand && subcommand.equalsIgnoreCase("reset")) { // /spec arena reset

			p.arenasManager.reset();

			sender.sendMessage(SpectatorPlus.prefix + "All arenas removed.");

		}

		else {
			String playerOnly = "";
			if(!(sender instanceof Player)) playerOnly = ChatColor.DARK_RED+""+ChatColor.STRIKETHROUGH;

			sender.sendMessage(SpectatorPlus.prefix + "Usage: " + ChatColor.RED + "/spec arena <" + playerOnly +"add <name>/lobby <name>" + ChatColor.RED + "/remove <name>/reset/list>");
		}
	}


	/**
	 * This command broadcasts a message to the spectators.<br>
	 * Usage: /spec say &lt;message>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doSay(CommandSender sender, Command command, String label, String[] args) {

		if(args.length == 1) {
			sender.sendMessage(SpectatorPlus.prefix + "Usage: " + ChatColor.RED + "/spec say <message>");
		}

		else {
			String message = "";
			for(int i = 1; i < args.length; i++) {
				message += args[i] + " ";
			}

			p.broadcastToSpectators(sender, message);
		}

	}
	
	/**
	 * This command hide a player from the spectators.<br>
	 * Usage: /spec hide [player]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doHide(CommandSender sender, Command command, String label, String[] args) {

		if(args.length == 0) {
			sender.sendMessage(SpectatorPlus.prefix + "Usage: " + ChatColor.RED + "/spec hide [player]");
		} else {
			// Set the target...
			Player target;
			if (args.length <= 1) {
				if(sender instanceof Player)
					target = (Player) sender;
				else {
					sender.sendMessage(SpectatorPlus.prefix + "Please specify a player: " + ChatColor.RED + "/spec hide <player>");
					return;
				}
			} else if (p.getServer().getPlayer(args[1]) != null) {
				target = p.getServer().getPlayer(args[1]);
			} else {
				sender.sendMessage(SpectatorPlus.prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online!");
				return;
			}
			
			// Toggle hide mode for them.
			p.getPlayerData(target).setHideFromTp(!p.user.get(target.getName()).isHideFromTp());
			
			// Notify the sender.
			String state = (p.getPlayerData(target).isHideFromTp()) ? ChatColor.GREEN+"enabled" : ChatColor.DARK_RED+"disabled";
			sender.sendMessage(SpectatorPlus.prefix + "Hide mode for " + ChatColor.RED + target.getName() + ChatColor.GOLD + " is now " + state);
		}

	}
	
	
	/**
	 * This is a temporary workaround to quit the no-clip mode for spectators.
	 * <p>
	 * Usage: /spec b
	 * <p>
	 * Why? Because there was a bug in Bukkit: the InventoryClickEvent was not called
	 * when the player was in the spectator gamemode. So, it was impossible to use our
	 * usual inventory-based GUIs.
	 * <p>
	 * The bug is now fixed, but this is kept here for the servers not updated.
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doB(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player && p.getPlayerData((Player) sender).isSpectating() && ((Player) sender).getGameMode() == GameMode.SPECTATOR) {
			if(!p.vanillaSpectate) {
				((Player) sender).setGameMode(GameMode.ADVENTURE);

				((Player) sender).setAllowFlight(true);
				((Player) sender).setFlying(true);

				p.updateSpectatorInventory((Player) sender);
			} else {
				sender.sendMessage(SpectatorPlus.prefix+ChatColor.DARK_RED+"Exiting no-clip mode is disabled.");
			}
		}
	}
	
	/**
	 * Returns a list of the commands.
	 * @return
	 */
	protected HashMap<String, String> getCommands() {
		return commands;
	}
}
