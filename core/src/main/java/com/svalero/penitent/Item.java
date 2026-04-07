package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Item {

    public enum ItemType {
        NUDOS_CORDON(
            "Nudos de Cordón de Rosario",
            "items/Nudos de Cordón de Rosario.png",
            "Cuentas de un rosario roto, atadas con hilos de pecado.\n" +
            "Cada nudo guarda una oración incompleta,\n" +
            "susurrada por labios que ya no existen."
        ),
        AZOQUE(
            "Azoque",
            "items/Azoque.png",
            "Metal vivo que no puede ser contenido.\n" +
            "Los alquimistas lo llamaban el espíritu de los metales.\n" +
            "Purifica el alma tanto como corroe la carne."
        ),
        MATRAZ_BILIAR(
            "Matraz biliar Vacío",
            "items/Matraz biliar Vacío.png",
            "Recipiente de vidrio teñido por la bilis de un penitente.\n" +
            "Vacío ahora, pero el olor acre que desprende\n" +
            "recuerda lo que alguna vez contuvo."
        ),
        VELO_NEGRO(
            "Velo Negro de Luto",
            "items/Velo Negro de Luto.png",
            "Tela de seda negra de una viuda\n" +
            "que juró no descubrirse hasta el fin de los tiempos.\n" +
            "Aún conserva el calor de su pena."
        ),
        CALIZ(
            "Cáliz de los Versos Invertidos",
            "items/Cáliz de los Versos Invertidos.png",
            "Copa de bronce ennegrecida por el tiempo.\n" +
            "Las palabras grabadas en su interior\n" +
            "solo pueden leerse desde el reflejo del agua bendita."
        );

        public final String displayName;
        public final String texturePath;
        public final String legend;

        ItemType(String displayName, String texturePath, String legend) {
            this.displayName = displayName;
            this.texturePath = texturePath;
            this.legend = legend;
        }
    }

    private static final float ITEM_SIZE   = 24f;
    private static final float BOB_SPEED   = 2.2f;
    private static final float BOB_AMP     = 3.5f;
    private static final float PICKUP_DIST = 36f;

    public final ItemType type;
    public final float spawnX, spawnY;
    public boolean collected = false;

    Texture texture; // package-private for InventoryOverlay
    private float bobTimer = 0f;

    public Item(ItemType type, float x, float y) {
        this.type   = type;
        this.spawnX = x;
        this.spawnY = y;
        this.texture = new Texture(type.texturePath);
    }

    public void update(float dt) {
        if (!collected) bobTimer += dt;
    }

    public void draw(SpriteBatch batch) {
        if (collected) return;
        float bobY = (float) Math.sin(bobTimer * BOB_SPEED) * BOB_AMP;
        batch.draw(texture, spawnX - ITEM_SIZE / 2f, spawnY + bobY, ITEM_SIZE, ITEM_SIZE);
    }

    public boolean isPlayerInRange(float playerCX, float playerCY) {
        if (collected) return false;
        float dx = playerCX - spawnX;
        float dy = playerCY - spawnY;
        return dx * dx + dy * dy <= PICKUP_DIST * PICKUP_DIST;
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
