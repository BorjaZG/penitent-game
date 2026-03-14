package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class BatEnemy {

    // ── Dimensiones en mundo ─────────────────────────────────────────────────
    public static final float W = 48f;
    public static final float H = 48f;

    // ── Posición ─────────────────────────────────────────────────────────────
    public float x, y;          // esquina inferior-izquierda del hitbox
    private float hangX, hangY; // posición inicial (punto de cuelgue)

    // Posición del jugador (actualizada cada frame)
    private float playerX, playerY;

    // ── IA – distancias ───────────────────────────────────────────────────────
    private static final float WAKE_DIST    = 180f; // detecta al jugador
    private static final float CHASE_DIST   = 220f; // sigue persiguiendo (leash corto)
    private static final float ATTACK_DIST  = 55f;  // lanza ataque
    private static final float CHASE_SPEED   = 90f;
    private static final float RETURN_SPEED  = 45f;  // velocidad de vuelta al punto de cuelgue

    // ── Ataque ────────────────────────────────────────────────────────────────
    private static final float ATTACK_W = 60f;
    private static final float ATTACK_H = 40f;
    private boolean attackActive  = false; // ventana de daño abierta
    private boolean useAttack2    = false; // alterna ataque 1/2
    private float   attackCooldown = 0f;
    private static final float ATTACK_CD = 1.2f;

    // ── Vida / estado ─────────────────────────────────────────────────────────
    private int     hp          = 2;
    private boolean alive       = true;
    private boolean dying       = false;
    private float   hurtTimer   = 0f;
    private static final float HURT_LOCK = 0.3f;

    // ── Eventos para GameScreen ────────────────────────────────────────────────
    public boolean eventDeath       = false;
    public boolean eventAttackStart = false;
    public boolean eventHit         = false;

    // ── Flip ──────────────────────────────────────────────────────────────────
    private boolean facingRight = true;

    // ── Estado ────────────────────────────────────────────────────────────────
    private enum State { SLEEP, WAKE_UP, IDLE_FLY, CHASE, RETURN, ATTACK1, ATTACK2, HURT, DIE }
    private State state = State.SLEEP;

    // ── Animaciones ───────────────────────────────────────────────────────────
    private Animation<TextureRegion> animSleep, animWakeUp, animIdleFly,
        animRun, animAttack1, animAttack2,
        animHurt, animDie;
    private float stateTime = 0f;

    // Texturas (guardadas para dispose)
    private Texture tSleep, tWakeUp, tIdleFly, tRun, tAttack1, tAttack2, tHurt, tDie;

    // ─────────────────────────────────────────────────────────────────────────

    public BatEnemy(float hangX, float hangY) {
        this.hangX = hangX;
        this.hangY = hangY;
        // En sleep el bat cuelga: su esquina superior está en hangY
        // x centrado en el punto de cuelgue
        this.x = hangX - W / 2f;
        this.y = hangY - H; // y = borde inferior del sprite
        loadAnimations();
    }

    // ── Carga de sprites ─────────────────────────────────────────────────────

    private Animation<TextureRegion> buildAnim(Texture tex, int frameCount, float fps) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++)
            frames[i] = new TextureRegion(tex, i * 64, 0, 64, 64);
        return new Animation<>(1f / fps, frames);
    }

    private void loadAnimations() {
        tSleep   = new Texture(Gdx.files.internal("enemies/bat/Bat-Sleep.png"));
        tWakeUp  = new Texture(Gdx.files.internal("enemies/bat/Bat-WakeUp.png"));
        tIdleFly = new Texture(Gdx.files.internal("enemies/bat/Bat-IdleFly.png"));
        tRun     = new Texture(Gdx.files.internal("enemies/bat/Bat-Run.png"));
        tAttack1 = new Texture(Gdx.files.internal("enemies/bat/Bat-Attack1.png"));
        tAttack2 = new Texture(Gdx.files.internal("enemies/bat/Bat-Attack2.png"));
        tHurt    = new Texture(Gdx.files.internal("enemies/bat/Bat-Hurt.png"));
        tDie     = new Texture(Gdx.files.internal("enemies/bat/Bat-Die.png"));

        animSleep   = buildAnim(tSleep,    3, 6f);
        animWakeUp  = buildAnim(tWakeUp,  16, 14f);
        animIdleFly = buildAnim(tIdleFly,  9, 10f);
        animRun     = buildAnim(tRun,      8, 12f);
        animAttack1 = buildAnim(tAttack1,  8, 14f);
        animAttack2 = buildAnim(tAttack2, 11, 14f);
        animHurt    = buildAnim(tHurt,     5, 14f);
        animDie     = buildAnim(tDie,     12, 12f);

        animSleep.setPlayMode(Animation.PlayMode.LOOP);
        animIdleFly.setPlayMode(Animation.PlayMode.LOOP);
        animRun.setPlayMode(Animation.PlayMode.LOOP);
        // El resto son NORMAL (oneshot por defecto)
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void setPlayerPosition(float px, float py) {
        playerX = px + Player.HITBOX_W / 2f;
        playerY = py + Player.HITBOX_H / 2f;
    }

    public void update(float dt) {
        if (!alive && !dying) return;

        stateTime += dt;
        if (attackCooldown > 0) attackCooldown -= dt;
        if (hurtTimer > 0) hurtTimer -= dt;

        switch (state) {
            case SLEEP:    updateSleep();   break;
            case WAKE_UP:  updateWakeUp();  break;
            case IDLE_FLY: updateIdleFly(); break;
            case CHASE:    updateChase(dt); break;
            case RETURN:   updateReturn();  break;
            case ATTACK1:  updateAttack1(); break;
            case ATTACK2:  updateAttack2(); break;
            case HURT:     updateHurt();    break;
            case DIE:      updateDie();     break;
        }
    }

    private float distToPlayer() {
        float cx = x + W / 2f;
        float cy = y + H / 2f;
        float dx = playerX - cx;
        float dy = playerY - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void updateSleep() {
        attackActive = false;
        if (distToPlayer() < WAKE_DIST) {
            setState(State.WAKE_UP);
        }
    }

    private void updateWakeUp() {
        attackActive = false;
        float dur = animWakeUp.getAnimationDuration();
        if (stateTime >= dur) {
            setState(State.IDLE_FLY);
        }
    }

    private void updateIdleFly() {
        attackActive = false;
        float dist = distToPlayer();
        if (dist < ATTACK_DIST && attackCooldown <= 0) {
            setState(useAttack2 ? State.ATTACK2 : State.ATTACK1);
        } else if (dist < CHASE_DIST) {
            setState(State.CHASE);
        } else {
            // Lejos del jugador: volver flotando al punto de cuelgue
            float hangCX = hangX;
            float hangCY = hangY - H / 2f;
            float dx = hangCX - (x + W / 2f);
            float dy = hangCY - (y + H / 2f);
            float distHang = (float) Math.sqrt(dx * dx + dy * dy);
            if (distHang > 8f) setState(State.RETURN);
        }
    }

    private void updateChase(float dt) {
        attackActive = false;
        float dist = distToPlayer();

        if (dist < ATTACK_DIST && attackCooldown <= 0) {
            setState(useAttack2 ? State.ATTACK2 : State.ATTACK1);
            return;
        }
        if (dist > CHASE_DIST) {
            setState(State.RETURN);  // vuelve a posición de cuelgue
            return;
        }

        // Mover hacia el jugador en 2D (vuela libremente)
        float cx = x + W / 2f;
        float cy = y + H / 2f;
        float dx = playerX - cx;
        float dy = playerY - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 1f) {
            x += (dx / len) * CHASE_SPEED * dt;
            y += (dy / len) * CHASE_SPEED * dt;
        }
        facingRight = (playerX > x + W / 2f);
    }

    private void updateReturn() {
        attackActive = false;
        // Volar de vuelta al punto de cuelgue
        float targetX = hangX;
        float targetY = hangY - H;
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 4f) {
            // Llegó al punto de cuelgue: volver a dormir
            x = targetX;
            y = targetY;
            setState(State.SLEEP);
            return;
        }
        // Mover hacia el punto de cuelgue
        x += (dx / dist) * RETURN_SPEED * (1f / 60f);  // aprox dt
        y += (dy / dist) * RETURN_SPEED * (1f / 60f);
        facingRight = (dx > 0);

        // Si el jugador se acerca de nuevo, retomar persecución
        if (distToPlayer() < CHASE_DIST) setState(State.CHASE);
    }

    private void updateAttack1() {
        // Ventana de daño: frames 3-6 (de 8)
        float frameDur = animAttack1.getFrameDuration();
        float elapsed  = stateTime;
        attackActive = (elapsed >= frameDur * 3 && elapsed < frameDur * 6);
        if (stateTime == 0f) {
            eventAttackStart = true;
            useAttack2 = true;
        }
        if (stateTime >= animAttack1.getAnimationDuration()) {
            attackCooldown = ATTACK_CD;
            attackActive   = false;
            setState(State.CHASE);
        }
    }

    private void updateAttack2() {
        float frameDur = animAttack2.getFrameDuration();
        float elapsed  = stateTime;
        attackActive = (elapsed >= frameDur * 3 && elapsed < frameDur * 7);
        if (stateTime == 0f) {
            eventAttackStart = true;
            useAttack2 = false;
        }
        if (stateTime >= animAttack2.getAnimationDuration()) {
            attackCooldown = ATTACK_CD;
            attackActive   = false;
            setState(State.CHASE);
        }
    }

    private void updateHurt() {
        attackActive = false;
        if (hurtTimer <= 0 && stateTime >= animHurt.getAnimationDuration()) {
            if (hp <= 0) {
                setState(State.DIE);
            } else {
                setState(State.CHASE);
            }
        }
    }

    private void updateDie() {
        attackActive = false;
        dying = true;
        if (stateTime >= animDie.getAnimationDuration()) {
            alive  = false;
            dying  = false;
            eventDeath = true;
        }
    }

    // ── Recibir daño ──────────────────────────────────────────────────────────

    public void hit() {
        if (!alive || dying || state == State.HURT || state == State.DIE) return;
        hp--;
        eventHit = true;
        hurtTimer = HURT_LOCK;
        if (hp <= 0) setState(State.DIE);
        else         setState(State.HURT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setState(State newState) {
        state     = newState;
        stateTime = 0f;
        if (newState == State.ATTACK1 || newState == State.ATTACK2)
            eventAttackStart = true;
    }

    public boolean isAlive()        { return alive; }
    public boolean isDying()        { return dying; }
    public boolean isAttackActive() { return attackActive; }
    public boolean isSleeping()     { return state == State.SLEEP; }

    public Rectangle getBounds() { return new Rectangle(x, y, W, H); }

    public Rectangle getAttackBounds() {
        // Hitbox de ataque delante del bat
        float ax = facingRight ? x + W : x - ATTACK_W;
        return new Rectangle(ax, y + H / 4f, ATTACK_W, ATTACK_H);
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    public void draw(SpriteBatch batch) {
        if (!alive && !dying) return;

        Animation<TextureRegion> anim = getCurrentAnim();
        boolean loop = anim.getPlayMode() == Animation.PlayMode.LOOP;
        TextureRegion frame = anim.getKeyFrame(stateTime, loop);

        // Flip horizontal según dirección
        boolean needFlip = (!facingRight && !frame.isFlipX()) ||
            ( facingRight &&  frame.isFlipX());
        if (needFlip) frame.flip(true, false);

        // En SLEEP: cuelga boca abajo (flip vertical)
        boolean sleepFlip = (state == State.SLEEP || state == State.WAKE_UP);
        if (sleepFlip && !frame.isFlipY()) frame.flip(false, true);
        else if (!sleepFlip && frame.isFlipY()) frame.flip(false, true);

        batch.draw(frame, x, y, W, H);
    }

    private Animation<TextureRegion> getCurrentAnim() {
        switch (state) {
            case SLEEP:    return animSleep;
            case WAKE_UP:  return animWakeUp;
            case IDLE_FLY: return animIdleFly;
            case CHASE:    return animRun;
            case RETURN:   return animIdleFly;
            case ATTACK1:  return animAttack1;
            case ATTACK2:  return animAttack2;
            case HURT:     return animHurt;
            case DIE:      return animDie;
            default:       return animIdleFly;
        }
    }

    // ── Dispose ───────────────────────────────────────────────────────────────

    public void dispose() {
        tSleep.dispose(); tWakeUp.dispose(); tIdleFly.dispose();
        tRun.dispose();   tAttack1.dispose(); tAttack2.dispose();
        tHurt.dispose();  tDie.dispose();
    }
}
