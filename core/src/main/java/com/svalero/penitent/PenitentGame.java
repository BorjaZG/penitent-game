package com.svalero.penitent;

import com.badlogic.gdx.Game;

public class PenitentGame extends Game {

    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;

    // Slot activo durante toda la partida - persiste entre pantallas
    private int activeSlot = 0;

    @Override
    public void create() {
        FontManager.load();
        showMenu();
    }

    public void showMenu() {
        setScreen(new MenuScreen(this));
    }

    /** Llamado desde MenuScreen al elegir slot para nueva partida */
    public void startNewGame(int slot) {
        activeSlot = slot;
        setScreen(new GameScreen(this, null));
    }

    /** Llamado desde MenuScreen al cargar partida existente */
    public void continueGame(int slot) {
        activeSlot = slot;
        SaveManager.SaveData data = SaveManager.load(slot);
        setScreen(new GameScreen(this, data));
    }

    /** Llamado desde GameScreen en Game Over si hay guardado */
    public void reloadActiveSlot() {
        SaveManager.SaveData data = SaveManager.load(activeSlot);
        setScreen(new GameScreen(this, data));
    }

    public int  getActiveSlot()   { return activeSlot; }
    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume()   { return sfxVolume; }

    public void setMusicVolume(float v) { musicVolume = v; }
    public void setSfxVolume(float v)   { sfxVolume   = v; }

    @Override
    public void dispose() {
        FontManager.dispose();
        super.dispose();
    }
}
