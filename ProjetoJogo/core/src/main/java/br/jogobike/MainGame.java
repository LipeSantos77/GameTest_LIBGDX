package br.jogobike;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    public Music backgroundMusic;

    // Cache para texturas placeholder
    public static Texture placeholderTexture;

    // --- MUDANÇA: Constantes de Resolução Centralizadas ---
    public static final float VIRTUAL_WIDTH = 800f;
    public static final float VIRTUAL_HEIGHT = 800f;
    // ----------------------------------------------------

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Carregar e configurar a música
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("corridasemfim.mp3"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.7f);
            playBackgroundMusic(); // Usar método centralizado
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Erro ao carregar música: " + e.getMessage());
        }

        setScreen(new MenuScreen(this));
    }

    // Métodos centralizados para controle de áudio
    public void playBackgroundMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }
    }
}
