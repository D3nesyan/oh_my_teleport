package com.ratoriku.ohmyteleport.command;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ratoriku.ohmyteleport.core.TpaRegistry;
import com.ratoriku.ohmyteleport.server.TeleportPoints;
import com.ratoriku.ohmyteleport.server.TeleportService;
import com.ratoriku.ohmyteleport.server.TpaService;
import com.ratoriku.ohmyteleport.text.Messages;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /tpa <player>}, {@code /tpaccept [player]}, {@code /tpadeny [player]}.
 *
 * <p>Accepting moves the <em>requester</em> to the player who accepted, which is what
 * "I want to come to you" means.
 *
 * <p>See {@link TpaRegistry} for how several requests at once are handled.
 */
public final class TpaCommand {

	private static final TpaRegistry REGISTRY = TpaService.registry();

	private TpaCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tpa")
				.then(Commands.argument("player", EntityArgument.player())
						.executes(TpaCommand::request)));

		dispatcher.register(Commands.literal("tpaccept")
				.executes(context -> accept(context, null))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> accept(context, EntityArgument.getPlayer(context, "player")))));

		dispatcher.register(Commands.literal("tpadeny")
				.executes(context -> deny(context, null))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> deny(context, EntityArgument.getPlayer(context, "player")))));
	}

	private static int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer requester = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");

		if (requester == target || requester.getUUID().equals(target.getUUID())) {
			Messages.failure(source, "tpa_self", "You cannot teleport to yourself.");
			return 0;
		}

		Optional<TpaRegistry.Request> replaced = REGISTRY.request(requester.getUUID(), target.getUUID());
		replaced.filter(previous -> !previous.target().equals(target.getUUID())).ifPresent(previous -> {
			ServerPlayer previousTarget = TeleportPoints.serverOf(requester).getPlayerList().getPlayer(previous.target());
			if (previousTarget != null) {
				previousTarget.sendSystemMessage(Messages.text("tpa_overwritten",
						"%s is asking somebody else instead; that request no longer stands.", requester.getName()));
			}
		});

		long seconds = REGISTRY.timeoutMillis() / 1000L;
		Messages.success(source, "tpa_sent", "Teleport request sent to %s. It expires in %s seconds.",
				target.getName(), String.valueOf(seconds));
		target.sendSystemMessage(Messages.text("tpa_received",
				"%s wants to teleport to you. Use /tpaccept or /tpadeny.", requester.getName()));
		return 1;
	}

	private static int accept(CommandContext<CommandSourceStack> context, ServerPlayer namedRequester)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer target = source.getPlayerOrException();

		Optional<TpaRegistry.Request> accepted;
		if (namedRequester != null) {
			accepted = REGISTRY.accept(target.getUUID(), namedRequester.getUUID());
		} else {
			List<TpaRegistry.Request> pending = REGISTRY.pendingFor(target.getUUID());
			if (pending.isEmpty()) {
				Messages.failure(source, "tpa_none", "You have no pending teleport requests.");
				return 0;
			}
			if (pending.size() > 1) {
				Messages.failure(source, "tpa_multiple",
						"You have several pending requests from %s. Use /tpaccept <player>.",
						describe(TeleportPoints.serverOf(target), pending));
				return 0;
			}
			accepted = REGISTRY.accept(target.getUUID(), pending.get(0).requester());
		}

		if (accepted.isEmpty()) {
			Messages.failure(source, "tpa_no_such", "You have no pending request from that player.");
			return 0;
		}

		ServerPlayer requester = TeleportPoints.serverOf(target).getPlayerList().getPlayer(accepted.get().requester());
		if (requester == null) {
			Messages.failure(source, "tpa_requester_offline", "That player is no longer online.");
			return 0;
		}

		Optional<TeleportService.Failure> failure = TeleportService.teleport(requester, TeleportPoints.of(target));
		if (failure.isPresent()) {
			Messages.reportFailure(source, failure.get());
			requester.sendSystemMessage(Messages.text(failure.get().key(), failure.get().fallback()));
			return 0;
		}

		Messages.success(source, "tpa_accepted_target", "You accepted %s's request.", requester.getName());
		requester.sendSystemMessage(Messages.text("tpa_accepted_requester",
				"%s accepted your request. Teleporting you over.", target.getName()));
		return 1;
	}

	private static int deny(CommandContext<CommandSourceStack> context, ServerPlayer namedRequester)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer target = source.getPlayerOrException();

		Optional<TpaRegistry.Request> denied;
		if (namedRequester != null) {
			denied = REGISTRY.deny(target.getUUID(), namedRequester.getUUID());
		} else {
			List<TpaRegistry.Request> pending = REGISTRY.pendingFor(target.getUUID());
			if (pending.isEmpty()) {
				Messages.failure(source, "tpa_none", "You have no pending teleport requests.");
				return 0;
			}
			if (pending.size() > 1) {
				Messages.failure(source, "tpa_multiple",
						"You have several pending requests from %s. Use /tpadeny <player>.",
						describe(TeleportPoints.serverOf(target), pending));
				return 0;
			}
			denied = REGISTRY.deny(target.getUUID(), pending.get(0).requester());
		}

		if (denied.isEmpty()) {
			Messages.failure(source, "tpa_no_such", "You have no pending request from that player.");
			return 0;
		}

		ServerPlayer requester = TeleportPoints.serverOf(target).getPlayerList().getPlayer(denied.get().requester());
		Component requesterName = requester == null ? Messages.name(denied.get().requester()) : requester.getName();

		Messages.success(source, "tpa_denied_target", "You denied %s's request.", requesterName);
		if (requester != null) {
			requester.sendSystemMessage(Messages.text("tpa_denied_requester", "%s denied your request.", target.getName()));
		}
		return 1;
	}

	private static Component describe(MinecraftServer server, List<TpaRegistry.Request> requests) {
		return Messages.name(requests.stream()
				.map(request -> {
					ServerPlayer player = server.getPlayerList().getPlayer(request.requester());
					return player == null ? request.requester().toString() : player.getGameProfile().name();
				})
				.collect(Collectors.joining(", ")));
	}
}
