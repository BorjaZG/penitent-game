package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuScreen implements Screen {

    private final PenitentGame game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private BitmapFont titleFont, menuFont, smallFont;
    private GlyphLayout layout;

    private static final float VIEW_W = 608f;
    private static final float VIEW_H = 320f;

    // Opciones del menú principal
    private enum MenuItem { NEW_GAME, CONTINUE, OPTIONS, CREDITS, EXIT }
    private MenuItem[] items = {
        MenuItem.NEW_GAME, MenuItem.CONTINUE, MenuItem.OPTIONS,
        MenuItem.CREDITS,  MenuItem.EXIT
    };
    private String[] labels = { "Nueva Partida", "Continuar", "Opciones", "Créditos", "Salir" };
    private int selectedIndex = 0;

    // Submenú activo
    private enum SubMenu { NONE, OPTIONS, CREDITS }
    private SubMenu activeSubMenu = SubMenu.NONE;

    // Opciones: volumen música y sfx
    private float musicVolume = 0.5f;
    private float sfxVolume   = 0.8f;
    private int   optionIndex = 0; // 0=música, 1=sfx

    // Animación cursor (parpadeo)
    private float blinkTimer   = 0f;
    private boolean blinkOn    = true;
    private static final float BLINK_RATE = 0.5f;

    // Fondo
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

        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.5f);

        menuFont = new BitmapFont();
        menuFont.getData().setScale(1.8f);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.2f);

        // Intentar cargar fondo si existe
        try {
            background    = new Texture("menu_background.png");
            hasBackground = true;
        } catch (Exception e) {
            hasBackground = false;
        }

        // ¿Hay partida guardada? Si no, deshabilitar "Continuar"
        // (Se comprueba más adelante con SaveManager)
    }

    @Override
    public void render(float dt) {
        // Input
        handleInput(dt);

        // Actualizar parpadeo del cursor
        blinkTimer += dt;
        if (blinkTimer >= BLINK_RATE) { blinkOn = !blinkOn; blinkTimer = 0f; }

        // Render
        Gdx.gl.glClearColor(0.05f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (hasBackground) batch.draw(background, 0, 0, VIEW_W, VIEW_H);

        if (activeSubMenu == SubMenu.NONE)    drawMainMenu();
        else if (activeSubMenu == SubMenu.OPTIONS) drawOptions();
        else if (activeSubMenu == SubMenu.CREDITS) drawCredits();

        batch.end();
    }

    private void handleInput(float dt) {
        if (activeSubMenu == SubMenu.NONE) {
            handleMainMenuInput();
        } else if (activeSubMenu == SubMenu.OPTIONS) {
            handleOptionsInput(dt);
        } else {
            // Créditos: cualquier tecla para volver
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER)  ||
                Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                activeSubMenu = SubMenu.NONE;
            }
        }
    }

    private void handleMainMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selectedIndex = (selectedIndex - 1 + items.length) % items.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selectedIndex = (selectedIndex + 1) % items.length;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            selectCurrent();
        }
    }

    private void selectCurrent() {
        switch (items[selectedIndex]) {
            case NEW_GAME:
                game.startNewGame();
                break;
            case CONTINUE:
                if (SaveManager.hasSave()) game.continueGame();
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            optionIndex = (optionIndex - 1 + 2) % 2;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            optionIndex = (optionIndex + 1) % 2;
        }

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            activeSubMenu = SubMenu.NONE;
        }
    }

    // --- Dibujo del menú principal ---
    private void drawMainMenu() {
        float centerX = VIEW_W / 2f;

        // Título
        titleFont.setColor(new Color(0.85f, 0.7f, 0.3f, 1f)); // dorado
        layout.setText(titleFont, "PENITENT");
        titleFont.draw(batch, "PENITENT", centerX - layout.width / 2f, VIEW_H - 30);

        // Subtítulo
        smallFont.setColor(new Color(0.6f, 0.5f, 0.5f, 1f));
        String sub = "Un juego de Borja";
        layout.setText(smallFont, sub);
        smallFont.draw(batch, sub, centerX - layout.width / 2f, VIEW_H - 70);

        // Opciones
        float startY = VIEW_H / 2f + 60;
        float spacing = 38f;

        for (int i = 0; i < labels.length; i++) {
            boolean selected = (i == selectedIndex);
            boolean disabled = (items[i] == MenuItem.CONTINUE && !SaveManager.hasSave());

            if (disabled) {
                menuFont.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
            } else if (selected) {
                menuFont.setColor(new Color(0.95f, 0.8f, 0.2f, 1f)); // dorado brillante
            } else {
                menuFont.setColor(new Color(0.75f, 0.65f, 0.65f, 1f)); // gris rosado
            }

            String label = labels[i];
            if (disabled) label += " (sin partida)";

            layout.setText(menuFont, label);
            float x = centerX - layout.width / 2f;
            float y = startY - i * spacing;

            menuFont.draw(batch, label, x, y);

            // Cursor parpadeante
            if (selected && blinkOn && !disabled) {
                smallFont.setColor(new Color(0.95f, 0.8f, 0.2f, 1f));
                smallFont.draw(batch, ">", x - 25, y);
                smallFont.draw(batch, "<", x + layout.width + 8, y);
            }
        }

        // Instrucciones abajo
        smallFont.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        String hint = "↑↓ Navegar   ENTER Seleccionar";
        layout.setText(smallFont, hint);
        smallFont.draw(batch, hint, centerX - layout.width / 2f, 20);
    }

    // --- Pantalla de opciones ---
    private void drawOptions() {
        float centerX = VIEW_W / 2f;

        titleFont.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(titleFont, "OPCIONES");
        titleFont.draw(batch, "OPCIONES", centerX - layout.width / 2f, VIEW_H - 40);

        String[] optLabels = { "Música", "Efectos" };
        float[]  optValues = { musicVolume, sfxVolume };

        float startY = VIEW_H / 2f + 30;
        for (int i = 0; i < 2; i++) {
            boolean sel = (i == optionIndex);
            menuFont.setColor(sel ? new Color(0.95f, 0.8f, 0.2f, 1f)
                                  : new Color(0.75f, 0.65f, 0.65f, 1f));

            String line = optLabels[i] + ":  < " + Math.round(optValues[i] * 100) + "% >";
            layout.setText(menuFont, line);
            menuFont.draw(batch, line, centerX - layout.width / 2f, startY - i * 50);
        }

        // Barra visual del volumen
        for (int i = 0; i < 2; i++) {
            drawVolumeBar(centerX, startY - i * 50 - 18, optValues[i], i == optionIndex);
        }

        smallFont.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        String hint = "↑↓ Seleccionar   ←→ Ajustar   ESC Volver";
        layout.setText(smallFont, hint);
        smallFont.draw(batch, hint, centerX - layout.width / 2f, 20);
    }

    private void drawVolumeBar(float cx, float y, float value, boolean active) {
        float barW = 200f, barH = 8f;
        float bx = cx - barW / 2f;

        // Fondo barra
        batch.setColor(active ? new Color(0.3f, 0.25f, 0.1f, 1f)
                              : new Color(0.2f, 0.2f, 0.2f, 1f));
        // Dibujamos un rectángulo con la textura blanca del font (truco libGDX)
        // Como no tenemos ShapeRenderer, usamos un rectángulo de color en el batch
        // Para esto necesitaríamos un pixel blanco, lo hacemos con texto
        smallFont.setColor(active ? new Color(0.95f, 0.75f, 0.1f, 1f)
                                  : new Color(0.5f, 0.5f, 0.5f, 1f));
        StringBuilder bar = new StringBuilder("|");
        int filled = Math.round(value * 20);
        for (int k = 0; k < 20; k++) bar.append(k < filled ? "█" : "░");
        bar.append("|");
        layout.setText(smallFont, bar.toString());
        smallFont.draw(batch, bar.toString(), cx - layout.width / 2f, y);
    }

    // --- Pantalla de créditos ---
    private void drawCredits() {
        float centerX = VIEW_W / 2f;

        titleFont.setColor(new Color(0.85f, 0.7f, 0.3f, 1f));
        layout.setText(titleFont, "CRÉDITOS");
        titleFont.draw(batch, "CRÉDITOS", centerX - layout.width / 2f, VIEW_H - 40);

        String[] lines = {
            "PENITENT",
            "",
            "Desarrollo: Borja",
            "",
            "Assets: Metroidvania Library Asset Pack",
            "Música: Cantes de Confesión - Carlos Viola",
            "Sonidos: freesound.org",
            "",
            "Desarrollado con libGDX",
            "",
            "--- Proyecto académico ---"
        };

        float y = VIEW_H - 100;
        for (String line : lines) {
            smallFont.setColor(line.startsWith("PENITENT")
                ? new Color(0.95f, 0.8f, 0.2f, 1f)
                : new Color(0.75f, 0.65f, 0.65f, 1f));
            layout.setText(smallFont, line);
            smallFont.draw(batch, line, centerX - layout.width / 2f, y);
            y -= 22;
        }

        smallFont.setColor(new Color(0.5f, 0.45f, 0.45f, 1f));
        String hint = "ENTER / ESC para volver";
        layout.setText(smallFont, hint);
        smallFont.draw(batch, hint, centerX - layout.width / 2f, 20);
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        titleFont.dispose(); menuFont.dispose(); smallFont.dispose();
        if (hasBackground) background.dispose();
    }
}
