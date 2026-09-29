import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal RCON client used to smoke test the mod against a running dev server.
 *
 * <p>Run with a single-file source launch, e.g.
 * {@code java tools/RconProbe.java localhost 25585 omtprobe help back sethome "warp list"}.
 *
 * <p>The dedicated server console is not a player, so player-only commands cannot succeed here.
 * What this proves is that the commands are registered at all: an unregistered command answers
 * "Unknown or incomplete command", while a registered one answers with one of the mod's own
 * messages.
 */
public final class RconProbe {

	private static final int TYPE_AUTH = 3;
	private static final int TYPE_COMMAND = 2;

	private record Response(int id, int type, String body) {
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 4) {
			System.err.println("usage: RconProbe <host> <port> <password> <command>...");
			System.exit(2);
		}
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String password = args[2];

		try (Socket socket = new Socket(host, port)) {
			socket.setSoTimeout(5000);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());

			write(out, 1, TYPE_AUTH, password);
			Response auth = read(in);
			if (auth.id() == -1) {
				System.err.println("AUTH FAILED");
				System.exit(3);
			}
			System.out.println("[auth] ok");

			int id = 2;
			for (int i = 3; i < args.length; i++) {
				write(out, id++, TYPE_COMMAND, args[i]);
				Response response = read(in);
				System.out.println(">>> " + args[i]);
				System.out.println("<<< " + response.body().replace("\n", "\n    "));
				System.out.println();
			}
		}
	}

	private static void write(DataOutputStream out, int id, int type, String body) throws IOException {
		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		out.writeInt(payload.length + 10);
		out.writeInt(id);
		out.writeInt(type);
		out.write(payload);
		out.writeByte(0);
		out.writeByte(0);
		out.flush();
	}

	private static Response read(DataInputStream in) throws IOException {
		int length = in.readInt();
		int id = in.readInt();
		int type = in.readInt();
		byte[] body = in.readNBytes(length - 10);
		in.readNBytes(2);
		return new Response(id, type, new String(body, StandardCharsets.UTF_8));
	}
}
