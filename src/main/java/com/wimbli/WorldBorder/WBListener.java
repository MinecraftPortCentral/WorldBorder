package com.wimbli.WorldBorder;


import org.bukkit.event.world.ChunkLoadEvent;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class WBListener
{
	@Listener(order = Order.PRE)
	@IsCancelled(Tristate.UNDEFINED)
	public void onPlayerTeleport(DisplaceEntityEvent.Teleport.TargetPlayer event, @First TeleportCause teleportCause) {
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		if (Config.Debug())
			Config.log("Teleport cause: " + teleportCause.getTeleportType().toString());

		Transform<World> newTransform = BorderCheckTask.checkPlayer(event.getTargetEntity(), event.getToTransform(), true, true);
		if (newTransform != null)
		{
			if(teleportCause.getTeleportType().equals(TeleportTypes.ENDER_PEARL) && Config.getDenyEnderpearl()) {
				event.setCancelled(true);
				return;
			}

			event.setToTransform(newTransform);
		}
	}

	/*@Listener(order = Order.PRE)
	@IsCancelled(Tristate.UNDEFINED)
	public void onPlayerPortal(Porta event)
	{
		// if knockback is set to 0, or portal redirection is disabled, simply return
		if (Config.KnockBack() == 0.0 || !Config.portalRedirection())
			return;

		Location newLoc = BorderCheckTask.checkPlayer(event.getPlayer(), event.getTo(), true, false);
		if (newLoc != null)
			event.setTo(newLoc);
	}*/

	@Listener(order = Order.POST)
	public void onChunkLoad(LoadChunkEvent event)
	{
/*		// tested, found to spam pretty rapidly as client repeatedly requests the same chunks since they're not being sent
		// definitely too spammy at only 16 blocks outside border
		// potentially useful at standard 208 block padding as it was triggering only occasionally while trying to get out all along edge of round border, though sometimes up to 3 triggers within a second corresponding to 3 adjacent chunks
		// would of course need to be further worked on to have it only affect chunks outside a border, along with an option somewhere to disable it or even set specified distance outside border for it to take effect; maybe  send client chunk composed entirely of air to shut it up

		// method to prevent new chunks from being generated, core method courtesy of code from NoNewChunk plugin (http://dev.bukkit.org/bukkit-plugins/nonewchunk/)
		if(event.isNewChunk())
		{
			Chunk chunk = event.getChunk();
			chunk.unload(false, false);
			Config.logWarn("New chunk generation has been prevented at X " + chunk.getX() + ", Z " + chunk.getZ());
		}
*/
		// make sure our border monitoring task is still running like it should
		if (Config.isBorderTimerRunning()) return;

		Config.logWarn("Border-checking task was not running! Something on your server apparently killed it. It will now be restarted.");
		Config.StartBorderTimer();
	}
}
