package com.svalero.penitent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * Gestor centralizado de fuentes.
 * Carga Cinzel desde assets/fonts/ y proporciona acceso global.
 * Llamar a FontManager.load() al iniciar y FontManager.dispose() al cerrar.
 */
public class FontManager {

    public static BitmapFont title;  // Cinzel Bold 48px  - títulos
    public static BitmapFont menu;   // Cinzel Regular 28px - opciones de menú
    public static BitmapFont small;  // Cinzel Regular 18px - HUD, textos pequeños

    public static void load() {
        title = new BitmapFont(Gdx.files.internal("fonts/cinzel_title.fnt"),
                               Gdx.files.internal("fonts/cinzel_title.png"), false);
        menu  = new BitmapFont(Gdx.files.internal("fonts/cinzel_menu.fnt"),
                               Gdx.files.internal("fonts/cinzel_menu.png"), false);
        small = new BitmapFont(Gdx.files.internal("fonts/cinzel_small.fnt"),
                               Gdx.files.internal("fonts/cinzel_small.png"), false);

        // Activar filtrado lineal para que escalen bien
        for (BitmapFont f : new BitmapFont[]{title, menu, small}) {
            f.getRegion().getTexture().setFilter(
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
            );
        }
    }

    public static void dispose() {
        if (title != null) title.dispose();
        if (menu  != null) menu.dispose();
        if (small != null) small.dispose();
    }
}
