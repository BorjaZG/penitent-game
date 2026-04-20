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
    private static final float PANEL_X  = 28f;
    private static final float PANEL_Y  = 12f;
    private static final float PANEL_W  = VIEW_W - 56f;
    private static final float PANEL_H  = VIEW_H - 24f;
    private static final float IMG_SIZE = 56f;
    private static final float SLOT_SZ  = 28f;
    private static final float SLOT_GAP = 8f;

    private int selectedIndex = 0;
    private final GlyphLayout layout = new GlyphLayout();
    private Texture uiTex;

    public InventoryOverlay() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        uiTex = new Texture(pm);
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
        float cx      = VIEW_W / 2f;
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();

        // ── Overlay oscuro ─────────────────────────────────────────────────────
        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(fadeTex, 0, 0, VIEW_W, VIEW_H);

        // ── Panel principal ────────────────────────────────────────────────────
        batch.setColor(0.04f, 0.02f, 0.06f, 0.94f);
        batch.draw(uiTex, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        drawBorder(batch, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 0.62f, 0.46f, 0.12f, 0.65f);
        // Acento superior
        batch.setColor(0.82f, 0.62f, 0.18f, 0.90f);
        batch.draw(uiTex, PANEL_X + 1, PANEL_Y + PANEL_H - 1, PANEL_W - 2, 2f);
        batch.setColor(Color.WHITE);

        // ── Título ─────────────────────────────────────────────────────────────
        float y = PANEL_Y + PANEL_H - 6f;
        FontManager.menu.setColor(0.96f, 0.80f, 0.28f, 1f);
        layout.setText(FontManager.menu, "INVENTARIO");
        FontManager.menu.draw(batch, "INVENTARIO", cx - layout.width / 2f, y);
        y -= menuLH + 4f;

        // Separador bajo título con extremos decorativos
        batch.setColor(0.62f, 0.46f, 0.12f, 0.42f);
        batch.draw(uiTex, PANEL_X + 20f, y, PANEL_W - 40f, 1f);
        batch.setColor(0.82f, 0.62f, 0.18f, 0.52f);
        batch.draw(uiTex, PANEL_X + 20f,           y - 1, 5f, 3f);
        batch.draw(uiTex, PANEL_X + PANEL_W - 25f, y - 1, 5f, 3f);
        batch.setColor(Color.WHITE);
        y -= 10f;

        if (items.isEmpty()) {
            float centY = PANEL_Y + PANEL_H / 2f;
            String emptyA = "No has recogido ningun objeto todavia.";
            String emptyB = "Explora el mundo para encontrar reliquias.";
            FontManager.small.setColor(0.52f, 0.47f, 0.44f, 1f);
            layout.setText(FontManager.small, emptyA);
            FontManager.small.draw(batch, emptyA, cx - layout.width / 2f, centY + smallLH);
            FontManager.small.setColor(0.36f, 0.32f, 0.30f, 0.80f);
            layout.setText(FontManager.small, emptyB);
            FontManager.small.draw(batch, emptyB, cx - layout.width / 2f, centY - 4f);
        } else {
            if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            Item item = items.get(selectedIndex);

            // ── Fila de miniaturas ─────────────────────────────────────────────
            float totalW = items.size() * (SLOT_SZ + SLOT_GAP) - SLOT_GAP;
            float rowX   = cx - totalW / 2f;
            float rowY   = y - SLOT_SZ;

            for (int i = 0; i < items.size(); i++) {
                float   sx  = rowX + i * (SLOT_SZ + SLOT_GAP);
                boolean sel = (i == selectedIndex);

                batch.setColor(sel
                    ? new Color(0.14f, 0.10f, 0.04f, 1f)
                    : new Color(0.10f, 0.07f, 0.10f, 1f));
                batch.draw(uiTex, sx - 3, rowY - 3, SLOT_SZ + 6, SLOT_SZ + 6);

                if (sel) {
                    drawBorder(batch, sx - 3, rowY - 3, SLOT_SZ + 6, SLOT_SZ + 6,
                        0.95f, 0.78f, 0.22f, 0.90f);
                    batch.setColor(0.95f, 0.78f, 0.22f, 0.28f);
                    batch.draw(uiTex, sx - 3, rowY - 3, SLOT_SZ + 6, 2f);
                } else {
                    drawBorder(batch, sx - 3, rowY - 3, SLOT_SZ + 6, SLOT_SZ + 6,
                        0.42f, 0.33f, 0.20f, 0.55f);
                }
                batch.setColor(Color.WHITE);
                batch.draw(items.get(i).texture, sx, rowY, SLOT_SZ, SLOT_SZ);
            }
            y = rowY - 12f;

            // Separador
            batch.setColor(0.42f, 0.32f, 0.10f, 0.32f);
            batch.draw(uiTex, PANEL_X + 60f, y, PANEL_W - 120f, 1f);
            batch.setColor(Color.WHITE);
            y -= 10f;

            // ── Imagen grande ──────────────────────────────────────────────────
            float imgX = cx - IMG_SIZE / 2f;
            float imgY = y - IMG_SIZE;

            batch.setColor(0.10f, 0.07f, 0.10f, 1f);
            batch.draw(uiTex, imgX - 4, imgY - 4, IMG_SIZE + 8, IMG_SIZE + 8);
            drawBorder(batch, imgX - 4, imgY - 4, IMG_SIZE + 8, IMG_SIZE + 8,
                0.82f, 0.62f, 0.18f, 0.72f);
            batch.setColor(Color.WHITE);
            batch.draw(item.texture, imgX, imgY, IMG_SIZE, IMG_SIZE);
            y = imgY - 8f;

            // ── Nombre del objeto ──────────────────────────────────────────────
            FontManager.menu.setColor(0.98f, 0.90f, 0.55f, 1f);
            layout.setText(FontManager.menu, item.type.displayName);
            FontManager.menu.draw(batch, item.type.displayName, cx - layout.width / 2f, y);
            // Subrayado del nombre
            batch.setColor(0.62f, 0.46f, 0.12f, 0.30f);
            batch.draw(uiTex, cx - layout.width / 2f, y - menuLH, layout.width, 1f);
            batch.setColor(Color.WHITE);
            y -= menuLH + 8f;

            // ── Leyenda (multi-línea) ──────────────────────────────────────────
            FontManager.small.setColor(0.72f, 0.65f, 0.58f, 1f);
            for (String line : item.type.legend.split("\n")) {
                layout.setText(FontManager.small, line);
                FontManager.small.draw(batch, line, cx - layout.width / 2f, y);
                y -= smallLH + 2f;
            }
        }

        // ── Hint de navegación ─────────────────────────────────────────────────
        float hintY = PANEL_Y + smallLH + 6f;
        batch.setColor(0.40f, 0.30f, 0.12f, 0.25f);
        batch.draw(uiTex, PANEL_X + 20f, hintY + 4f, PANEL_W - 40f, 1f);
        batch.setColor(Color.WHITE);
        FontManager.small.setColor(0.38f, 0.34f, 0.32f, 0.78f);
        String hint = items.size() > 1
            ? "< >  Navegar     I / ESC  Cerrar"
            : "I / ESC  Cerrar";
        layout.setText(FontManager.small, hint);
        FontManager.small.draw(batch, hint, cx - layout.width / 2f, hintY);
    }

    private void drawBorder(SpriteBatch batch, float x, float y, float w, float h,
                             float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(uiTex, x,         y + h - 1, w, 1);
        batch.draw(uiTex, x,         y,         w, 1);
        batch.draw(uiTex, x,         y,         1, h);
        batch.draw(uiTex, x + w - 1, y,         1, h);
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        if (uiTex != null) uiTex.dispose();
    }
}
