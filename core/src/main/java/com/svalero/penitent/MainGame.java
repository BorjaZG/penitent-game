package com.svalero.penitent;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

public class MainGame extends ApplicationAdapter {

    private OrthographicCamera camera;

    private static final int   TILE_SIZE   = 32;
    private static final int   MAP_TILES_W = 38;
    private static final int   MAP_TILES_H = 20;
    private static final float MAP_W  = MAP_TILES_W * TILE_SIZE;  // 1216
    private static final float MAP_H  = MAP_TILES_H * TILE_SIZE;  // 640

    private static final int   MAP2_TILES_W = 39;
    private static final float MAP2_W = MAP2_TILES_W * TILE_SIZE; // 1248

    private int   currentMap  = 1;
    private float currentMapW = MAP_W;

    private static final float VIEW_W = 608f;
    private static final float VIEW_H = 320f;

    // Suavizado de cámara
    private static final float CAM_LERP = 6f;
    private float camTargetX, camTargetY;

    private TiledMap tiledMap, tiledMap2;
    private OrthogonalTiledMapRenderer mapRenderer, mapRenderer2;
    private TiledMapTileLayer collisionLayer, collisionLayer2;

    private SpriteBatch batch;
    private Player player;
    private List<Enemy> enemies     = new ArrayList<>();
    private Set<Enemy> hitThisAttack = new HashSet<>();
    private boolean wasAttacking    = false;

    // HUD
    private OrthographicCamera hudCamera;
    private Texture heartFull, heartEmpty;
    private static final int HEART_SIZE   = 34;
    private static final int HEART_MARGIN = 6;

    // Flash de daño en pantalla
    private float damageFlashTimer = 0f;
    private static final float DAMAGE_FLASH_DURATION = 0.15f;

    // Audio
    private SoundManager sound;

    // Game Over
    private boolean gameOver = false;
    private BitmapFont font;
    private GlyphLayout layout;

    @Override
    public void create() {
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

        font   = new BitmapFont();
        layout = new GlyphLayout();

        sound = new SoundManager();
        sound.playMap1Music();

        initMap1();
    }

    private void initMap1() {
        player = new Player(200, 32, collisionLayer);
        player.mapMinX = 0f;
        player.mapMaxX = MAP_W + 50f; // permite llegar al trigger de cambio de mapa
        enemies.clear();
        enemies.add(new Enemy(500, 32, 300, 700, collisionLayer));
        enemies.add(new Enemy(850, 32, 650, 1050, collisionLayer));
        camTargetX = player.x;
        camTargetY = player.y;
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f); // cap delta para evitar saltos

        if (gameOver) { drawGameOver(); return; }

        if (!player.isAlive()) { gameOver = true; return; }

        // Actualizar
        player.update(dt);

        // --- Sonidos del jugador ---
        if (player.eventAttack)   { sound.playAttack();      player.eventAttack = false; }
        if (player.eventJump)     { sound.playJump();        player.eventJump   = false; }
        if (player.eventDash)     { sound.playDash();        player.eventDash   = false; }
        if (player.eventDeath)    { sound.playPlayerDeath(); player.eventDeath  = false; }
        if (player.eventHit)      { sound.playHit();         player.eventHit    = false; }
        sound.updatePlayerSounds(dt, player.eventRunning,
            player.eventOnGround, player.eventWasOnGround);

        for (Enemy e : enemies) {
            e.setPlayerPosition(player.x, player.y);
            e.update(dt);

            // Sonidos del enemigo
            if (e.eventDeath)       { sound.playEnemyDeath();  e.eventDeath       = false; }
            if (e.eventAttackStart) { sound.playEnemyAttack(); e.eventAttackStart  = false; }
            sound.updateEnemySteps(dt, e.eventMoving && e.isAlive() && !e.isDying());
        }

        // Cambio de mapa
        if (currentMap == 1 && player.x > currentMapW - 10) switchToMap2();
        if (currentMap == 2 && player.x < 10)               switchToMap1();

        // Reset hit set al terminar el ataque
        if (wasAttacking && !player.isAttacking()) hitThisAttack.clear();
        wasAttacking = player.isAttacking();

        checkCombat();

        // Flash de daño
        if (damageFlashTimer > 0) damageFlashTimer -= dt;

        // --- Cámara con lerp suave ---
        camTargetX = player.x + Player.HITBOX_W / 2f;
        camTargetY = player.y + Player.HITBOX_H / 2f;
        camTargetX = Math.max(VIEW_W / 2f, Math.min(camTargetX, currentMapW - VIEW_W / 2f));
        camTargetY = Math.max(VIEW_H / 2f, Math.min(camTargetY, MAP_H - VIEW_H / 2f));

        camera.position.x += (camTargetX - camera.position.x) * CAM_LERP * dt;
        camera.position.y += (camTargetY - camera.position.y) * CAM_LERP * dt;
        camera.update();

        // --- Render ---
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        OrthogonalTiledMapRenderer active = (currentMap == 1) ? mapRenderer : mapRenderer2;
        active.setView(camera);
        active.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Flash rojo al recibir daño
        if (damageFlashTimer > 0) batch.setColor(1f, 0.3f, 0.3f, 1f);
        player.draw(batch);
        batch.setColor(Color.WHITE);
        for (Enemy e : enemies) e.draw(batch);
        batch.end();

        drawHUD();
    }

    private void checkCombat() {
        if (!player.isAlive()) return;

        Rectangle playerBounds = player.getBounds();
        Rectangle attackRange  = player.getAttackRange();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || enemy.isDying()) continue;
            Rectangle enemyBounds = enemy.getBounds();

            // Jugador ataca enemigo (1 hit por swing)
            if (player.isAttacking() && attackRange.overlaps(enemyBounds)
                && !hitThisAttack.contains(enemy)) {
                enemy.hit();
                hitThisAttack.add(enemy);
            }

            // Enemigo ataca al jugador (ventana de daño precisa)
            if (enemy.isAttackActive() && enemy.getAttackBounds().overlaps(playerBounds)
                && !player.isHit()) {
                player.takeHit(enemy.x);
                damageFlashTimer = DAMAGE_FLASH_DURATION;
            }

            // Contacto simple
            if (playerBounds.overlaps(enemyBounds) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(enemy.x);
                damageFlashTimer = DAMAGE_FLASH_DURATION;
            }
        }
    }

    private void drawHUD() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        int maxH = player.getMaxHealth();
        int curH = player.getHealth();
        for (int i = 0; i < maxH; i++) {
            float hx = 10 + i * (HEART_SIZE + HEART_MARGIN);
            float hy = VIEW_H - HEART_SIZE - 10;
            batch.draw((i < curH) ? heartFull : heartEmpty, hx, hy, HEART_SIZE, HEART_SIZE);
        }
        batch.end();
    }

    private void drawGameOver() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        font.getData().setScale(3f);
        font.setColor(Color.RED);
        layout.setText(font, "GAME OVER");
        font.draw(batch, "GAME OVER", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 50);

        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        layout.setText(font, "Pulsa R para volver a intentarlo");
        font.draw(batch, "Pulsa R para volver a intentarlo",
            (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 10);

        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            gameOver    = false;
            currentMap  = 1;
            currentMapW = MAP_W;
            initMap1();
            hitThisAttack.clear();
            wasAttacking = false;
        }
    }

    private void switchToMap1() {
        sound.playMap1Music();
        currentMap  = 1;
        currentMapW = MAP_W;
        player.x    = MAP_W - 60;
        player.collisionLayer = collisionLayer;
        player.mapMinX = 0f;
        player.mapMaxX = MAP_W + 50f; // permite llegar al trigger de cambio de mapa
        enemies.clear();
        enemies.add(new Enemy(500, 32, 300, 700, collisionLayer));
        enemies.add(new Enemy(850, 32, 650, 1050, collisionLayer));
        hitThisAttack.clear();
    }

    private void switchToMap2() {
        sound.playMap2Music();
        currentMap  = 2;
        currentMapW = MAP2_W;
        player.x    = 30;
        player.collisionLayer = collisionLayer2;
        player.mapMinX = 0f;
        player.mapMaxX = MAP2_W + 50f; // permite llegar al trigger de cambio de mapa
        enemies.clear();
        enemies.add(new Enemy(400, 32, 200, 800, collisionLayer2));
        enemies.add(new Enemy(900, 32, 700, 1150, collisionLayer2));
        hitThisAttack.clear();
    }

    @Override
    public void dispose() {
        sound.dispose();
        tiledMap.dispose();   mapRenderer.dispose();
        tiledMap2.dispose();  mapRenderer2.dispose();
        batch.dispose();
        player.dispose();
        for (Enemy e : enemies) e.dispose();
        heartFull.dispose();  heartEmpty.dispose();
        font.dispose();
    }
}
