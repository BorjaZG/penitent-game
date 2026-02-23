package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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

    // ── Menú principal ────────────────────────────────────────────────────────
    private enum MenuItem { NEW_GAME, CONTINUE, OPTIONS, CREDITS, EXIT }
    private final MenuItem[] items  = {
        MenuItem.NEW_GAME, MenuItem.CONTINUE, MenuItem.OPTIONS,
        MenuItem.CREDITS,  MenuItem.EXIT
    };
    private final String[] labels = {
        "Nueva Partida", "Continuar", "Opciones", "Creditos", "Salir"
    };
    private int selectedIndex = 0;

    // ── Submenús ──────────────────────────────────────────────────────────────
    private enum SubMenu { NONE, OPTIONS, CREDITS, SAVE_SELECT }
    private SubMenu activeSubMenu = SubMenu.NONE;

    // ── Opciones de volumen ───────────────────────────────────────────────────
    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;
    private int   optionIndex = 0;

    // ── Selección de slot ─────────────────────────────────────────────────────
    private int slotIndex       = 0;
    private SaveManager.SaveData[] slots;
    private boolean confirmDelete = false;

    // ── Cursor parpadeante ────────────────────────────────────────────────────
    private float   blinkTimer = 0f;
    private boolean blinkOn    = true;
    private static final float BLINK_RATE = 0.5f;

    // ── Audio ─────────────────────────────────────────────────────────────────
    private SoundManager sound;

    // ── Fondo ─────────────────────────────────────────────────────────────────
    private Texture background;
    private boolean hasBackground = false;

    public MenuScreen(PenitentGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIEW_W, VIEW_H);
        layout = new GlyphLayout();

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
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(float dt) {
        handleInput(dt);

        blinkTimer += dt;
        if (blinkTimer >= BLINK_RATE) { blinkOn = !blinkOn; blinkTimer = 0f; }

        Gdx.gl.glClearColor(0.05f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (hasBackground) batch.draw(background, 0, 0, VIEW_W, VIEW_H);

        switch (activeSubMenu) {
            case NONE:        drawMainMenu();   break;
            case OPTIONS:     drawOptions();    break;
            case CREDITS:     drawCredits();    break;
            case SAVE_SELECT: drawSaveSelect(); break;
        }

        batch.end();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput(float dt) {
        switch (activeSubMenu) {
            case NONE:        handleMainMenuInput();   break;
            case OPTIONS:     handleOptionsInput(dt);  break;
            case SAVE_SELECT: handleSaveSelectInput(); break;
            case CREDITS:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.ENTER)  ||
                    Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                    activeSubMenu = SubMenu.NONE;
                }
                break;
        }
    }

    private void handleMainMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
            selectedIndex = (selectedIndex - 1 + items.length) % items.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            selectedIndex = (selectedIndex + 1) % items.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            selectCurrent();
    }

    private void selectCurrent() {
        switch (items[selectedIndex]) {
            case NEW_GAME:
                sound.stopMusic();
                game.startNewGame();
                break;
            case CONTINUE:
                if (SaveManager.hasSave()) {
                    sound.stopMusic();
                    slots = SaveManager.loadAll();
                    slotIndex = 0;
                    for (int i = 0; i < SaveManager.getMaxSlots(); i++) {
                        if (slots[i] != null) { slotIndex = i; break; }
                    }
                    confirmDelete = false;
                    activeSubMenu = SubMenu.SAVE_SELECT;
                }
                break;
            case OPTIONS:
                activeSubMenu = SubMenu.OPTIONS;
                optionIndex   = 0;
                break;
            case CREDITS:
                activeSubMenu = SubMenu.CREDITS;
                break;
            case EXIT:
                Gdx.app.exit();
                break;
        }
    }

    private void handleOptionsInput(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
            optionIndex = (optionIndex - 1 + 2) % 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            optionIndex = (optionIndex + 1) % 2;

        float step = 0.05f;
        if (optionIndex == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) musicVolume = Math.min(1f, musicVolume + step);
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  musicVolume = Math.max(0f, musicVolume - step);
            game.setMusicVolume(musicVolume);
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) sfxVolume = Math.min(1f, sfxVolume + step);
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  sfxVolume = Math.max(0f, sfxVolume - step);
            game.setSfxVolume(sfxVolume);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE))
            activeSubMenu = SubMenu.NONE;
    }

    private void handleSaveSelectInput() {
        int maxSlots = SaveManager.getMaxSlots();

        // Esperando confirmación de borrado
        if (confirmDelete) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Y) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                SaveManager.deleteSlot(slotIndex);
                slots = SaveManager.loadAll();
                confirmDelete = false;
                if (!SaveManager.hasSave()) activeSubMenu = SubMenu.NONE;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.N) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                confirmDelete = false;
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
            slotIndex = (slotIndex - 1 + maxSlots) % maxSlots;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
            slotIndex = (slotIndex + 1) % maxSlots;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (slots != null && slots[slotIndex] != null)
                game.continueGame(slotIndex);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DEL) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            if (slots != null && slots[slotIndex] != null)
                confirmDelete = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            activeSubMenu = SubMenu.NONE;
    }

    // ── Dibujo: Menú principal ────────────────────────────────────────────────

    private void drawMainMenu() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(FontManager.title, "PENITENT");
        FontManager.title.draw(batch, "PENITENT", cx - layout.width / 2f, VIEW_H - 30);

        FontManager.small.setColor(new Color(0.6f, 0.5f, 0.5f, 1f));
        layout.setText(FontManager.small, "Un juego de Borja");
        FontManager.small.draw(batch, "Un juego de Borja", cx - layout.width / 2f, VIEW_H - 70);

        float startY  = VIEW_H / 2f + 60;
        float spacing = 38f;

        for (int i = 0; i < labels.length; i++) {
            boolean sel      = (i == selectedIndex);
            boolean disabled = (items[i] == MenuItem.CONTINUE && !SaveManager.hasSave());
            String  label    = labels[i] + (disabled ? " (sin partida)" : "");

            if      (disabled) FontManager.menu.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
            else if (sel)      FontManager.menu.setColor(new Color(0.95f, 0.8f, 0.2f, 1f));
            else               FontManager.menu.setColor(new Color(0.75f, 0.65f, 0.65f, 1f));

            layout.setText(FontManager.menu, label);
            float lx = cx - layout.width / 2f;
            float ly = startY - i * spacing;
            FontManager.menu.draw(batch, label, lx, ly);

            if (sel && blinkOn && !disabled) {
                FontManager.small.setColor(new Color(0.95f, 0.8f, 0.2f, 1f));
                FontManager.small.draw(batch, ">", lx - 25, ly);
                FontManager.small.draw(batch, "<", lx + layout.width + 8, ly);
            }
        }

        FontManager.small.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        layout.setText(FontManager.small, "Flechas Navegar   ENTER Seleccionar");
        FontManager.small.draw(batch, "Flechas Navegar   ENTER Seleccionar",
            cx - layout.width / 2f, 20);
    }

    // ── Dibujo: Opciones ─────────────────────────────────────────────────────

    private void drawOptions() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(FontManager.title, "OPCIONES");
        FontManager.title.draw(batch, "OPCIONES", cx - layout.width / 2f, VIEW_H - 40);

        String[] optLabels = { "Musica", "Efectos" };
        float[]  optValues = { musicVolume, sfxVolume };
        float    startY    = VIEW_H / 2f + 30;

        for (int i = 0; i < 2; i++) {
            boolean sel = (i == optionIndex);
            FontManager.menu.setColor(sel ? new Color(0.95f, 0.8f, 0.2f, 1f)
                : new Color(0.75f, 0.65f, 0.65f, 1f));
            String line = optLabels[i] + ":  < " + Math.round(optValues[i] * 100) + "% >";
            layout.setText(FontManager.menu, line);
            FontManager.menu.draw(batch, line, cx - layout.width / 2f, startY - i * 50);
            drawVolumeBar(cx, startY - i * 50 - 18, optValues[i], sel);
        }

        FontManager.small.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        layout.setText(FontManager.small, "Flechas Navegar   Izq/Der Ajustar   ESC Volver");
        FontManager.small.draw(batch, "Flechas Navegar   Izq/Der Ajustar   ESC Volver",
            cx - layout.width / 2f, 20);
    }

    private void drawVolumeBar(float cx, float y, float value, boolean active) {
        FontManager.small.setColor(active ? new Color(0.95f, 0.75f, 0.1f, 1f)
            : new Color(0.5f, 0.5f, 0.5f, 1f));
        StringBuilder bar = new StringBuilder("|");
        int filled = Math.round(value * 20);
        for (int k = 0; k < 20; k++) bar.append(k < filled ? "\u2588" : "\u2591");
        bar.append("|");
        layout.setText(FontManager.small, bar.toString());
        FontManager.small.draw(batch, bar.toString(), cx - layout.width / 2f, y);
    }

    // ── Dibujo: Creditos ─────────────────────────────────────────────────────

    private void drawCredits() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(FontManager.title, "CREDITOS");
        FontManager.title.draw(batch, "CREDITOS", cx - layout.width / 2f, VIEW_H - 40);

        String[] lines = {
            "PENITENT", "",
            "Desarrollo: Borja", "",
            "Assets: Metroidvania Library Asset Pack",
            "Musica: Cantes de Confesion - Carlos Viola",
            "Sonidos: freesound.org", "",
            "Desarrollado con libGDX", "",
            "--- Proyecto academico ---"
        };

        float y = VIEW_H - 100;
        for (String line : lines) {
            FontManager.small.setColor(line.startsWith("PENITENT")
                ? new Color(0.95f, 0.8f, 0.2f, 1f)
                : new Color(0.75f, 0.65f, 0.65f, 1f));
            layout.setText(FontManager.small, line);
            FontManager.small.draw(batch, line, cx - layout.width / 2f, y);
            y -= 22;
        }

        FontManager.small.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        layout.setText(FontManager.small, "ENTER / ESC para volver");
        FontManager.small.draw(batch, "ENTER / ESC para volver", cx - layout.width / 2f, 20);
    }

    // ── Dibujo: Selección de slot ─────────────────────────────────────────────

    private void drawSaveSelect() {
        float cx = VIEW_W / 2f;

        FontManager.title.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(FontManager.title, "CONTINUAR");
        FontManager.title.draw(batch, "CONTINUAR", cx - layout.width / 2f, VIEW_H - 40);

        float startY  = VIEW_H / 2f + 55;
        float spacing = 60f;
        float sx      = cx - 160;

        for (int i = 0; i < SaveManager.getMaxSlots(); i++) {
            boolean sel  = (i == slotIndex);
            SaveManager.SaveData data = (slots != null) ? slots[i] : null;

            FontManager.menu.setColor(sel ? new Color(0.95f, 0.8f, 0.2f, 1f)
                : new Color(0.6f, 0.55f, 0.55f, 1f));
            FontManager.menu.draw(batch, "RANURA " + (i + 1), sx, startY - i * spacing);

            if (data != null) {
                FontManager.small.setColor(sel ? new Color(1f, 0.9f, 0.6f, 1f)
                    : new Color(0.7f, 0.65f, 0.6f, 1f));
                String hearts = "";
                for (int h = 0; h < 3; h++) hearts += (h < data.health) ? "\u2665 " : "\u2661 ";
                FontManager.small.draw(batch,
                    data.zoneName + "   " + hearts,
                    sx + 10, startY - i * spacing - 20);
                FontManager.small.setColor(new Color(0.55f, 0.5f, 0.45f, 1f));
                FontManager.small.draw(batch, data.timestamp, sx + 10, startY - i * spacing - 36);
            } else {
                FontManager.small.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
                FontManager.small.draw(batch, "-- Vacia --", sx + 10, startY - i * spacing - 22);
            }

            if (sel && blinkOn && data != null) {
                FontManager.small.setColor(new Color(0.95f, 0.8f, 0.2f, 1f));
                FontManager.small.draw(batch, ">", sx - 18, startY - i * spacing);
            }
        }

        FontManager.small.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        layout.setText(FontManager.small, "ENTER Cargar   DEL Eliminar   ESC Volver");
        FontManager.small.draw(batch, "ENTER Cargar   DEL Eliminar   ESC Volver",
            cx - layout.width / 2f, 20);

        // Overlay confirmación de borrado
        if (confirmDelete) {
            FontManager.menu.setColor(new Color(0.9f, 0.15f, 0.15f, 1f));
            String msg = "Eliminar RANURA " + (slotIndex + 1) + "?";
            layout.setText(FontManager.menu, msg);
            FontManager.menu.draw(batch, msg, cx - layout.width / 2f, VIEW_H / 2f + 25);

            FontManager.small.setColor(new Color(1f, 0.9f, 0.5f, 1f));
            layout.setText(FontManager.small, "S / Y = Confirmar      N / ESC = Cancelar");
            FontManager.small.draw(batch, "S / Y = Confirmar      N / ESC = Cancelar",
                cx - layout.width / 2f, VIEW_H / 2f + 3);
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
        if (hasBackground) background.dispose();
        if (sound != null) sound.dispose();
    }
}
