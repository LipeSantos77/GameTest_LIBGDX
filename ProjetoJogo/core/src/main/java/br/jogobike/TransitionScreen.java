
package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class TransitionScreen implements Screen {
    private final MainGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont titleFont;
    private final BitmapFont optionFont;
    private final BitmapFont promptFont;
    private final GlyphLayout layout;

    private final int currentLevel;
    private final int nextLevel;
    private final int pontos;

    private float fadeAlpha = 0f;
    private boolean fadeIn = true;
    private boolean waitingForInput = false;
    private boolean transitionComplete = false;

    // Opções do menu
    private Rectangle yesButton;
    private Rectangle noButton;
    private boolean yesHovered = false;
    private boolean noHovered = false;

    // Efeitos visuais
    private float pulseTime = 0f;
    private Texture backgroundTexture;

    // Dimensões para centralização
    private final float CONTENT_WIDTH = MainGame.VIRTUAL_WIDTH * 0.8f;
    private final float CONTENT_HEIGHT = MainGame.VIRTUAL_HEIGHT * 0.6f;
    private final float CONTENT_X = (MainGame.VIRTUAL_WIDTH - CONTENT_WIDTH) / 2f;
    private final float CONTENT_Y = (MainGame.VIRTUAL_HEIGHT - CONTENT_HEIGHT) / 2f;

    public TransitionScreen(MainGame game, int currentLevel, int nextLevel, int pontos) {
        this.game = game;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.pontos = pontos;

        camera = new OrthographicCamera();
        viewport = new FitViewport(MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT, camera);
        camera.setToOrtho(false, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);

        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        // Carregar fontes
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.2f);
        titleFont.setColor(new Color(1, 0.9f, 0.3f, 1));

        optionFont = new BitmapFont();
        optionFont.getData().setScale(1.8f);

        promptFont = new BitmapFont();
        promptFont.getData().setScale(1.3f);
        promptFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1));

        // Carregar background
        try {
            backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        } catch (Exception e) {
            backgroundTexture = createPlaceholderTexture((int)MainGame.VIRTUAL_WIDTH, (int)MainGame.VIRTUAL_HEIGHT, new Color(0.1f, 0.1f, 0.3f, 1));
        }

        // Definir áreas dos botões - CENTRALIZADOS
        float buttonWidth = 200f;
        float buttonHeight = 60f;
        float centerX = MainGame.VIRTUAL_WIDTH / 2f;
        float buttonY = CONTENT_Y + 120f;

        yesButton = new Rectangle(centerX - buttonWidth - 20f, buttonY, buttonWidth, buttonHeight);
        noButton = new Rectangle(centerX + 20f, buttonY, buttonWidth, buttonHeight);
    }

    @Override
    public void render(float delta) {
        update(delta);

        // Limpar tela
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        // Desenhar background
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.end();

        // Desenhar overlay de fade
        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha * 0.8f);
            shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Desenhar conteúdo principal (quando não estiver em fade completo)
        if (fadeAlpha < 0.9f || waitingForInput) {
            drawContent();
        }

        // Processar input
        handleInput();
    }

    private void update(float delta) {
        pulseTime += delta;

        // Animação de fade
        if (fadeIn) {
            fadeAlpha += delta * 2f;
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                fadeIn = false;
                waitingForInput = true;
            }
        }

        // Atualizar hover dos botões
        if (waitingForInput) {
            updateButtonHover();
        }
    }

    private void updateButtonHover() {
        // Usar a mesma viewport para unproject
        com.badlogic.gdx.math.Vector3 touchPos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);

        yesHovered = yesButton.contains(touchPos.x, touchPos.y);
        noHovered = noButton.contains(touchPos.x, touchPos.y);
    }

    private void drawContent() {
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Overlay semi-transparente para melhor legibilidade - CENTRALIZADO
        game.batch.setColor(0, 0, 0, 0.7f);
        game.batch.draw(createPlaceholderTexture(1, 1, Color.BLACK),
            CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        game.batch.setColor(Color.WHITE);

        // Título principal com efeito de pulso - CENTRALIZADO
        float pulse = (float) Math.sin(pulseTime * 3) * 0.1f + 0.9f;
        titleFont.setColor(1, 0.9f, 0.3f * pulse, 1);

        String title = "FASE " + currentLevel + " COMPLETA!";
        layout.setText(titleFont, title);
        float titleX = (MainGame.VIRTUAL_WIDTH - layout.width) / 2;
        float titleY = CONTENT_Y + CONTENT_HEIGHT - 50f;
        titleFont.draw(game.batch, title, titleX, titleY);

        // Pontuação - CENTRALIZADO
        promptFont.setColor(1, 1, 1, 1);
        String scoreText = "Pontuação: " + pontos;
        layout.setText(promptFont, scoreText);
        float scoreX = (MainGame.VIRTUAL_WIDTH - layout.width) / 2;
        float scoreY = titleY - 50f;
        promptFont.draw(game.batch, scoreText, scoreX, scoreY);

        // Mensagem - CENTRALIZADO
        String message = "Deseja continuar para a próxima fase?";
        layout.setText(promptFont, message);
        float messageX = (MainGame.VIRTUAL_WIDTH - layout.width) / 2;
        float messageY = scoreY - 40f;
        promptFont.draw(game.batch, message, messageX, messageY);

        // Desenhar botões
        drawButton(yesButton, "SIM, VAMOS LÁ!", yesHovered);
        drawButton(noButton, "NÃO, SAIR", noHovered);

        // Instrução - CENTRALIZADA
        if (waitingForInput) {
            String instruction = "Use o mouse para selecionar uma opção";
            layout.setText(promptFont, instruction);
            promptFont.setColor(1, 1, 1, (float) (Math.sin(pulseTime * 4) * 0.3f + 0.7f));
            float instructionX = (MainGame.VIRTUAL_WIDTH - layout.width) / 2;
            float instructionY = CONTENT_Y + 30f;
            promptFont.draw(game.batch, instruction, instructionX, instructionY);
        }

        game.batch.end();

        // Desenhar bordas dos botões com ShapeRenderer
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Botão Sim
        if (yesHovered) {
            shapeRenderer.setColor(1, 0.8f, 0.2f, 0.8f);
        } else {
            shapeRenderer.setColor(0.3f, 0.7f, 0.3f, 0.6f);
        }
        shapeRenderer.rect(yesButton.x, yesButton.y, yesButton.width, yesButton.height);

        // Botão Não
        if (noHovered) {
            shapeRenderer.setColor(1, 0.8f, 0.2f, 0.8f);
        } else {
            shapeRenderer.setColor(0.7f, 0.3f, 0.3f, 0.6f);
        }
        shapeRenderer.rect(noButton.x, noButton.y, noButton.width, noButton.height);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawButton(Rectangle button, String text, boolean hovered) {
        // Cor do botão baseada no hover
        if (hovered) {
            optionFont.setColor(1, 0.9f, 0.3f, 1);
        } else {
            optionFont.setColor(0.9f, 0.9f, 0.9f, 1);
        }

        layout.setText(optionFont, text);
        float textX = button.x + (button.width - layout.width) / 2;
        float textY = button.y + (button.height + layout.height) / 2;
        optionFont.draw(game.batch, text, textX, textY);
    }

    private void handleInput() {
        if (waitingForInput && Gdx.input.justTouched()) {
            // Usar a mesma viewport para unproject
            com.badlogic.gdx.math.Vector3 touchPos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            if (yesButton.contains(touchPos.x, touchPos.y)) {
                // Jogador escolheu continuar
                waitingForInput = false;
                fadeIn = false;
                startFadeOut();
            } else if (noButton.contains(touchPos.x, touchPos.y)) {
                // Jogador escolheu sair
                waitingForInput = false;
                fadeIn = false;
                returnToMenu();
            }
        }

        // Também permitir seleção por teclado
        if (waitingForInput) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.Y) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
                waitingForInput = false;
                fadeIn = false;
                startFadeOut();
            } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.N) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
                waitingForInput = false;
                fadeIn = false;
                returnToMenu();
            }
        }
    }

    private void startFadeOut() {
        // Iniciar fade out para próxima fase
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Aguardar um pouco antes do fade out
                    Thread.sleep(500);

                    // Fazer fade out
                    while (fadeAlpha > 0) {
                        fadeAlpha -= 0.05f;
                        if (fadeAlpha < 0) fadeAlpha = 0;
                        Thread.sleep(50);
                    }

                    // Mudar para próxima fase
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            GameScreen nextGameScreen = new GameScreen(game, nextLevel);
                            game.setScreen(nextGameScreen);
                            dispose();
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void returnToMenu() {
        // Voltar ao menu principal
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fazer fade out
                    while (fadeAlpha > 0) {
                        fadeAlpha -= 0.05f;
                        if (fadeAlpha < 0) fadeAlpha = 0;
                        Thread.sleep(50);
                    }

                    // Voltar ao menu
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            MenuScreen menuScreen = new MenuScreen(game);
                            game.setScreen(menuScreen);
                            dispose();
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Texture createPlaceholderTexture(int width, int height, Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        // Garantir que a câmera fique centralizada
        camera.position.set(MainGame.VIRTUAL_WIDTH / 2, MainGame.VIRTUAL_HEIGHT / 2, 0);
        camera.update();
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        optionFont.dispose();
        promptFont.dispose();
        shapeRenderer.dispose();
        if (backgroundTexture != null && backgroundTexture != game.placeholderTexture) {
            backgroundTexture.dispose();
        }
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
