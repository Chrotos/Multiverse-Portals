package com.onarandombox.MultiversePortals;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Type;
import org.bukkit.util.Vector;

import com.fernferret.allpay.GenericBank;
import com.onarandombox.MultiversePortals.utils.MultiverseRegion;
import com.onarandombox.MultiversePortals.utils.PortalManager;
//import com.sk89q.worldedit.IncompleteRegionException;
//import com.sk89q.worldedit.LocalSession;
//import com.sk89q.worldedit.bukkit.WorldEditAPI;
//import com.sk89q.worldedit.regions.Region;

public class PortalPlayerSession {
    private MultiversePortals plugin;
    private Player player;

    private MVPortal portalSelection = null;
    private MVPortal standingIn = null;
    private boolean debugMode;
    private boolean staleLocation;
    private boolean hasMovedOutOfPortal = true;
    private Location loc;
    private Vector rightClick;
    private Vector leftClick;

    public PortalPlayerSession(MultiversePortals plugin, Player p) {
        this.plugin = plugin;
        this.player = p;
        this.setLocation(this.player.getLocation());
    }

    public boolean selectPortal(MVPortal portal) {
        this.portalSelection = portal;
        return true;
    }

    public MVPortal getSelectedPortal() {
        return this.portalSelection;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (this.debugMode) {
            this.player.sendMessage("Portal debug mode " + ChatColor.GREEN + "ENABLED");
            this.player.sendMessage("Use " + ChatColor.DARK_AQUA + "/mvp debug" + ChatColor.WHITE + " to disable.");
        } else {
            this.player.sendMessage("Portal debug mode " + ChatColor.RED + "DISABLED");
        }
    }

    public boolean isDebugModeOn() {
        return this.debugMode;
    }

    public void setStaleLocation(boolean active) {
        this.staleLocation = active;
    }

    public boolean isStaleLocation() {
        return this.staleLocation;
    }

    private void setLocation(Location loc) {
        this.loc = loc;
        this.setStandinginLocation();
    }

    private void setStandinginLocation() {
        if (this.standingIn == null) {
            this.standingIn = this.plugin.getPortalManager().isPortal(this.player, this.loc);
        } else if (this.plugin.getPortalManager().isPortal(this.player, this.loc) == null) {
            this.hasMovedOutOfPortal = true;
            this.standingIn = null;
        } else {
            this.hasMovedOutOfPortal = false;
        }
    }

    public boolean doTeleportPlayer(Type eventType) {
        if (eventType == Type.PLAYER_MOVE && this.player.isInsideVehicle()) {
            return false;
        }
        return this.hasMovedOutOfPortal == true && this.standingIn != null;
    }

    public Location getLocation() {
        return this.loc;
    }

    public void setStaleLocation(Location loc, Type moveType) {
        if (this.player.isInsideVehicle() && moveType != Type.VEHICLE_MOVE) {
            return;
        }
        if (this.getLocation().getBlockX() == loc.getBlockX() && this.getLocation().getBlockY() == loc.getBlockY() && this.getLocation().getBlockZ() == loc.getBlockZ()) {
            this.setStaleLocation(true);
        } else {
            this.setLocation(loc); // Update the Players Session to the new Location.
            this.setStaleLocation(false);
        }

    }
    
    public void setLeftClickSelection(Vector v) {
        this.leftClick = v;
    }
    
    public void setRightClickSelection(Vector v) {
        this.rightClick = v;
    }

    public MultiverseRegion getSelectedRegion() {
        // Did not find WE
        MultiverseRegion r = null;
        if (this.plugin.getWEAPI() != null) {
            //this.player.sendMessage("Did not find the WorldEdit API...");
            //this.player.sendMessage("It is currently required to use Multiverse-Portals.");
            // BEYAHH NOT ANYMORE
            //return null;
            try {
                // GAH this looks SO ugly keeping no imports :( see if I can find a workaround
                r = new MultiverseRegion(this.plugin.getWEAPI().getSession(this.player).getSelection(this.plugin.getWEAPI().getSession(this.player).getSelectionWorld()).getMinimumPoint(),
                        this.plugin.getWEAPI().getSession(this.player).getSelection(this.plugin.getWEAPI().getSession(this.player).getSelectionWorld()).getMaximumPoint().add(1, 1, 1), 
                        this.plugin.getCore().getMVWorld(this.player.getWorld().getName()));
            } catch (Exception e) {
                this.player.sendMessage("You haven't finished your selection.");
                return null;
            }
            return r;
        }
        // They're using our crappy selection:
        if(this.leftClick == null) {
            this.player.sendMessage("You need to LEFT click on a block with your wand(INSERT WAND NAME HERE)!");
            return null;
        }
        if(this.rightClick == null) {
            this.player.sendMessage("You need to RIGHT click on a block with your wand(INSERT WAND NAME HERE)!");
            return null;
        }
        return new MultiverseRegion(this.leftClick, this.rightClick, this.plugin.getCore().getMVWorld(this.player.getWorld().getName()));
    }

    public MVPortal getStandingInPortal() {
        return this.standingIn;
    }

    /**
     * This method should be called every time a player telports to a portal.
     * 
     * @param location
     */
    public void playerDidTeleport(Location location) {
        PortalManager pm = this.plugin.getPortalManager();
        if (pm.isPortal(this.player, location) != null) {
            this.hasMovedOutOfPortal = false;
            return;
        }
        this.hasMovedOutOfPortal = true;
    }

    public boolean hasMovedOutOfPortal() {
        return this.hasMovedOutOfPortal;
    }

    public boolean showDebugInfo() {
        if (!this.isDebugModeOn()) {
            return false;
        }

        if (this.standingIn == null) {
            return false;
        }

        player.sendMessage("You are currently standing in " + ChatColor.DARK_AQUA + this.standingIn.getName());
        player.sendMessage("It's coords are: " + ChatColor.GOLD + this.standingIn.getLocation().toString());
        player.sendMessage("It will take you to a location of type: " + ChatColor.AQUA + this.standingIn.getDestination().getType());
        player.sendMessage("The destination's name is: " + ChatColor.GREEN + this.standingIn.getDestination().getName());
        
        player.sendMessage("More details for you: " + ChatColor.GREEN + this.standingIn.getDestination());
        if(this.standingIn.getPrice() > 0) {
            GenericBank bank = this.plugin.getCore().getBank();
            player.sendMessage("Price: " + ChatColor.GREEN + bank.getFormattedAmount(this.standingIn.getPrice(), this.standingIn.getCurrency()));
        } else {
            player.sendMessage("Price: " + ChatColor.GREEN + "FREE!");
        }
        return true;
    }
}