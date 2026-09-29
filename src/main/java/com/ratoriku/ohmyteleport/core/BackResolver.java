package com.ratoriku.ohmyteleport.core;

import java.util.Optional;

/**
 * Decides where {@code /back} should send a player.
 *
 * <p>The death location wins whenever one is set. Because the caller clears the vanilla death
 * location after using it, the next {@code /back} falls through to the last teleport origin.
 */
public final class BackResolver {

	/** @param fromDeath whether the caller used the death location and must therefore clear it */
	public record BackTarget(TeleportPoint point, boolean fromDeath) {
	}

	private BackResolver() {
	}

	public static Optional<BackTarget> resolve(Optional<TeleportPoint> deathPoint,
			Optional<TeleportPoint> lastTeleportOrigin) {
		if (deathPoint.isPresent()) {
			return Optional.of(new BackTarget(deathPoint.get(), true));
		}
		return lastTeleportOrigin.map(point -> new BackTarget(point, false));
	}
}
