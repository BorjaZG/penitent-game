package com.svalero.penitent;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

public class Enemy {

    private static final int   FRAME_W  = 99;
    private static final int   FRAME_H  = 46;
    public  static final float HITBOX_W = 40f;
    public  static final float HITBOX_H = 44f;

    private float leftLimit, rightLimit;
    private float speed = 60f;
    private boolean movingRight = true;

    public float x, y;
    public float velocityY = 0;
    private boolean alive     = true;
    private float   stateTime = 0f;
    private boolean dying     = false;
    private int     health    = 2;

    // Física
    private static final float GRAVITY   = 800f;
    private static final int   TILE_SIZE = 32;

    // IA
    private static final float CHASE_START_RANGE  = 250f; // empieza a perseguir
    private static final float CHASE_STOP_RANGE   = 380f; // deja de perseguir (leash)
    private static final float ATTACK_RANGE       = 55f;
    private static final float ATTACK_COOLDOWN    = 3f;
    private float attackCooldownTimer = 0f;
    private boolean isAttacking       = false;
    private float   attackAnimTimer   = 0f;
    private static final float ATTACK_ANIM_DURATION = 13 * 0.07f;

    // Ventana de daño: frames 5-8
    private static final float DAMAGE_WINDOW_START = 5 * 0.07f;
    private static final float DAMAGE_WINDOW_END   = 9 * 0.07f;

    private enum AIState { PATROL, CHASE, ATTACK, RETURN }
    private AIState aiState = AIState.PATROL;
    private float patrolReturnX; // posición central de patrulla para volver

    // Hit y knockback
    private boolean isHit      = false;
    private float   hitTimer   = 0f;
    private float   knockbackX = 0f;
    private static final float HIT_DURATION     = 0.2f;
    private static final float KNOCKBACK_FORCE  = 150f;
    private static final float KNOCKBACK_DECAY  = 600f;

    // Parpadeo al recibir hit
    private static final float BLINK_INTERVAL = 0.06f;
    private float   blinkTimer   = 0f;
    private boolean blinkVisible = true;

    private TiledMapTileLayer collisionLayer;

    private Texture idleSheet, runSheet, deathSheet, hitSheet, attackSheet;
    private Animation<TextureRegion> idleAnim, runAnim, deathAnim, hitAnim, attackAnim;

    private float playerX, playerY;

    // Eventos de sonido
    public boolean eventDeath      = false;
    public boolean eventAttackStart = false;
    public boolean eventMoving      = false;

    public Enemy(float startX, float startY, float leftLimit, float rightLimit,
                 TiledMapTileLayer collisionLayer) {
        this.x             = startX;
        this.y             = startY;
        this.leftLimit     = leftLimit;
        this.rightLimit    = rightLimit;
        this.patrolReturnX = startX;
        this.collisionLayer = collisionLayer;

        idleSheet   = new Texture("enemies/enemy_idle.png");
        runSheet    = new Texture("enemies/enemy_run.png");
        deathSheet  = new Texture("enemies/enemy_death.png");
        hitSheet    = new Texture("enemies/enemy_hit.png");
        attackSheet = new Texture("enemies/enemy_attack.png");

        idleAnim   = makeAnim(idleSheet,    8, 0.10f, true);
        runAnim    = makeAnim(runSheet,     8, 0.08f, true);
        deathAnim  = makeAnim(deathSheet,  14, 0.07f, false);
        hitAnim    = makeAnim(hitSheet,     2, 0.08f, false);
        attackAnim = makeAnim(attackSheet, 13, 0.07f, false);
    }

    private Animation<TextureRegion> makeAnim(Texture sheet, int frames, float duration, boolean loop) {
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++)
            regions[i] = new TextureRegion(sheet, i * FRAME_W, 0, FRAME_W, FRAME_H);
        Animation<TextureRegion> anim = new Animation<>(duration, regions);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return anim;
    }

    public void setPlayerPosition(float px, float py) {
        this.playerX = px;
        this.playerY = py;
    }

    public void update(float dt) {
        stateTime += dt;

        // Parpadeo al recibir hit
        if (isHit) {
            hitTimer -= dt;
            blinkTimer -= dt;
            if (blinkTimer <= 0) { blinkVisible = !blinkVisible; blinkTimer = BLINK_INTERVAL; }
            if (hitTimer <= 0) { isHit = false; blinkVisible = true; }
        }

        // Knockback
        if (knockbackX != 0) {
            x += knockbackX * dt;
            knockbackX = (knockbackX > 0)
                ? Math.max(0, knockbackX - KNOCKBACK_DECAY * dt)
                : Math.min(0, knockbackX + KNOCKBACK_DECAY * dt);
        }

        // Cooldown ataque
        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;

        // Muriendo
        if (dying) {
            if (deathAnim.isAnimationFinished(stateTime)) alive = false;
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

        // --- Máquina de estados IA con leash ---
        switch (aiState) {
            case PATROL:
                if (dist <= ATTACK_RANGE && attackCooldownTimer <= 0)
                    aiState = AIState.ATTACK;
                else if (dist <= CHASE_START_RANGE)
                    aiState = AIState.CHASE;
                else
                    patrol(dt);
                break;

            case CHASE:
                if (dist <= ATTACK_RANGE && attackCooldownTimer <= 0) {
                    aiState = AIState.ATTACK;
                } else if (dist > CHASE_STOP_RANGE) {
                    // Perdió al jugador: volver a patrulla
                    aiState = AIState.RETURN;
                } else {
                    // Perseguir más rápido que patrulla
                    float chaseSpeed = speed * 1.5f;
                    if (playerX > x) { x += chaseSpeed * dt; movingRight = true; }
                    else             { x -= chaseSpeed * dt; movingRight = false; }
                }
                break;

            case ATTACK:
                movingRight      = (playerX > x);
                isAttacking      = true;
                attackAnimTimer  = ATTACK_ANIM_DURATION;
                stateTime        = 0f;
                eventAttackStart = true;
                aiState = AIState.CHASE;
                break;

            case RETURN:
                // Volver lentamente a la posición de patrulla
                if (Math.abs(x - patrolReturnX) > 5f) {
                    if (patrolReturnX > x) { x += speed * dt; movingRight = true; }
                    else                   { x -= speed * dt; movingRight = false; }
                } else {
                    aiState = AIState.PATROL;
                }
                // Si el jugador vuelve a acercarse, retomar chase
                if (dist <= CHASE_START_RANGE) aiState = AIState.CHASE;
                break;
        }

        eventMoving = (aiState == AIState.PATROL || aiState == AIState.CHASE || aiState == AIState.RETURN);

        applyGravity(dt);
    }

    private void patrol(float dt) {
        if (movingRight) { x += speed * dt; if (x >= rightLimit) { x = rightLimit; movingRight = false; } }
        else             { x -= speed * dt; if (x <= leftLimit)  { x = leftLimit;  movingRight = true;  } }
    }

    private void applyGravity(float dt) {
        velocityY -= GRAVITY * dt;
        y += velocityY * dt;
        if (velocityY <= 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)(y / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y = (ty + 1) * TILE_SIZE; velocityY = 0;
            }
        }
    }

    private boolean isSolid(int tx, int ty) {
        if (collisionLayer == null) return false;
        return collisionLayer.getCell(tx, ty) != null;
    }

    public void draw(SpriteBatch batch) {
        if (isHit && !blinkVisible) return;

        Animation<TextureRegion> anim;
        if      (dying)       anim = deathAnim;
        else if (isHit)       anim = hitAnim;
        else if (isAttacking) anim = attackAnim;
        else if (aiState == AIState.CHASE || aiState == AIState.RETURN) anim = runAnim;
        else if (aiState == AIState.PATROL) anim = runAnim;
        else                  anim = idleAnim;

        TextureRegion frame = anim.getKeyFrame(stateTime);
        float drawX = x - (FRAME_W - HITBOX_W) / 2f;
        float drawY = y;
        if (movingRight) batch.draw(frame, drawX, drawY, FRAME_W, FRAME_H);
        else             batch.draw(frame, drawX + FRAME_W, drawY, -FRAME_W, FRAME_H);
    }

    public void hit() {
        if (dying) return;
        health--;
        if (health <= 0) {
            dying = true; stateTime = 0f;
            eventDeath = true;
        } else {
            isHit = true; hitTimer = HIT_DURATION;
            blinkTimer = BLINK_INTERVAL; blinkVisible = true;
            // Knockback alejándose del jugador
            knockbackX = (x > playerX) ? KNOCKBACK_FORCE : -KNOCKBACK_FORCE;
        }
    }

    public void kill() { health = 0; hit(); }

    public boolean isAttackActive() {
        if (!isAttacking) return false;
        float elapsed = ATTACK_ANIM_DURATION - attackAnimTimer;
        return elapsed >= DAMAGE_WINDOW_START && elapsed <= DAMAGE_WINDOW_END;
    }

    public Rectangle getBounds() { return new Rectangle(x, y, HITBOX_W, HITBOX_H); }
    public Rectangle getAttackBounds() {
        float ax = movingRight ? x + HITBOX_W : x - 30f;
        return new Rectangle(ax, y, 30f, HITBOX_H);
    }
    public boolean isAlive()  { return alive; }
    public boolean isDying()  { return dying; }

    public void dispose() {
        idleSheet.dispose(); runSheet.dispose(); deathSheet.dispose();
        hitSheet.dispose();  attackSheet.dispose();
    }
}
