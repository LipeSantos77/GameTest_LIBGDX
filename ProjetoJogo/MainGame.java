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

    // VARIÁVEIS DE MÚSICA PARA CADA FASE
    private Music musicLevel1;
    private Music musicLevel2;
    private Music musicLevel3;
    private Music currentMusic; // Referência para a música que está tocando

    // Cache para texturas placeholder
    public static Texture placeholderTexture;

    // --- Constantes de Resolução Centralizadas ---
    public static final float VIRTUAL_WIDTH = 800f;
    public static final float VIRTUAL_HEIGHT = 800f;
    // ----------------------------------------------------

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // --- CARREGAMENTO DAS MÚSICAS ---
        try {
            musicLevel1 = Gdx.audio.newMusic(Gdx.files.internal("Inicio do Caos.mp3"));
            musicLevel2 = Gdx.audio.newMusic(Gdx.files.internal("Ascensão ao Caos.mp3"));
            musicLevel3 = Gdx.audio.newMusic(Gdx.files.internal("Fuga do Caos.mp3"));

            musicLevel1.setLooping(true);
            musicLevel2.setLooping(true);
            musicLevel3.setLooping(true);

        } catch (Exception e) {
            Gdx.app.error("MainGame", "Erro ao carregar músicas: " + e.getMessage());
        }

        setScreen(new MenuScreen(this));
    }

    // --- Método de troca de música ---
    public void changeMusic(int level) {
        // Para a música anterior se estiver tocando
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.stop();
        }

        switch (level) {
            case 1:
                currentMusic = musicLevel1;
                break;
            case 2:
                currentMusic = musicLevel2;
                break;
            case 3:
                currentMusic = musicLevel3;
                break;
            default:
                currentMusic = musicLevel1;
                break;
        }

        if (currentMusic != null) {
            currentMusic.setVolume(0.7f);
            currentMusic.play();
            Gdx.app.log("MainGame", "Música trocada para Fase " + level);
        }
    }

    public void playBackgroundMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public void pauseBackgroundMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void stopBackgroundMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();

        if (musicLevel1 != null) musicLevel1.dispose();
        if (musicLevel2 != null) musicLevel2.dispose();
        if (musicLevel3 != null) musicLevel3.dispose();

        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }
    }
}
