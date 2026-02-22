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
    private boolean attacking   = false;
    private boolean combo       = false;
    private boolean comboQueued = false;
    private float   attackTimer = 0f;
    private static final float ATTACK_DURATION = 0.32f;
    private static final float COMBO_DURATION  = 0.70f;

    // Dash
    private boolean dashing   = false;
    private float   dashTimer = 0f;

    // Hit / invencibilidad
    private boolean hit      = false;
    private float   hitTimer = 0f;
    private static final float HIT_DURATION = 0.6f; // más corto → más dinámico

    // Parpadeo durante invencibilidad
    private static final float BLINK_INTERVAL = 0.08f;
    private float blinkTimer   = 0f;
    private boolean blinkVisible = true;

    // Knockback al recibir daño
    private float knockbackVelX = 0f;
    private static final float KNOCKBACK_FORCE = 220f;
    private static final float KNOCKBACK_DECAY = 800f;

    // Muerte
    private boolean dying = false;

    // Eventos de sonido (se consumen cada frame desde MainGame)
    public boolean eventAttack    = false;
    public boolean eventJump      = false;
    public boolean eventDash      = false;
    public boolean eventDeath     = false;
    public boolean eventHit       = false;
    public boolean eventRunning   = false;
    public boolean eventOnGround  = false;
    public boolean eventWasOnGround = false;

    // Sistema de vida
    private int maxHealth     = 3;
    private int currentHealth = 3;

    private enum State { IDLE, RUN, JUMP, FALL, ATTACK, DASH, HIT, DEATH }
    private State currentState = State.IDLE;

    private Texture idleSheet, runSheet, jumpSheet, fallSheet;
    private Texture attackSheet, attackComboSheet, deathSheet, hitSheet, dashSheet;
    private Animation<TextureRegion> idleAnim, runAnim, jumpAnim, fallAnim;
    private Animation<TextureRegion> attackAnim, attackComboAnim, deathAnim, hitAnim, dashAnim;

    public TiledMapTileLayer collisionLayer;

    // Límites del mapa para no salirse
    public float mapMinX = 0f;
    public float mapMaxX = Float.MAX_VALUE;

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

        // --- Invencibilidad y parpadeo ---
        if (hit) {
            hitTimer -= dt;
            blinkTimer -= dt;
            if (blinkTimer <= 0) {
                blinkVisible = !blinkVisible;
                blinkTimer = BLINK_INTERVAL;
            }
            if (hitTimer <= 0) {
                hit = false;
                blinkVisible = true;
            }
        }

        // --- Knockback ---
        if (knockbackVelX != 0) {
            x += knockbackVelX * dt;
            if (knockbackVelX > 0) {
                knockbackVelX = Math.max(0, knockbackVelX - KNOCKBACK_DECAY * dt);
            } else {
                knockbackVelX = Math.min(0, knockbackVelX + KNOCKBACK_DECAY * dt);
            }
        }

        // --- Ataque y combo ---
        if (attacking) {
            attackTimer -= dt;
            if (!combo && Gdx.input.isKeyJustPressed(Input.Keys.J)) comboQueued = true;
            if (attackTimer <= 0) {
                if (!combo && comboQueued) {
                    combo = true; comboQueued = false;
                    attackTimer = COMBO_DURATION; stateTime = 0f;
                } else {
                    attacking = false; combo = false; comboQueued = false;
                }
            }
        }

        // --- Dash ---
        if (dashing) {
            dashTimer -= dt;
            x += (facingRight ? DASH_SPEED : -DASH_SPEED) * dt;
            if (dashTimer <= 0) dashing = false;
        }

        // --- Movimiento ---
        boolean moving = false;
        if (!dashing && !attacking) {
            if (Gdx.input.isKeyPressed(Input.Keys.A)) { x -= SPEED * dt; facingRight = false; moving = true; }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) { x += SPEED * dt; facingRight = true;  moving = true; }
        }

        // Guardar estado anterior de suelo
        eventWasOnGround = onGround;

        // --- Gravedad ---
        velocityY -= GRAVITY * dt;
        y += velocityY * dt;

        resolveMapCollision();
        eventOnGround = onGround;

        // Limitar dentro del mapa
        if (x < mapMinX) x = mapMinX;
        if (x > mapMaxX - HITBOX_W) x = mapMaxX - HITBOX_W;

        // --- Inputs ---
        if (!attacking && !dashing) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && onGround) {
                velocityY = JUMP_FORCE; onGround = false;
                eventJump = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                attacking = true; combo = false; comboQueued = false;
                attackTimer = ATTACK_DURATION; stateTime = 0f;
                eventAttack = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.K) && onGround) {
                dashing = true; dashTimer = DASH_TIME; stateTime = 0f;
                eventDash = true;
            }
        }

        eventRunning = moving && onGround;

        // --- Estado animación ---
        State newState;
        if      (dying)      newState = State.DEATH;
        else if (hit)        newState = State.HIT;
        else if (dashing)    newState = State.DASH;
        else if (attacking)  newState = State.ATTACK;
        else if (!onGround)  newState = velocityY > 0 ? State.JUMP : State.FALL;
        else                 newState = moving ? State.RUN : State.IDLE;

        if (newState != currentState) { currentState = newState; stateTime = 0f; }
    }

    private void resolveMapCollision() {
        if (velocityY <= 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)(y / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y = (ty + 1) * TILE_SIZE; velocityY = 0; onGround = true; return;
            }
        }
        onGround = false;
        if (velocityY > 0) {
            int tx1 = (int)(x / TILE_SIZE);
            int tx2 = (int)((x + HITBOX_W - 1) / TILE_SIZE);
            int ty  = (int)((y + HITBOX_H) / TILE_SIZE);
            if (isSolid(tx1, ty) || isSolid(tx2, ty)) {
                y = ty * TILE_SIZE - HITBOX_H; velocityY = 0;
            }
        }
    }

    private boolean isSolid(int tx, int ty) {
        if (collisionLayer == null) return false;
        return collisionLayer.getCell(tx, ty) != null;
    }

    public void draw(SpriteBatch batch) {
        // No dibujar durante los frames de parpadeo
        if (hit && !blinkVisible) return;

        Animation<TextureRegion> anim;
        switch (currentState) {
            case RUN:    anim = runAnim;  break;
            case JUMP:   anim = jumpAnim; break;
            case FALL:   anim = fallAnim; break;
            case ATTACK: anim = combo ? attackComboAnim : attackAnim; break;
            case DASH:   anim = dashAnim; break;
            case HIT:    anim = hitAnim;  break;
            case DEATH:  anim = deathAnim; break;
            default:     anim = idleAnim; break;
        }
        TextureRegion frame = anim.getKeyFrame(stateTime);
        float drawX = x - (FRAME_W - HITBOX_W) / 2f;
        float drawY = y;
        if (facingRight) batch.draw(frame, drawX, drawY, FRAME_W, FRAME_H);
        else             batch.draw(frame, drawX + FRAME_W, drawY, -FRAME_W, FRAME_H);
    }

    public void takeHit(float enemyX) {
        if (hit || dying) return;
        hit = true; hitTimer = HIT_DURATION;
        eventHit = true;
        blinkTimer = BLINK_INTERVAL; blinkVisible = true;
        currentHealth--;
        // Knockback alejándose del enemigo
        knockbackVelX = (x > enemyX) ? KNOCKBACK_FORCE : -KNOCKBACK_FORCE;
        if (currentHealth <= 0) kill();
    }

    // Sobrecarga sin knockback por compatibilidad
    public void takeHit() { takeHit(x); }

    public boolean isHit()        { return hit; }
    public int getHealth()        { return currentHealth; }
    public int getMaxHealth()     { return maxHealth; }
    public boolean isAttacking()  { return attacking; }
    public boolean isAlive()      { return alive; }
    public float getX()           { return x; }
    public float getY()           { return y; }

    public void kill() {
        if (dying) return;
        dying = true; stateTime = 0f; currentState = State.DEATH;
        eventDeath = true;
    }

    public Rectangle getBounds() { return new Rectangle(x, y, HITBOX_W, HITBOX_H); }
    public Rectangle getAttackRange() {
        float ax = facingRight ? x + HITBOX_W : x - 25f;
        return new Rectangle(ax, y, 25f, HITBOX_H);
    }

    public void dispose() {
        idleSheet.dispose(); runSheet.dispose(); jumpSheet.dispose(); fallSheet.dispose();
        attackSheet.dispose(); attackComboSheet.dispose(); deathSheet.dispose();
        hitSheet.dispose(); dashSheet.dispose();
    }
}
