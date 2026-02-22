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

    // Mapa: 38x20 tiles de 32px = 1216x640px
    private static final int   TILE_SIZE = 32;
    private static final int   MAP_TILES_W = 38;
    private static final int   MAP_TILES_H = 20;
    private static final float MAP_W  = MAP_TILES_W * TILE_SIZE;  // 1216
    private static final float MAP_H  = MAP_TILES_H * TILE_SIZE;  // 640

    // Viewport (lo que se ve): mitad del mapa para hacer zoom cómodo
    private static final float VIEW_W = 608f;
    private static final float VIEW_H = 320f;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer collisionLayer;

    private SpriteBatch batch;
    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private Set<Enemy> hitThisAttack = new HashSet<>();
    private boolean wasAttacking = false;

    // HUD
    private OrthographicCamera hudCamera;
    private Texture heartFull, heartEmpty;
    private static final int HEART_SIZE   = 34; // 17px x2 para que se vea bien
    private static final int HEART_MARGIN = 6;

    // Game Over
    private boolean gameOver = false;
    private BitmapFont font;
    private GlyphLayout layout;

    @Override
    public void create() {
        // Y ARRIBA (false) = coordenadas libGDX estándar
        // El mapa se renderiza con Y=0 abajo, igual que libGDX
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIEW_W, VIEW_H);

        tiledMap = new TmxMapLoader().load("mapa.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);

        collisionLayer = (TiledMapTileLayer) tiledMap.getLayers().get("suelo");

        batch = new SpriteBatch();

        // HUD camera (fija, no sigue al jugador)
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, VIEW_W, VIEW_H);
        hudCamera.update();

        heartFull  = new Texture("hud/heart_full.png");
        heartEmpty = new Texture("hud/heart_empty.png");

        font   = new BitmapFont();
        layout = new GlyphLayout();

        // Con Y-arriba: el suelo (fila 19 en Tiled) está en Y=0..32 en libGDX
        // (Tiled invierte Y: fila 0 Tiled = Y alto en libGDX, fila 19 = Y=0)
        // Jugador spawna encima del suelo: Y = 32
        player = new Player(200, 32, collisionLayer);

        enemies.add(new Enemy(400, 32, 300, 700, collisionLayer));
        enemies.add(new Enemy(700, 32, 500, 1000, collisionLayer));
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        // --- Game Over ---
        if (gameOver) {
            drawGameOver();
            return;
        }

        // Detectar muerte del jugador
        if (!player.isAlive()) {
            gameOver = true;
        }

        player.update(dt);
        for (Enemy e : enemies) e.update(dt);
        // Resetear enemigos golpeados cuando termina el ataque
        if (wasAttacking && !player.isAttacking()) {
            hitThisAttack.clear();
        }
        wasAttacking = player.isAttacking();
        checkCombat();

        // Cámara sigue al jugador con límites del mapa
        camera.position.x = player.x + Player.HITBOX_W / 2f;
        camera.position.y = player.y + Player.HITBOX_H / 2f;
        camera.position.x = Math.max(VIEW_W / 2f, Math.min(camera.position.x, MAP_W - VIEW_W / 2f));
        camera.position.y = Math.max(VIEW_H / 2f, Math.min(camera.position.y, MAP_H - VIEW_H / 2f));
        camera.update();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        for (Enemy e : enemies) e.draw(batch);
        batch.end();

        // --- HUD (corazones) ---
        drawHUD();
    }

    private void drawHUD() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        int maxH = player.getMaxHealth();
        int curH = player.getHealth();
        for (int i = 0; i < maxH; i++) {
            float hx = 10 + i * (HEART_SIZE + HEART_MARGIN);
            float hy = VIEW_H - HEART_SIZE - 10;
            Texture tex = (i < curH) ? heartFull : heartEmpty;
            batch.draw(tex, hx, hy, HEART_SIZE, HEART_SIZE);
        }
        batch.end();
    }

    private void drawGameOver() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // Título
        font.getData().setScale(3f);
        font.setColor(Color.RED);
        layout.setText(font, "GAME OVER");
        font.draw(batch, "GAME OVER",
            (VIEW_W - layout.width) / 2f,
            VIEW_H / 2f + 40);

        // Instrucción
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        layout.setText(font, "Pulsa R para volver a intentarlo");
        font.draw(batch, "Pulsa R para volver a intentarlo",
            (VIEW_W - layout.width) / 2f,
            VIEW_H / 2f - 20);

        batch.end();

        // Reiniciar con R
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
        }
    }

    private void restartGame() {
        gameOver = false;
        player = new Player(200, 32, collisionLayer);
        enemies.clear();
        enemies.add(new Enemy(400, 32, 300, 700, collisionLayer));
        enemies.add(new Enemy(700, 32, 500, 1000, collisionLayer));
        hitThisAttack.clear();
        wasAttacking = false;
    }

    private void checkCombat() {
        if (!player.isAlive()) return;

        Rectangle playerBounds = player.getBounds();
        Rectangle attackRange  = player.getAttackRange();

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            Rectangle enemyBounds = enemy.getBounds();

            // Jugador ataca al enemigo con J (1 hit por swing)
            if (player.isAttacking() && attackRange.overlaps(enemyBounds) && !hitThisAttack.contains(enemy)) {
                enemy.hit();
                hitThisAttack.add(enemy);
            }

            // Contacto con enemigo daña al jugador (solo si no está en frames de invencibilidad)
            if (playerBounds.overlaps(enemyBounds) && !player.isAttacking() && !player.isHit()) {
                player.takeHit();
            }
        }
    }

    @Override
    public void dispose() {
        tiledMap.dispose();
        mapRenderer.dispose();
        batch.dispose();
        player.dispose();
        for (Enemy e : enemies) e.dispose();
        heartFull.dispose();
        heartEmpty.dispose();
        font.dispose();
    }
}
