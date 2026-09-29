package com.ratoriku.ohmyteleport.server;

import java.util.Optional;

import com.ratoriku.ohmyteleport.core.TeleportPoint;
import com.ratoriku.ohmyteleport.data.PlayerDataSavedData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Moves players around and remembers where they came from, so {@code /back} can undo the move.
 *
 * <p>Every teleport in this mod goes through here, which is what keeps "last teleport origin"
 * trustworthy.
 */
public final class TeleportService {

	/** A reason a teleport did not happen, as a translation key plus an English fallback. */
	public record Failure(String key, String fallback) {
	}

	private TeleportService() {
	}

	public static Optional<Failure> teleport(ServerPlayer player, TeleportPoint target) {
		ServerLevel level = TeleportPoints.levelOf(TeleportPoints.serverOf(player), target);
		if (level == null) {
			return Optional.of(new Failure("unknown_dimension", "The target dimension is not loaded."));
		}
		TeleportPoint origin = TeleportPoints.of(player);
		PlayerDataSavedData.get(TeleportPoints.serverOf(player))
				.mutate(player.getUUID(), data -> data.setLastTeleportOrigin(origin));
		TeleportPoints.move(player, level, target);
		return Optional.empty();
	}
}
