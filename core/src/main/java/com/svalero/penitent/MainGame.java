package com.svalero.penitent;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MainGame extends ApplicationAdapter {
    private SpriteBatch batch;

    private Texture idleSheet, runSheet, jumpSheet, fallSheet;
    private Animation<TextureRegion> idleAnim, runAnim, jumpAnim, fallAnim;

    private float stateTime = 0f;

    private float x = 200, y = 120;
    private float velocityY = 0;
    private boolean onGround = false;

    private float gravity = -800f;
    private float jumpForce = 380f;

    private float groundY = 100; // altura del suelo
    private boolean facingRight = true;

    private static final int FRAME_W = 120;
    private static final int FRAME_H = 80;

    // Para resetear animación al cambiar de estado
    private enum State { IDLE, RUN, JUMP, FALL }
    private State currentState = State.IDLE;

    @Override
    public void create() {
        batch = new SpriteBatch();

        idleSheet = new Texture("player/idle.png");
        runSheet  = new Texture("player/run.png");
        jumpSheet = new Texture("player/jump.png");
        fallSheet = new Texture("player/fall.png");

        idleAnim = makeAnim(idleSheet, 0.10f); // 10 fps aprox
        runAnim  = makeAnim(runSheet,  0.08f); // un pelín más rápido
        jumpAnim = makeAnim(jumpSheet, 0.12f); // 3 frames
        fallAnim = makeAnim(fallSheet, 0.10f); // 2 frames
    }

    private Animation<TextureRegion> makeAnim(Texture sheet, float frameDuration) {
        TextureRegion[][] grid = TextureRegion.split(sheet, FRAME_W, FRAME_H);
        TextureRegion[] frames = grid[0]; // primera fila
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        stateTime += dt;

        float speed = 150f * dt;
        boolean moving = false;

        // Movimiento lateral + dirección
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            x -= speed;
            moving = true;
            facingRight = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            x += speed;
            moving = true;
            facingRight = true;
        }

        // Gravedad
        velocityY += gravity * dt;
        y += velocityY * dt;

        // Suelo
        if (y <= groundY) {
            y = groundY;
            velocityY = 0;
            onGround = true;
        } else {
            onGround = false;
        }

        // Salto
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && onGround) {
            velocityY = jumpForce;
            onGround = false;
        }

        // Fondo oscuro
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Estado actual (Idle/Run/Jump/Fall)
        State newState;
        if (!onGround) {
            newState = (velocityY > 0) ? State.JUMP : State.FALL;
        } else {
            newState = moving ? State.RUN : State.IDLE;
        }

        // Si cambia el estado, resetea la animación para que empiece desde el frame 1
        if (newState != currentState) {
            currentState = newState;
            stateTime = 0f;
        }

        // Elegir animación según estado
        Animation<TextureRegion> anim;
        switch (currentState) {
            case RUN:  anim = runAnim;  break;
            case JUMP: anim = jumpAnim; break;
            case FALL: anim = fallAnim; break;
            case IDLE:
            default:   anim = idleAnim; break;
        }

        TextureRegion frame = anim.getKeyFrame(stateTime);

        // Dibujar con flip (mirar izq/der)
        batch.begin();
        if (facingRight) {
            batch.draw(frame, x, y);
        } else {
            batch.draw(frame, x + FRAME_W, y, -FRAME_W, FRAME_H);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        idleSheet.dispose();
        runSheet.dispose();
        jumpSheet.dispose();
        fallSheet.dispose();
    }
}
