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

    // ── Cámara / tamaños ───────────────────────────────────────────────────────
    private OrthographicCamera camera;
    private static final int   TILE      = 32;
    private static final float VIEW_W    = 608f;
    private static final float VIEW_H    = 320f;
    private static final float CAM_LERP  = 3.5f;

    // Dimensiones por mapa
    private static final float MAP1_W  = 38 * TILE;   // 1216
    private static final float MAP1_H  = 20 * TILE;   // 640
    private static final float MAP2_W  = 39 * TILE;   // 1248
    private static final float MAP2_H  = 20 * TILE;
    private static final float MAP3_W = 42 * TILE;  // 1344px   // 1344
    private static final float MAP3_H = 25 * TILE;  // 800px  // 800

    private int   currentMap  = 1;
    private float currentMapW = MAP1_W;
    private float currentMapH = MAP1_H;
    private float camTargetX, camTargetY;

    // ── Mapas Tiled ───────────────────────────────────────────────────────────
    private TiledMap map1, map2, map3;
    private OrthogonalTiledMapRenderer rend1, rend2, rend3;
    private TiledMapTileLayer col1, col2, col3;

    // ── Sprites / batch ────────────────────────────────────────────────────────
    private SpriteBatch batch;
    private Player player;

    // ── Enemigos (Enemy genérico) por mapa ────────────────────────────────────
    private List<Enemy> enemiesMap1 = new ArrayList<>();
    private List<Enemy> enemiesMap2 = new ArrayList<>();
    // Mapa 3 solo tiene BatEnemy
    private boolean pendingResetMap1 = false;
    private boolean pendingResetMap2 = false;

    // ── Bats (mapa 3) ─────────────────────────────────────────────────────────
    private List<BatEnemy> batsMap3 = new ArrayList<>();
    private List<SkeletonEnemy> skeletonsMap3 = new ArrayList<>();
    private boolean pendingResetMap3 = false;

    // ── Checkpoints ───────────────────────────────────────────────────────────
    // Mapa 1: posición original
    // Mapa 2: reubicado a la derecha del XXXXXXXX central (x=672, y=224)
    // Mapa 3: al fondo derecha
    private List<Checkpoint> cpMap1 = new ArrayList<>();
    private List<Checkpoint> cpMap2 = new ArrayList<>();
    private List<Checkpoint> cpMap3 = new ArrayList<>();

    private float checkpointToastTimer = 0f;
    private static final float CP_TOAST_DUR = 3f;

    // ── HUD ────────────────────────────────────────────────────────────────────
    private OrthographicCamera hudCam;
    private Texture heartFull, heartEmpty;
    private static final int HEART_SZ  = 34;
    private static final int HEART_GAP = 6;

    // ── Flash de daño ─────────────────────────────────────────────────────────
    private float damageFlash = 0f;
    private static final float FLASH_DUR = 0.15f;

    // ── Combate ───────────────────────────────────────────────────────────────
    private Set<Object> hitThisAttack = new HashSet<>();
    private boolean wasAttacking = false;

    // ── Fade ──────────────────────────────────────────────────────────────────
    private enum FadeState { NONE, FADE_OUT, FADE_IN }
    private FadeState fadeState = FadeState.FADE_IN;
    private float     fadeAlpha = 1f;
    private static final float FADE_SPEED = 2.5f;
    private Runnable  fadeCallback = null;
    private Texture   fadeTex;

    // ── Audio ──────────────────────────────────────────────────────────────────
    private SoundManager sound;

    // ── Estado ────────────────────────────────────────────────────────────────
    private boolean gameOver = false;
    private boolean paused   = false;
    private GlyphLayout layout;

    // ── Menú de pausa ─────────────────────────────────────────────────────────
    private int     pauseIndex      = 0;
    private boolean soundMuted      = false;
    private float   pauseBlinkTimer = 0f;
    private boolean pauseBlinkOn    = true;
    private static final int   PAUSE_ITEMS      = 4;
    private static final float PAUSE_BLINK_RATE = 0.5f;

    // ── Items / Inventario ────────────────────────────────────────────────────
    private List<Item> itemsMap1      = new ArrayList<>();
    private List<Item> itemsMap2      = new ArrayList<>();
    private List<Item> itemsMap3      = new ArrayList<>();
    private List<Item> collectedItems = new ArrayList<>();
    private InventoryOverlay inventoryOverlay;
    private boolean inventoryOpen     = false;
    private Item    nearbyItem        = null;
    private float   itemToastTimer    = 0f;
    private String  itemToastName     = "";
    private static final float ITEM_TOAST_DUR = 2.5f;

    // ── Save data de inicio ────────────────────────────────────────────────────
    private int   saveMap    = 1;
    private float saveX      = 200f;
    private float saveY      = 32f;
    private int   saveHealth = 3;
    private List<String> saveCollectedItems = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public GameScreen(PenitentGame game, SaveManager.SaveData data) {
        this.game = game;
        if (data != null) {
            saveMap          = data.map;
            saveX            = data.playerX;
            saveY            = data.playerY;
            saveHealth       = data.health;
            if (data.collectedItems != null)
                saveCollectedItems = data.collectedItems;
        }
    }

    // ── show ──────────────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIEW_W, VIEW_H);

        // Cargar los tres mapas
        map1  = new TmxMapLoader().load("mapa.tmx");
        rend1 = new OrthogonalTiledMapRenderer(map1, 1f);
        col1  = (TiledMapTileLayer) map1.getLayers().get("suelo");

        map2  = new TmxMapLoader().load("mapa_2.tmx");
        rend2 = new OrthogonalTiledMapRenderer(map2, 1f);
        col2  = (TiledMapTileLayer) map2.getLayers().get("suelo");

        map3  = new TmxMapLoader().load("mapa_3.tmx");
        rend3 = new OrthogonalTiledMapRenderer(map3, 1f);
        col3  = (TiledMapTileLayer) map3.getLayers().get("suelo");

        batch  = new SpriteBatch();
        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, VIEW_W, VIEW_H);
        hudCam.update();

        heartFull  = new Texture("hud/heart_full.png");
        heartEmpty = new Texture("hud/heart_empty.png");

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK); pm.fill();
        fadeTex = new Texture(pm);
        pm.dispose();

        layout = new GlyphLayout();
        sound  = new SoundManager();
        sound.setMusicVolume(game.getMusicVolume());
        sound.setSfxVolume(game.getSfxVolume());

        // Crear enemigos, checkpoints e items
        spawnEnemiesMap1();
        spawnEnemiesMap2();
        spawnBatsMap3();
        spawnSkeletonsMap3();

        inventoryOverlay = new InventoryOverlay();
        spawnItems();
        restoreCollectedItems(saveCollectedItems);

        // ── Checkpoints ──
        // Mapa 1: zona central
        cpMap1.add(new Checkpoint(580, 32));

        // Mapa 2: reubicado a la derecha del bloque central (cursor en la foto ~x=672)
        // El bloque XXXXXXXX en row 13 va de col 16 a 23 (world x 512-736)
        // Cursor apuntaba a col ~22-23 → x=672, plataforma en y=192+32=224
        cpMap2.add(new Checkpoint(672, 224));

        // Mapa 3: al fondo derecho (plataforma en col 26-29 row 15, world y=128)
        cpMap3.add(new Checkpoint(704, 384));

        // Posicionar jugador
        initPlayer();

        camTargetX = player.x;
        camTargetY = player.y;
    }

    private void initPlayer() {
        switch (saveMap) {
            case 3:
                currentMap  = 3; currentMapW = MAP3_W; currentMapH = MAP3_H;
                spawnPlayer(saveX, saveY, col3);
                player.mapMinX = -200f; player.mapMaxX = MAP3_W - 32f;
                player.setHealth(saveHealth);
                sound.playMap3Music();
                break;
            case 2:
                currentMap  = 2; currentMapW = MAP2_W; currentMapH = MAP2_H;
                spawnPlayer(saveX, saveY, col2);
                player.mapMinX = 32f; player.mapMaxX = MAP2_W;
                player.setHealth(saveHealth);
                sound.playMap2Music();
                break;
            default:
                currentMap  = 1; currentMapW = MAP1_W; currentMapH = MAP1_H;
                spawnPlayer(saveX, saveY, col1);
                player.mapMinX = 0f; player.mapMaxX = MAP1_W + 200f;
                player.setHealth(saveHealth);
                sound.playMap1Music();
                break;
        }
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(float dt) {
        dt = Math.min(dt, 0.05f);

        tickFade(dt);

        // Toggle inventario / pausa
        if (fadeState == FadeState.NONE && !gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
                if (inventoryOpen) inventoryOpen = false;
                else if (!paused)  inventoryOpen = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (inventoryOpen) {
                    inventoryOpen = false;
                } else {
                    paused = !paused;
                    if (paused) { pauseIndex = 0; pauseBlinkTimer = 0f; pauseBlinkOn = true; }
                }
            }
        }

        if (gameOver) { drawGameOver(); drawFade(); return; }
        if (!player.isAlive()) { gameOver = true; return; }

        // Lógica de juego — solo si no estamos pausados ni en inventario ni en fade-out
        if (!paused && !inventoryOpen && fadeState != FadeState.FADE_OUT) {
            player.update(dt);
            processSoundEvents(dt);
            updateEnemies(dt);
            updateItems(dt);
            checkCombat();
            checkCheckpoints(dt);
        }

        if (!paused && fadeState == FadeState.NONE) checkMapTransitions();

        if (wasAttacking && !player.isAttacking()) hitThisAttack.clear();
        wasAttacking = player.isAttacking();

        if (!paused && !inventoryOpen) {
            if (damageFlash          > 0) damageFlash          -= dt;
            if (checkpointToastTimer > 0) checkpointToastTimer -= dt;
            if (itemToastTimer       > 0) itemToastTimer       -= dt;
        }

        // Cámara
        camTargetX = Math.max(VIEW_W / 2f, Math.min(player.x + Player.HITBOX_W / 2f, currentMapW - VIEW_W / 2f));
        camTargetY = Math.max(VIEW_H / 2f, Math.min(player.y + Player.HITBOX_H / 2f, currentMapH - VIEW_H / 2f));
        camera.position.x += (camTargetX - camera.position.x) * CAM_LERP * dt;
        camera.position.y += (camTargetY - camera.position.y) * CAM_LERP * dt;
        camera.update();

        // Dibujo del mundo (siempre, también durante la pausa)
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        currentRenderer().setView(camera);
        currentRenderer().render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        for (Item it : currentItems())              it.draw(batch);
        for (Checkpoint cp : currentCheckpoints()) cp.draw(batch);
        for (Enemy e : currentEnemies())            e.draw(batch);
        for (BatEnemy b : currentBats())            b.draw(batch);
        for (SkeletonEnemy s : currentSkeletons())  s.draw(batch);

        if (damageFlash > 0) batch.setColor(1f, 0.3f, 0.3f, 1f);
        player.draw(batch);
        batch.setColor(Color.WHITE);
        batch.end();

        drawHUD();
        if (inventoryOpen) {
            inventoryOverlay.handleInput(collectedItems);
            drawInventory();
        } else if (paused) {
            handlePauseInput(dt);
            drawPause();
        }
        drawFade();
    }

    // ── Transiciones de mapa ──────────────────────────────────────────────────

    private void checkMapTransitions() {
        float playerCenterX = player.x + Player.HITBOX_W / 2f;

        // ── Mapa 1 → Mapa 2: jugador sale por la derecha ──────────────────
        // Mapa1 no tiene pared derecha (cols 36-37 vacías), solo suelo en row19
        // El jugador llega al final del mapa y cruza
        if (currentMap == 1 && player.x + Player.HITBOX_W >= MAP1_W)
            startFade(this::switchTo2);

        // ── Mapa 2 → Mapa 1: jugador sale por la izquierda ────────────────
        // Mapa2 izquierda (cols 0-1) vacía en rows 0-13, sólida abajo
        if (currentMap == 2 && player.x <= 0)
            startFade(this::switchTo1);

        // ── Mapa 2 → Mapa 3: jugador cae por el agujero central ───────────
        // Agujero en row 19 cols 18-20 → world x 576-672px
        if (currentMap == 2
            && player.y < -16f
            && playerCenterX >= 576f
            && playerCenterX <= 672f)
            startFade(this::switchTo3);

        // ── Mapa 3: muerte al caer por el abismo inferior ─────────────────
        if (currentMap == 3 && player.y < -32f)
            player.kill();

        if (currentMap == 3
            && player.y + Player.HITBOX_H >= 512f
            && player.y <= 672f) {
            player.mapMinX = -200f;  // abre el paso solo en ese rango
        } else if (currentMap == 3) {
            player.mapMinX = 32f;    // resto del borde izquierdo bloqueado
        }

    }


    private void switchTo1() {
        if (pendingResetMap2) { resetEnemiesMap2(); pendingResetMap2 = false; }
        currentMap = 1; currentMapW = MAP1_W; currentMapH = MAP1_H;
        player.x = MAP1_W - 80f;   // entra por la derecha del mapa 1
        player.y = Math.max(player.y, 32f);
        player.velocityY = 0f;
        player.collisionLayer = col1;
        player.mapMinX = 0f;
        player.mapMaxX = MAP1_W + 200f;  // margen para poder cruzar al mapa 2
        hitThisAttack.clear();
        sound.playMap1Music();
    }


    private void switchTo2() {
        if (pendingResetMap1) { resetEnemiesMap1(); pendingResetMap1 = false; }
        currentMap = 2; currentMapW = MAP2_W; currentMapH = MAP2_H;
        player.x = 10f;            // entra por la izquierda del mapa 2
        player.y = Math.max(player.y, 32f);
        player.velocityY = 0f;
        player.collisionLayer = col2;
        player.mapMinX = 32f;    // margen para poder volver al mapa 1
        player.mapMaxX = MAP2_W;
        hitThisAttack.clear();
        sound.playMap2Music();
    }


    private void switchTo3() {
        if (pendingResetMap2) { resetEnemiesMap2(); pendingResetMap2 = false; }
        if (pendingResetMap3) { resetBatsMap3();    pendingResetMap3 = false; }
        currentMap = 3; currentMapW = MAP3_W; currentMapH = MAP3_H;
        player.x = MAP3_W / 2f - Player.HITBOX_W / 2f;  // centro del mapa 3
        player.y = MAP3_H - 64f;   // cima, cae por gravedad
        player.velocityY = 0f;
        player.collisionLayer = col3;
        player.mapMinX = -200f;          // tapa huecos del borde izquierdo
        player.mapMaxX = MAP3_W - 32f; // tapa huecos del borde derecho
        hitThisAttack.clear();
        sound.playMap3Music();
    }



    // ── Enemigos ──────────────────────────────────────────────────────────────

    private List<Enemy> currentEnemies() {
        if (currentMap == 1) return enemiesMap1;
        if (currentMap == 2) return enemiesMap2;
        return new ArrayList<>(); // mapa 3 no tiene Enemy genérico
    }

    private List<SkeletonEnemy> currentSkeletons() {
        return currentMap == 3 ? skeletonsMap3 : new ArrayList<>();
    }

    private List<BatEnemy> currentBats() {
        if (currentMap == 3) return batsMap3;
        return new ArrayList<>();
    }

    private List<Checkpoint> currentCheckpoints() {
        if (currentMap == 1) return cpMap1;
        if (currentMap == 2) return cpMap2;
        return cpMap3;
    }

    private OrthogonalTiledMapRenderer currentRenderer() {
        if (currentMap == 1) return rend1;
        if (currentMap == 2) return rend2;
        return rend3;
    }

    private void updateEnemies(float dt) {
        boolean anyMoving = false;

        for (Enemy e : currentEnemies()) {
            e.setPlayerPosition(player.x, player.y);
            e.update(dt);
            if (e.eventDeath)       { sound.playEnemyDeath();  e.eventDeath = false; }
            if (e.eventAttackStart) { sound.playEnemyAttack(); e.eventAttackStart = false; }
            if (e.eventMoving && e.isAlive() && !e.isDying()) anyMoving = true;
        }
        sound.updateEnemySteps(dt, anyMoving);

        for (BatEnemy b : currentBats()) {
            b.setPlayerPosition(player.x, player.y);
            b.update(dt);
            if (b.eventDeath)       { sound.playEnemyDeath();  b.eventDeath = false; }
            if (b.eventAttackStart) { sound.playEnemyAttack(); b.eventAttackStart = false; }
            if (b.eventHit)         { b.eventHit = false; }
        }
        for (SkeletonEnemy s : currentSkeletons()) {
            s.setPlayerPosition(player.x, player.y);
            s.update(dt);
            if (s.eventDeath)       { sound.playEnemyDeath();  s.eventDeath = false; }
            if (s.eventAttackStart) { sound.playEnemyAttack(); s.eventAttackStart = false; }
        }
    }

    private void spawnEnemiesMap1() {
        enemiesMap1.clear();
        enemiesMap1.add(new Enemy(500, 32, 300,  700, col1));
        enemiesMap1.add(new Enemy(850, 32, 650, 1050, col1));
    }

    private void spawnEnemiesMap2() {
        enemiesMap2.clear();
        enemiesMap2.add(new Enemy(200, 224, 100, 500, col2));
        enemiesMap2.add(new Enemy(950, 224, 700, 1150, col2));
    }

    private void spawnSkeletonsMap3() {
        for (SkeletonEnemy s : skeletonsMap3) s.dispose();
        skeletonsMap3.clear();
        skeletonsMap3.add(new SkeletonEnemy(512,  128, 480,  672, col3));
    }

    private void resetSkeletonsMap3() {
        for (SkeletonEnemy s : skeletonsMap3) s.dispose();
        spawnSkeletonsMap3();
    }

    private void spawnBatsMap3() {
        for (BatEnemy b : batsMap3) b.dispose();
        batsMap3.clear();
        // Bat 1: cuelga de plataforma izquierda (col 8, row 10 → underside at y=288)
        //   hangX = centro de la plataforma = 8*32 + 16 = 272
        //   hangY = parte inferior de la plataforma = 288 (el tile ocupa y=288-320)
        batsMap3.add(new BatEnemy(272, 288));
        // Bat 2: cuelga de plataforma central-derecha (col 21, row 13 → underside at y=192)
        //   hangX = 21*32+16 = 688, hangY = 192
        batsMap3.add(new BatEnemy(688, 192));
    }

    private void resetEnemiesMap1() {
        for (Enemy e : enemiesMap1) e.dispose();
        spawnEnemiesMap1();
        if (currentMap == 1) hitThisAttack.clear();
    }

    private void resetEnemiesMap2() {
        for (Enemy e : enemiesMap2) e.dispose();
        spawnEnemiesMap2();
        if (currentMap == 2) hitThisAttack.clear();
    }

    private void resetBatsMap3() {
        spawnBatsMap3();
        spawnSkeletonsMap3();
        if (currentMap == 3) hitThisAttack.clear();
    }

    // ── Checkpoints ───────────────────────────────────────────────────────────

    private void checkCheckpoints(float dt) {
        for (Checkpoint cp : currentCheckpoints()) {
            if (cp.update(dt, player.x, player.y))
                onCheckpointActivated(cp);
        }
    }

    private void onCheckpointActivated(Checkpoint cp) {
        player.setHealth(player.getMaxHealth());
        List<String> savedNames = new ArrayList<>();
        for (Item it : collectedItems) savedNames.add(it.type.name());
        SaveManager.save(game.getActiveSlot(), currentMap, cp.x, cp.y, player.getMaxHealth(), savedNames);

        // Reset diferido del mapa actual, inmediato del resto
        if (currentMap == 1) {
            resetEnemiesMap2(); resetBatsMap3(); pendingResetMap1 = true;
        } else if (currentMap == 2) {
            resetEnemiesMap1(); resetBatsMap3(); pendingResetMap2 = true;
        } else {
            resetEnemiesMap1(); resetEnemiesMap2(); pendingResetMap3 = true;
        }
        checkpointToastTimer = CP_TOAST_DUR;
    }

    // ── Combate ───────────────────────────────────────────────────────────────

    private void checkCombat() {
        if (!player.isAlive()) return;
        Rectangle pb = player.getBounds();
        Rectangle ar = player.getAttackRange();

        // Enemy genérico
        for (Enemy e : currentEnemies()) {
            if (!e.isAlive() || e.isDying()) continue;
            Rectangle eb = e.getBounds();
            if (player.isAttacking() && ar.overlaps(eb) && !hitThisAttack.contains(e)) {
                e.hit(); hitThisAttack.add(e);
            }
            if (e.isAttackActive() && e.getAttackBounds().overlaps(pb) && !player.isHit()) {
                player.takeHit(e.x); damageFlash = FLASH_DUR;
            }
            if (pb.overlaps(eb) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(e.x); damageFlash = FLASH_DUR;
            }
        }

        // Bats
        for (BatEnemy b : currentBats()) {
            if (!b.isAlive() || b.isDying()) continue;
            Rectangle bb = b.getBounds();
            if (player.isAttacking() && ar.overlaps(bb) && !hitThisAttack.contains(b)) {
                b.hit(); hitThisAttack.add(b);
            }
            if (b.isAttackActive() && b.getAttackBounds().overlaps(pb) && !player.isHit()) {
                player.takeHit(b.x); damageFlash = FLASH_DUR;
            }
            if (!b.isSleeping() && pb.overlaps(bb) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(b.x); damageFlash = FLASH_DUR;
            }
        }

        // Skeletons
        for (SkeletonEnemy s : currentSkeletons()) {
            if (!s.isAlive() || s.isDying()) continue;
            Rectangle sb = s.getBounds();
            if (player.isAttacking() && ar.overlaps(sb) && !hitThisAttack.contains(s)) {
                s.hit(); hitThisAttack.add(s);
            }
            if (s.isAttackActive() && s.getAttackBounds().overlaps(pb) && !player.isHit()) {
                player.takeHit(s.x, SkeletonEnemy.DAMAGE); damageFlash = FLASH_DUR;
            }
            if (!s.isDying() && pb.overlaps(sb) && !player.isAttacking() && !player.isHit()) {
                player.takeHit(s.x, SkeletonEnemy.DAMAGE); damageFlash = FLASH_DUR;
            }
        }
    }

    // ── Fade ─────────────────────────────────────────────────────────────────

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
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        batch.setColor(0f, 0f, 0f, fadeAlpha);
        batch.draw(fadeTex, 0, 0, VIEW_W, VIEW_H);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void startFade(Runnable cb) {
        if (fadeState != FadeState.NONE) return;
        fadeState = FadeState.FADE_OUT; fadeAlpha = 0f; fadeCallback = cb;
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

    // ── HUD ───────────────────────────────────────────────────────────────────

    private void drawHUD() {
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        for (int i = 0; i < player.getMaxHealth(); i++) {
            float hx = 10 + i * (HEART_SZ + HEART_GAP);
            float hy = VIEW_H - HEART_SZ - 10;
            batch.draw(i < player.getHealth() ? heartFull : heartEmpty, hx, hy, HEART_SZ, HEART_SZ);
        }

        FontManager.small.setColor(new Color(0.7f, 0.6f, 0.6f, 0.8f));
        FontManager.small.draw(batch, SaveManager.getZoneName(currentMap), VIEW_W - 150, VIEW_H - 12);

        // Prompt checkpoint
        for (Checkpoint cp : currentCheckpoints()) {
            if (cp.isPlayerInRange()) {
                FontManager.menu.setColor(new Color(0.95f, 0.85f, 0.4f, 1f));
                String prompt = "B  -  Rezar";
                layout.setText(FontManager.menu, prompt);
                FontManager.menu.draw(batch, prompt, (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 80);
            }
        }

        // Prompt recoger item
        if (nearbyItem != null && !inventoryOpen && !paused) {
            FontManager.menu.setColor(new Color(0.65f, 0.92f, 0.48f, 1f));
            String pick = "E  -  Recoger";
            layout.setText(FontManager.menu, pick);
            FontManager.menu.draw(batch, pick, (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 100);
        }

        // Toast item recogido
        if (itemToastTimer > 0) {
            float alpha = Math.min(1f, itemToastTimer * 1.5f);
            FontManager.menu.setColor(new Color(0.65f, 0.92f, 0.48f, alpha));
            String msg = "Objeto recogido";
            layout.setText(FontManager.menu, msg);
            FontManager.menu.draw(batch, msg, (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 20);
            FontManager.small.setColor(new Color(0.82f, 0.75f, 0.58f, alpha * 0.9f));
            layout.setText(FontManager.small, itemToastName);
            FontManager.small.draw(batch, itemToastName, (VIEW_W - layout.width) / 2f, VIEW_H / 2f);
        }

        // Toast checkpoint
        if (checkpointToastTimer > 0) {
            float alpha = Math.min(1f, checkpointToastTimer * 1.2f);
            FontManager.menu.setColor(new Color(0.95f, 0.78f, 0.2f, alpha));
            String msg = "Oracion concedida";
            layout.setText(FontManager.menu, msg);
            FontManager.menu.draw(batch, msg, (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 20);
            FontManager.small.setColor(new Color(0.85f, 0.75f, 0.55f, alpha * 0.8f));
            String sub = "Partida guardada  -  Vida restaurada";
            layout.setText(FontManager.small, sub);
            FontManager.small.draw(batch, sub, (VIEW_W - layout.width) / 2f, VIEW_H / 2f);
        }
        batch.end();
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    private void spawnItems() {
        // Mapa 1 – Las Entrañas (suelo en y=32)
        itemsMap1.add(new Item(Item.ItemType.NUDOS_CORDON,  150f, 32f));
        itemsMap1.add(new Item(Item.ItemType.AZOQUE,       1080f, 32f));

        // Mapa 2 – El Osario (plataformas en y=224)
        itemsMap2.add(new Item(Item.ItemType.MATRAZ_BILIAR, 340f, 224f));
        itemsMap2.add(new Item(Item.ItemType.VELO_NEGRO,   1040f, 224f));

        // Mapa 3 – Las Catacumbas (plataforma derecha del checkpoint, y=384)
        itemsMap3.add(new Item(Item.ItemType.CALIZ,         900f, 384f));
    }

    private void restoreCollectedItems(List<String> savedNames) {
        List<List<Item>> allMaps = new ArrayList<>();
        allMaps.add(itemsMap1);
        allMaps.add(itemsMap2);
        allMaps.add(itemsMap3);
        for (String name : savedNames) {
            for (List<Item> mapItems : allMaps) {
                for (Item it : mapItems) {
                    if (it.type.name().equals(name) && !it.collected) {
                        it.collected = true;
                        collectedItems.add(it);
                    }
                }
            }
        }
    }

    private List<Item> currentItems() {
        if (currentMap == 1) return itemsMap1;
        if (currentMap == 2) return itemsMap2;
        return itemsMap3;
    }

    private void updateItems(float dt) {
        nearbyItem = null;
        float px = player.x + Player.HITBOX_W / 2f;
        float py = player.y + Player.HITBOX_H / 2f;
        for (Item item : currentItems()) {
            item.update(dt);
            if (!item.collected && item.isPlayerInRange(px, py)) {
                nearbyItem = item;
                if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    item.collected    = true;
                    itemToastName     = item.type.displayName;
                    itemToastTimer    = ITEM_TOAST_DUR;
                    collectedItems.add(item);
                }
                break; // solo un item a la vez
            }
        }
    }

    private void drawInventory() {
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        inventoryOverlay.draw(batch, collectedItems, fadeTex);
        batch.end();
    }

    // ── Pausa ─────────────────────────────────────────────────────────────────

    private void handlePauseInput(float dt) {
        pauseBlinkTimer += dt;
        if (pauseBlinkTimer >= PAUSE_BLINK_RATE) { pauseBlinkOn = !pauseBlinkOn; pauseBlinkTimer = 0f; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            pauseIndex = (pauseIndex - 1 + PAUSE_ITEMS) % PAUSE_ITEMS;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            pauseIndex = (pauseIndex + 1) % PAUSE_ITEMS;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            selectPauseItem();
    }

    private void selectPauseItem() {
        switch (pauseIndex) {
            case 0: // Continuar
                paused = false;
                pauseIndex = 0;
                break;
            case 1: // Activar / desactivar sonido
                soundMuted = !soundMuted;
                if (soundMuted) {
                    sound.setMusicVolume(0f);
                    sound.setSfxVolume(0f);
                } else {
                    sound.setMusicVolume(game.getMusicVolume());
                    sound.setSfxVolume(game.getSfxVolume());
                }
                break;
            case 2: // Volver al menú principal
                paused = false;
                startFade(() -> { sound.stopMusic(); game.showMenu(); });
                break;
            case 3: // Salir del juego
                Gdx.app.exit();
                break;
        }
    }

    private void drawPause() {
        float cx = VIEW_W / 2f;

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        // Overlay semitransparente sobre el mundo
        batch.setColor(0f, 0f, 0f, 0.62f);
        batch.draw(fadeTex, 0, 0, VIEW_W, VIEW_H);

        // Título
        batch.setColor(Color.WHITE);
        FontManager.menu.setColor(new Color(0.92f, 0.78f, 0.28f, 1f));
        layout.setText(FontManager.menu, "PAUSA");
        FontManager.menu.draw(batch, "PAUSA", cx - layout.width / 2f, VIEW_H / 2f + 90);

        // Opciones
        String[] labels = {
            "Continuar",
            "Sonido: " + (soundMuted ? "OFF" : "ON"),
            "Menu principal",
            "Salir"
        };
        float startY  = VIEW_H / 2f + 48f;
        float spacing = 28f;

        for (int i = 0; i < labels.length; i++) {
            boolean sel = (i == pauseIndex);
            FontManager.small.setColor(sel
                ? new Color(0.95f, 0.82f, 0.22f, 1f)
                : new Color(0.72f, 0.65f, 0.58f, 1f));
            layout.setText(FontManager.small, labels[i]);
            float lx = cx - layout.width / 2f;
            float ly = startY - i * spacing;
            FontManager.small.draw(batch, labels[i], lx, ly);
            if (sel && pauseBlinkOn) {
                FontManager.small.setColor(new Color(0.95f, 0.82f, 0.22f, 1f));
                FontManager.small.draw(batch, ">", lx - 16, ly);
            }
        }

        // Hint inferior
        FontManager.small.setColor(new Color(0.45f, 0.42f, 0.42f, 0.85f));
        String hint = "Flechas Navegar   ENTER Seleccionar   ESC Continuar";
        layout.setText(FontManager.small, hint);
        FontManager.small.draw(batch, hint, cx - layout.width / 2f, 18);

        batch.end();
    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void drawGameOver() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        FontManager.title.setColor(Color.RED);
        layout.setText(FontManager.title, "GAME OVER");
        FontManager.title.draw(batch, "GAME OVER", (VIEW_W - layout.width) / 2f, VIEW_H / 2f + 50);

        FontManager.menu.setColor(Color.WHITE);
        boolean hasSave   = SaveManager.hasSlot(game.getActiveSlot());
        String retryLabel = hasSave ? "R  -  Volver al ultimo checkpoint" : "R  -  Nueva Partida";
        String[] opts = { retryLabel, "M  -  Volver al menu" };
        for (int i = 0; i < opts.length; i++) {
            layout.setText(FontManager.menu, opts[i]);
            FontManager.menu.draw(batch, opts[i], (VIEW_W - layout.width) / 2f, VIEW_H / 2f - 10 - i * 35);
        }
        batch.end();

        if (fadeState == FadeState.NONE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                startFade(() -> {
                    sound.stopMusic();
                    if (SaveManager.hasSlot(game.getActiveSlot())) game.reloadActiveSlot();
                    else game.startNewGame(game.getActiveSlot());
                });
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.M))
                startFade(() -> { sound.stopMusic(); game.showMenu(); });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void spawnPlayer(float x, float y, TiledMapTileLayer layer) {
        player = new Player(x, y, layer);
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {}
    @Override public void pause()  { paused = true; }
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        if (sound   != null) sound.dispose();
        if (map1    != null) { map1.dispose(); rend1.dispose(); }
        if (map2    != null) { map2.dispose(); rend2.dispose(); }
        if (map3    != null) { map3.dispose(); rend3.dispose(); }
        if (batch   != null) batch.dispose();
        if (player  != null) player.dispose();
        for (Enemy e  : enemiesMap1)  e.dispose();
        for (Enemy e  : enemiesMap2)  e.dispose();
        for (BatEnemy b : batsMap3)         b.dispose();
        for (SkeletonEnemy s : skeletonsMap3) s.dispose();
        for (Checkpoint cp : cpMap1)        cp.dispose();
        for (Checkpoint cp : cpMap2)        cp.dispose();
        for (Checkpoint cp : cpMap3)        cp.dispose();
        for (Item it : itemsMap1)           it.dispose();
        for (Item it : itemsMap2)           it.dispose();
        for (Item it : itemsMap3)           it.dispose();
        if (inventoryOverlay != null) inventoryOverlay.dispose();
        if (heartFull  != null) heartFull.dispose();
        if (heartEmpty != null) heartEmpty.dispose();
        if (fadeTex    != null) fadeTex.dispose();
    }
}
