package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;

public class GameScreen implements Screen {
    private MainGame game;
    private GameWorld world;
    private GameRenderer renderer;

    public GameScreen(MainGame game, int level) {
        this.game = game;
        this.world = new GameWorld(game);
        this.renderer = new GameRenderer(game, world);
    }

    @Override
    public void render(float delta) {
        // --- CONTROLE (INPUT) ---
        handleInput();

        // --- LÓGICA (MODEL) ---
        if (!world.isPaused()) {
            world.update(delta);
        }

        // --- DESENHO (VIEW) ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(delta);

        // --- VERIFICA GAME OVER ---
        if (world.isGameOver()) {
            Gdx.app.log("GameScreen", "Game Over! Pontos finais: " + world.getPontos());
            game.setScreen(new MenuScreen(game));
            // dispose() é chamado automaticamente pela MainGame quando setScreen é usado
        }
    }

    private void handleInput() {
        // Input de Pause (tecla e clique)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            world.togglePause();
        }

        if (Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            renderer.getViewport().unproject(touch); // Usa a viewport do renderer
            if (renderer.getPauseButtonRect().contains(touch.x, touch.y)) {
                world.togglePause();
            }
        }

        // Se não estiver pausado, processa input de movimento
        if (!world.isPaused()) {
            boolean isMovingUp = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP);
            boolean isMovingDown = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN);
            boolean isAccelerating = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT);
            boolean isBraking = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT);

            world.handlePlayerInput(isMovingUp, isMovingDown, isAccelerating, isBraking);
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        // Ocultado o dispose individual pois o setScreen fará isso
        // Apenas para garantir, chamamos o dispose do renderer e world
        renderer.dispose();
        world.dispose();
    }

    @Override
    public void show() {
        game.playBackgroundMusic();
    }

    @Override
    public void hide() {
        // Não é necessário pausar a música aqui, pois MenuScreen vai dar play
    }

    @Override public void pause() {
        world.setPaused(true);
    }
    @Override public void resume() {
        // Não despausa automaticamente, o jogador decide
    }
}
