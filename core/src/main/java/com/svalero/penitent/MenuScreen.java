package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuScreen implements Screen {

    private final PenitentGame game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private GlyphLayout layout;

    private static final float VIEW_W = 608f;
    private static final float VIEW_H = 320f;

    // ── Menú principal ─────────────────────────────────────────────────────────
    private enum MenuItem { NEW_GAME, CONTINUE, OPTIONS, CREDITS, EXIT }
    private final MenuItem[] items = {
        MenuItem.NEW_GAME, MenuItem.CONTINUE, MenuItem.OPTIONS,
        MenuItem.CREDITS,  MenuItem.EXIT
    };
    private final String[] labels = {
        "Nueva Partida", "Continuar", "Opciones", "Creditos", "Salir"
    };
    private int selectedIndex = 0;

    // ── Submenús ───────────────────────────────────────────────────────────────
    private enum SubMenu { NONE, OPTIONS, CREDITS, CONTINUE_SELECT, NEW_GAME_SELECT, NEW_GAME_OVERWRITE }
    private SubMenu activeSubMenu = SubMenu.NONE;

    // ── Opciones ───────────────────────────────────────────────────────────────
    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;
    private int   optionIndex = 0;

    // ── Selección de slot ──────────────────────────────────────────────────────
    private int slotIndex = 0;
    private SaveManager.SaveData[] slots;
    private boolean confirmDelete = false;

    // ── Animación ──────────────────────────────────────────────────────────────
    private float   blinkTimer = 0f;
    private boolean blinkOn    = true;
    private float   titleTimer = 0f;
    private static final float BLINK_RATE = 0.55f;

    // ── Audio ──────────────────────────────────────────────────────────────────
    private SoundManager sound;

    // ── Texturas ───────────────────────────────────────────────────────────────
    private Texture background;
    private boolean hasBackground = false;
    private Texture uiTex;

    public MenuScreen(PenitentGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIEW_W, VIEW_H);
        layout = new GlyphLayout();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        uiTex = new Texture(pm);
        pm.dispose();

        try {
            background    = new Texture("menu_background.png");
            hasBackground = true;
        } catch (Exception e) {
            hasBackground = false;
        }

        sound = new SoundManager();
        sound.setMusicVolume(game.getMusicVolume());
        sound.setSfxVolume(game.getSfxVolume());
        sound.playMenuMusic();

        musicVolume = game.getMusicVolume();
        sfxVolume   = game.getSfxVolume();
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(float dt) {
        handleInput(dt);

        titleTimer += dt;
        blinkTimer += dt;
        if (blinkTimer >= BLINK_RATE) { blinkOn = !blinkOn; blinkTimer = 0f; }

        Gdx.gl.glClearColor(0.04f, 0.02f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (hasBackground) batch.draw(background, 0, 0, VIEW_W, VIEW_H);

        switch (activeSubMenu) {
            case NONE:               drawMainMenu();         break;
            case OPTIONS:            drawOptions();          break;
            case CREDITS:            drawCredits();          break;
            case CONTINUE_SELECT:    drawContinueSelect();   break;
            case NEW_GAME_SELECT:    drawNewGameSelect();    break;
            case NEW_GAME_OVERWRITE: drawNewGameOverwrite(); break;
        }
        batch.end();
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    private void handleInput(float dt) {
        switch (activeSubMenu) {
            case NONE:               handleMainMenuInput();     break;
            case OPTIONS:            handleOptionsInput(dt);   break;
            case CONTINUE_SELECT:    handleContinueSelect();   break;
            case NEW_GAME_SELECT:    handleNewGameSelect();    break;
            case NEW_GAME_OVERWRITE: handleNewGameOverwrite(); break;
            case CREDITS:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
                    activeSubMenu = SubMenu.NONE;
                break;
        }
    }

    private void handleMainMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            selectedIndex = (selectedIndex - 1 + items.length) % items.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            selectedIndex = (selectedIndex + 1) % items.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            selectCurrent();
    }

    private void selectCurrent() {
        switch (items[selectedIndex]) {
            case NEW_GAME:
                slots     = SaveManager.loadAll();
                slotIndex = 0;
                for (int i = 0; i < SaveManager.getMaxSlots(); i++) {
                    if (slots[i] == null) { slotIndex = i; break; }
                }
                activeSubMenu = SubMenu.NEW_GAME_SELECT;
                break;
            case CONTINUE:
                if (SaveManager.hasSave()) {
                    slots     = SaveManager.loadAll();
                    slotIndex = 0;
                    for (int i = 0; i < SaveManager.getMaxSlots(); i++) {
                        if (slots[i] != null) { slotIndex = i; break; }
                    }
                    confirmDelete = false;
                    activeSubMenu = SubMenu.CONTINUE_SELECT;
                }
                break;
            case OPTIONS:  activeSubMenu = SubMenu.OPTIONS; optionIndex = 0; break;
            case CREDITS:  activeSubMenu = SubMenu.CREDITS; break;
            case EXIT:     Gdx.app.exit(); break;
        }
    }

    private void handleNewGameSelect() {
        int max = SaveManager.getMaxSlots();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            slotIndex = (slotIndex - 1 + max) % max;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            slotIndex = (slotIndex + 1) % max;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (SaveManager.hasSlot(slotIndex)) activeSubMenu = SubMenu.NEW_GAME_OVERWRITE;
            else                                launchNewGame(slotIndex);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) activeSubMenu = SubMenu.NONE;
    }

    private void handleNewGameOverwrite() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            SaveManager.deleteSlot(slotIndex); launchNewGame(slotIndex);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            activeSubMenu = SubMenu.NEW_GAME_SELECT;
    }

    private void launchNewGame(int slot) { sound.stopMusic(); game.startNewGame(slot); }

    private void handleContinueSelect() {
        int max = SaveManager.getMaxSlots();
        if (confirmDelete) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Y) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                SaveManager.deleteSlot(slotIndex);
                slots = SaveManager.loadAll();
                confirmDelete = false;
                if (!SaveManager.hasSave()) activeSubMenu = SubMenu.NONE;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
                confirmDelete = false;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            slotIndex = (slotIndex - 1 + max) % max;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            slotIndex = (slotIndex + 1) % max;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (slots != null && slots[slotIndex] != null) {
                sound.stopMusic(); game.continueGame(slotIndex);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DEL) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            if (slots != null && slots[slotIndex] != null) confirmDelete = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) activeSubMenu = SubMenu.NONE;
    }

    private void handleOptionsInput(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)   || Gdx.input.isKeyJustPressed(Input.Keys.W))
            optionIndex = (optionIndex - 1 + 2) % 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            optionIndex = (optionIndex + 1) % 2;
        float step = 0.05f;
        if (optionIndex == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) musicVolume = Math.min(1f, musicVolume + step);
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  musicVolume = Math.max(0f, musicVolume - step);
            sound.setMusicVolume(musicVolume); game.setMusicVolume(musicVolume);
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) sfxVolume = Math.min(1f, sfxVolume + step);
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  sfxVolume = Math.max(0f, sfxVolume - step);
            game.setSfxVolume(sfxVolume);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) activeSubMenu = SubMenu.NONE;
    }

    // ── Primitivas UI ──────────────────────────────────────────────────────────

    private void drawOverlay(float alpha) {
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(uiTex, 0, 0, VIEW_W, VIEW_H);
        batch.setColor(Color.WHITE);
    }

    private void drawGoldLine(float x, float y, float w) {
        batch.setColor(0.68f, 0.50f, 0.13f, 0.55f);
        batch.draw(uiTex, x, y, w, 1f);
        batch.setColor(Color.WHITE);
    }

    /** Título de submenú centrado con línea dorada bajo él. */
    private void drawSubtitle(float cx, float y, String text) {
        FontManager.menu.setColor(0.90f, 0.72f, 0.22f, 1f);
        drawCX(cx, y, text, FontManager.menu);
        drawGoldLine(cx - layout.width / 2f, y - FontManager.menu.getLineHeight() - 3f, layout.width);
    }

    /** Centra texto horizontalmente. drawY = top del texto. */
    private void drawCX(float cx, float drawY, String text,
                         com.badlogic.gdx.graphics.g2d.BitmapFont font) {
        layout.setText(font, text);
        font.draw(batch, text, cx - layout.width / 2f, drawY);
    }

    /** Panel oscuro con borde dorado fino. x,y = esquina inferior-izquierda. */
    private void drawPanel(float x, float y, float w, float h) {
        batch.setColor(0.04f, 0.02f, 0.07f, 0.93f);
        batch.draw(uiTex, x, y, w, h);
        batch.setColor(0.52f, 0.38f, 0.10f, 0.48f);
        batch.draw(uiTex, x,         y + h - 1, w, 1);
        batch.draw(uiTex, x,         y,         w, 1);
        batch.draw(uiTex, x,         y,         1, h);
        batch.draw(uiTex, x + w - 1, y,         1, h);
        batch.setColor(Color.WHITE);
    }

    private void drawBottomHint(float cx, String text) {
        float smallLH = FontManager.small.getLineHeight();
        FontManager.small.setColor(0.34f, 0.30f, 0.28f, 0.55f);
        drawCX(cx, 5f + smallLH, text, FontManager.small);
    }

    // ── Menú principal (estilo Blasphemous) ────────────────────────────────────

    private void drawMainMenu() {
        float cx      = VIEW_W / 2f;
        float titleLH = FontManager.title.getLineHeight();
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();

        // Título: esquina superior izquierda con pulso dorado
        float pulse = 0.92f + 0.08f * (float) Math.sin(titleTimer * 1.3f);
        FontManager.title.setColor(0.94f * pulse, 0.76f * pulse, 0.24f, 1f);
        FontManager.title.draw(batch, "PENITENT", 24f, VIEW_H - 8f);

        // Items: sin caja, alineados a la derecha, centrados verticalmente
        float rowH      = menuLH + 16f;
        float itemsH    = labels.length * rowH;
        float rightEdge = VIEW_W - 28f;
        float startY    = VIEW_H / 2f + itemsH / 2f;

        for (int i = 0; i < labels.length; i++) {
            boolean sel      = (i == selectedIndex);
            boolean disabled = (items[i] == MenuItem.CONTINUE && !SaveManager.hasSave());
            String  lbl      = disabled ? labels[i] + "  —" : labels[i];

            float itemTop = startY - i * rowH;

            if (disabled)  FontManager.menu.setColor(0.28f, 0.26f, 0.26f, 0.50f);
            else if (sel)  FontManager.menu.setColor(1.00f, 0.86f, 0.22f, 1f);
            else           FontManager.menu.setColor(0.84f, 0.78f, 0.74f, 0.82f);

            layout.setText(FontManager.menu, lbl);
            float drawX = rightEdge - layout.width;
            FontManager.menu.draw(batch, lbl, drawX, itemTop);

            // Subrayado dorado parpadeante bajo el item seleccionado
            if (sel && !disabled) {
                float alpha = blinkOn ? 0.72f : 0.32f;
                batch.setColor(0.92f, 0.72f, 0.18f, alpha);
                batch.draw(uiTex, drawX, itemTop - layout.height - 2f, layout.width, 1f);
                batch.setColor(Color.WHITE);
            }
        }

        // Hint inferior
        FontManager.small.setColor(0.34f, 0.30f, 0.28f, 0.50f);
        drawCX(cx, 5f + smallLH, "Flechas  Navegar     ENTER  Seleccionar", FontManager.small);
    }

    // ── Nueva Partida ──────────────────────────────────────────────────────────

    private void drawNewGameSelect() {
        float cx      = VIEW_W / 2f;
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();
        drawOverlay(0.62f);

        float titleY = VIEW_H - 14f;
        drawSubtitle(cx, titleY, "NUEVA PARTIDA");

        float subY = titleY - menuLH - 16f;
        FontManager.small.setColor(0.50f, 0.44f, 0.38f, 0.82f);
        drawCX(cx, subY, "Elige un archivo de guardado", FontManager.small);

        drawSlotCards(cx, subY - smallLH - 12f, false);
        drawBottomHint(cx, "Flechas  Navegar     ENTER  Elegir     ESC  Volver");
    }

    // ── Sobreescribir ──────────────────────────────────────────────────────────

    private void drawNewGameOverwrite() {
        float cx      = VIEW_W / 2f;
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();
        drawOverlay(0.62f);

        float panelH = menuLH + 4 * (smallLH + 6f) + 28f;
        float panelW = 380f;
        float panelY = VIEW_H / 2f - panelH / 2f;
        drawPanel(cx - panelW / 2f, panelY, panelW, panelH);

        float y = panelY + panelH - 12f;

        FontManager.menu.setColor(0.90f, 0.22f, 0.22f, 1f);
        drawCX(cx, y, "SOBREESCRIBIR ARCHIVO", FontManager.menu);
        y -= menuLH + 4f;

        drawGoldLine(cx - 145f, y, 290f);
        y -= 12f;

        FontManager.small.setColor(0.88f, 0.78f, 0.58f, 1f);
        drawCX(cx, y, "El ARCHIVO " + (slotIndex + 1) + " ya tiene una partida guardada.", FontManager.small);
        y -= smallLH + 4f;

        FontManager.small.setColor(0.72f, 0.38f, 0.38f, 1f);
        drawCX(cx, y, "Los datos anteriores se perderan para siempre.", FontManager.small);
        y -= smallLH + 12f;

        drawGoldLine(cx - 95f, y, 190f);
        y -= 12f;

        FontManager.small.setColor(1f, 0.88f, 0.35f, 1f);
        drawCX(cx, y, "S / Y  -  Confirmar", FontManager.small);
        y -= smallLH + 4f;

        FontManager.small.setColor(0.42f, 0.38f, 0.38f, 1f);
        drawCX(cx, y, "N / ESC  -  Cancelar", FontManager.small);
    }

    // ── Continuar ─────────────────────────────────────────────────────────────

    private void drawContinueSelect() {
        float cx      = VIEW_W / 2f;
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();
        drawOverlay(0.62f);

        float titleY = VIEW_H - 14f;
        drawSubtitle(cx, titleY, "CONTINUAR");

        float subY = titleY - menuLH - 16f;
        FontManager.small.setColor(0.50f, 0.44f, 0.38f, 0.82f);
        drawCX(cx, subY, "Selecciona un archivo de guardado", FontManager.small);

        drawSlotCards(cx, subY - smallLH - 12f, true);
        drawBottomHint(cx, "ENTER  Cargar     DEL  Eliminar     ESC  Volver");

        if (confirmDelete) {
            float ph = smallLH * 2f + 28f;
            float pw = 350f;
            float py = VIEW_H / 2f - ph / 2f;
            drawPanel(cx - pw / 2f, py, pw, ph);
            FontManager.small.setColor(0.92f, 0.22f, 0.22f, 1f);
            drawCX(cx, py + ph - 10f, "Eliminar ARCHIVO " + (slotIndex + 1) + "?", FontManager.small);
            FontManager.small.setColor(1f, 0.88f, 0.35f, 1f);
            drawCX(cx, py + ph - 10f - smallLH - 8f,
                "S / Y = Si     N / ESC = Cancelar", FontManager.small);
        }
    }

    // ── Opciones ──────────────────────────────────────────────────────────────

    private void drawOptions() {
        float cx    = VIEW_W / 2f;
        float menuLH = FontManager.menu.getLineHeight();
        drawOverlay(0.62f);

        float titleY = VIEW_H - 14f;
        drawSubtitle(cx, titleY, "OPCIONES");

        String[] optLabels = { "Musica", "Efectos de sonido" };
        float[]  optValues = { musicVolume, sfxVolume };

        float rowH   = menuLH + 14f + 8f;
        float padV   = 14f;
        float panelH = 2f * rowH + 2f * padV;
        float panelW = 340f;
        float panelX = cx - panelW / 2f;
        float panelY = VIEW_H / 2f - panelH / 2f;
        drawPanel(panelX, panelY, panelW, panelH);

        float rowTop = panelY + panelH - padV;

        for (int i = 0; i < 2; i++) {
            boolean sel  = (i == optionIndex);
            float   rTop = rowTop - i * rowH;
            float   lblY = rTop - 2f;
            float   barY = lblY - menuLH - 4f;

            if (sel) {
                batch.setColor(0.95f, 0.75f, 0.20f, 0.08f);
                batch.draw(uiTex, panelX + 2, rTop - rowH, panelW - 4, rowH);
                batch.setColor(0.95f, 0.78f, 0.22f, 0.60f);
                batch.draw(uiTex, panelX + 2, rTop - rowH, 2f, rowH);
                batch.setColor(Color.WHITE);
            }

            String lbl = optLabels[i] + ":   < " + Math.round(optValues[i] * 100) + "% >";
            FontManager.menu.setColor(sel
                ? new Color(1f, 0.90f, 0.36f, 1f)
                : new Color(0.66f, 0.58f, 0.54f, 1f));
            FontManager.menu.draw(batch, lbl, panelX + 18f, lblY);

            drawVolumeBar(panelX + 18f, barY, panelW - 36f, optValues[i], sel);
        }

        drawBottomHint(cx, "Flechas  Navegar     Izq/Der  Ajustar     ESC  Volver");
    }

    private void drawVolumeBar(float x, float y, float w, float value, boolean active) {
        float h = 6f;
        batch.setColor(0.08f, 0.05f, 0.10f, 1f);
        batch.draw(uiTex, x, y, w, h);
        batch.setColor(active
            ? new Color(0.80f, 0.60f, 0.18f, 0.88f)
            : new Color(0.34f, 0.28f, 0.22f, 0.70f));
        batch.draw(uiTex, x + 1f, y + 1f, value * (w - 2f), h - 2f);
        batch.setColor(Color.WHITE);
    }

    // ── Créditos ──────────────────────────────────────────────────────────────

    private void drawCredits() {
        float cx      = VIEW_W / 2f;
        float menuLH  = FontManager.menu.getLineHeight();
        float smallLH = FontManager.small.getLineHeight();
        drawOverlay(0.62f);

        String[][] rows = {
            { "Desarrollo", "Borja Zorrilla Gracia" },
            { "Arte",       "itch.io"               },
            { "Musica",     "Carlos Viola"           },
            { "Sonidos",    "freesound.org"          },
            { "Motor",      "libGDX 1.14"            },
        };

        float rowLH  = smallLH + 4f;
        float panelH = menuLH + 14f + rows.length * rowLH + 24f;
        float panelW = 420f;
        float panelX = cx - panelW / 2f;
        float panelY = VIEW_H / 2f - panelH / 2f;
        drawPanel(panelX, panelY, panelW, panelH);

        float y = panelY + panelH - 12f;

        float pulse = 0.90f + 0.10f * (float) Math.sin(titleTimer * 1.3f);
        FontManager.menu.setColor(0.92f * pulse, 0.74f * pulse, 0.24f, 1f);
        drawCX(cx, y, "PENITENT", FontManager.menu);
        y -= menuLH + 4f;

        drawGoldLine(cx - 110f, y, 220f);
        y -= 14f;

        for (String[] row : rows) {
            if (row[0].isEmpty() && row[1].isEmpty()) { y -= 6f; continue; }
            if (!row[0].isEmpty()) {
                FontManager.small.setColor(0.80f, 0.62f, 0.18f, 1f);
                FontManager.small.draw(batch, row[0] + ":", panelX + 22f, y);
            }
            if (!row[1].isEmpty()) {
                FontManager.small.setColor(0.70f, 0.62f, 0.56f, 1f);
                FontManager.small.draw(batch, row[1], cx - 10f, y);
            }
            y -= rowLH;
        }

        drawBottomHint(cx, "ENTER / ESC  para volver");
    }

    // ── Slot cards ────────────────────────────────────────────────────────────

    private void drawSlotCards(float cx, float startY, boolean onlySaved) {
        int   max     = SaveManager.getMaxSlots();
        float smallLH = FontManager.small.getLineHeight();
        float cardW   = 350f;
        float cardH   = smallLH * 3f + 16f;
        float cardGap = 8f;

        for (int i = 0; i < max; i++) {
            boolean          sel    = (i == slotIndex);
            SaveManager.SaveData data   = (slots != null) ? slots[i] : null;
            boolean          canSel = !onlySaved || data != null;
            float            cardTop = startY - i * (cardH + cardGap);
            float            cardY   = cardTop - cardH;
            float            cardX   = cx - cardW / 2f;

            batch.setColor(sel && canSel
                ? new Color(0.10f, 0.07f, 0.03f, 0.92f)
                : new Color(0.04f, 0.02f, 0.07f, 0.80f));
            batch.draw(uiTex, cardX, cardY, cardW, cardH);

            float ba = sel && canSel ? 0.65f : 0.25f;
            float br = sel && canSel ? 0.72f : 0.32f;
            float bg = sel && canSel ? 0.52f : 0.28f;
            float bb = sel && canSel ? 0.13f : 0.16f;
            batch.setColor(br, bg, bb, ba);
            batch.draw(uiTex, cardX,              cardY + cardH - 1, cardW, 1);
            batch.draw(uiTex, cardX,              cardY,             cardW, 1);
            batch.draw(uiTex, cardX,              cardY,             1,     cardH);
            batch.draw(uiTex, cardX + cardW - 1,  cardY,             1,     cardH);

            if (sel && canSel) {
                batch.setColor(0.95f, 0.80f, 0.25f, 0.78f);
                batch.draw(uiTex, cardX, cardY, 2f, cardH);
            }
            batch.setColor(Color.WHITE);

            float lineY = cardTop - 6f;
            Color nameCol = !canSel
                ? new Color(0.28f, 0.28f, 0.28f, 1f)
                : sel ? new Color(1f, 0.88f, 0.28f, 1f)
                      : new Color(0.66f, 0.58f, 0.52f, 1f);
            FontManager.small.setColor(nameCol);
            FontManager.small.draw(batch, "ARCHIVO  " + (i + 1), cardX + 14f, lineY);

            lineY -= smallLH + 2f;
            if (data != null) {
                FontManager.small.setColor(sel
                    ? new Color(0.96f, 0.88f, 0.60f, 0.92f)
                    : new Color(0.58f, 0.52f, 0.46f, 0.78f));
                FontManager.small.draw(batch,
                    data.zoneName + "   Vida: " + data.health + "/3",
                    cardX + 14f, lineY);
                lineY -= smallLH + 2f;
                FontManager.small.setColor(0.36f, 0.32f, 0.28f, 0.62f);
                FontManager.small.draw(batch, data.timestamp, cardX + 14f, lineY);
            } else {
                FontManager.small.setColor(0.26f, 0.24f, 0.24f, 0.82f);
                FontManager.small.draw(batch, "-- Vacio --", cardX + 14f, lineY);
            }

            if (sel && canSel && blinkOn) {
                FontManager.small.setColor(1f, 0.88f, 0.28f, 1f);
                FontManager.small.draw(batch, ">", cardX + cardW - 18f, cardTop - 6f);
            }
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        if (uiTex        != null) uiTex.dispose();
        if (hasBackground)        background.dispose();
        if (sound        != null) sound.dispose();
    }
}