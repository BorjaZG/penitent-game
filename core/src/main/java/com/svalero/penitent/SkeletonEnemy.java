package com.svalero.penitent;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

public class SkeletonEnemy {

    // ── Hitbox en mundo ───────────────────────────────────────────────────────
    public  static final float HITBOX_W = 36f;
    public  static final float HITBOX_H = 52f;

    // ── Frame sizes ───────────────────────────────────────────────────────────
    private static final int FRAME_STD_W  = 64;   // idle, move, hurt
    private static final int FRAME_ATK_W  = 146;  // attack
    private static final int FRAME_DIE_W  = 118;  // die
    private static final int FRAME_H      = 64;

    // ── Posición ──────────────────────────────────────────────────────────────
    public float x, y;
    public float velocityY = 0f;
    private boolean movingRight = true;

    // ── Física ────────────────────────────────────────────────────────────────
    private static final float GRAVITY   = 800f;
    private static final int   TILE_SIZE = 32;
    private TiledMapTileLayer  collisionLayer;

    // ── IA ────────────────────────────────────────────────────────────────────
    private static final float PATROL_SPEED      = 35f;
    private static final float CHASE_SPEED       = 55f;
    private static final float CHASE_RANGE       = 180f;  // distancia para empezar a perseguir
    private static final float LEASH_RANGE       = 260f;  // distancia para dejar de perseguir
    private static final float ATTACK_RANGE      = 52f;
    private static final float ATTACK_COOLDOWN   = 2.2f;

    private float leftLimit, rightLimit, patrolReturnX;
    private float playerX, playerY;
    private float attackCooldownTimer = 0f;

    private enum AIState { PATROL, CHASE, ATTACK, RETURN }
    private AIState aiState = AIState.PATROL;

    // ── Ataque ────────────────────────────────────────────────────────────────
    // Attack sheet: 25 frames a 12fps → duración total ~2.08s
    // Ventana de daño: frames 10-14 (zona de impacto de la cadena)
    private static final float ATK_FPS           = 12f;
    private static final float ATK_DURATION      = 25f / ATK_FPS;  // ~2.08s
    private static final float DMG_WINDOW_START  = 10f / ATK_FPS;
    private static final float DMG_WINDOW_END    = 15f / ATK_FPS;
    public  static final int   DAMAGE            = 2;  // corazones por golpe

    private boolean isAttacking      = false;
    private float   attackAnimTimer  = 0f;

    // ── Vida / estado ─────────────────────────────────────────────────────────
    private int     hp      = 4;
    private boolean alive   = true;
    private boolean dying   = false;
    private float   stateTime = 0f;

    // ── Hit / knockback ───────────────────────────────────────────────────────
    private boolean isHit      = false;
    private float   hitTimer   = 0f;
    private float   knockbackX = 0f;
    private static final float HIT_DURATION    = 0.25f;
    private static final float KNOCKBACK_FORCE = 120f;
    private static final float KNOCKBACK_DECAY = 500f;

    // ── Parpadeo ──────────────────────────────────────────────────────────────
    private static final float BLINK_INTERVAL = 0.06f;
    private float   blinkTimer   = 0f;
    private boolean blinkVisible = true;

    // ── Eventos ───────────────────────────────────────────────────────────────
    public boolean eventDeath       = false;
    public boolean eventAttackStart = false;
    public boolean eventMoving      = false;

    // ── Animaciones ───────────────────────────────────────────────────────────
    private Texture tIdle, tMove, tHurt, tAttack, tDie;
    private Animation<TextureRegion> animIdle, animMove, animHurt, animAttack, animDie;

    // ─────────────────────────────────────────────────────────────────────────

    public SkeletonEnemy(float startX, float startY,
                         float leftLimit, float rightLimit,
                         TiledMapTileLayer collisionLayer) {
        this.x              = startX;
        this.y              = startY;
        this.leftLimit      = leftLimit;
        this.rightLimit     = rightLimit;
        this.patrolReturnX  = startX;
        this.collisionLayer = collisionLayer;
        loadAnimations();
    }

    // ── Carga de animaciones ──────────────────────────────────────────────────

    private Animation<TextureRegion> makeAnim(Texture tex, int frameW, int cols, int rows,
                                              float fps, boolean loop) {
        int total = cols * rows;
        TextureRegion[] frames = new TextureRegion[total];
        int idx = 0;
        for (int row = 0; row < rows; row++)
            for (int col = 0; col < cols; col++)
                frames[idx++] = new TextureRegion(tex, col * frameW, row * FRAME_H, frameW, FRAME_H);
        Animation<TextureRegion> anim = new Animation<>(1f / fps, frames);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return anim;
    }

    private void loadAnimations() {
        tIdle   = new Texture("enemies/skeleton/skeletonIdle-Sheet64x64.png");
        tMove   = new Texture("enemies/skeleton/skeletonMove-Sheet64x64.png");
        tHurt   = new Texture("enemies/skeleton/skeletonHurt-Sheet64x64.png");
        tAttack = new Texture("enemies/skeleton/skeletonAttack-Sheet146x64.png");
        tDie    = new Texture("enemies/skeleton/skeletonDie-Sheet118x64_all.png");

        animIdle   = makeAnim(tIdle,   FRAME_STD_W,  8, 1, 10f, true);
        animMove   = makeAnim(tMove,   FRAME_STD_W, 10, 1, 10f, true);
        animHurt   = makeAnim(tHurt,   FRAME_STD_W,  3, 1, 12f, false);
        animAttack = makeAnim(tAttack, FRAME_ATK_W,  5, 5, ATK_FPS, false);
        animDie    = makeAnim(tDie,    FRAME_DIE_W,  5, 5, 10f, false);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void setPlayerPosition(float px, float py) {
        this.playerX = px + Player.HITBOX_W / 2f;
        this.playerY = py;
    }

    public void update(float dt) {
        if (!alive) return;
        stateTime += dt;

        // Parpadeo
        if (isHit) {
            hitTimer -= dt;
            blinkTimer -= dt;
            if (blinkTimer <= 0) { blinkVisible = !blinkVisible; blinkTimer = BLINK_INTERVAL; }
            if (hitTimer <= 0)   { isHit = false; blinkVisible = true; }
        }

        // Knockback
        if (knockbackX != 0) {
            x += knockbackX * dt;
            knockbackX = knockbackX > 0
                ? Math.max(0, knockbackX - KNOCKBACK_DECAY * dt)
                : Math.min(0, knockbackX + KNOCKBACK_DECAY * dt);
        }

        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;

        // Muriendo
        if (dying) {
            if (animDie.isAnimationFinished(stateTime)) alive = false;
            applyGravity(dt);
            return;
        }

        // Atacando
        if (isAttacking) {
            attackAnimTimer -= dt;
            if (attackAnimTimer <= 0) {
                isAttacking = false;
                attackCooldownTimer = ATTACK_COOLDOWN;
            }
            applyGravity(dt);
            return;
        }

        float dist = Math.abs(playerX - x);

        switch (aiState) {
            case PATROL:
                if (dist <= ATTACK_RANGE && attackCooldownTimer <= 0)
                    aiState = AIState.ATTACK;
                else if (dist <= CHASE_RANGE)
                    aiState = AIState.CHASE;
                else
                    patrol(dt);
                break;

            case CHASE:
                if (dist <= ATTACK_RANGE && attackCooldownTimer <= 0) {
                    aiState = AIState.ATTACK;
                } else if (dist > LEASH_RANGE) {
                    aiState = AIState.RETURN;
                } else {
                    if (playerX > x) { x += CHASE_SPEED * dt; movingRight = true; }
                    else             { x -= CHASE_SPEED * dt; movingRight = false; }
                    x = Math.max(leftLimit, Math.min(rightLimit, x));
                }
                break;

            case ATTACK:
                movingRight      = (playerX > x);
                isAttacking      = true;
                attackAnimTimer  = ATK_DURATION;
                stateTime        = 0f;
                eventAttackStart = true;
                aiState = AIState.CHASE;
                break;

            case RETURN:
                if (Math.abs(x - patrolReturnX) > 5f) {
                    if (patrolReturnX > x) { x += PATROL_SPEED * dt; movingRight = true; }
                    else                   { x -= PATROL_SPEED * dt; movingRight = false; }
                } else {
                    aiState = AIState.PATROL;
                }
                if (dist <= CHASE_RANGE) aiState = AIState.CHASE;
                break;
        }

        eventMoving = (aiState == AIState.PATROL || aiState == AIState.CHASE || aiState == AIState.RETURN);
        applyGravity(dt);
    }

    private void patrol(float dt) {
        if (movingRight) { x += PATROL_SPEED * dt; if (x >= rightLimit) { x = rightLimit; movingRight = false; } }
        else             { x -= PATROL_SPEED * dt; if (x <= leftLimit)  { x = leftLimit;  movingRight = true;  } }
    }

    private void applyGravity(float dt) {
        velocityY -= GRAVITY * dt;
        y += velocityY * dt;
        if (velocityY <= 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)(y / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y = (ty + 1) * TILE_SIZE;
                velocityY = 0;
            }
        }
    }

    private boolean isSolid(int tx, int ty) {
        if (collisionLayer == null) return false;
        return collisionLayer.getCell(tx, ty) != null;
    }

    // ── Recibir daño ──────────────────────────────────────────────────────────

    public void hit() {
        if (dying) return;
        hp--;
        if (hp <= 0) {
            dying = true; stateTime = 0f;
            eventDeath = true;
        } else {
            isHit = true; hitTimer = HIT_DURATION;
            blinkTimer = BLINK_INTERVAL; blinkVisible = true;
            knockbackX = (x > playerX) ? KNOCKBACK_FORCE : -KNOCKBACK_FORCE;
        }
    }

    // ── Hitboxes ──────────────────────────────────────────────────────────────

    public boolean isAttackActive() {
        if (!isAttacking) return false;
        float elapsed = ATK_DURATION - attackAnimTimer;
        return elapsed >= DMG_WINDOW_START && elapsed <= DMG_WINDOW_END;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, HITBOX_W, HITBOX_H);
    }

    public Rectangle getAttackBounds() {
        // La cadena se extiende bastante delante del esqueleto
        float aw = 80f;
        float ax = movingRight ? x + HITBOX_W : x - aw;
        return new Rectangle(ax, y + 4f, aw, HITBOX_H - 8f);
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    public void draw(SpriteBatch batch) {
        if (!alive) return;
        if (isHit && !blinkVisible) return;

        Animation<TextureRegion> anim;
        int frameW;
        if      (dying)       { anim = animDie;    frameW = FRAME_DIE_W; }
        else if (isHit)       { anim = animHurt;   frameW = FRAME_STD_W; }
        else if (isAttacking) { anim = animAttack; frameW = FRAME_ATK_W; }
        else if (aiState == AIState.CHASE || aiState == AIState.RETURN
            || aiState == AIState.PATROL) { anim = animMove; frameW = FRAME_STD_W; }
        else                  { anim = animIdle;   frameW = FRAME_STD_W; }

        boolean loop = anim.getPlayMode() == Animation.PlayMode.LOOP;
        TextureRegion frame = anim.getKeyFrame(stateTime, loop);

        // Centro el sprite sobre el hitbox
        float drawX = x - (frameW - HITBOX_W) / 2f;
        float drawY = y;

        if (movingRight) batch.draw(frame, drawX + frameW,  drawY, -frameW, FRAME_H);
        else             batch.draw(frame, drawX,            drawY,  frameW, FRAME_H);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isAlive()  { return alive; }
    public boolean isDying()  { return dying; }

    public void dispose() {
        tIdle.dispose(); tMove.dispose(); tHurt.dispose();
        tAttack.dispose(); tDie.dispose();
    }
}
