package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;

public class GameScreen implements Screen {
    private MainGame game;
    private GameWorld world;
    private GameRenderer renderer;

    public GameScreen(MainGame game, int level) {
        this.game = game;

        // --- CORREÇÃO MÚSICA: Força a troca da música ao criar a tela ---
        game.changeMusic(level);

        // O GameWorld agora é criado com o nível
        this.world = new GameWorld(game, level);
        this.renderer = new GameRenderer(game, world);
    }

    @Override
    public void render(float delta) {
        // --- CONTROLE (INPUT) ---
        handleInput();

        // --- LÓGICA (MODEL) ---
        // Só atualiza o mundo se não estiver pausado e não for Game Over
        if (!world.isPaused() && !world.isGameOver()) {
            world.update(delta);
        }

        // --- DESENHO (VIEW) ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render(delta);

        // --- VERIFICA TRANSIÇÕES DE TELA ---

        // 1. Se a fase estiver completa
        if (world.isLevelComplete()) {
            Gdx.app.log("GameScreen", "Nível " + world.getCurrentLevel() + " Completo! Mostrando tela de transição...");

            TransitionScreen transitionScreen = new TransitionScreen(
                game,
                world.getCurrentLevel(),
                world.getCurrentLevel() + 1,
                world.getPontos()
            );
            game.setScreen(transitionScreen);
            dispose();
            return;
        }

        // 2. --- CORREÇÃO GAME OVER (Request 2): Reiniciar Jogo ---
        if (world.isGameOver()) {
            // Permite reiniciar clicando ou apertando ENTER
            if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // Reinicia a MESMA fase atual
                game.setScreen(new GameScreen(game, world.getCurrentLevel()));
                dispose();
            }
        }
    }

    private void handleInput() {
        // Input de Pause (tecla e clique) - Só funciona se não for Game Over
        if (!world.isGameOver()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                world.togglePause();
            }

            if (Gdx.input.justTouched()) {
                Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                renderer.getViewport().unproject(touch);
                if (renderer.getPauseButtonRect().contains(touch.x, touch.y)) {
                    world.togglePause();
                }
            }
        }

        // Se não estiver pausado e nem game over, processa movimento
        if (!world.isPaused() && !world.isGameOver()) {
            boolean isMovingUp = Gdx.input.isKeyPressed(Input.Keys.UP);
            boolean isMovingDown = Gdx.input.isKeyPressed(Input.Keys.DOWN);
            boolean isAccelerating = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
            boolean isBraking = Gdx.input.isKeyPressed(Input.Keys.LEFT);

            world.handlePlayerInput(isMovingUp, isMovingDown, isAccelerating, isBraking);
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        world.dispose();
    }

    @Override
    public void show() {
        // Garante que a música esteja tocando ao mostrar a tela
        game.playBackgroundMusic();
    }

    @Override public void hide() {}
    @Override public void pause() {
        // Pausa automática se o app for para background (ex: celular toca)
        world.setPaused(true);
    }
    @Override public void resume() {}
}
