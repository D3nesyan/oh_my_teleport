package com.ratoriku.ohmyteleport.server;

import java.util.Optional;

import com.ratoriku.ohmyteleport.core.TeleportPoint;
import com.ratoriku.ohmyteleport.data.PlayerDataSavedData;
import com.ratoriku.ohmyteleport.text.Messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Moves players around and remembers where they came from, so {@code /back} can undo the move.
 *
 * <p>Every teleport in this mod goes through here, which is what keeps "last teleport origin"
 * trustworthy and what guarantees that every teleport announces itself.
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

		player.sendSystemMessage(arrival(origin, target, level));
		return Optional.empty();
	}

	/**
	 * The report the player gets after being moved, in the style of vanilla's teleport feedback.
	 *
	 * <p>The dimension is only mentioned when the teleport crossed one. Nothing here can know what a
	 * third party dimension is called - vanilla ships no dimension name translations at all - so a
	 * modded dimension would otherwise print its raw id on every single {@code /home} inside it.
	 * Naming one is still possible from a resource pack; see the README.
	 */
	private static Component arrival(TeleportPoint origin, TeleportPoint target, ServerLevel level) {
		String x = Messages.coordinate(target.x());
		String y = Messages.coordinate(target.y());
		String z = Messages.coordinate(target.z());

		if (origin.dimensionId().equals(target.dimensionId())) {
			return Messages.text("teleported", "Arrived at %s, %s, %s", x, y, z);
		}
		return Messages.text("teleported_in_dimension", "Arrived in %s at %s, %s, %s",
				TeleportPoints.dimensionName(level), x, y, z);
	}
}
