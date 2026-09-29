package com.ratoriku.ohmyteleport.core;

import java.util.Objects;

/**
 * A place a player can be sent to.
 *
 * <p>The dimension is stored as a plain id string rather than a {@code ResourceKey<Level>} on
 * purpose: it keeps this class - and everything that stores it - free of Minecraft imports, so the
 * whole core package can be unit tested without starting the game.
 */
public record TeleportPoint(String dimensionId, double x, double y, double z, float yaw, float pitch) {

	public TeleportPoint {
		Objects.requireNonNull(dimensionId, "dimensionId");
	}

	public static TeleportPoint of(String dimensionId, double x, double y, double z) {
		return new TeleportPoint(dimensionId, x, y, z, 0.0f, 0.0f);
	}

	/** Same block column, ignoring rotation. Used by tests and by "are we already there" checks. */
	public boolean samePosition(TeleportPoint other) {
		return other != null
				&& dimensionId.equals(other.dimensionId)
				&& x == other.x && y == other.y && z == other.z;
	}
}
