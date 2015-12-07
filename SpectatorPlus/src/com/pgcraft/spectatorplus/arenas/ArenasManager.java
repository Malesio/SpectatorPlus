package com.pgcraft.spectatorplus.arenas;

import com.pgcraft.spectatorplus.ConfigAccessor;
import com.pgcraft.spectatorplus.SpectatorPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * This class manages the arenas of SpectatorPlusOld. <p> For an {@link Arena} to be recognized by
 * SpectatorPlus, the object needs to be registered using {@link #registerArena(Arena)}.
 *
 * The Arena class is registered as a serializable one in the onLoad method of the main class.
 *
 * @since 2.0
 */
public class ArenasManager
{
	private SpectatorPlus p;

	private Map<UUID, Arena> arenas = new HashMap<>();
	private ConfigAccessor storageConfig;

	private static final String STORAGE_KEY = "arenas";


	public ArenasManager(SpectatorPlus plugin)
	{
		p = plugin;
		storageConfig = new ConfigAccessor(plugin, "setup");

		// Loads the arenas from the config
		reload();

		// Migrates the old arenas to the new storage
		migrate();
	}


	/**
	 * (Re)loads the arenas from the configuration file.
	 *
	 * @since 2.0
	 */
	public void reload()
	{
		if (storageConfig.getConfig().isConfigurationSection(STORAGE_KEY))
		{
			ConfigurationSection configArenas = storageConfig.getConfig().getConfigurationSection(STORAGE_KEY);

			for (String key : configArenas.getKeys(false))
			{
				arenas.put(UUID.fromString(key), (Arena) configArenas.get(key));
			}
		}
	}

	/**
	 * Saves all registered arenas in the configuration file.
	 *
	 * @since 2.0
	 */
	public void save()
	{
		// The configuration is rewrote every time, to take deletions into account.
		if (storageConfig.getConfig().isConfigurationSection(STORAGE_KEY))
		{
			storageConfig.getConfig().set(STORAGE_KEY, null);
		}

		ConfigurationSection configArenas = storageConfig.getConfig().createSection(STORAGE_KEY);

		for (UUID id : arenas.keySet())
		{
			configArenas.set(id.toString(), arenas.get(id));
		}

		storageConfig.saveConfig();
	}

	/**
	 * Removes all the registered arenas.
	 *
	 * WARNING - CANNOT BE CANCELLED.
	 *
	 * @since 2.0
	 */
	public void reset()
	{
		for (Arena arena : getArenas())
		{
			arena.setRegistered(false);
		}

		arenas = new HashMap<UUID, Arena>();

		save();
	}

	/**
	 * Returns an Arena from his UUID.
	 *
	 * @param id The UUID of the Arena.
	 *
	 * @return The Arena, or null if there isn't any Arena with this UUID.
	 * @since 2.0
	 */
	public Arena getArena(UUID id)
	{
		return arenas.get(id);
	}

	/**
	 * Returns an Arena from his name.
	 *
	 * Case sensitive.
	 *
	 * @param name The name of the Arena.
	 *
	 * @return The Arena, or null if there isn't any Arena with this name.
	 * @since 2.0
	 */
	public Arena getArena(String name)
	{
		for (Arena arena : arenas.values())
		{
			if (arena.getName().equals(name))
			{
				return arena;
			}
		}

		return null;
	}

	/**
	 * Returns a collection of the registered arenas.
	 *
	 * @return the collection.
	 * @since 2.0
	 */
	public Collection<Arena> getArenas()
	{
		return arenas.values();
	}

	/**
	 * Registers an arena.
	 *
	 * @param arena The arena to register.
	 *
	 * @throws IllegalArgumentException if an arena with the same UUID is already registered.
	 * @throws IllegalArgumentException if an arena with the same name is already registered.
	 * @since 2.0
	 */
	public void registerArena(Arena arena)
	{
		if (arenas.containsKey(arena.getUUID()))
		{
			throw new IllegalArgumentException("An arena with the UUID " + arena.getUUID().toString() + " already exists!");
		}

		if (this.getArena(arena.getName()) != null)
		{
			throw new IllegalArgumentException("An arena with the name " + arena.getName() + " already exists!");
		}

		arenas.put(arena.getUUID(), arena);
		arena.setRegistered(true);

		save();
	}

	/**
	 * Unregisters an arena.
	 *
	 * @param arena The arena to unregister.
	 *
	 * @since 2.0
	 */
	public void unregisterArena(Arena arena)
	{
		arenas.remove(arena.getUUID());
		arena.setRegistered(false);

		save();
	}


	/**
	 * Migrates the arenas stored in the old format to this new one.
	 *
	 * @since 2.0
	 */
	private void migrate()
	{
		final String OLD_STORAGE_KEY = "arena";
		final String NEXT_ARENA_KEY = "nextarena";

		if (storageConfig.getConfig().isConfigurationSection(OLD_STORAGE_KEY) && storageConfig.getConfig().contains(NEXT_ARENA_KEY))
		{

			int lastNumericID = storageConfig.getConfig().getInt(NEXT_ARENA_KEY);

			for (int i = 1; i < lastNumericID; i++)
			{

				String name = storageConfig.getConfig().getString(OLD_STORAGE_KEY + "." + i + ".name");

				World defaultWorld = Bukkit.getWorlds().get(0);
				Location corner1 = new Location(defaultWorld,
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".1.x"),
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".1.y"),
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".1.z"));
				Location corner2 = new Location(defaultWorld,
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".2.x"),
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".2.y"),
						storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".2.z"));

				Arena importedArena = new Arena(name, corner1, corner2);

				// Is a lobby registered?
				if (storageConfig.getConfig().isConfigurationSection(OLD_STORAGE_KEY + "." + i + ".lobby"))
				{
					Location lobby = new Location(Bukkit.getWorld(storageConfig.getConfig().getString(OLD_STORAGE_KEY + "." + i + ".lobby.world")),
							storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".lobby.x"),
							storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".lobby.y"),
							storageConfig.getConfig().getDouble(OLD_STORAGE_KEY + "." + i + ".lobby.z"));
					importedArena.setLobby(lobby);
				}

				this.registerArena(importedArena);
			}

			// The old configuration is removed
			storageConfig.getConfig().set(OLD_STORAGE_KEY, null);
			storageConfig.getConfig().set(NEXT_ARENA_KEY, null);

			// The config file is wrote
			this.save();
		}
	}
}
