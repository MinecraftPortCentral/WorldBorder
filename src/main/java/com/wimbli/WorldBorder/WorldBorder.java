package com.wimbli.WorldBorder;

import com.google.common.collect.Iterables;
import com.wimbli.WorldBorder.cmd.CmdBypass;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Inject;

@Plugin(id = "worldborder", name = "WorldBorder", version = "1.0.0")
public class WorldBorder

{
	public static volatile WorldBorder plugin = null;
    public static PluginContainer container;
	public static volatile WBCommand wbCommand = null;
	private BlockPlaceListener blockPlaceListener = null;
	private MobSpawnListener mobSpawnListener = null;

    @Inject
    public Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    public ConfigurationNode rootNode;

	@Listener
	public void onInit(GameInitializationEvent event) throws IOException, ObjectMappingException {
		plugin = this;
        container = Sponge.getPluginManager().fromInstance(this).get();
		if (wbCommand == null)
			wbCommand = new WBCommand();

        this.rootNode = this.configManager.load();

		// Load (or create new) config file
		Config.load(this, false);


        this.registerCommands();


		// our one real command, though it does also have aliases "wb" and "worldborder"
		//getCommand("wborder").setExecutor(wbCommand);

		// keep an eye on teleports, to redirect them to a spot inside the border if necessary
		Sponge.getEventManager().registerListeners(this, new WBListener());

		if (Config.preventBlockPlace())
			enableBlockPlaceListener(true);

		if (Config.preventMobSpawn())
			enableMobSpawnListener(true);

		// integrate with DynMap if it's available
		DynMapFeatures.setup();

		// Well I for one find this info useful, so...
	}

    private void registerCommands() {

        CommandSpec bypass = CommandSpec.builder()
                .permission("worldborder.bypass")
                .extendedDescription(CmdBypass.helpText)
                .arguments(GenericArguments.playerOrSource(Text.of("source")), GenericArguments.optional(GenericArguments.bool(Text.of("bypass"))))
                .executor(new CmdBypass())
                .build();


        CommandSpec base = CommandSpec.builder()
                .child(bypass)
                .build();

        Sponge.getCommandManager().register(this, base, "worldborder", "wb");


    }

    @SuppressWarnings("ConstantConditions")
    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        Location<World> spawn = Iterables.getFirst(Sponge.getServer().getWorlds(), null).getSpawnLocation();
        Config.log("For reference, the main world's spawn location is at X: " + Config.coord.format(spawn.getX()) + " Y: " + Config.coord.format(spawn.getY()) + " Z: " + Config.coord.format(spawn.getZ()));
    }

	@Listener
	public void onStopping(GameStoppingServerEvent event) {
		DynMapFeatures.removeAllBorders();
		Config.StopBorderTimer();
		Config.StoreFillTask();
		Config.StopFillTask();
	}

	// for other plugins to hook into
	public BorderData getWorldBorder(String worldName)
	{
		return Config.Border(worldName);
	}

	/**
	 * @deprecated  Replaced by {@link #getWorldBorder(String worldName)};
	 * this method name starts with an uppercase letter, which it shouldn't
	 */
	public BorderData GetWorldBorder(String worldName)
	{
		return getWorldBorder(worldName);
	}

	public void enableBlockPlaceListener(boolean enable)
	{
		if (enable)
			Sponge.getEventManager().registerListeners(this, (this.blockPlaceListener = new BlockPlaceListener()));
		else if (blockPlaceListener != null)
			Sponge.getEventManager().unregisterListeners(blockPlaceListener);
	}

	public void enableMobSpawnListener(boolean enable)
	{
		if (enable)
			Sponge.getEventManager().registerListeners(this, (this.mobSpawnListener = new MobSpawnListener()));
		else if (mobSpawnListener != null)
			mobSpawnListener.unregister();
	}

    public void saveConfig() {
        try {
            this.configManager.save(this.rootNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadConfig() {
        try {
            this.rootNode = this.configManager.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
