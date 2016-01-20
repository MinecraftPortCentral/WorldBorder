package com.wimbli.WorldBorder;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Sets;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class BorderData
{
	// the main data interacted with
	private double x = 0;
	private double z = 0;
	private int radiusX = 0;
	private int radiusZ = 0;
	private Boolean shapeRound = null;
	private boolean wrapping = false;

	// some extra data kept handy for faster border checks
	private double maxX;
	private double minX;
	private double maxZ;
	private double minZ;
	private double radiusXSquared;
	private double radiusZSquared;
	private double DefiniteRectangleX;
	private double DefiniteRectangleZ;
	private double radiusSquaredQuotient;

	public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
	{
		setData(x, z, radiusX, radiusZ, shapeRound, wrap);
	}
	public BorderData(double x, double z, int radiusX, int radiusZ)
	{
		setData(x, z, radiusX, radiusZ, null);
	}
	public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
	{
		setData(x, z, radiusX, radiusZ, shapeRound);
	}
	public BorderData(double x, double z, int radius)
	{
		setData(x, z, radius, null);
	}
	public BorderData(double x, double z, int radius, Boolean shapeRound)
	{
		setData(x, z, radius, shapeRound);
	}

	public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
	{
		this.x = x;
		this.z = z;
		this.shapeRound = shapeRound;
		this.wrapping = wrap;
		this.setRadiusX(radiusX);
		this.setRadiusZ(radiusZ);
	}
	public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
	{
		setData(x, z, radiusX, radiusZ, shapeRound, false);
	}
	public final void setData(double x, double z, int radius, Boolean shapeRound)
	{
		setData(x, z, radius, radius, shapeRound, false);
	}

	public BorderData copy()
	{
		return new BorderData(x, z, radiusX, radiusZ, shapeRound, wrapping);
	}

	public double getX()
	{
		return x;
	}
	public void setX(double x)
	{
		this.x = x;
		this.maxX = x + radiusX;
		this.minX = x - radiusX;
	}
	public double getZ()
	{
		return z;
	}
	public void setZ(double z)
	{
		this.z = z;
		this.maxZ = z + radiusZ;
		this.minZ = z - radiusZ;
	}
	public int getRadiusX()
	{
		return radiusX;
	}
	public int getRadiusZ()
	{
		return radiusZ;
	}
	public void setRadiusX(int radiusX)
	{
		this.radiusX = radiusX;
		this.maxX = x + radiusX;
		this.minX = x - radiusX;
		this.radiusXSquared = (double)radiusX * (double)radiusX;
		this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
		this.DefiniteRectangleX = Math.sqrt(.5 * this.radiusXSquared);
	}
	public void setRadiusZ(int radiusZ)
	{
		this.radiusZ = radiusZ;
		this.maxZ = z + radiusZ;
		this.minZ = z - radiusZ;
		this.radiusZSquared = (double)radiusZ * (double)radiusZ;
		this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
		this.DefiniteRectangleZ = Math.sqrt(.5 * this.radiusZSquared);
	}


	// backwards-compatible methods from before elliptical/rectangular shapes were supported
	/**
	 * @deprecated  Replaced by {@link #getRadiusX()} and {@link #getRadiusZ()};
	 * this method now returns an average of those two values and is thus imprecise
	 */
	public int getRadius()
	{
		return (radiusX + radiusZ) / 2;  // average radius; not great, but probably best for backwards compatibility
	}
	public void setRadius(int radius)
	{
		setRadiusX(radius);
		setRadiusZ(radius);
	}


	public Boolean getShape()
	{
		return shapeRound;
	}
	public void setShape(Boolean shapeRound)
	{
		this.shapeRound = shapeRound;
	}


	public boolean getWrapping()
	{
		return wrapping;
	}
	public void setWrapping(boolean wrap)
	{
		this.wrapping = wrap;
	}


	@Override
	public String toString()
	{
		return "radius " + ((radiusX == radiusZ) ? radiusX : radiusX + "x" + radiusZ) + " at X: " + Config.coord.format(x) + " Z: " + Config.coord.format(z) + (shapeRound != null ? (" (shape override: " + Config.ShapeName(shapeRound.booleanValue()) + ")") : "") + (wrapping ? (" (wrapping)") : "");
	}

	// This algorithm of course needs to be fast, since it will be run very frequently
	public boolean insideBorder(double xLoc, double zLoc, boolean round)
	{
		// if this border has a shape override set, use it
		if (shapeRound != null)
			round = shapeRound.booleanValue();

		// square border
		if (!round)
			return !(xLoc < minX || xLoc > maxX || zLoc < minZ || zLoc > maxZ);

		// round border
		else
		{
			// elegant round border checking algorithm is from rBorder by Reil with almost no changes, all credit to him for it
			double X = Math.abs(x - xLoc);
			double Z = Math.abs(z - zLoc);

			if (X < DefiniteRectangleX && Z < DefiniteRectangleZ)
				return true;	// Definitely inside
			else if (X >= radiusX || Z >= radiusZ)
				return false;	// Definitely outside
			else if (X * X + Z * Z * radiusSquaredQuotient < radiusXSquared)
				return true;	// After further calculation, inside
			else
				return false;	// Apparently outside, then
		}
	}
	public boolean insideBorder(double xLoc, double zLoc)
	{
		return insideBorder(xLoc, zLoc, Config.ShapeRound());
	}
	public boolean insideBorder(Location<World> loc)
	{
		return insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound());
	}
	public boolean insideBorder(CoordXZ coord, boolean round)
	{
		return insideBorder(coord.x, coord.z, round);
	}
	public boolean insideBorder(CoordXZ coord)
	{
		return insideBorder(coord.x, coord.z, Config.ShapeRound());
	}

	public Transform<World> correctedPosition(Transform<World> transform, boolean round, boolean flying)
	{
		// if this border has a shape override set, use it
		if (shapeRound != null)
			round = shapeRound.booleanValue();

		double xLoc = transform.getPosition().getX();
		double zLoc = transform.getPosition().getZ();
		double yLoc = transform.getPosition().getY();

		// square border
		if (!round)
		{
			if (wrapping)
			{
				if (xLoc <= minX)
					xLoc = maxX - Config.KnockBack();
				else if (xLoc >= maxX)
					xLoc = minX + Config.KnockBack();
				if (zLoc <= minZ)
					zLoc = maxZ - Config.KnockBack();
				else if (zLoc >= maxZ)
					zLoc = minZ + Config.KnockBack();
			}
			else
			{
				if (xLoc <= minX)
					xLoc = minX + Config.KnockBack();
				else if (xLoc >= maxX)
					xLoc = maxX - Config.KnockBack();
				if (zLoc <= minZ)
					zLoc = minZ + Config.KnockBack();
				else if (zLoc >= maxZ)
					zLoc = maxZ - Config.KnockBack();
			}
		}

		// round border
		else
		{
			// algorithm originally from: http://stackoverflow.com/questions/300871/best-way-to-find-a-point-on-a-circle-closest-to-a-given-point
			// modified by Lang Lukas to support elliptical border shape

			//Transform the ellipse to a circle with radius 1 (we need to transform the point the same way)
			double dX = xLoc - x;
			double dZ = zLoc - z;
			double dU = Math.sqrt(dX *dX + dZ * dZ); //distance of the untransformed point from the center
			double dT = Math.sqrt(dX *dX / radiusXSquared + dZ * dZ / radiusZSquared); //distance of the transformed point from the center
			double f = (1 / dT - Config.KnockBack() / dU); //"correction" factor for the distances
			if (wrapping)
			{
				xLoc = x - dX * f;
				zLoc = z - dZ * f;
			} else {
				xLoc = x + dX * f;
				zLoc = z + dZ * f;
			}
		}

		int ixLoc = (int) Math.floor(xLoc);
		int izLoc = (int) Math.floor(zLoc);

		// Make sure the chunk we're checking in is actually loaded

		transform.getExtent().loadChunk(CoordXZ.blockToChunk(ixLoc), 0, CoordXZ.blockToChunk(izLoc), true).get();

		yLoc = getSafeY(transform.getExtent(), ixLoc, (int) Math.floor(yLoc), izLoc, flying);
		if (yLoc == -1) {
			return null;
		}

		return new Transform<>(transform.getExtent(),
                new Vector3d(transform.getLocation().getBlockX() + 0.5, yLoc, transform.getLocation().getBlockZ() + 0.5)).setRotation(new Vector3d(transform.getYaw(), transform.getPitch(), 0));
	}
	public Transform<World> correctedPosition(Transform<World> transform, boolean round)
	{
		return correctedPosition(transform, round, false);
	}
	public Transform<World> correctedPosition(Transform<World> transform)
	{
		return correctedPosition(transform, Config.ShapeRound(), false);
	}

	//these material IDs are acceptable for places to teleport player; breathable blocks and water
	public static final LinkedHashSet<BlockType> safeOpenBlocks = Sets.newLinkedHashSet(Arrays.asList(
			BlockTypes.AIR, BlockTypes.SAPLING, BlockTypes.FLOWING_WATER, BlockTypes.WATER, BlockTypes.GOLDEN_RAIL,
			BlockTypes.DETECTOR_RAIL, BlockTypes.WEB, BlockTypes.TALLGRASS, BlockTypes.DEADBUSH, BlockTypes.YELLOW_FLOWER,
			BlockTypes.RED_FLOWER, BlockTypes.BROWN_MUSHROOM, BlockTypes.RED_MUSHROOM, BlockTypes.TORCH, BlockTypes.REDSTONE_WIRE,
			BlockTypes.WHEAT, BlockTypes.STANDING_SIGN, BlockTypes.WOODEN_DOOR, BlockTypes.LADDER, BlockTypes.RAIL, BlockTypes.WALL_SIGN,
			BlockTypes.LEVER, BlockTypes.STONE_PRESSURE_PLATE, BlockTypes.IRON_DOOR, BlockTypes.WOODEN_PRESSURE_PLATE, BlockTypes.UNLIT_REDSTONE_TORCH,
			BlockTypes.REDSTONE_TORCH, BlockTypes.STONE_BUTTON, BlockTypes.SNOW, BlockTypes.REEDS, BlockTypes.PORTAL, BlockTypes.UNPOWERED_REPEATER,
			BlockTypes.POWERED_REPEATER, BlockTypes.TRAPDOOR, BlockTypes.PUMPKIN_STEM, BlockTypes.MELON_STEM, BlockTypes.VINE, BlockTypes.NETHER_WART,
			BlockTypes.TRIPWIRE_HOOK, BlockTypes.TRIPWIRE, BlockTypes.CARROTS, BlockTypes.POTATOES, BlockTypes.UNPOWERED_COMPARATOR, BlockTypes.POWERED_COMPARATOR,
			BlockTypes.ACTIVATOR_RAIL, BlockTypes.CARPET
	));

	//these material IDs are ones we don't want to drop the player onto, like cactus or lava or fire or activated Ender portal
	public static final LinkedHashSet<BlockType> painfulBlocks = new LinkedHashSet<>(Arrays.asList(
		 BlockTypes.FLOWING_LAVA, BlockTypes.LAVA, BlockTypes.FIRE, BlockTypes.CACTUS, BlockTypes.END_PORTAL
	));

	// check if a particular spot consists of 2 breathable blocks over something relatively solid
	private boolean isSafeSpot(World world, int X, int Y, int Z, boolean flying)
	{
		boolean safe = safeOpenBlocks.contains(world.getBlockType(X, Y, Z))		// target block open and safe
					&& safeOpenBlocks.contains(world.getBlockType(X, Y + 1, Z));	// above target block open and safe
		if (!safe || flying)
			return safe;

		BlockType below = world.getBlockType(X, Y - 1, Z);
		return (safe
			 && (!safeOpenBlocks.contains(below) || below.equals(BlockTypes.FLOWING_WATER) || below.equals(BlockTypes.WATER))	// below target block not open/breathable (so presumably solid), or is water
			 && !painfulBlocks.contains(below)									// below target block not painful
			);
	}

	private static final int limBot = 1;

	// find closest safe Y position from the starting position
	private double getSafeY(World world, int X, int Y, int Z, boolean flying)
	{
		// artificial height limit of 127 added for Nether worlds since CraftBukkit still incorrectly returns 255 for their max height, leading to players sent to the "roof" of the Nether
		final int limTop = world.getDimension().getBuildHeight();
		// Expanding Y search method adapted from Acru's code in the Nether plugin

		for(int y1 = Y, y2 = Y; (y1 > limBot) || (y2 < limTop); y1--, y2++){
			// Look below.
			if(y1 > limBot)
			{
				if (isSafeSpot(world, X, y1, Z, flying))
					return (double)y1;
			}

			// Look above.
			if(y2 < limTop && y2 != y1)
			{
				if (isSafeSpot(world, X, y2, Z, flying))
					return (double)y2;
			}
		}

		return -1.0;	// no safe Y location?!?!? Must be a rare spot in a Nether world or something
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		else if (obj == null || obj.getClass() != this.getClass())
			return false;

		BorderData test = (BorderData)obj;
		return test.x == this.x && test.z == this.z && test.radiusX == this.radiusX && test.radiusZ == this.radiusZ;
	}

	@Override
	public int hashCode()
	{
		return (((int)(this.x * 10) << 4) + (int)this.z + (this.radiusX << 2) + (this.radiusZ << 3));
	}
}
