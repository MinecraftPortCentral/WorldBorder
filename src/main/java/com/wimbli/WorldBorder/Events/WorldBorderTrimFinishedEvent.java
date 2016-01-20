package com.wimbli.WorldBorder.Events;

import com.wimbli.WorldBorder.WorldBorder;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.world.World;

/**
 * Created by timafh on 04.09.2015.
 */
public class WorldBorderTrimFinishedEvent extends AbstractEvent {

	private final Cause cause;
	private World world;
	private long totalChunks;

	public WorldBorderTrimFinishedEvent(World world, long totalChunks)
	{
		this.world = world;
		this.totalChunks = totalChunks;
		this.cause = Cause.of(WorldBorder.container);
	}

	public World getWorld()
	{
		return world;
	}

	public long getTotalChunks()
	{
		return totalChunks;
	}

	@Override
	public Cause getCause() {
		return this.cause;
	}
}
