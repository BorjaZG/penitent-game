package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {

    // Hasta 3 slots de guardado
    private static final int MAX_SLOTS = 3;
    private static final String SAVE_PREFIX = "save_slot_";

    public static class SaveData {
        public int    slot    = 0;
        public int    map     = 1;
        public float  playerX = 200f;
        public float  playerY = 32f;
        public int    health  = 3;
        public String zoneName = "Las Entrañas";
        public String timestamp = "";  // cuándo se guardó
    }

    private static String slotFile(int slot) {
        return SAVE_PREFIX + slot + ".json";
    }

    /** Guarda en el slot indicado (0, 1 o 2) */
    public static void save(int slot, int map, float x, float y, int health) {
        SaveData data = new SaveData();
        data.slot     = slot;
        data.map      = map;
        data.playerX  = x;
        data.playerY  = y;
        data.health   = health;
        data.zoneName = getZoneName(map);
        data.timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        Json json = new Json();
        Gdx.files.local(slotFile(slot)).writeString(json.toJson(data), false);
    }

    /** Carga un slot concreto */
    public static SaveData load(int slot) {
        FileHandle file = Gdx.files.local(slotFile(slot));
        if (!file.exists()) return null;
        try {
            return new Json().fromJson(SaveData.class, file.readString());
        } catch (Exception e) {
            return null;
        }
    }

    /** Devuelve todos los slots (null si vacío) */
    public static SaveData[] loadAll() {
        SaveData[] slots = new SaveData[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) slots[i] = load(i);
        return slots;
    }

    /** True si al menos un slot tiene datos */
    public static boolean hasSave() {
        for (int i = 0; i < MAX_SLOTS; i++)
            if (Gdx.files.local(slotFile(i)).exists()) return true;
        return false;
    }

    public static boolean hasSlot(int slot) {
        return Gdx.files.local(slotFile(slot)).exists();
    }

    public static void deleteSlot(int slot) {
        FileHandle f = Gdx.files.local(slotFile(slot));
        if (f.exists()) f.delete();
    }

    public static int getMaxSlots() { return MAX_SLOTS; }

    public static String getZoneName(int map) {
        switch (map) {
            case 1: return "Las Entrañas";
            case 2: return "El Osario";
            default: return "Zona " + map;
        }
    }
}
