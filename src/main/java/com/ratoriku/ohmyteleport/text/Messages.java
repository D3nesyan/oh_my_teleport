package com.ratoriku.ohmyteleport.command;

import com.ratoriku.ohmyteleport.server.TeleportService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * All player-facing text.
 *
 * <p>Messages use a translation key with an English fallback rather than a plain literal. That way
 * a client that speaks the player's language gets a translated message when the mod is installed on
 * the client too, and a client without the mod still sees readable English instead of a raw key.
 */
public final class Messages {

	private static final String PREFIX = "oh_my_teleport.msg.";

	private Messages() {
	}

	public static Component text(String key, String fallback, Object... args) {
		return Component.translatableWithFallback(PREFIX + key, fallback, args);
	}

	public static Component name(Object value) {
		return Component.literal(String.valueOf(value));
	}

	public static void success(CommandSourceStack source, String key, String fallback, Object... args) {
		source.sendSuccess(() -> text(key, fallback, args), false);
	}

	public static void failure(CommandSourceStack source, String key, String fallback, Object... args) {
		source.sendFailure(text(key, fallback, args));
	}

	public static void reportFailure(CommandSourceStack source, TeleportService.Failure failure) {
		source.sendFailure(text(failure.key(), failure.fallback()));
	}
}
