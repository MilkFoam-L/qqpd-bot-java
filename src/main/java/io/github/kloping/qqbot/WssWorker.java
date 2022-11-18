package io.github.kloping.qqbot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.github.kloping.MySpringTool.annotations.AutoStand;
import io.github.kloping.MySpringTool.annotations.Entity;
import io.github.kloping.MySpringTool.interfaces.Logger;
import io.github.kloping.MySpringTool.interfaces.component.ContextManager;
import io.github.kloping.date.FrameUtils;
import io.github.kloping.qqbot.api.message.Message;
import io.github.kloping.qqbot.entitys.Pack;
import io.github.kloping.qqbot.http.BotBase;
import io.github.kloping.qqbot.interfaces.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.kloping.qqbot.Starter.*;

/**
 * @author github.kloping
 */
@Entity
public class WssWorker implements Runnable {
    private WebSocketClient webSocket;
    private boolean isFirst = true;
    public Pack jumpPack = new Pack();
    public long heartbeatInterval;
    public int newstId = 1;
    public Pack<JSONObject> authPack = null;
    public String sessionId = "";
    private Boolean isReconnect = false;

    @AutoStand
    ContextManager contextManager;

    public WssWorker() {
    }

    @AutoStand
    private BotBase botBase;

    private Logger logger;

    @Override
    public void run() {
        init();
        URI u = null;
        try {
            u = new URI(botBase.gateway().getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.log("ws url:" + u);
        webSocket = new WebSocketClient(u) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                logger.info("wss opened");
            }

            @Override
            public void onMessage(String s) {
                if (isFirst) {
                    logger.info("鉴权");
                    webSocket.send(JSON.toJSONString(authPack));
                    isFirst = false;
                    Pack<JSONObject> pack = JSON.parseObject(s).toJavaObject(Pack.class);
                    heartbeatInterval = pack.getD().getLong("heartbeat_interval");
                    jumpPack.setOp(1);
                    FrameUtils.SERVICE.scheduleAtFixedRate(() -> {
                                if (newstId != -1) {
                                    jumpPack.setD(newstId);
                                }
                                webSocket.send(JSON.toJSONString(jumpPack));
                            }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
                } else {
                    Pack<JSONObject> pack = JSON.parseObject(s, Pack.class);
                    logger.log("receive " + pack);
                    if (pack.getS() != null) {
                        newstId = pack.getS().intValue();
                    }
                    if (pack.getOp().equals(0)) {
                        try {
                            sessionId = pack.getD().getString("session_id");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (pack.getOp().equals(7)) {
                        logger.info("断线重连");
                        authPack.setOp(6);
                        authPack.getD().put("session_id", sessionId);
                        authPack.getD().put("seq", newstId);
                        webSocket.send(JSON.toJSONString(authPack));
                    }
                    if (onPackReceive != null) {
                        try {
                            onPackReceive.onReceive(pack);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    onReceive(pack);
                }
            }

            @Override
            public void send(String text) throws NotYetConnectedException {
                super.send(text);
                logger.log("wss send: " + text);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                try {
                    logger.waring("wss closed");
                    for (OnCloseListener onCloseListener : closeListeners) {
                        onCloseListener.onReceive();
                    }
                } finally {
                    if (isReconnect) {
                        authPack.setOp(6);
                        authPack.getD().put("session_id", sessionId);
                        authPack.getD().put("seq", newstId);
                        webSocket.send(JSON.toJSONString(authPack));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                logger.error("wss error");
                e.printStackTrace();
            }
        };
        webSocket.run();
    }

    private void onReceive(Pack<JSONObject> pack) {
        String t = pack.getT();
        if (t == null) return;
        JSONObject jo = pack.getD();
        Message m = jo.toJavaObject(Message.class);
        switch (t) {
            case "MESSAGE_CREATE":
                Iterator<OnMessageListener> iterator0 = messageListeners.iterator();
                while (iterator0.hasNext()) {
                    iterator0.next().onMessage(m);
                }
                return;
            case "AT_MESSAGE_CREATE":
                Iterator<OnAtMessageListener> iterator1 = atMessageListeners.iterator();
                while (iterator1.hasNext()) {
                    iterator1.next().onMessage(m);
                }
                return;
            case "PUBLIC_MESSAGE_DELETE":
            case "MESSAGE_DELETE":
                Iterator<OnMessageDeleteListener> iterator2 = messageDeleteListeners.iterator();
                while (iterator2.hasNext()) {
                    iterator2.next().onDelete(m);
                }
            default:
                Iterator<OnOtherEventListener> iterator = otherEventListeners.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onEvent(t, m);
                }
                break;
        }
    }

    public List<OnMessageListener> messageListeners = new ArrayList<>();
    public List<OnAtMessageListener> atMessageListeners = new ArrayList<>();
    public List<OnCloseListener> closeListeners = new ArrayList<>();
    public List<OnOtherEventListener> otherEventListeners = new ArrayList<>();
    public List<OnMessageDeleteListener> messageDeleteListeners = new ArrayList<>();

    private OnPackReceive onPackReceive;

    public void setOnPackReceive(OnPackReceive onPackReceive) {
        this.onPackReceive = onPackReceive;
    }

    private boolean uninitialized = true;

    private void init() {
        if (!uninitialized)
            return;
        uninitialized = false;
        authPack = new Pack();
        authPack.setOp(2);
        JSONObject jo = new JSONObject();
        jo.put(TOKEN_ID, contextManager.getContextEntity(String.class, AUTH_ID));
        jo.put(INTENTS_ID, contextManager.getContextEntity(String.class, INTENTS_ID));
        jo.put(SHARD_ID, contextManager.getContextEntity(Integer[].class, SHARD_ID));
        jo.put(PROPERTIES_ID, new Object());
        authPack.setD(jo);
        logger = contextManager.getContextEntity(Logger.class);
    }

    public Boolean getReconnect() {
        return isReconnect;
    }

    public void setReconnect(Boolean reconnect) {
        isReconnect = reconnect;
    }
}
