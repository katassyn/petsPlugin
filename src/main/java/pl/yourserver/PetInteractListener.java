// PetInteractListener.java  
package pl.yourserver;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
// Fixed imports to match actual package structure

import java.util.UUID;

public class PetInteractListener implements Listener {

    private final PetPlugin plugin;
    private final PetGUI petGUI;

    public PetInteractListener(PetPlugin plugin) {
        this.plugin = plugin;
        this.petGUI = new PetGUI(plugin);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Sprawdź czy to pet
        if (!entity.hasMetadata("pet")) return;

        // Sprawdź czy to pet tego gracza
        String ownerUUID = entity.getMetadata("petOwner").get(0).asString();
        if (!ownerUUID.equals(player.getUniqueId().toString())) {
            return;
        }

        event.setCancelled(true);

        // Pobierz peta
        String petUUID = entity.getMetadata("petUUID").get(0).asString();
        Pet pet = null;

        for (Pet p : plugin.getPetManager().getPlayerPets(player)) {
            if (p.getUuid().toString().equals(petUUID)) {
                pet = p;
                break;
            }
        }

        if (pet != null) {
            // Otwórz menu peta
            petGUI.openPetMenu(player, pet);
        }
    }
}