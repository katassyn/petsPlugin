package pl.yourserver;

import org.bukkit.entity.Player;
// Fixed imports to match actual package structure

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PetDataManager {

    private final PetPlugin plugin;

    public PetDataManager(PetPlugin plugin) {
        this.plugin = plugin;
    }

    // Zapisz peta asynchronicznie
    public CompletableFuture<Void> savePetAsync(Pet pet) {
        return CompletableFuture.runAsync(() -> {
            plugin.getDatabaseManager().savePet(pet);
        });
    }

    // Zapisz peta synchronicznie
    public void savePet(Pet pet) {
        plugin.getDatabaseManager().savePet(pet);
    }

    // Wczytaj pety gracza
    public List<Pet> loadPlayerPets(Player player) {
        return plugin.getDatabaseManager().loadPlayerPets(player.getUniqueId());
    }

    // Wczytaj pety gracza asynchronicznie
    public CompletableFuture<List<Pet>> loadPlayerPetsAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            return plugin.getDatabaseManager().loadPlayerPets(player.getUniqueId());
        });
    }

    // Zapisz wszystkie pety gracza
    public void savePlayerPets(Player player) {
        List<Pet> pets = plugin.getPetManager().getPlayerPets(player);
        for (Pet pet : pets) {
            savePet(pet);
        }
    }

    // Zapisz wszystkie pety gracza asynchronicznie
    public CompletableFuture<Void> savePlayerPetsAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            savePlayerPets(player);
        });
    }

    // Usuń peta
    public void deletePet(Pet pet) {
        plugin.getDatabaseManager().deletePet(pet.getUuid());
    }

    // Usuń peta asynchronicznie
    public CompletableFuture<Void> deletePetAsync(Pet pet) {
        return CompletableFuture.runAsync(() -> {
            plugin.getDatabaseManager().deletePet(pet.getUuid());
        });
    }

    // Zapisz backpack gracza (dla Donkey peta)
    public void saveBackpack(Player player, String inventoryData) {
        plugin.getDatabaseManager().saveBackpack(player.getUniqueId(), inventoryData);
    }

    // Wczytaj backpack gracza
    public String loadBackpack(Player player) {
        return plugin.getDatabaseManager().loadBackpack(player.getUniqueId());
    }

    // Aktualizuj statystyki peta
    public void updatePetStat(Pet pet, String stat, int increment) {
        plugin.getDatabaseManager().updatePetStats(pet.getUuid(), stat, increment);
    }

    // Pobierz top pety
    public CompletableFuture<List<Pet>> getTopPetsAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return plugin.getDatabaseManager().getTopPets(limit);
        });
    }
}