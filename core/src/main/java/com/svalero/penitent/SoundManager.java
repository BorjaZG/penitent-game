package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class SoundManager {

    private Music musicMap1;
    private Music musicMap2;
    private Music musicMenu;
    private Music currentMusic;
    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;

    private Sound sfxAttack;
    private Sound sfxJump;
    private Sound sfxLand;
    private Sound sfxDash;
    private Sound sfxPlayerDeath;
    private Sound sfxPlayerStep;

    private Sound sfxEnemyDeath;
    private Sound sfxEnemyAttack;
    private Sound sfxEnemyStep;
    private Sound sfxHit;

    private float playerStepTimer = 0f;
    private float enemyStepTimer  = 0f;
    private static final float STEP_INTERVAL = 0.35f;

    public SoundManager() {
        musicMap1 = Gdx.audio.newMusic(Gdx.files.internal("audio/music.ogg"));
        musicMap2 = musicMap1;
        musicMenu = Gdx.audio.newMusic(Gdx.files.internal("audio/menu_music.ogg"));
        musicMenu.setLooping(true);
        musicMenu.setVolume(musicVolume);
        musicMap1.setLooping(true);
        musicMap1.setVolume(musicVolume);

        sfxAttack      = Gdx.audio.newSound(Gdx.files.internal("audio/attack.ogg"));
        sfxJump        = Gdx.audio.newSound(Gdx.files.internal("audio/jump.ogg"));
        sfxLand        = Gdx.audio.newSound(Gdx.files.internal("audio/land.ogg"));
        sfxDash        = Gdx.audio.newSound(Gdx.files.internal("audio/dash.ogg"));
        sfxPlayerDeath = Gdx.audio.newSound(Gdx.files.internal("audio/player_death.ogg"));
        sfxPlayerStep  = Gdx.audio.newSound(Gdx.files.internal("audio/player_step.ogg"));

        sfxEnemyDeath  = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_death.ogg"));
        sfxEnemyAttack = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_attack.ogg"));
        sfxEnemyStep   = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_step.ogg"));
        sfxHit         = Gdx.audio.newSound(Gdx.files.internal("audio/hit.ogg"));
    }

    public void playMenuMusic()  { switchMusic(musicMenu); }
    public void playMap1Music()  { switchMusic(musicMap1); }
    public void playMap2Music()  { switchMusic(musicMap2); }

    private void switchMusic(Music newMusic) {
        if (currentMusic == newMusic && currentMusic.isPlaying()) return;
        if (currentMusic != null && currentMusic != newMusic) currentMusic.stop();
        currentMusic = newMusic;
        if (!currentMusic.isPlaying()) currentMusic.play();
    }

    public void stopMusic() {
        if (currentMusic != null) currentMusic.stop();
    }

    public void playAttack()      { sfxAttack.play(sfxVolume * 0.8f); }
    public void playJump()        { sfxJump.play(sfxVolume * 0.7f); }
    public void playLand()        { sfxLand.play(sfxVolume * 0.6f); }
    public void playDash()        { sfxDash.play(sfxVolume * 0.7f); }
    public void playPlayerDeath() { sfxPlayerDeath.play(sfxVolume); }
    public void playHit()         { sfxHit.play(sfxVolume * 0.9f); }
    public void playEnemyDeath()  { sfxEnemyDeath.play(sfxVolume * 0.8f); }
    public void playEnemyAttack() { sfxEnemyAttack.play(sfxVolume * 0.6f); }

    public void updatePlayerSounds(float dt, boolean isRunning, boolean onGround,
                                   boolean wasOnGroundPrev) {
        if (onGround && !wasOnGroundPrev) playLand();
        if (isRunning && onGround) {
            playerStepTimer -= dt;
            if (playerStepTimer <= 0) {
                sfxPlayerStep.play(sfxVolume * 0.4f);
                playerStepTimer = STEP_INTERVAL;
            }
        } else {
            playerStepTimer = 0f;
        }
    }

    /**
     * Llamar UNA SOLA VEZ por frame con el OR de todos los enemigos en movimiento.
     * Así el timer no se resetea al morir un enemigo concreto.
     */
    public void updateEnemySteps(float dt, boolean anyEnemyMoving) {
        if (anyEnemyMoving) {
            enemyStepTimer -= dt;
            if (enemyStepTimer <= 0) {
                sfxEnemyStep.play(sfxVolume * 0.25f);
                enemyStepTimer = STEP_INTERVAL + 0.05f;
            }
            // NO resetear a 0 cuando no se mueve: mantener el valor
            // para que no dispare inmediatamente en el siguiente frame
        }
    }

    public void setMusicVolume(float v) {
        musicVolume = v;
        if (musicMap1 != null) musicMap1.setVolume(v);
        if (musicMenu != null) musicMenu.setVolume(v);
    }

    public void setSfxVolume(float v) { sfxVolume = v; }

    public void dispose() {
        if (musicMap1 != null) musicMap1.dispose();
        if (musicMenu != null) musicMenu.dispose();
        sfxAttack.dispose();      sfxJump.dispose();
        sfxLand.dispose();        sfxDash.dispose();
        sfxPlayerDeath.dispose(); sfxPlayerStep.dispose();
        sfxEnemyDeath.dispose();  sfxEnemyAttack.dispose();
        sfxEnemyStep.dispose();   sfxHit.dispose();
    }
}
