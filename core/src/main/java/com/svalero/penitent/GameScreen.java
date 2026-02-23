package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
    private static final int   TILE_SIZE   = 32;
    private static final int   MAP_TILES_W = 38;
    private static final int   MAP_TILES_H = 20;
    private static final float MAP_W  = MAP_TILES_W * TILE_SIZE;
    private static final float MAP_H  = MAP_TILES_H * TILE_SIZE;
    private static final int   MAP2_TILES_W = 39;
    private static final float MAP2_W = MAP2_TILES_W * TILE_SIZE;
    private static final float VIEW_W = 608f;
    private static final float VIEW_H = 320f;
    private static final float CAM_LERP = 3.5f;

    private int   currentMap  = 1;
    private float currentMapW = MAP_W;
    private float camTargetX, camTargetY;

    private TiledMap tiledMap, tiledMap2;
    private OrthogonalTiledMapRenderer mapRenderer, mapRenderer2;
    private TiledMapTileLayer collisionLayer, collisionLayer2;

    private SpriteBatch batch;
    private Player player;
    private List<Enemy> enemies      = new ArrayList<>();
    private Set<Enemy>  hitThisAttack = new HashSet<>();
    private boolean wasAttacking     = false;

    // HUD
    private OrthographicCamera hudCamera;
    private Texture heartFull, heartEmpty;
    private static final int HEART_SIZE   = 34;
    private static final int HEART_MARGIN = 6;

    // Fondo parallax
    private Texture bgTexture;
    private boolean hasBg = false;
    private static final float PARALLAX_FACTOR = 0.3f; // 0=fijo, 1=mismo que cámara

    // Flash daño
    private float damageFlashTimer = 0f;
    private static final float DAMAGE_FLASH_DURATION = 0.15f;

    // Confirmación de guardado
    private float saveConfirmTimer = 0f;
    private static final float SAVE_CONFIRM_DURATION = 2.5f;

    // Audio
    private SoundManager sound;

    // Game Over / Pausa
    private boolean gameOver = false;
    private boolean paused   = false;
    private GlyphLayout layout;

    // Punto de guardado más reciente (para el checkpoint)
    private int   saveMap    = 1;
    private float saveX      = 200f;
    private float saveY      = 32f;
    private int   saveHealth = 3;

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

        tiledMap    = new TmxMapLoader().load("mapa.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);
        collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("suelo");

        tiledMap2    = new TmxMapLoader().load("mapa_2.tmx");
        mapRenderer2 = new OrthogonalTiledMapRenderer(tiledMap2, 1f);
        collisionLayer2 = (TiledMapTileLayer) tiledMap2.getLayers().get("suelo");

        batch = new SpriteBatch();

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, VIEW_W, VIEW_H);
        hudCamera.update();

        heartFull  = new Texture("hud/heart_full.png");
        heartEmpty = new Texture("hud/heart_empty.png");

        // Fondo parallax (mismo PNG que el menú si existe)
        try {
            bgTexture = new Texture("menu_background.png");
            hasBg = true;
        } catch (Exception e) {
            hasBg = false;
        }

        layout = new GlyphLayout();

        sound = new SoundManager();
        sound.setMusicVolume(game.getMusicVolume());
        sound.setSfxVolume(game.getSfxVolume());

        // Iniciar desde el punto de guardado
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

    @Override
    public void render(float dt) {
        dt = Math.min(dt, 0.05f);

        // Pausa con ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) paused = !paused;

        if (paused)  { drawPause(); return; }
        if (gameOver){ drawGameOver(); return; }
        if (!player.isAlive()) { gameOver = true; return; }

        // Actualizar
        player.update(dt);
        processSoundEvents(dt);

        for (Enemy e : enemies) {
            e.setPlayerPosition(player.x, player.y);
            e.update(dt);
            if (e.eventDeath)       { sound.playEnemyDeath();  e.eventDeath = false; }
            if (e.eventAttackStart) { sound.playEnemyAttack(); e.eventAttackStart = false; }
            sound.updateEnemySteps(dt, e.eventMoving && e.isAlive() && !e.isDying());
        }

        // Cambio de mapa
        if (currentMap == 1 && player.x > currentMapW - 10) switchToMap2();
        if (currentMap == 2 && player.x < 10)               switchToMap1();

        if (wasAttacking && !player.isAttacking()) hitThisAttack.clear();
        wasAttacking = player.isAttacking();

        checkCombat();
        if (damageFlashTimer > 0) damageFlashTimer -= dt;
        if (saveConfirmTimer  > 0) saveConfirmTimer  -= dt;

        // Cámara lerp
        camTargetX = Math.max(VIEW_W/2f, Math.min(player.x + Player.HITBOX_W/2f, currentMapW - VIEW_W/2f));
        camTargetY = Math.max(VIEW_H/2f, Math.min(player.y + Player.HITBOX_H/2f, MAP_H - VIEW_H/2f));
        camera.position.x += (camTargetX - camera.position.x) * CAM_LERP * dt;
        camera.position.y += (camTargetY - camera.position.y) * CAM_LERP * dt;
        camera.update();

        // Render
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Fondo parallax
        if (hasBg) {
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            float bgOffsetX = (camera.position.x - VIEW_W / 2f) * PARALLAX_FACTOR;
            float bgOffsetY = (camera.position.y - VIEW_H / 2f) * PARALLAX_FACTOR * 0.5f;
            // Repetir horizontalmente si el mapa es más ancho que la pantalla
            float bgW = VIEW_W + 60;
            float bgX = -bgOffsetX % bgW - 30;
            batch.setColor(0.6f, 0.6f, 0.6f, 1f); // oscurecer un poco para no tapar el gameplay
            batch.draw(bgTexture, bgX, -bgOffsetY, bgW * 2, VIEW_H + 40);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        ((currentMap == 1) ? mapRenderer : mapRenderer2).setView(camera);
        ((currentMap == 1) ? mapRenderer : mapRenderer2).render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (damageFlashTimer > 0) batch.setColor(1f, 0.3f, 0.3f, 1f);
        player.draw(batch);
        batch.setColor(Color.WHITE);
        for (Enemy e : enemies) e.draw(batch);
        batch.end();

        drawHUD();
    }

    private void processSoundEvents(float dt) {
        if (player.eventAttack) { sound.playAttack();      player.eventAttack = false; }
        if (player.eventJump)   { sound.playJump();        player.eventJump   = false; }
        if (player.eventDash)   { sound.playDash();        player.eventDash   = false; }
        if (player.eventDeath)  { sound.playPlayerDeath(); player.eventDeath  = false; }
        if (player.eventHit)    { sound.playHit();         player.eventHit    = false; }
        sound.updatePlayerSounds(dt, player.eventRunning, player.eventOnGround, player.eventWasOnGround);
    }

    private void checkCombat() {
        if (!player.isAlive()) return;
        Rectangle playerBounds = player.getBounds();
        Rectangle attackRange  = player.getAttackRange();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || enemy.isDying()) continue;
            Rectangle enemyBounds = enemy.getBounds();

            if (player.isAttacking() && attackRange.overlaps(enemyBounds)
                && !hitThisAttack.contains(enemy)) {
                enemy.hit(); hitThisAttack.add(enemy);
            }
            if (enemy.isAttackActive() && enemy.getAttackBounds().overlaps(playerBounds)
                && !player.isHit()) {
                player.takeHit(enemy.x); damageFlashTimer = DAMAGE_FLASH_DURATION;
            }
            if (playerBounds.overlaps(enemyBounds) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(enemy.x); damageFlashTimer = DAMAGE_FLASH_DURATION;
            }
        }
    }

    private void drawHUD() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        for (int i = 0; i < player.getMaxHealth(); i++) {
            float hx = 10 + i * (HEART_SIZE + HEART_MARGIN);
            float hy = VIEW_H - HEART_SIZE - 10;
            batch.draw(i < player.getHealth() ? heartFull : heartEmpty, hx, hy, HEART_SIZE, HEART_SIZE);
        }
        // Indicador de mapa
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

    private void drawPause() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        FontManager.title.setColor(Color.WHITE);
        layout.setText(FontManager.title, "PAUSA");
        FontManager.title.draw(batch, "PAUSA", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 60);

        FontManager.menu.setColor(new Color(0.8f, 0.7f, 0.5f, 1f));
        String[] opts = { "ESC  -  Continuar", "G  -  Guardar partida", "M  -  Volver al menu" };
        for (int i = 0; i < opts.length; i++) {
            layout.setText(FontManager.menu, opts[i]);
            FontManager.menu.draw(batch, opts[i], (VIEW_W - layout.width) / 2f, VIEW_H / 2f - i * 35);
        }
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            SaveManager.save(0, currentMap, player.x, player.y, player.getHealth());
            saveConfirmTimer = SAVE_CONFIRM_DURATION;
            paused = false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            sound.stopMusic();
            game.showMenu();
        }
    }

    private void drawGameOver() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        FontManager.title.setColor(Color.RED);
        layout.setText(FontManager.title, "GAME OVER");
        FontManager.title.draw(batch, "GAME OVER", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 50);

        FontManager.menu.setColor(Color.WHITE);
        boolean hasSave = SaveManager.hasSave();
        String retryLabel = hasSave ? "R  -  Volver al ultimo guardado" : "R  -  Nueva Partida";
        String[] opts = { retryLabel, "M  -  Volver al menu" };
        for (int i = 0; i < opts.length; i++) {
            layout.setText(FontManager.menu, opts[i]);
            FontManager.menu.draw(batch, opts[i], (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 10 - i * 35);
        }
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            sound.dispose();
            if (SaveManager.hasSave()) {
                // Cargar el slot 0 (el más reciente)
                game.continueGame(0);
            } else {
                game.startNewGame();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            sound.stopMusic();
            game.showMenu();
        }
    }

    private void switchToMap1() {
        sound.playMap1Music();
        currentMap = 1; currentMapW = MAP_W;
        player.x = MAP_W - 60;
        player.collisionLayer = collisionLayer;
        player.mapMinX = 0f; player.mapMaxX = MAP_W + 50f;
        spawnEnemiesMap1();
        hitThisAttack.clear();
    }

    private void switchToMap2() {
        sound.playMap2Music();
        currentMap = 2; currentMapW = MAP2_W;
        player.x = 30;
        player.collisionLayer = collisionLayer2;
        player.mapMinX = 0f; player.mapMaxX = MAP2_W + 50f;
        spawnEnemiesMap2();
        hitThisAttack.clear();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  { paused = true; }
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        sound.dispose();
        tiledMap.dispose();  mapRenderer.dispose();
        tiledMap2.dispose(); mapRenderer2.dispose();
        batch.dispose(); player.dispose();
        for (Enemy e : enemies) e.dispose();
        heartFull.dispose(); heartEmpty.dispose();
        if (hasBg) bgTexture.dispose();
    }
}
