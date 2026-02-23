package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameScreen implements Screen {

    private final PenitentGame game;

    private OrthographicCamera camera;
    private static final int   TILE_SIZE    = 32;
    private static final int   MAP_TILES_W  = 38;
    private static final int   MAP_TILES_H  = 20;
    private static final float MAP_W        = MAP_TILES_W * TILE_SIZE;
    private static final float MAP_H        = MAP_TILES_H * TILE_SIZE;
    private static final int   MAP2_TILES_W = 39;
    private static final float MAP2_W       = MAP2_TILES_W * TILE_SIZE;
    private static final float VIEW_W       = 608f;
    private static final float VIEW_H       = 320f;
    private static final float CAM_LERP     = 3.5f;

    private int   currentMap  = 1;
    private float currentMapW = MAP_W;
    private float camTargetX, camTargetY;

    private TiledMap tiledMap, tiledMap2;
    private OrthogonalTiledMapRenderer mapRenderer, mapRenderer2;
    private TiledMapTileLayer collisionLayer, collisionLayer2;

    private SpriteBatch batch;
    private Player player;
    private List<Enemy> enemies       = new ArrayList<>();
    private Set<Enemy>  hitThisAttack = new HashSet<>();
    private boolean wasAttacking      = false;

    // HUD
    private OrthographicCamera hudCamera;
    private Texture heartFull, heartEmpty;
    private static final int HEART_SIZE   = 34;
    private static final int HEART_MARGIN = 6;

    // Flash daño
    private float damageFlashTimer = 0f;
    private static final float DAMAGE_FLASH_DURATION = 0.15f;

    // ── Fade ──────────────────────────────────────────────────────────────────
    private enum FadeState { NONE, FADE_OUT, FADE_IN }
    private FadeState fadeState = FadeState.FADE_IN;
    private float     fadeAlpha = 1f;
    private static final float FADE_SPEED = 2.5f;
    private Runnable  fadeCallback = null;
    private Texture   fadeTexture;

    // Audio
    private SoundManager sound;

    // Game Over / Pausa
    private boolean gameOver = false;
    private boolean paused   = false;
    private GlyphLayout layout;

    // ── Submenú de selección de ranura al guardar ─────────────────────────────
    private enum PauseSubMenu { NONE, SAVE_SLOT_SELECT, SAVE_OVERWRITE_CONFIRM }
    private PauseSubMenu pauseSubMenu    = PauseSubMenu.NONE;
    private int          saveSlotIndex  = 0;           // ranura seleccionada
    private SaveManager.SaveData[] saveSlots;          // estado actual de ranuras

    // Confirmación de guardado (toast)
    private float saveConfirmTimer = 0f;
    private static final float SAVE_CONFIRM_DURATION = 2.5f;

    // Punto de guardado
    private int   saveMap    = 1;
    private float saveX      = 200f;
    private float saveY      = 32f;
    private int   saveHealth = 3;

    // ─────────────────────────────────────────────────────────────────────────

    public GameScreen(PenitentGame game, SaveManager.SaveData saveData) {
        this.game = game;
        if (saveData != null) {
            saveMap    = saveData.map;
            saveX      = saveData.playerX;
            saveY      = saveData.playerY;
            saveHealth = saveData.health;
        }
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIEW_W, VIEW_H);

        tiledMap       = new TmxMapLoader().load("mapa.tmx");
        mapRenderer    = new OrthogonalTiledMapRenderer(tiledMap, 1f);
        collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("suelo");

        tiledMap2       = new TmxMapLoader().load("mapa_2.tmx");
        mapRenderer2    = new OrthogonalTiledMapRenderer(tiledMap2, 1f);
        collisionLayer2 = (TiledMapTileLayer) tiledMap2.getLayers().get("suelo");

        batch = new SpriteBatch();

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, VIEW_W, VIEW_H);
        hudCamera.update();

        heartFull  = new Texture("hud/heart_full.png");
        heartEmpty = new Texture("hud/heart_empty.png");

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        fadeTexture = new Texture(pm);
        pm.dispose();

        layout = new GlyphLayout();

        sound = new SoundManager();
        sound.setMusicVolume(game.getMusicVolume());
        sound.setSfxVolume(game.getSfxVolume());

        if (saveMap == 2) {
            currentMap  = 2; currentMapW = MAP2_W;
            spawnPlayer(saveX, saveY, collisionLayer2);
            player.mapMinX = 0f; player.mapMaxX = MAP2_W + 50f;
            player.setHealth(saveHealth);
            spawnEnemiesMap2();
            sound.playMap2Music();
        } else {
            currentMap  = 1; currentMapW = MAP_W;
            spawnPlayer(saveX, saveY, collisionLayer);
            player.mapMinX = 0f; player.mapMaxX = MAP_W + 50f;
            player.setHealth(saveHealth);
            spawnEnemiesMap1();
            sound.playMap1Music();
        }

        camTargetX = player.x;
        camTargetY = player.y;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(float dt) {
        dt = Math.min(dt, 0.05f);

        // Tick fade SIEMPRE antes de cualquier early-return
        tickFade(dt);

        // Input de pausa (bloqueado si hay fade activo)
        if (fadeState == FadeState.NONE && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (paused && pauseSubMenu != PauseSubMenu.NONE) {
                // ESC dentro de submenú de pausa: volver a pausa principal
                pauseSubMenu = PauseSubMenu.NONE;
            } else {
                paused = !paused;
                if (!paused) pauseSubMenu = PauseSubMenu.NONE;
            }
        }

        if (paused)  { drawPause();    drawFade(); return; }
        if (gameOver){ drawGameOver(); drawFade(); return; }
        if (!player.isAlive()) { gameOver = true; return; }

        // Lógica de juego (bloqueada durante fade out)
        if (fadeState != FadeState.FADE_OUT) {
            player.update(dt);
            processSoundEvents(dt);

            // BUG 3 FIX: acumular un único OR de todos los enemigos en movimiento
            // y llamar a updateEnemySteps UNA sola vez con ese valor
            boolean anyEnemyMoving = false;
            for (Enemy e : enemies) {
                e.setPlayerPosition(player.x, player.y);
                e.update(dt);
                if (e.eventDeath)       { sound.playEnemyDeath();  e.eventDeath = false; }
                if (e.eventAttackStart) { sound.playEnemyAttack(); e.eventAttackStart = false; }
                if (e.eventMoving && e.isAlive() && !e.isDying()) anyEnemyMoving = true;
            }
            sound.updateEnemySteps(dt, anyEnemyMoving); // ← una sola llamada

            checkCombat();
        }

        // Trigger cambio de mapa
        if (fadeState == FadeState.NONE) {
            if (currentMap == 1 && player.x > currentMapW - 10)
                startFade(this::switchToMap2);
            if (currentMap == 2 && player.x < 10)
                startFade(this::switchToMap1);
        }

        if (wasAttacking && !player.isAttacking()) hitThisAttack.clear();
        wasAttacking = player.isAttacking();

        if (damageFlashTimer > 0) damageFlashTimer -= dt;
        if (saveConfirmTimer  > 0) saveConfirmTimer  -= dt;

        // Cámara
        camTargetX = Math.max(VIEW_W / 2f, Math.min(player.x + Player.HITBOX_W / 2f, currentMapW - VIEW_W / 2f));
        camTargetY = Math.max(VIEW_H / 2f, Math.min(player.y + Player.HITBOX_H / 2f, MAP_H - VIEW_H / 2f));
        camera.position.x += (camTargetX - camera.position.x) * CAM_LERP * dt;
        camera.position.y += (camTargetY - camera.position.y) * CAM_LERP * dt;
        camera.update();

        // Dibujo
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        ((currentMap == 1) ? mapRenderer : mapRenderer2).setView(camera);
        ((currentMap == 1) ? mapRenderer : mapRenderer2).render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        if (damageFlashTimer > 0) batch.setColor(1f, 0.3f, 0.3f, 1f);
        player.draw(batch);
        batch.setColor(Color.WHITE);
        for (Enemy e : enemies) e.draw(batch);
        batch.end();

        drawHUD();
        drawFade();
    }

    // ── Fade ──────────────────────────────────────────────────────────────────

    private void tickFade(float dt) {
        if (fadeState == FadeState.FADE_OUT) {
            fadeAlpha += FADE_SPEED * dt;
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                if (fadeCallback != null) { fadeCallback.run(); fadeCallback = null; }
                fadeState = FadeState.FADE_IN;
            }
        } else if (fadeState == FadeState.FADE_IN) {
            fadeAlpha -= FADE_SPEED * dt;
            if (fadeAlpha <= 0f) { fadeAlpha = 0f; fadeState = FadeState.NONE; }
        }
    }

    private void drawFade() {
        if (fadeAlpha <= 0f) return;
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.setColor(0f, 0f, 0f, fadeAlpha);
        batch.draw(fadeTexture, 0, 0, VIEW_W, VIEW_H);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void startFade(Runnable onBlack) {
        if (fadeState != FadeState.NONE) return;
        fadeState    = FadeState.FADE_OUT;
        fadeAlpha    = 0f;
        fadeCallback = onBlack;
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private void processSoundEvents(float dt) {
        if (player.eventAttack) { sound.playAttack();      player.eventAttack = false; }
        if (player.eventJump)   { sound.playJump();        player.eventJump   = false; }
        if (player.eventDash)   { sound.playDash();        player.eventDash   = false; }
        if (player.eventDeath)  { sound.playPlayerDeath(); player.eventDeath  = false; }
        if (player.eventHit)    { sound.playHit();         player.eventHit    = false; }
        sound.updatePlayerSounds(dt, player.eventRunning, player.eventOnGround, player.eventWasOnGround);
    }

    // ── Combate ───────────────────────────────────────────────────────────────

    private void checkCombat() {
        if (!player.isAlive()) return;
        Rectangle playerBounds = player.getBounds();
        Rectangle attackRange  = player.getAttackRange();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || enemy.isDying()) continue;
            Rectangle enemyBounds = enemy.getBounds();

            if (player.isAttacking() && attackRange.overlaps(enemyBounds)
                && !hitThisAttack.contains(enemy)) {
                enemy.hit();
                hitThisAttack.add(enemy);
            }
            if (enemy.isAttackActive() && enemy.getAttackBounds().overlaps(playerBounds)
                && !player.isHit()) {
                player.takeHit(enemy.x);
                damageFlashTimer = DAMAGE_FLASH_DURATION;
            }
            if (playerBounds.overlaps(enemyBounds) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(enemy.x);
                damageFlashTimer = DAMAGE_FLASH_DURATION;
            }
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────────────

    private void drawHUD() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        for (int i = 0; i < player.getMaxHealth(); i++) {
            float hx = 10 + i * (HEART_SIZE + HEART_MARGIN);
            float hy = VIEW_H - HEART_SIZE - 10;
            batch.draw(i < player.getHealth() ? heartFull : heartEmpty, hx, hy, HEART_SIZE, HEART_SIZE);
        }

        FontManager.small.setColor(new Color(0.7f, 0.6f, 0.6f, 0.8f));
        FontManager.small.draw(batch, SaveManager.getZoneName(currentMap), VIEW_W - 150, VIEW_H - 12);

        if (saveConfirmTimer > 0) {
            FontManager.small.setColor(new Color(0.3f, 1f, 0.4f, Math.min(1f, saveConfirmTimer * 2f)));
            String msg = "Partida guardada correctamente";
            layout.setText(FontManager.small, msg);
            FontManager.small.draw(batch, msg, (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 60);
        }
        batch.end();
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void drawPause() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        switch (pauseSubMenu) {
            case SAVE_SLOT_SELECT:    drawSaveSlotSelect();    break;
            case SAVE_OVERWRITE_CONFIRM: drawOverwriteConfirm(); break;
            default:                  drawPauseMain();         break;
        }

        batch.end();

        if (fadeState == FadeState.NONE) handlePauseInput();
    }

    private void drawPauseMain() {
        FontManager.title.setColor(Color.WHITE);
        layout.setText(FontManager.title, "PAUSA");
        FontManager.title.draw(batch, "PAUSA", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 70);

        FontManager.menu.setColor(new Color(0.8f, 0.7f, 0.5f, 1f));
        String[] opts = { "ESC  -  Continuar", "G  -  Guardar partida", "M  -  Volver al menu" };
        for (int i = 0; i < opts.length; i++) {
            layout.setText(FontManager.menu, opts[i]);
            FontManager.menu.draw(batch, opts[i], (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 15 - i * 38);
        }
    }

    private void drawSaveSlotSelect() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(FontManager.title, "GUARDAR EN...");
        FontManager.title.draw(batch, "GUARDAR EN...", cx - layout.width / 2f, VIEW_H - 40);

        float startY  = VIEW_H / 2f + 55;
        float spacing = 58f;
        float sx      = cx - 150;

        for (int i = 0; i < SaveManager.getMaxSlots(); i++) {
            boolean sel  = (i == saveSlotIndex);
            SaveManager.SaveData data = (saveSlots != null) ? saveSlots[i] : null;

            FontManager.menu.setColor(sel ? new Color(0.95f, 0.8f, 0.2f, 1f)
                : new Color(0.6f, 0.55f, 0.55f, 1f));
            FontManager.menu.draw(batch, "RANURA " + (i + 1), sx, startY - i * spacing);

            if (data != null) {
                FontManager.small.setColor(sel ? new Color(1f, 0.9f, 0.6f, 1f)
                    : new Color(0.7f, 0.65f, 0.6f, 1f));
                String hearts = "";
                for (int h = 0; h < 3; h++) hearts += (h < data.health) ? "\u2665 " : "\u2661 ";
                FontManager.small.draw(batch,
                    data.zoneName + "   " + hearts, sx + 10, startY - i * spacing - 20);
                FontManager.small.setColor(new Color(0.55f, 0.5f, 0.45f, 1f));
                FontManager.small.draw(batch, data.timestamp, sx + 10, startY - i * spacing - 36);
            } else {
                FontManager.small.setColor(new Color(0.5f, 0.8f, 0.5f, 1f));
                FontManager.small.draw(batch, "-- Vacia --", sx + 10, startY - i * spacing - 22);
            }

            if (sel) {
                FontManager.small.setColor(new Color(0.95f, 0.8f, 0.2f, 1f));
                FontManager.small.draw(batch, ">", sx - 20, startY - i * spacing);
            }
        }

        FontManager.small.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        layout.setText(FontManager.small, "Flechas Navegar   ENTER Guardar   ESC Cancelar");
        FontManager.small.draw(batch, "Flechas Navegar   ENTER Guardar   ESC Cancelar",
            cx - layout.width / 2f, 20);
    }

    private void drawOverwriteConfirm() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.9f, 0.2f, 0.2f, 1f));
        layout.setText(FontManager.title, "SOBREESCRIBIR?");
        FontManager.title.draw(batch, "SOBREESCRIBIR?", cx - layout.width / 2f, VIEW_H / 2f + 70);

        FontManager.menu.setColor(new Color(0.9f, 0.8f, 0.6f, 1f));
        String warn = "RANURA " + (saveSlotIndex + 1) + " ya tiene datos guardados.";
        layout.setText(FontManager.menu, warn);
        FontManager.menu.draw(batch, warn, cx - layout.width / 2f, VIEW_H / 2f + 20);

        FontManager.small.setColor(new Color(0.8f, 0.6f, 0.6f, 1f));
        String warn2 = "Los datos anteriores se perderan.";
        layout.setText(FontManager.small, warn2);
        FontManager.small.draw(batch, warn2, cx - layout.width / 2f, VIEW_H / 2f - 5);

        FontManager.menu.setColor(new Color(1f, 0.9f, 0.4f, 1f));
        layout.setText(FontManager.menu, "S / Y  =  Confirmar");
        FontManager.menu.draw(batch, "S / Y  =  Confirmar", cx - layout.width / 2f, VIEW_H / 2f - 45);

        FontManager.menu.setColor(new Color(0.7f, 0.65f, 0.65f, 1f));
        layout.setText(FontManager.menu, "N / ESC  =  Cancelar");
        FontManager.menu.draw(batch, "N / ESC  =  Cancelar", cx - layout.width / 2f, VIEW_H / 2f - 83);
    }

    private void handlePauseInput() {
        switch (pauseSubMenu) {

            case NONE:
                if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
                    // Abrir selección de ranura
                    saveSlots    = SaveManager.loadAll();
                    saveSlotIndex = 0;
                    pauseSubMenu = PauseSubMenu.SAVE_SLOT_SELECT;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    paused = false;
                    startFade(() -> { sound.stopMusic(); game.showMenu(); });
                }
                break;

            case SAVE_SLOT_SELECT:
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                    saveSlotIndex = (saveSlotIndex - 1 + SaveManager.getMaxSlots()) % SaveManager.getMaxSlots();
                if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
                    saveSlotIndex = (saveSlotIndex + 1) % SaveManager.getMaxSlots();

                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (SaveManager.hasSlot(saveSlotIndex)) {
                        // Ranura ocupada → pedir confirmación
                        pauseSubMenu = PauseSubMenu.SAVE_OVERWRITE_CONFIRM;
                    } else {
                        // Ranura vacía → guardar directamente
                        doSave(saveSlotIndex);
                    }
                }
                break;

            case SAVE_OVERWRITE_CONFIRM:
                if (Gdx.input.isKeyJustPressed(Input.Keys.Y) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                    doSave(saveSlotIndex);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.N) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    pauseSubMenu = PauseSubMenu.SAVE_SLOT_SELECT;
                }
                break;
        }
    }

    private void doSave(int slot) {
        SaveManager.save(slot, currentMap, player.x, player.y, player.getHealth());
        saveConfirmTimer = SAVE_CONFIRM_DURATION;
        pauseSubMenu     = PauseSubMenu.NONE;
        paused           = false;
    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void drawGameOver() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        FontManager.title.setColor(Color.RED);
        layout.setText(FontManager.title, "GAME OVER");
        FontManager.title.draw(batch, "GAME OVER", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 50);

        FontManager.menu.setColor(Color.WHITE);
        boolean hasSave   = SaveManager.hasSave();
        String retryLabel = hasSave ? "R  -  Volver al ultimo guardado" : "R  -  Nueva Partida";
        String[] opts     = { retryLabel, "M  -  Volver al menu" };
        for (int i = 0; i < opts.length; i++) {
            layout.setText(FontManager.menu, opts[i]);
            FontManager.menu.draw(batch, opts[i], (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 10 - i * 35);
        }
        batch.end();

        if (fadeState == FadeState.NONE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                startFade(() -> {
                    sound.stopMusic();
                    if (SaveManager.hasSave()) game.continueGame(0);
                    else game.startNewGame();
                });
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                startFade(() -> { sound.stopMusic(); game.showMenu(); });
            }
        }
    }

    // ── Cambio de mapa ────────────────────────────────────────────────────────

    private void switchToMap1() {
        currentMap = 1; currentMapW = MAP_W;
        player.x = MAP_W - 60;
        player.collisionLayer = collisionLayer;
        player.mapMinX = 0f; player.mapMaxX = MAP_W + 50f;
        spawnEnemiesMap1();
        hitThisAttack.clear();
        sound.playMap1Music();
    }

    private void switchToMap2() {
        currentMap = 2; currentMapW = MAP2_W;
        player.x = 30;
        player.collisionLayer = collisionLayer2;
        player.mapMinX = 0f; player.mapMaxX = MAP2_W + 50f;
        spawnEnemiesMap2();
        hitThisAttack.clear();
        sound.playMap2Music();
    }

    private void spawnPlayer(float x, float y, TiledMapTileLayer layer) {
        player = new Player(x, y, layer);
    }

    private void spawnEnemiesMap1() {
        enemies.clear();
        enemies.add(new Enemy(500, 32, 300, 700, collisionLayer));
        enemies.add(new Enemy(850, 32, 650, 1050, collisionLayer));
    }

    private void spawnEnemiesMap2() {
        enemies.clear();
        enemies.add(new Enemy(400, 32, 200, 800, collisionLayer2));
        enemies.add(new Enemy(900, 32, 700, 1150, collisionLayer2));
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {}
    @Override public void pause()  { paused = true; }
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        if (sound != null)       sound.dispose();
        if (tiledMap != null)    { tiledMap.dispose();  mapRenderer.dispose();  }
        if (tiledMap2 != null)   { tiledMap2.dispose(); mapRenderer2.dispose(); }
        if (batch != null)       batch.dispose();
        if (player != null)      player.dispose();
        for (Enemy e : enemies)  e.dispose();
        if (heartFull != null)   heartFull.dispose();
        if (heartEmpty != null)  heartEmpty.dispose();
        if (fadeTexture != null) fadeTexture.dispose();
    }
}
