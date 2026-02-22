package com.svalero.penitent;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;

public class Enemy {

    private static final int FRAME_W = 99;
    private static final int FRAME_H = 46;

    public static final float HITBOX_W = 40f;
    public static final float HITBOX_H = 44f;

    private float leftLimit, rightLimit;
    private float speed = 60f;
    private boolean movingRight = true;

    public float x, y;
    public float velocityY = 0;
    private boolean alive    = true;
    private float   stateTime = 0f;
    private boolean dying    = false;
    private int health = 2; // golpes para morir

    // Física Y-arriba (igual que Player)
    private static final float GRAVITY   = 800f;
    private static final int   TILE_SIZE = 32;

    private TiledMapTileLayer collisionLayer;

    private enum State { RUN, DEATH }
    private State currentState = State.RUN;

    private Texture idleSheet, runSheet, deathSheet, hitSheet;
    private Animation<TextureRegion> idleAnim, runAnim, deathAnim, hitAnim;

    private boolean isHit    = false;
    private float   hitTimer = 0f;
    private static final float HIT_DURATION = 0.2f;

    public Enemy(float startX, float startY, float leftLimit, float rightLimit,
                 TiledMapTileLayer collisionLayer) {
        this.x = startX;
        this.y = startY;
        this.leftLimit  = leftLimit;
        this.rightLimit = rightLimit;
        this.collisionLayer = collisionLayer;

        idleSheet  = new Texture("enemies/enemy_idle.png");
        runSheet   = new Texture("enemies/enemy_run.png");
        deathSheet = new Texture("enemies/enemy_death.png");
        hitSheet   = new Texture("enemies/enemy_hit.png");

        idleAnim  = makeAnim(idleSheet,   8, 0.10f, true);
        runAnim   = makeAnim(runSheet,    8, 0.08f, true);
        deathAnim = makeAnim(deathSheet, 14, 0.07f, false);
        hitAnim   = makeAnim(hitSheet,    2, 0.08f, false);
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
        stateTime += dt;

        // Hit timer
        if (isHit) {
            hitTimer -= dt;
            if (hitTimer <= 0) isHit = false;
        }

        // Muriendo: espera a que termine la animación
        if (dying) {
            if (deathAnim.isAnimationFinished(stateTime)) alive = false;
            return;
        }

        // Patrulla
        if (movingRight) { x += speed * dt; if (x >= rightLimit) { x = rightLimit; movingRight = false; } }
        else             { x -= speed * dt; if (x <= leftLimit)  { x = leftLimit;  movingRight = true;  } }

        // Gravedad (Y-arriba: resta)
        velocityY -= GRAVITY * dt;
        y += velocityY * dt;

        // Colisión suelo
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

    public void draw(SpriteBatch batch) {
        Animation<TextureRegion> anim;
        if (dying)       anim = deathAnim;
        else if (isHit)  anim = hitAnim;
        else             anim = runAnim;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        float drawX = x - (FRAME_W - HITBOX_W) / 2f;
        float drawY = y - (FRAME_H - HITBOX_H) / 2f;
        if (movingRight) batch.draw(frame, drawX, drawY, FRAME_W, FRAME_H);
        else             batch.draw(frame, drawX + FRAME_W, drawY, -FRAME_W, FRAME_H);
    }

    public void hit() {
        if (dying) return;
        health--;
        if (health <= 0) {
            dying     = true;
            stateTime = 0f;
        } else {
            isHit    = true;
            hitTimer = HIT_DURATION;
        }
    }

    // Mantenemos kill() para compatibilidad (mata de un golpe)
    public void kill() {
        health = 0;
        hit();
    }

    public Rectangle getBounds() { return new Rectangle(x, y, HITBOX_W, HITBOX_H); }
    public boolean isAlive()     { return alive; }

    public void dispose() {
        idleSheet.dispose(); runSheet.dispose(); deathSheet.dispose(); hitSheet.dispose();
    }
}
