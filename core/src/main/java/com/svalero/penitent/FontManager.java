package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * Gestor centralizado de fuentes.
 * Genera fuentes P052 (Palatino) via FreeType sin sombras.
 */
public class FontManager {

    /** Títulos de pantalla  — 40 px Bold */
    public static BitmapFont title;
    /** Opciones de menú     — 26 px Bold */
    public static BitmapFont menu;
    /** HUD y textos pequeños — 16 px Regular */
    public static BitmapFont small;

    private static final String CHARS =
        FreeTypeFontGenerator.DEFAULT_CHARS
        + "áéíóúÁÉÍÓÚñÑüÜ¿¡";

    public static void load() {
        FreeTypeFontGenerator boldGen    = new FreeTypeFontGenerator(Gdx.files.internal("fonts/p052_bold.otf"));
        FreeTypeFontGenerator regularGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/p052_regular.otf"));

        title = generate(boldGen,    32);
        menu  = generate(boldGen,    20);
        small = generate(regularGen, 13);

        boldGen.dispose();
        regularGen.dispose();
    }

    private static BitmapFont generate(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size       = size;
        p.characters = CHARS;
        p.minFilter  = Texture.TextureFilter.Linear;
        p.magFilter  = Texture.TextureFilter.Linear;
        p.mono       = false;
        return gen.generateFont(p);
    }

    public static void dispose() {
        if (title != null) { title.dispose(); title = null; }
        if (menu  != null) { menu.dispose();  menu  = null; }
        if (small != null) { small.dispose(); small = null; }
    }
}
