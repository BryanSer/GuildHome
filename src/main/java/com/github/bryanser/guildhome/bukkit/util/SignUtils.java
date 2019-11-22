package com.github.bryanser.guildhome.bukkit.util;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.github.bryanser.guildhome.bukkit.BukkitMain;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Bryan_lzh
 */
public class SignUtils {

    private static SignUtils SU = null;

    // private Set<String> Listener = new HashSet<>();
    private Map<String, String> Listener = new HashMap<>();

    private void changeFakeBlock(Player p, String[] lines, Location loc) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        PacketContainer blockChange = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        try {
            blockChange.getBlockPositionModifier().write(0, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            blockChange.getBlockData().write(0, WrappedBlockData.createData(Material.SIGN_POST, 0));
        } catch (Throwable e) {
            e.printStackTrace();
            blockChange.getIntegers().write(0, loc.getBlockX());
            blockChange.getIntegers().write(1, loc.getBlockY());
            blockChange.getIntegers().write(2, loc.getBlockZ());
            blockChange.getBlocks().write(0, Material.SIGN_POST);
        }
        //WrappedBlockData
        try {
            manager.sendServerPacket(p, blockChange);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SignUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        PacketContainer updateSign = manager.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);
        try {
            updateSign.getBlockPositionModifier().write(0, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            updateSign.getIntegers().write(0, 9);
            NbtCompound nc = NbtFactory.ofCompound("tag");
            nc.put("Text1",  WrappedChatComponent.fromText(lines[0]).getJson());
            nc.put("Text2",  WrappedChatComponent.fromText(lines[1]).getJson());
            nc.put("Text3",  WrappedChatComponent.fromText(lines[2]).getJson());
            nc.put("Text4",  WrappedChatComponent.fromText(lines[3]).getJson());
            updateSign.getNbtModifier().write(0, nc);
//            updateSign.getChatComponentArrays().write(0, new WrappedChatComponent[]{
//                WrappedChatComponent.fromText(lines[0]),
//                WrappedChatComponent.fromText(lines[1]),
//                WrappedChatComponent.fromText(lines[2]),
//                WrappedChatComponent.fromText(lines[3])
//            });
        } catch (Throwable e) {
            e.printStackTrace();
            updateSign.getIntegers().write(0, loc.getBlockX());
            updateSign.getIntegers().write(1, loc.getBlockY());
            updateSign.getIntegers().write(2, loc.getBlockZ());
            updateSign.getStringArrays().write(0, lines);
        }
        try {
            manager.sendServerPacket(p, updateSign);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SignUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void sendSignRequest(Player p, String[] msg, String id) {
        Location loc = p.getLocation();
        loc.setY(0);
        changeFakeBlock(p, msg, loc);
        Listener.put(p.getName(), id);
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        PacketType pt;
        try {
            pt = PacketType.Play.Server.OPEN_SIGN_EDITOR;
        } catch (Throwable e) {
            pt = PacketType.Play.Server.OPEN_SIGN_ENTITY;
        }
        PacketContainer pc = new PacketContainer(pt);
        try {
            pc.getBlockPositionModifier().write(0, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        } catch (Throwable e) {
            e.printStackTrace();
            pc.getIntegers().write(0, loc.getBlockX());
            pc.getIntegers().write(1, loc.getBlockY());
            pc.getIntegers().write(2, loc.getBlockZ());
        }
        try {
            pm.sendServerPacket(p, pc, false);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SignUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Map<String, Map.Entry<String, BiConsumer<Player, String[]>>> Callbacks = new HashMap<>();

    public void sendSignRequest(Player p, String[] msg, BiConsumer<Player, String[]> callback) {
        String id = String.valueOf(System.currentTimeMillis());
        Callbacks.put(p.getName(), new AbstractMap.SimpleEntry<>(id, callback));
        sendSignRequest(p, msg, id);
    }

    private SignUtils() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onSign(WriteSignEvent evt) {
                if (Callbacks.containsKey(evt.getPlayer().getName())) {
                    Map.Entry<String, BiConsumer<Player, String[]>> v = Callbacks.get(evt.getPlayer().getName());
                    if (evt.getID().equals(v.getKey())) {
                        v.getValue().accept(evt.getPlayer(), evt.getWrite());
                        v.setValue((t, u) -> {//debug
                        });
                    }
                    Callbacks.remove(evt.getPlayer().getName());
                }
            }
        }, BukkitMain.Companion.getPlugin());
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new PacketAdapter(PacketAdapter
                .params()
                .plugin(BukkitMain.Companion.getPlugin())
                .clientSide()
                .listenerPriority(ListenerPriority.LOWEST)
                .gamePhase(GamePhase.PLAYING)
                .types(PacketType.Play.Client.UPDATE_SIGN)) {
            @Override
            public void onPacketReceiving(PacketEvent e) {
                Player p = e.getPlayer();
                if (Listener.containsKey(p.getName())) {
                    final String id = Listener.get(p.getName());
                    Listener.remove(p.getName());
                    String[] read = e.getPacket().getStringArrays().read(0);
                    WriteSignEvent wse = new WriteSignEvent(p, read, id);
                    Bukkit.getPluginManager().callEvent(wse);
                    e.setCancelled(true);
                }
            }

        });
    }

    public static SignUtils getSignUtils() throws NullPointerException {
        if (SU == null) {
            if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                throw new NullPointerException("§c找不到插件ProtocolLib");
            }
            SU = new SignUtils();
        }
        return SU;
    }

}
