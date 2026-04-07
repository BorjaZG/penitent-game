package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;

public class InventoryOverlay {

    private static final float VIEW_W   = 608f;
    private static final float VIEW_H   = 320f;
    private static final float PANEL_X  = 32f;
    private static final float PANEL_Y  = 14f;
    private static final float PANEL_W  = VIEW_W - 64f;
    private static final float PANEL_H  = VIEW_H - 28f;
    private static final float IMG_SIZE = 60f;
    private static final float SLOT_SZ  = 26f;
    private static final float SLOT_GAP = 6f;

    private int selectedIndex = 0;
    private final GlyphLayout layout = new GlyphLayout();
    private Texture bgTex;
    private Texture slotTex;

    public InventoryOverlay() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.07f, 0.04f, 0.04f, 0.96f);
        pm.fill();
        bgTex = new Texture(pm);
        pm.dispose();

        pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.2f, 0.15f, 0.12f, 1f);
        pm.fill();
        slotTex = new Texture(pm);
        pm.dispose();
    }

    public void handleInput(List<Item> items) {
        if (items.isEmpty()) return;
        if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)  || Gdx.input.isKeyJustPressed(Input.Keys.A))
            selectedIndex = (selectedIndex - 1 + items.size()) % items.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D))
            selectedIndex = (selectedIndex + 1) % items.size();
    }

    public void draw(SpriteBatch batch, List<Item> items, Texture fadeTex) {
        // Dark full-screen overlay
        batch.setColor(0f, 0f, 0f, 0.72f);
        batch.draw(fadeTex, 0, 0, VIEW_W, VIEW_H);

        // Panel
        batch.setColor(Color.WHITE);
        batch.draw(bgTex, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // ── Título ────────────────────────────────────────────────────────────
        FontManager.menu.setColor(new Color(0.92f, 0.78f, 0.28f, 1f));
        String title = "INVENTARIO";
        layout.setText(FontManager.menu, title);
        FontManager.menu.draw(batch, title,
            VIEW_W / 2f - layout.width / 2f,
            PANEL_Y + PANEL_H - 10f);

        // Separator line (thin yellow)
        batch.setColor(new Color(0.92f, 0.78f, 0.28f, 0.35f));
        batch.draw(fadeTex, PANEL_X + 12f, PANEL_Y + PANEL_H - 30f, PANEL_W - 24f, 1f);
        batch.setColor(Color.WHITE);

        if (items.isEmpty()) {
            // ── Sin objetos ────────────────────────────────────────────────────
            FontManager.small.setColor(new Color(0.58f, 0.52f, 0.48f, 1f));
            String empty = "No has recogido ningún objeto todavía.";
            layout.setText(FontManager.small, empty);
            FontManager.small.draw(batch, empty,
                VIEW_W / 2f - layout.width / 2f, VIEW_H / 2f + 10f);
        } else {
            if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            Item item = items.get(selectedIndex);

            // ── Fila de miniaturas ────────────────────────────────────────────
            float totalW = items.size() * (SLOT_SZ + SLOT_GAP) - SLOT_GAP;
            float rowX   = VIEW_W / 2f - totalW / 2f;
            float rowY   = PANEL_Y + PANEL_H - 60f;

            for (int i = 0; i < items.size(); i++) {
                float sx = rowX + i * (SLOT_SZ + SLOT_GAP);
                boolean sel = (i == selectedIndex);

                // Slot background
                batch.setColor(sel
                    ? new Color(0.35f, 0.28f, 0.12f, 1f)
                    : new Color(0.18f, 0.14f, 0.11f, 1f));
                batch.draw(slotTex, sx - 2, rowY - 2, SLOT_SZ + 4, SLOT_SZ + 4);

                // Border highlight
                if (sel) {
                    batch.setColor(new Color(0.92f, 0.78f, 0.28f, 0.9f));
                    batch.draw(fadeTex, sx - 2, rowY + SLOT_SZ + 2, SLOT_SZ + 4, 1f); // top
                    batch.draw(fadeTex, sx - 2, rowY - 3, SLOT_SZ + 4, 1f);           // bottom
                    batch.draw(fadeTex, sx - 3, rowY - 3, 1f, SLOT_SZ + 6);           // left
                    batch.draw(fadeTex, sx + SLOT_SZ + 2, rowY - 3, 1f, SLOT_SZ + 6); // right
                }

                batch.setColor(Color.WHITE);
                batch.draw(items.get(i).texture, sx, rowY, SLOT_SZ, SLOT_SZ);
            }

            // ── Imagen grande ─────────────────────────────────────────────────
            float imgX = VIEW_W / 2f - IMG_SIZE / 2f;
            float imgY = rowY - IMG_SIZE - 16f;
            batch.setColor(Color.WHITE);
            batch.draw(item.texture, imgX, imgY, IMG_SIZE, IMG_SIZE);

            // ── Nombre del objeto ─────────────────────────────────────────────
            FontManager.menu.setColor(new Color(0.95f, 0.88f, 0.55f, 1f));
            layout.setText(FontManager.menu, item.type.displayName);
            FontManager.menu.draw(batch, item.type.displayName,
                VIEW_W / 2f - layout.width / 2f, imgY - 10f);

            // ── Leyenda (multi-línea) ─────────────────────────────────────────
            FontManager.small.setColor(new Color(0.78f, 0.70f, 0.62f, 1f));
            String[] lines = item.type.legend.split("\n");
            float legendY  = imgY - 28f;
            for (String line : lines) {
                layout.setText(FontManager.small, line);
                FontManager.small.draw(batch, line,
                    VIEW_W / 2f - layout.width / 2f, legendY);
                legendY -= 15f;
            }
        }

        // ── Hint de navegación ────────────────────────────────────────────────
        FontManager.small.setColor(new Color(0.42f, 0.38f, 0.36f, 0.85f));
        String hint = items.size() > 1
            ? "< > Navegar   I / ESC Cerrar"
            : "I / ESC Cerrar";
        layout.setText(FontManager.small, hint);
        FontManager.small.draw(batch, hint,
            VIEW_W / 2f - layout.width / 2f, PANEL_Y + 12f);
    }

    public void dispose() {
        if (bgTex   != null) bgTex.dispose();
        if (slotTex != null) slotTex.dispose();
    }
}