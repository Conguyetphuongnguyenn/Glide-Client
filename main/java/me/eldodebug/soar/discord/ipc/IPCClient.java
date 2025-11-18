package me.eldodebug.soar.discord.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import me.eldodebug.soar.discord.ipc.entities.Callback;
import me.eldodebug.soar.discord.ipc.entities.DiscordBuild;
import me.eldodebug.soar.discord.ipc.entities.Packet;
import me.eldodebug.soar.discord.ipc.entities.Packet.OpCode;
import me.eldodebug.soar.discord.ipc.entities.RichPresence;
import me.eldodebug.soar.discord.ipc.entities.User;
import me.eldodebug.soar.discord.ipc.entities.pipe.Pipe;
import me.eldodebug.soar.discord.ipc.entities.pipe.PipeStatus;
import me.eldodebug.soar.discord.ipc.exceptions.NoDiscordClientException;

public final class IPCClient implements Closeable {
	
    private static final Logger LOGGER = LogManager.getLogger(IPCClient.class);
    
    // ✅ Constants
    private static final String CMD_SET_ACTIVITY = "SET_ACTIVITY";
    private static final String CMD_SUBSCRIBE = "SUBSCRIBE";
    private static final String CMD_DISPATCH = "DISPATCH";
    
    private static final String PROP_PID = "pid";
    private static final String PROP_ACTIVITY = "activity";
    private static final String PROP_CMD = "cmd";
    private static final String PROP_ARGS = "args";
    private static final String PROP_EVT = "evt";
    private static final String PROP_NONCE = "nonce";
    private static final String PROP_DATA = "data";
    private static final String PROP_MESSAGE = "message";
    private static final String PROP_SECRET = "secret";
    private static final String PROP_USER = "user";
    private static final String PROP_USERNAME = "username";
    private static final String PROP_DISCRIMINATOR = "discriminator";
    private static final String PROP_ID = "id";
    private static final String PROP_AVATAR = "avatar";
    
    private final long clientId;
    private final HashMap<String, Callback> callbacks = new HashMap<>();
    private volatile Pipe pipe;
    private IPCListener listener = null;
    private Thread readThread = null;
    
    public IPCClient(long clientId) {
        this.clientId = clientId;
    }
    
    public void setListener(IPCListener listener) {
        this.listener = listener;
        
        if (pipe != null) {
            pipe.setListener(listener);
        }
    }
    
    public void connect(DiscordBuild... preferredOrder) throws NoDiscordClientException {
        if (getStatus() == PipeStatus.CONNECTED) {
            LOGGER.debug("Client is already connected");
            return;
        }
        
        callbacks.clear();
        pipe = null;

        pipe = Pipe.openPipe(this, clientId, callbacks, preferredOrder);

        LOGGER.debug("Client is now connected and ready!");
        
        if (listener != null) {
            listener.onReady(this);
        }
        
        startReading();
    }
    
    public void sendRichPresence(RichPresence presence) {
        sendRichPresence(presence, null);
    }
    
    public void sendRichPresence(RichPresence presence, Callback callback) {
        if (getStatus() != PipeStatus.CONNECTED) {
            LOGGER.warn("Cannot send RichPresence: not connected");
            return;
        }
        
        LOGGER.debug("Sending RichPresence to discord: " + (presence == null ? "null" : presence.toJson().toString()));

        JsonObject argsObject = new JsonObject();
        argsObject.addProperty(PROP_PID, getPID());
        argsObject.add(PROP_ACTIVITY, presence == null ? null : presence.toJson());

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PROP_CMD, CMD_SET_ACTIVITY);
        jsonObject.add(PROP_ARGS, argsObject);

        pipe.send(OpCode.FRAME, jsonObject, callback);
    }

    public void subscribe(Event sub) {
        subscribe(sub, null);
    }
    
    public void subscribe(Event sub, Callback callback) {
        if (getStatus() != PipeStatus.CONNECTED) {
            LOGGER.warn("Cannot subscribe: not connected");
            return;
        }
        
        if (!sub.isSubscribable()) {
            throw new IllegalStateException("Cannot subscribe to " + sub + " event!");
        }
        
        LOGGER.debug("Subscribing to Event: " + sub.name());
        
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PROP_CMD, CMD_SUBSCRIBE);
        jsonObject.addProperty(PROP_EVT, sub.name());

        pipe.send(OpCode.FRAME, jsonObject, callback);
    }

    public PipeStatus getStatus() {
        if (pipe == null) {
            return PipeStatus.UNINITIALIZED;
        }
        return pipe.getStatus();
    }

    @Override
    public void close() {
        if (getStatus() != PipeStatus.CONNECTED) {
            LOGGER.debug("Client is not connected, nothing to close");
            return;
        }
        
        try {
            pipe.close();
        } catch (IOException e) {
            LOGGER.debug("Failed to close pipe", e);
        }
    }

    public DiscordBuild getDiscordBuild() {
        if (pipe == null) {
            return null;
        }
        return pipe.getDiscordBuild();
    }

    public enum Event {
        NULL(false),
        READY(false),
        ERROR(false),
        ACTIVITY_JOIN(true),
        ACTIVITY_SPECTATE(true),
        ACTIVITY_JOIN_REQUEST(true),
        UNKNOWN(false);
        
        private final boolean subscribable;
        
        Event(boolean subscribable) {
            this.subscribable = subscribable;
        }
        
        public boolean isSubscribable() {
            return subscribable;
        }
        
        static Event of(String str) {
            if (str == null) {
                return NULL;
            }
            
            for (Event s : Event.values()) {
                if (s != UNKNOWN && s.name().equalsIgnoreCase(str)) {
                    return s;
                }
            }
            
            return UNKNOWN;
        }
    }
    
    // ✅ REFACTORED: Giảm từ 189 → ~30 complexity
    private void startReading() {
        readThread = new Thread(() -> {
            try {
                Packet packet;
                while ((packet = pipe.read()).getOp() != OpCode.CLOSE) {
                    handlePacket(packet);
                }
                
                handleDisconnect(packet);
                
            } catch (IOException ex) {
                handleReadError(ex);
            }
        });

        LOGGER.debug("Starting IPCClient reading thread!");
        readThread.start();
    }
    
    // ✅ Extract method: Handle single packet
    private void handlePacket(Packet packet) {
        JsonObject json = packet.getJson();
        if (json == null) return;

        Event event = Event.of(json.has(PROP_EVT) ? json.get(PROP_EVT).getAsString() : null);
        String nonce = json.has(PROP_NONCE) ? json.get(PROP_NONCE).getAsString() : null;

        switch (event) {
            case NULL:
                handleNullEvent(nonce, packet);
                break;

            case ERROR:
                handleErrorEvent(nonce, json);
                break;
                
            case ACTIVITY_JOIN:
            case ACTIVITY_SPECTATE:
            case ACTIVITY_JOIN_REQUEST:
                handleActivityEvent(event);
                break;

            case UNKNOWN:
                handleUnknownEvent(json);
                break;
                
            default:
                break;
        }
        
        // ✅ Handle dispatch events
        if (listener != null && json.has(PROP_CMD) && CMD_DISPATCH.equals(json.get(PROP_CMD).getAsString())) {
            handleDispatchEvent(json);
        }
    }
    
    private void handleNullEvent(String nonce, Packet packet) {
        if (nonce != null && callbacks.containsKey(nonce)) {
            callbacks.remove(nonce).succeed(packet);
        }
    }
    
    private void handleErrorEvent(String nonce, JsonObject json) {
        if (nonce != null && callbacks.containsKey(nonce)) {
            JsonObject data = json.getAsJsonObject(PROP_DATA);
            String errorMessage = data != null && data.has(PROP_MESSAGE) 
                ? data.get(PROP_MESSAGE).getAsString() 
                : null;
            callbacks.remove(nonce).fail(errorMessage);
        }
    }
    
    private void handleActivityEvent(Event event) {
        LOGGER.debug("Reading thread received a '" + event.name().toLowerCase() + "' event.");
    }
    
    private void handleUnknownEvent(JsonObject json) {
        if (json.has(PROP_EVT)) {
            LOGGER.debug("Reading thread encountered an event with an unknown type: " + json.get(PROP_EVT).getAsString());
        }
    }
    
    private void handleDispatchEvent(JsonObject json) {
        try {
            JsonObject data = json.getAsJsonObject(PROP_DATA);
            if (data == null) return;
            
            Event event = Event.of(json.get(PROP_EVT).getAsString());
            
            switch (event) {
                case ACTIVITY_JOIN:
                    handleActivityJoin(data);
                    break;

                case ACTIVITY_SPECTATE:
                    handleActivitySpectate(data);
                    break;

                case ACTIVITY_JOIN_REQUEST:
                    handleActivityJoinRequest(data);
                    break;
                    
                default:
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Exception when handling event: ", e);
        }
    }
    
    private void handleActivityJoin(JsonObject data) {
        if (data.has(PROP_SECRET)) {
            listener.onActivityJoin(this, data.get(PROP_SECRET).getAsString());
        }
    }
    
    private void handleActivitySpectate(JsonObject data) {
        if (data.has(PROP_SECRET)) {
            listener.onActivitySpectate(this, data.get(PROP_SECRET).getAsString());
        }
    }
    
    private void handleActivityJoinRequest(JsonObject data) {
        JsonObject userObject = data.getAsJsonObject(PROP_USER);
        if (userObject == null) return;
        
        User user = new User(
            userObject.get(PROP_USERNAME).getAsString(),
            userObject.get(PROP_DISCRIMINATOR).getAsString(),
            Long.parseLong(userObject.get(PROP_ID).getAsString()),
            userObject.has(PROP_AVATAR) ? userObject.get(PROP_AVATAR).getAsString() : null
        );
        
        String secret = data.has(PROP_SECRET) ? data.get(PROP_SECRET).getAsString() : null;
        listener.onActivityJoinRequest(this, secret, user);
    }
    
    private void handleDisconnect(Packet packet) {
        pipe.setStatus(PipeStatus.DISCONNECTED);
        
        if (listener != null) {
            listener.onClose(this, packet.getJson());
        }
    }
    
    private void handleReadError(IOException ex) {
        LOGGER.error("Reading thread encountered an IOException", ex);

        pipe.setStatus(PipeStatus.DISCONNECTED);
        
        if (listener != null) {
            listener.onDisconnect(this, ex);
        }
    }
    
    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }
}