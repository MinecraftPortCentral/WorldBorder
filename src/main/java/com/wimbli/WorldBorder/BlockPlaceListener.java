package com.wimbli.WorldBorder;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

public class BlockPlaceListener {
	@Listener(order = Order.PRE)
	@IsCancelled(Tristate.UNDEFINED)
	public void onBlockPlace(ChangeBlockEvent.Place event)
	{
		for (Transaction<BlockSnapshot> transaction: event.getTransactions()) {
			transaction.getOriginal().getLocation().ifPresent(loc -> {

				World world = loc.getExtent();
				BorderData border = Config.Border(world.getName());
				if (border == null)
					return;

				if (!border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound())) {
					transaction.setValid(false);
				}
			});
		}
	}
}
