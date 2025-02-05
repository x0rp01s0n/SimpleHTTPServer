package me.mrletsplay.simplehttpserver.http.websocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpHeaderFields;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;

public abstract class WebSocketEndpoint implements HttpDocument {

	private static final MessageDigest SHA_1;

	static {
		try {
			SHA_1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new FriendlyException(e);
		}
	}

	private List<WebSocketConnection> connections = new ArrayList<>();

	@Override
	public void createContent() {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
		HttpClientHeader cH = ctx.getClientHeader();
		HttpServerHeader sH = ctx.getServerHeader();
		HttpHeaderFields clientFields = cH.getFields();

		sH.setCompressionEnabled(false);

		if(!"websocket".equalsIgnoreCase(clientFields.getFirst("Upgrade"))
				|| clientFields.getFirst("Connection") == null
				|| !Arrays.stream(clientFields.getFirst("Connection").split(",")).anyMatch(h -> h.trim().equalsIgnoreCase("Upgrade"))
				|| clientFields.getFirst("Sec-WebSocket-Key") == null
				|| clientFields.getFirst("Sec-WebSocket-Version") == null) {
			sH.setStatusCode(HttpStatusCodes.BAD_REQUEST_400);
			sH.setContent("text/plain", "400 Bad Request".getBytes(StandardCharsets.UTF_8));
			return;
		}

		String key = clientFields.getFirst("Sec-WebSocket-Key");
		String version = clientFields.getFirst("Sec-WebSocket-Version");
//		String protocols = clientFields.getFieldValue("Sec-WebSocket-Protocol");
//		String extensions = clientFields.getFieldValue("Sec-WebSocket-Extensions");

		if(!version.equals("13")) {
			sH.setStatusCode(HttpStatusCodes.BAD_REQUEST_400);
			sH.getFields().set("Sec-WebSocket-Version", "13");
			sH.setContent("text/plain", "400 Bad Request".getBytes(StandardCharsets.UTF_8));
			return;
		}

		sH.setStatusCode(HttpStatusCodes.SWITCHING_PROTOCOLS_101);
		sH.getFields().set("Upgrade", "websocket");
		sH.getFields().set("Connection", "Upgrade");

		String keyConcat = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		SHA_1.reset();
		sH.getFields().set("Sec-WebSocket-Accept", Base64.getEncoder().encodeToString(SHA_1.digest(keyConcat.getBytes(StandardCharsets.UTF_8))));

		WebSocketConnection con = new WebSocketConnection(ctx.getConnection(), this);
		ctx.getConnection().setWebsocketConnection(con);
		onOpen(con);
		connections.add(con);
	}

	public List<WebSocketConnection> getConnections() {
		return connections;
	}

	public void onOpen(WebSocketConnection connection) {}

	public void onClose(WebSocketConnection connection, CloseFrame closeFrame) {}

	public void onFrameReceived(WebSocketConnection connection, WebSocketFrame frame) {}

	public void onCompleteFrameReceived(WebSocketConnection connection, WebSocketFrame frame) {}

	public void onTextMessage(WebSocketConnection connection, String message) {}

	public void onBinaryMessage(WebSocketConnection connection, byte[] message) {}

	public void onPing(WebSocketConnection connection, byte[] payload) {}

	public void onPong(WebSocketConnection connection, byte[] payload) {}

}
