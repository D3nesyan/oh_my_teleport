package com.ratoriku.ohmyteleport.command;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ratoriku.ohmyteleport.core.BackResolver;
import com.ratoriku.ohmyteleport.core.PlayerData;
import com.ratoriku.ohmyteleport.data.PlayerDataSavedData;
import com.ratoriku.ohmyteleport.server.TeleportPoints;
import com.ratoriku.ohmyteleport.server.TeleportService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /back} - to where the player died, and when that has already been used, to where they
 * stood before their last teleport.
 *
 * <p>The death location is Minecraft's own, and it is cleared once used. That makes the "one shot"
 * behaviour survive a disconnect, without this mod storing a death location of its own.
 */
public final class BackCommand {

	private BackCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("back").executes(BackCommand::back));
	}

	private static int back(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();

		PlayerData data = PlayerDataSavedData.get(TeleportPoints.serverOf(player)).dataFor(player.getUUID());
		Optional<BackResolver.BackTarget> target = BackResolver.resolve(
				TeleportPoints.deathPoint(player),
				data.lastTeleportOrigin());

		if (target.isEmpty()) {
			Messages.failure(source, "back_none", "There is nowhere to go back to.");
			return 0;
		}

		BackResolver.BackTarget resolved = target.get();
		Optional<TeleportService.Failure> failure = TeleportService.teleport(player, resolved.point());
		if (failure.isPresent()) {
			Messages.reportFailure(source, failure.get());
			return 0;
		}

		if (resolved.fromDeath()) {
			player.setLastDeathLocation(Optional.empty());
			Messages.success(source, "back_death", "Returned to where you died.");
		} else {
			Messages.success(source, "back_origin", "Returned to where you were before your last teleport.");
		}
		return 1;
	}
}
