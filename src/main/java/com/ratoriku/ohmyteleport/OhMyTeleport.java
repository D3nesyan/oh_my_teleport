package com.ratoriku.ohmyteleport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ratoriku.ohmyteleport.command.CommandRegistry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Entry point. Does nothing but wire the commands up - all behaviour lives in the command and
 * core packages.
 *
 * <p>This mod is server side only in substance, but {@code fabric.mod.json} declares
 * {@code "environment": "*"} because {@code "server"} mods are not loaded by the singleplayer
 * client, and this mod is meant to work in singleplayer too.
 */
public final class OhMyTeleport implements ModInitializer {

	public static final String MOD_ID = "oh_my_teleport";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		CommandRegistry.register();
	}
}
