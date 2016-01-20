package com.wimbli.WorldBorder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class BorderCheckTask implements Runnable
{
	@Override
	public void run()
	{
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		Collection<Player> players = ImmutableList.copyOf(Sponge.getServer().getOnlinePlayers());

		for (Player player : players)
		{
			checkPlayer(player, null, false, true);
		}
	}

	// track players who are being handled (moved back inside the border) already; needed since Bukkit is sometimes sending teleport events with the old (now incorrect) location still indicated, which can lead to a loop when we then teleport them thinking they're outside the border, triggering event again, etc.
	private static Set<String> handlingPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());

	// set targetLoc only if not current player location; set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
	public static Transform<World> checkPlayer(Player player, Transform<World> targetLoc, boolean returnLocationOnly, boolean notify)
	{
		if (player == null) return null;

		Transform<World> loc = (targetLoc == null) ? player.getTransform() : targetLoc;
		if (loc == null) return null;

		World world = loc.getExtent();
		BorderData border = Config.Border(world.getName());
		if (border == null) return null;

		if (border.insideBorder(loc.getPosition().getX(), loc.getPosition().getZ(), Config.ShapeRound()))
			return null;

		// if player is in bypass list (from bypass command), allow them beyond border; also ignore players currently being handled already
		if (Config.isPlayerBypassing(player.getUniqueId()) || handlingPlayers.contains(player.getName().toLowerCase()))
			return null;

		// tag this player as being handled so we can't get stuck in a loop due to Bukkit currently sometimes repeatedly providing incorrect location through teleport event
		handlingPlayers.add(player.getName().toLowerCase());

		Transform<World> newLoc = newLocation(player, loc, border, notify);
		boolean handlingVehicle = false;

		/*
		 * since we need to forcibly eject players who are inside vehicles, that fires a teleport event (go figure) and
		 * so would effectively double trigger for us, so we need to handle it here to prevent sending two messages and
		 * two log entries etc.
		 * after players are ejected we can wait a few ticks (long enough for their client to receive new entity location)
		 * and then set them as passenger of the vehicle again
		 */
		if (player.get(Keys.VEHICLE).isPresent())
		{
			Entity ride = player.get(Keys.VEHICLE).get().restore().get();
			player.remove(Keys.VEHICLE);
			if (ride != null)
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = (ride instanceof Living) ? 0 : ride.getLocation().getY() - loc.getPosition().getY();
				Transform<World> rideLoc = newLoc;
				rideLoc = rideLoc.setPosition(new Vector3d(rideLoc.getPosition().getX(), newLoc.getPosition().getY() + vertOffset, rideLoc.getPosition().getZ()));
				if (Config.Debug())
					Config.logWarn("Player was riding a \"" + ride.toString() + "\".");
				if (ride instanceof Boat)
				{	// boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
					ride.remove();
					ride = world.createEntity(EntityTypes.BOAT, rideLoc.getPosition()).get();
					world.spawnEntity(ride, Cause.of(WorldBorder.container));
				}
				else
				{
					ride.setVelocity(new Vector3d(0, 0, 0));
					ride.setTransform(rideLoc);
				}

				if (Config.RemountTicks() > 0)
				{
					setPassengerDelayed(ride, player, player.getName(), Config.RemountTicks());
					handlingVehicle = true;
				}
			}
		}

		// check if player has something (a pet, maybe?) riding them; only possible through odd plugins.
		// it can prevent all teleportation of the player completely, so it's very much not good and needs handling
		player.get(Keys.PASSENGER).ifPresent(p -> {
            Entity passenger = p.restore().get();
			player.remove(Keys.PASSENGER);
			passenger.setTransform(newLoc);
			player.sendMessage(Text.of("Your passenger has been ejected."));
			if (Config.Debug())
				Config.logWarn("Player had a passenger riding on them: " + passenger.getType());
		});


		// give some particle and sound effects where the player was beyond the border, if "whoosh effect" is enabled
		Config.showWhooshEffect(loc);

		if (!returnLocationOnly)
			player.setTransform(newLoc);

		if (!handlingVehicle)
			handlingPlayers.remove(player.getName().toLowerCase());

		if (returnLocationOnly)
			return newLoc;

		return null;
	}
	public static Transform<World> checkPlayer(Player player, Transform<World> targetLoc, boolean returnLocationOnly)
	{
		return checkPlayer(player, targetLoc, returnLocationOnly, true);
	}

	private static Transform<World> newLocation(Player player, Transform<World> transform, BorderData border, boolean notify)
	{
		if (Config.Debug())
		{
			Config.logWarn((notify ? "Border crossing" : "Check was run") + " in \"" + transform.getExtent().getName() + "\". Border " + border.toString());
			Config.logWarn("Player position X: " + Config.coord.format(transform.getPosition().getX()) + " Y: " + Config.coord.format(transform.getPosition().getY()) + " Z: " + Config.coord.format(transform.getPosition().getZ()));
		}

		Transform<World> newLoc = border.correctedPosition(transform, Config.ShapeRound(), player.get(Keys.IS_FLYING).get());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.logWarn("Target new location unviable, using spawn or killing player.");
			if (Config.getIfPlayerKill())
			{
				player.offer(Keys.HEALTH, 0.0D);
				return null;
			}
			newLoc = new Transform<>(player.getWorld().getSpawnLocation());
		}

		if (Config.Debug())
			Config.logWarn("New position in world \"" + newLoc.getExtent().getName() + "\" at X: " + Config.coord.format(newLoc.getPosition().getX()) + " Y: " + Config.coord.format(newLoc.getPosition().getY()) + " Z: " + Config.coord.format(newLoc.getPosition().getZ()));

		if (notify)
			player.sendMessage(Text.of(Config.Message()));

		return newLoc;
	}

	private static void setPassengerDelayed(final Entity vehicle, final Player player, final String playerName, long delay)
	{
		Sponge.getScheduler().createTaskBuilder().delayTicks(delay).execute(() -> {
            handlingPlayers.remove(playerName.toLowerCase());
            if (vehicle == null || player == null)
                return;

            vehicle.setPassenger(player);
        }).submit(WorldBorder.plugin);
	}
}
