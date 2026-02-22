package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

public class Player {

    private static final int   FRAME_W   = 120;
    private static final int   FRAME_H   = 80;
    public  static final float HITBOX_W  = 30f;
    public  static final float HITBOX_H  = 50f;
    private static final int   TILE_SIZE = 32;

    // Física (Y arriba = positivo, libGDX estándar)
    private static final float GRAVITY    =  800f;
    private static final float JUMP_FORCE =  400f;
    private static final float SPEED      =  160f;
    private static final float DASH_SPEED =  400f;
    private static final float DASH_TIME  =  0.15f;

    public float x, y;
    public float velocityY  = 0;
    public boolean onGround = false;
    private boolean facingRight = true;
    private boolean alive       = true;
    private float stateTime     = 0f;

    // Ataque y combo
    private boolean attacking    = false;
    private boolean combo        = false;    // true = en fase combo
    private boolean comboQueued  = false;    // J pulsado durante ataque normal
    private float   attackTimer  = 0f;
    private static final float ATTACK_DURATION = 0.32f; // 4 frames x 0.08s
    private static final float COMBO_DURATION  = 0.70f; // 10 frames x 0.07s

    // Dash
    private boolean dashing   = false;
    private float   dashTimer = 0f;

    // Hit / muerte
    private boolean hit      = false;
    private float   hitTimer = 0f;
    private static final float HIT_DURATION = 1.5f;
    private boolean dying    = false;

    // Sistema de vida
    private int maxHealth     = 3;
    private int currentHealth = 3;

    private enum State { IDLE, RUN, JUMP, FALL, ATTACK, DASH, HIT, DEATH }
    private State currentState = State.IDLE;

    // Animaciones
    private Texture idleSheet, runSheet, jumpSheet, fallSheet;
    private Texture attackSheet, attackComboSheet, deathSheet, hitSheet, dashSheet;
    private Animation<TextureRegion> idleAnim, runAnim, jumpAnim, fallAnim;
    private Animation<TextureRegion> attackAnim, attackComboAnim, deathAnim, hitAnim, dashAnim;

    private TiledMapTileLayer collisionLayer;

    public Player(float startX, float startY, TiledMapTileLayer collisionLayer) {
        this.x = startX;
        this.y = startY;
        this.collisionLayer = collisionLayer;

        idleSheet        = new Texture("player/idle.png");
        runSheet         = new Texture("player/run.png");
        jumpSheet        = new Texture("player/jump.png");
        fallSheet        = new Texture("player/fall.png");
        attackSheet      = new Texture("player/attack.png");
        attackComboSheet = new Texture("player/attack_combo.png");
        deathSheet       = new Texture("player/death.png");
        hitSheet         = new Texture("player/hit.png");
        dashSheet        = new Texture("player/dash.png");

        idleAnim        = makeAnim(idleSheet,        10, 0.10f, true);
        runAnim         = makeAnim(runSheet,          10, 0.07f, true);
        jumpAnim        = makeAnim(jumpSheet,          3, 0.10f, false);
        fallAnim        = makeAnim(fallSheet,          3, 0.10f, true);
        attackAnim      = makeAnim(attackSheet,        4, 0.08f, false);
        attackComboAnim = makeAnim(attackComboSheet,  10, 0.07f, false);
        deathAnim       = makeAnim(deathSheet,        10, 0.09f, false);
        hitAnim         = makeAnim(hitSheet,           1, 0.10f, false);
        dashAnim        = makeAnim(dashSheet,          2, 0.08f, false);
    }

    private Animation<TextureRegion> makeAnim(Texture sheet, int frames, float duration, boolean loop) {
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++)
            regions[i] = new TextureRegion(sheet, i * FRAME_W, 0, FRAME_W, FRAME_H);
        Animation<TextureRegion> anim = new Animation<>(duration, regions);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return anim;
    }

    public void update(float dt) {
        if (!alive) return;
        stateTime += dt;

        // --- Muerte en curso ---
        if (dying) {
            if (deathAnim.isAnimationFinished(stateTime)) alive = false;
            return;
        }

        // --- Hit en curso ---
        if (hit) {
            hitTimer -= dt;
            if (hitTimer <= 0) hit = false;
        }

        // --- Ataque y combo ---
        if (attacking) {
            attackTimer -= dt;
            // Si se pulsa J durante el ataque normal, encola el combo
            if (!combo && Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                comboQueued = true;
            }
            if (attackTimer <= 0) {
                if (!combo && comboQueued) {
                    // Activar combo
                    combo       = true;
                    comboQueued = false;
                    attackTimer = COMBO_DURATION;
                    stateTime   = 0f;
                } else {
                    // Fin del ataque
                    attacking   = false;
                    combo       = false;
                    comboQueued = false;
                }
            }
        }

        // --- Dash ---
        if (dashing) {
            dashTimer -= dt;
            x += (facingRight ? DASH_SPEED : -DASH_SPEED) * dt;
            if (dashTimer <= 0) dashing = false;
        }

        // --- Movimiento normal (solo si no ataca ni dashea) ---
        boolean moving = false;
        if (!dashing && !attacking) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) { x -= SPEED * dt; facingRight = false; moving = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) { x += SPEED * dt; facingRight = true;  moving = true; }
        }

        // --- Gravedad ---
        velocityY -= GRAVITY * dt;
        y += velocityY * dt;

        resolveMapCollision();

        // --- Inputs de acción ---
        if (!attacking && !dashing) {
            // Salto
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && onGround) {
                velocityY = JUMP_FORCE;
                onGround = false;
            }
            // Ataque (J) - solo si no está ya atacando
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                attacking   = true;
                combo       = false;
                comboQueued = false;
                attackTimer = ATTACK_DURATION;
                stateTime   = 0f;
            }
            // Dash (K)
            if (Gdx.input.isKeyJustPressed(Input.Keys.K) && onGround) {
                dashing   = true;
                dashTimer = DASH_TIME;
                stateTime = 0f;
            }
        }

        // --- Estado animación ---
        State newState;
        if (dying)          newState = State.DEATH;
        else if (hit)       newState = State.HIT;
        else if (dashing)   newState = State.DASH;
        else if (attacking) newState = State.ATTACK;
        else if (!onGround) newState = velocityY > 0 ? State.JUMP : State.FALL;
        else                newState = moving ? State.RUN : State.IDLE;

        if (newState != currentState) { currentState = newState; stateTime = 0f; }
    }

    private void resolveMapCollision() {
        // Caída (Y disminuye)
        if (velocityY <= 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)(y / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y        = (ty + 1) * TILE_SIZE;
                velocityY = 0;
                onGround  = true;
                return;
            }
        }
        onGround = false;

        // Subida (Y aumenta)
        if (velocityY > 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)((y + HITBOX_H) / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y        = ty * TILE_SIZE - HITBOX_H;
                velocityY = 0;
            }
        }
    }

    private boolean isSolid(int tx, int ty) {
        if (collisionLayer == null) return false;
        return collisionLayer.getCell(tx, ty) != null;
    }

    public void draw(SpriteBatch batch) {
        Animation<TextureRegion> anim;
        switch (currentState) {
            case RUN:    anim = runAnim;         break;
            case JUMP:   anim = jumpAnim;        break;
            case FALL:   anim = fallAnim;        break;
            case ATTACK: anim = combo ? attackComboAnim : attackAnim; break;
            case DASH:   anim = dashAnim;        break;
            case HIT:    anim = hitAnim;         break;
            case DEATH:  anim = deathAnim;       break;
            default:     anim = idleAnim;        break;
        }
        TextureRegion frame = anim.getKeyFrame(stateTime);
        float drawX = x - (FRAME_W - HITBOX_W) / 2f;
        float drawY = y;
        if (facingRight) batch.draw(frame, drawX, drawY, FRAME_W, FRAME_H);
        else             batch.draw(frame, drawX + FRAME_W, drawY, -FRAME_W, FRAME_H);
    }

    /** Llamado cuando un enemigo golpea al jugador */
    public void takeHit() {
        if (hit || dying) return;
        hit           = true;
        hitTimer      = HIT_DURATION;
        currentHealth--;
        if (currentHealth <= 0) {
            kill();
        }
    }

    public boolean isHit() { return hit; }

    public int getHealth()    { return currentHealth; }
    public int getMaxHealth() { return maxHealth; }

    /** Llamado cuando el jugador muere */
    public void kill() {
        if (dying) return;
        dying     = true;
        stateTime = 0f;
        currentState = State.DEATH;
    }

    /** Devuelve true si el ataque está activo (para detectar si golpea a enemigos) */
    public boolean isAttacking() { return attacking; }

    public Rectangle getBounds()      { return new Rectangle(x, y, HITBOX_W, HITBOX_H); }
    public Rectangle getAttackRange() {
        // Hitbox del ataque delante del jugador
        float ax = facingRight ? x + HITBOX_W : x - 25f;
        return new Rectangle(ax, y, 25f, HITBOX_H);
    }

    public boolean isAlive() { return alive; }
    public float   getX()    { return x; }
    public float   getY()    { return y; }

    public void dispose() {
        idleSheet.dispose(); runSheet.dispose(); jumpSheet.dispose(); fallSheet.dispose();
        attackSheet.dispose(); attackComboSheet.dispose(); deathSheet.dispose();
        hitSheet.dispose(); dashSheet.dispose();
    }
}
