package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Altar de checkpoint al estilo Blasphemous.
 * - Inactivo (nunca usado): blanco y negro
 * - En rango pero sin activar: muestra "B - Rezar"
 * - Activo (ya usado 1+ veces): color normal + brillo dorado
 * Tecla B activa: cura, guarda, resetea enemigos del otro mapa.
 */
public class Checkpoint {

    public  final float x, y;
    public  static final float W = 96f;   // 3 tiles
    public  static final float H = 128f;  // 4 tiles

    // Distancia para mostrar el prompt y poder rezar
    private static final float INTERACT_RADIUS = 48f;

    // Estados
    private boolean everActivated = false;  // ¿se ha usado alguna vez?
    private boolean playerInRange = false;  // para mostrar el prompt

    private float glowTimer = 0f;

    private Texture texture;

    public Checkpoint(float x, float y) {
        this.x = x;
        this.y = y;
        texture = new Texture("totem_checkpoint.png");
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public boolean isEverActivated() { return everActivated; }
    public boolean isPlayerInRange() { return playerInRange; }

    /**
     * Actualiza el estado de rango del jugador.
     * Devuelve true si el jugador pulsó B estando en rango.
     */
    public boolean update(float dt, float playerX, float playerY) {
        if (everActivated) glowTimer += dt;

        float cx = x + W / 2f;
        float cy = y + H / 2f;
        float px = playerX + Player.HITBOX_W / 2f;
        float py = playerY + Player.HITBOX_H / 2f;
        double dist = Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
        playerInRange = dist < INTERACT_RADIUS;

        if (playerInRange && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            everActivated = true;
            return true;  // señal de activación
        }
        return false;
    }

    public void draw(SpriteBatch batch) {
        if (!everActivated) {
            // Blanco y negro: desaturar con gris uniforme
            batch.setColor(0.45f, 0.45f, 0.45f, 1f);
        } else {
            // Color con brillo dorado parpadeante
            float glow = 0.88f + 0.12f * (float) Math.sin(glowTimer * 5f);
            batch.setColor(glow, glow * 0.90f, glow * 0.55f, 1f);
        }
        batch.draw(texture, x, y, W, H);
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
