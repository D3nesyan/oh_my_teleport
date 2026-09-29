package com.ratoriku.ohmyteleport.command;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ratoriku.ohmyteleport.core.NameRules;
import com.ratoriku.ohmyteleport.core.PointStore;
import com.ratoriku.ohmyteleport.core.TeleportPoint;
import com.ratoriku.ohmyteleport.data.PlayerDataSavedData;
import com.ratoriku.ohmyteleport.server.TeleportPoints;
import com.ratoriku.ohmyteleport.server.TeleportService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /warp set <name>}, {@code /warp <name>}, {@code /warp del <name>}, {@code /warp list}.
 *
 * <p>Warps are personal: they belong to the player who made them and nobody else can see or use
 * them.
 *
 * <p>Because {@code set}, {@code del} and {@code list} are literal subcommands, a warp cannot be
 * named after them.
 */
public final class WarpCommand {

	private WarpCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("warp")
				.executes(WarpCommand::listWarps)
				.then(Commands.literal("set")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(WarpCommand::setWarp)))
				.then(Commands.literal("del")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(WarpCommand::suggestWarps)
								.executes(WarpCommand::deleteWarp)))
				.then(Commands.literal("list")
						.executes(WarpCommand::listWarps))
				.then(Commands.argument("name", StringArgumentType.word())
						.suggests(WarpCommand::suggestWarps)
						.executes(WarpCommand::goWarp)));
	}

	private static int setWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(StringArgumentType.getString(context, "name"));
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		TeleportPoint point = TeleportPoints.of(player);
		PlayerDataSavedData store = PlayerDataSavedData.get(TeleportPoints.serverOf(player));
		boolean replaced = store.dataFor(player.getUUID()).warps().has(name);
		store.mutate(player.getUUID(), data -> data.warps().put(name, point));

		if (replaced) {
			Messages.success(source, "warp_replaced", "Warp '%s' moved to here.", name);
		} else {
			Messages.success(source, "warp_set", "Warp '%s' set.", name);
		}
		return 1;
	}

	private static int goWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(StringArgumentType.getString(context, "name"));
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		Optional<TeleportPoint> warp = warps(player).get(name);
		if (warp.isEmpty()) {
			Messages.failure(source, "no_warp", "You have no warp called '%s'.", name);
			return 0;
		}

		Optional<TeleportService.Failure> failure = TeleportService.teleport(player, warp.get());
		if (failure.isPresent()) {
			Messages.reportFailure(source, failure.get());
			return 0;
		}

		Messages.success(source, "teleported_warp", "Warped to '%s'.", name);
		return 1;
	}

	private static int deleteWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(StringArgumentType.getString(context, "name"));
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		PlayerDataSavedData store = PlayerDataSavedData.get(TeleportPoints.serverOf(player));
		if (!store.dataFor(player.getUUID()).warps().has(name)) {
			Messages.failure(source, "no_warp", "You have no warp called '%s'.", name);
			return 0;
		}

		store.mutate(player.getUUID(), data -> data.warps().remove(name));
		Messages.success(source, "warp_removed", "Warp '%s' deleted.", name);
		return 1;
	}

	private static int listWarps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		List<String> names = warps(player).names();

		if (names.isEmpty()) {
			Messages.failure(source, "no_warps", "You have not set any warps yet. Use /warp set <name>.");
			return 0;
		}

		Messages.success(source, "warps_list", "Your warps: %s", String.join(", ", names));
		return names.size();
	}

	private static PointStore warps(ServerPlayer player) {
		return PlayerDataSavedData.get(TeleportPoints.serverOf(player)).dataFor(player.getUUID()).warps();
	}

	private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		ServerPlayer player = context.getSource().getPlayer();
		List<String> names = player == null ? List.of() : warps(player).names();
		return SharedSuggestionProvider.suggest(names, builder);
	}
}
