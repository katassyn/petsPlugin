# MythicMobs Integration dla PetPlugin

## Instalacja

1. **Zainstaluj MythicMobs** na swoim serwerze
2. **Skopiuj plik konfiguracyjny**:
   ```
   mythicmobs-configs/PetMobs.yml -> plugins/MythicMobs/Mobs/PetMobs.yml
   ```
3. **Zrestartuj serwer** lub użyj `/mm reload`

## Obsługiwane duże pety

Plugin automatycznie wykryje czy MythicMobs jest zainstalowany i spróbuje użyć go dla następujących dużych mobów:

- **Ender Dragon** (PetEnderDragon)
- **Wither** (PetWither)
- **Giant** (PetGiant)
- **Warden** (PetWarden)

## Konfiguracja rozmiaru

W pliku `PetMobs.yml` możesz dostosować rozmiar petów używając opcji `Scale`:

```yaml
PetEnderDragon:
  Type: ENDER_DRAGON
  # Ustaw rozmiar (0.1 = 10% oryginalnego rozmiaru)
  Scale: 0.3
```

**Uwaga**: Opcja `Scale` jest dostępna w nowszych wersjach MythicMobs. Jeśli nie działa, usuń linię `Scale` z konfiguracji.

## Fallback System

Jeśli MythicMobs nie jest zainstalowany lub konfiguracja jest niepoprawna, plugin automatycznie wróci do standardowego spawnu z oryginalnymi mobami.

## Debugowanie

Sprawdź logi serwera pod kątem komunikatów:
- `Successfully spawned MythicMob pet: [typ]`
- `Failed to spawn MythicMob pet: [typ]`

Jeśli spawning się nie powiedzie, sprawdź:
1. Czy MythicMobs jest zainstalowany
2. Czy plik `PetMobs.yml` znajduje się w `plugins/MythicMobs/Mobs/`
3. Czy nie ma błędów składni w YAML
4. Użyj `/mm reload` po zmianach

## Dodatkowe opcje

Możesz dostosować każdego peta w pliku `PetMobs.yml`:

- **Health**: Punkty życia
- **Damage**: Obrażenia (ustaw na 0 dla petów)
- **Options**: Różne opcje zachowania
- **Scale**: Rozmiar (jeśli obsługiwane)

## Wsparcie

Jeśli masz problemy z konfiguracją MythicMobs, sprawdź:
- [Dokumentacja MythicMobs](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis)
- Logi serwera w `logs/latest.log`