package br.jogobike;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    public Music backgroundMusic;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Carregar e configurar a música
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("corridasemfim.mp3"));
            backgroundMusic.setLooping(true); // Repetir continuamente
            backgroundMusic.setVolume(0.7f); // Volume em 70%
            backgroundMusic.play(); // Iniciar a música
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Erro ao carregar música: " + e.getMessage());
        }

        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
    }
}
