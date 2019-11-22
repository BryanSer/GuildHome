package com.github.bryanser.guildhome.bukkit.util;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Bryan_lzh
 */
public class WriteSignEvent extends Event {

    private Player pl;
    private String[] wi;
    private String id;

    public WriteSignEvent(Player p, String[] write, String id) {
        this.pl = p;
        this.wi = write;
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public Player getPlayer() {
        return pl;
    }

    public String[] getWrite() {
        return wi;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
