package com.ratoriku.ohmyteleport.server;

import com.ratoriku.ohmyteleport.core.TpaRegistry;

/**
 * The single pending-request table for this server process.
 *
 * <p>It lives here rather than inside the command class so that command classes stay pure dispatch
 * and so tests can seed requests directly.
 */
public final class TpaService {

	private static final TpaRegistry REGISTRY = new TpaRegistry(System::currentTimeMillis);

	private TpaService() {
	}

	public static TpaRegistry registry() {
		return REGISTRY;
	}
}
