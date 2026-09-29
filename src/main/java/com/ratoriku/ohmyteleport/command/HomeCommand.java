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
import com.ratoriku.ohmyteleport.text.Messages;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/** {@code /sethome}, {@code /home}, {@code /delhome}, {@code /homes}. */
public final class HomeCommand {

	public static final String DEFAULT_HOME = "home";

	private HomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("sethome")
				.executes(context -> setHome(context, DEFAULT_HOME))
				.then(Commands.argument("name", StringArgumentType.word())
						.executes(context -> setHome(context, StringArgumentType.getString(context, "name")))));

		dispatcher.register(Commands.literal("home")
				.executes(context -> goHome(context, DEFAULT_HOME))
				.then(Commands.argument("name", StringArgumentType.word())
						.suggests(HomeCommand::suggestHomes)
						.executes(context -> goHome(context, StringArgumentType.getString(context, "name")))));

		dispatcher.register(Commands.literal("delhome")
				.then(Commands.argument("name", StringArgumentType.word())
						.suggests(HomeCommand::suggestHomes)
						.executes(HomeCommand::deleteHome)));

		dispatcher.register(Commands.literal("homes").executes(HomeCommand::listHomes));
	}

	private static int setHome(CommandContext<CommandSourceStack> context, String rawName) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(rawName);
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		TeleportPoint point = TeleportPoints.of(player);
		PlayerDataSavedData store = PlayerDataSavedData.get(TeleportPoints.serverOf(player));
		boolean replaced = store.dataFor(player.getUUID()).homes().has(name);
		store.mutate(player.getUUID(), data -> data.homes().put(name, point));

		if (replaced) {
			Messages.success(source, "home_replaced", "Home '%s' moved to here.", name);
		} else {
			Messages.success(source, "home_set", "Home '%s' set.", name);
		}
		return 1;
	}

	private static int goHome(CommandContext<CommandSourceStack> context, String rawName) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(rawName);
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		Optional<TeleportPoint> home = homes(player).get(name);
		if (home.isEmpty()) {
			Messages.failure(source, "no_home", "You have no home called '%s'.", name);
			return 0;
		}

		Optional<TeleportService.Failure> failure = TeleportService.teleport(player, home.get());
		if (failure.isPresent()) {
			Messages.reportFailure(source, failure.get());
			return 0;
		}

		Messages.success(source, "teleported_home", "Welcome home, '%s'.", name);
		return 1;
	}

	private static int deleteHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = NameRules.normalize(StringArgumentType.getString(context, "name"));
		if (name == null) {
			Messages.failure(source, "invalid_name",
					"That name cannot be used. Use up to 32 characters without spaces.");
			return 0;
		}

		PlayerDataSavedData store = PlayerDataSavedData.get(TeleportPoints.serverOf(player));
		if (!store.dataFor(player.getUUID()).homes().has(name)) {
			Messages.failure(source, "no_home", "You have no home called '%s'.", name);
			return 0;
		}

		store.mutate(player.getUUID(), data -> data.homes().remove(name));
		Messages.success(source, "home_removed", "Home '%s' deleted.", name);
		return 1;
	}

	private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		List<String> names = homes(player).names();

		if (names.isEmpty()) {
			Messages.failure(source, "no_homes", "You have not set any homes yet. Use /sethome.");
			return 0;
		}

		Messages.success(source, "homes_list", "Your homes: %s", String.join(", ", names));
		return names.size();
	}

	private static PointStore homes(ServerPlayer player) {
		return PlayerDataSavedData.get(TeleportPoints.serverOf(player)).dataFor(player.getUUID()).homes();
	}

	private static CompletableFuture<Suggestions> suggestHomes(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		ServerPlayer player = context.getSource().getPlayer();
		List<String> names = player == null ? List.of() : homes(player).names();
		return SharedSuggestionProvider.suggest(names, builder);
	}
}
