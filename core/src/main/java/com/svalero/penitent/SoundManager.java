package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class SoundManager {

    // Música de fondo
    private Music musicMap1;
    private Music musicMap2;
    private Music currentMusic;
    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;

    // Sonidos del jugador
    private Sound sfxAttack;
    private Sound sfxJump;
    private Sound sfxLand;
    private Sound sfxDash;
    private Sound sfxPlayerDeath;
    private Sound sfxPlayerStep;

    // Sonidos del enemigo
    private Sound sfxEnemyDeath;
    private Sound sfxEnemyAttack;
    private Sound sfxEnemyStep;
    private Sound sfxHit;

    // Control de pasos (para no reproducirlos cada frame)
    private float playerStepTimer = 0f;
    private float enemyStepTimer  = 0f;
    private static final float STEP_INTERVAL = 0.35f;

    // Control de aterrizaje (evitar spam)
    private boolean wasOnGround = false;

    public SoundManager() {
        // Música
        musicMap1 = Gdx.audio.newMusic(Gdx.files.internal("audio/music.ogg"));
        musicMap2 = musicMap1; // misma referencia, misma música
        musicMap1.setLooping(true);
        musicMap1.setVolume(musicVolume);

        // Sonidos jugador
        sfxAttack      = Gdx.audio.newSound(Gdx.files.internal("audio/attack.ogg"));
        sfxJump        = Gdx.audio.newSound(Gdx.files.internal("audio/jump.ogg"));
        sfxLand        = Gdx.audio.newSound(Gdx.files.internal("audio/land.ogg"));
        sfxDash        = Gdx.audio.newSound(Gdx.files.internal("audio/dash.ogg"));
        sfxPlayerDeath = Gdx.audio.newSound(Gdx.files.internal("audio/player_death.ogg"));
        sfxPlayerStep  = Gdx.audio.newSound(Gdx.files.internal("audio/player_step.ogg"));

        // Sonidos enemigo
        sfxEnemyDeath  = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_death.ogg"));
        sfxEnemyAttack = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_attack.ogg"));
        sfxEnemyStep   = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_step.ogg"));
        sfxHit         = Gdx.audio.newSound(Gdx.files.internal("audio/hit.ogg"));
    }

    // --- Música ---
    public void playMap1Music() { switchMusic(musicMap1); }
    public void playMap2Music() { switchMusic(musicMap2); }

    private void switchMusic(Music newMusic) {
        if (currentMusic == newMusic && currentMusic.isPlaying()) return;
        if (currentMusic != null && currentMusic != newMusic) currentMusic.stop();
        currentMusic = newMusic;
        if (!currentMusic.isPlaying()) currentMusic.play();
    }

    public void stopMusic() {
        if (currentMusic != null) currentMusic.stop();
    }

    // --- Jugador ---
    public void playAttack()      { sfxAttack.play(0.8f); }
    public void playJump()        { sfxJump.play(0.7f); }
    public void playLand()        { sfxLand.play(0.6f); }
    public void playDash()        { sfxDash.play(0.7f); }
    public void playPlayerDeath() { sfxPlayerDeath.play(1.0f); }
    public void playHit()         { sfxHit.play(0.9f); }

    // --- Enemigo ---
    public void playEnemyDeath()  { sfxEnemyDeath.play(0.8f); }
    public void playEnemyAttack() { sfxEnemyAttack.play(0.6f); }

    /**
     * Llamado cada frame con el estado del jugador.
     * Gestiona pasos y aterrizaje automáticamente.
     */
    public void updatePlayerSounds(float dt, boolean isRunning, boolean onGround,
                                   boolean wasOnGroundPrev) {
        // Aterrizaje
        if (onGround && !wasOnGroundPrev) playLand();

        // Pasos al correr
        if (isRunning && onGround) {
            playerStepTimer -= dt;
            if (playerStepTimer <= 0) {
                sfxPlayerStep.play(0.4f);
                playerStepTimer = STEP_INTERVAL;
            }
        } else {
            playerStepTimer = 0f;
        }
    }

    /**
     * Llamado cada frame por cada enemigo que esté en movimiento.
     */
    public void updateEnemySteps(float dt, boolean isMoving) {
        if (isMoving) {
            enemyStepTimer -= dt;
            if (enemyStepTimer <= 0) {
                sfxEnemyStep.play(0.25f);
                enemyStepTimer = STEP_INTERVAL + 0.05f;
            }
        } else {
            enemyStepTimer = 0f;
        }
    }

    public void setMusicVolume(float v) {
        musicVolume = v;
        if (musicMap1 != null) musicMap1.setVolume(v);
    }

    public void setSfxVolume(float v) {
        sfxVolume = v;
    }

    public void dispose() {
        if (musicMap1 != null) musicMap1.dispose();
        // musicMap2 apunta al mismo objeto que musicMap1, no hacer dispose dos veces
        sfxAttack.dispose();      sfxJump.dispose();
        sfxLand.dispose();        sfxDash.dispose();
        sfxPlayerDeath.dispose(); sfxPlayerStep.dispose();
        sfxEnemyDeath.dispose();  sfxEnemyAttack.dispose();
        sfxEnemyStep.dispose();   sfxHit.dispose();
    }
}
