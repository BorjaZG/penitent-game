package com.svalero.penitent;

import com.badlogic.gdx.Game;

public class PenitentGame extends Game {

    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;

    private SoundManager activeSoundManager;

    @Override
    public void create() {
        FontManager.load();   // ← carga Cinzel antes de cualquier pantalla
        showMenu();
    }

    public void showMenu() {
        setScreen(new MenuScreen(this));
    }

    public void startNewGame() {
        setScreen(new GameScreen(this, null));
    }

    public void continueGame(int slot) {
        SaveManager.SaveData data = SaveManager.load(slot);
        setScreen(new GameScreen(this, data));
    }

    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume()   { return sfxVolume; }

    public void setMusicVolume(float v) {
        musicVolume = v;
        if (activeSoundManager != null) activeSoundManager.setMusicVolume(v);
    }

    public void setSfxVolume(float v) {
        sfxVolume = v;
        if (activeSoundManager != null) activeSoundManager.setSfxVolume(v);
    }

    public void registerSoundManager(SoundManager sm) {
        this.activeSoundManager = sm;
    }

    @Override
    public void dispose() {
        FontManager.dispose();
        super.dispose();
    }
}
