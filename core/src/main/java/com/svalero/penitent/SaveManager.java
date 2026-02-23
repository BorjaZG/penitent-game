package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";

    public static class SaveData {
        public int   map        = 1;
        public float playerX    = 200f;
        public float playerY    = 32f;
        public int   health     = 3;
    }

    /** Guarda la partida en el archivo local */
    public static void save(int map, float x, float y, int health) {
        SaveData data = new SaveData();
        data.map     = map;
        data.playerX = x;
        data.playerY = y;
        data.health  = health;

        Json json = new Json();
        FileHandle file = Gdx.files.local(SAVE_FILE);
        file.writeString(json.toJson(data), false);
    }

    /** Carga la partida guardada, o null si no existe */
    public static SaveData load() {
        FileHandle file = Gdx.files.local(SAVE_FILE);
        if (!file.exists()) return null;
        try {
            Json json = new Json();
            return json.fromJson(SaveData.class, file.readString());
        } catch (Exception e) {
            return null;
        }
    }

    /** True si existe una partida guardada */
    public static boolean hasSave() {
        return Gdx.files.local(SAVE_FILE).exists();
    }

    /** Borra la partida guardada */
    public static void deleteSave() {
        FileHandle file = Gdx.files.local(SAVE_FILE);
        if (file.exists()) file.delete();
    }
}
