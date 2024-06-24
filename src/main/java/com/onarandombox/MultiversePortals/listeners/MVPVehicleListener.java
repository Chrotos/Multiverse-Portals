/*
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.
 * Multiverse 2 is licensed under the BSD License.
 * For more information please check the README.md file included
 * with this project
 */

package com.onarandombox.MultiversePortals.listeners;

import java.util.ArrayList;
import java.util.Date;

import com.onarandombox.MultiversePortals.enums.MoveType;
import io.papermc.paper.entity.TeleportFlag;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.destination.InvalidDestination;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.onarandombox.MultiverseCore.api.LocationManipulation;
import com.onarandombox.MultiverseCore.api.SafeTTeleporter;
import com.onarandombox.MultiversePortals.MVPortal;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.MultiversePortals.PortalPlayerSession;
import com.onarandombox.MultiversePortals.destination.PortalDestination;

public class MVPVehicleListener implements Listener {
    private MultiversePortals plugin;
    private LocationManipulation locationManipulation;
    private SafeTTeleporter safeTTeleporter;

    public MVPVehicleListener(MultiversePortals plugin) {
        this.plugin = plugin;
        this.locationManipulation = this.plugin.getCore().getLocationManipulation();
        this.safeTTeleporter = this.plugin.getCore().getSafeTTeleporter();
    }

    @EventHandler
    public void vehicleMove(VehicleMoveEvent event) {
        teleportVehicle(event.getVehicle(), event.getTo());
    }

    public boolean teleportVehicle(Vehicle v, Location to) {
        Vector vehicleVec = v.getVelocity();
        MVPortal portal = this.plugin.getPortalManager().getPortal(to);

        if (portal == null) {
            return false;
        }

        MVDestination d = portal.getDestination();
        if (d == null || d instanceof InvalidDestination) {
            return false;
        }

        // Check the portal's frame.
        if (!portal.isFrameValid(v.getLocation())) {
            return false;
        }

        // 0 Yaw in dest = 0,X
        if (d instanceof PortalDestination pd) {
            // Translate the direction of travel.
            vehicleVec = this.locationManipulation.getTranslatedVector(vehicleVec, pd.getOrientationString());
        }

        for (Entity e : v.getPassengers()) {
            if (e instanceof Player p) {
                PortalPlayerSession ps = this.plugin.getPortalSession(p);
                if (ps.checkAndSendCooldownMessage()) {
                    return false;
                }
                // TODO: Money

                p.setFallDistance(0);

                if (this.safeTTeleporter.safelyTeleport(p, v, d) == TeleportResult.SUCCESS) {
                    ps.playerDidTeleport(to);
                    ps.setTeleportTime(new Date());
                }
            } else if (!portal.getTeleportNonPlayers()) {
                return false;
            }
        }

        Location target = d.getLocation(v);

        // Set the velocity
        // Will set to the destination's velocity if one is present
        // Or
        this.setVehicleVelocity(vehicleVec, d, v);

        if (target == null) {
            return false;
        }

        if (target.getWorld().equals(v.getWorld())) {
            v.teleport(this.safeTTeleporter.getSafeLocation(target), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        } else {
            Location safeLocation = this.safeTTeleporter.getSafeLocation(target);
            ArrayList<Entity> passengers = new ArrayList<>(v.getPassengers());

            v.eject();

            boolean success = v.teleport(safeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

            for (Entity passenger : passengers) {
                if (success) {
                    boolean formerSuccess = success;
                    success = passenger.teleport(safeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

                    if (formerSuccess != success) {
                        v.teleport(to);
                    }
                }

                v.addPassenger(passenger);
            }

            return success;
        }

        return true;
    }

    public void setVehicleVelocity(Vector calculated, MVDestination to, Vehicle newVehicle) {
        // If the destination has a non-zero velocity, use that,
        // otherwise use the existing velocity, because velocities
        // are preserved through portals... duh.
        if (!to.getVelocity().equals(new Vector(0, 0, 0))) {
            newVehicle.setVelocity(to.getVelocity());
        } else {
            newVehicle.setVelocity(calculated);
        }
    }
}
