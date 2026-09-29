package com.ratoriku.ohmyteleport.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Wires every command onto the server dispatcher. */
public final class CommandRegistry {

	private CommandRegistry() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			HomeCommand.register(dispatcher);
			WarpCommand.register(dispatcher);
			TpaCommand.register(dispatcher);
			BackCommand.register(dispatcher);
		});
	}
}
