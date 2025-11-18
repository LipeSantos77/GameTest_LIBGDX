package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Vector3; // Import essencial

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

    // Altura Virtual do Jogo (800)
    private static final float V_HEIGHT = MainGame.VIRTUAL_HEIGHT;
    private static final float V_WIDTH = MainGame.VIRTUAL_WIDTH;

    // --- Estado da Transição ---
    private enum ScreenState {
        FADE_IN,
        WAITING_INPUT,
        FADE_OUT_NEXT,
        FADE_OUT_MENU,
        TRANSITIONING
    }
    private ScreenState state;
    private float fadeAlpha = 0f;
    private float pulseTime = 0f;

    // --- Opções de Menu e Interatividade ---
    private Rectangle yesButton;
    private Rectangle noButton;
    private boolean yesHovered = false;
    private boolean noHovered = false;

    private Texture backgroundTexture;

    public TransitionScreen(MainGame game, int currentLevel, int nextLevel, int pontos) {
        this.game = game;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.pontos = pontos;

        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        this.state = ScreenState.FADE_IN;

        // Configuração de Fontes
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(new Color(1, 0.9f, 0.3f, 1));

        optionFont = new BitmapFont();
        optionFont.getData().setScale(2.0f);

        promptFont = new BitmapFont();
        promptFont.getData().setScale(1.5f);
        promptFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1));

        // Carregar background
        try {
            backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        } catch (Exception e) {
            backgroundTexture = createPlaceholderTexture((int)V_WIDTH, (int)V_HEIGHT, new Color(0.1f, 0.1f, 0.3f, 1));
        }

        // Definir áreas dos botões
        float buttonWidth = 250f;
        float buttonHeight = 80f;
        float centerX = V_WIDTH / 2f;
        float baseY = 200f;

        yesButton = new Rectangle(centerX - buttonWidth - 40f, baseY, buttonWidth, buttonHeight);
        noButton = new Rectangle(centerX + 40f, baseY, buttonWidth, buttonHeight);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        // Desenhar background
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, V_WIDTH, V_HEIGHT);
        game.batch.end();

        // Desenhar overlay de fade (se aplicável)
        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, V_WIDTH, V_HEIGHT);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Desenhar conteúdo principal (título, botões, etc.)
        if (state != ScreenState.FADE_IN && state != ScreenState.TRANSITIONING) {
            drawContent();
        }

        handleInput();
    }

    private void update(float delta) {
        pulseTime += delta;

        switch (state) {
            case FADE_IN:
                fadeAlpha += delta * 1.5f;
                if (fadeAlpha >= 1f) {
                    fadeAlpha = 1f;
                    state = ScreenState.WAITING_INPUT;
                }
                break;

            case WAITING_INPUT:
                updateButtonHover();
                break;

            case FADE_OUT_NEXT:
            case FADE_OUT_MENU:
                fadeAlpha -= delta * 1.5f;

                if (fadeAlpha <= 0.1f) {
                    fadeAlpha = 0f;
                    state = ScreenState.TRANSITIONING;

                    // --- TRATAMENTO DE ERRO CRÍTICO NA TRANSIÇÃO (EXECUTA NA THREAD PRINCIPAL) ---
                    if (state == ScreenState.FADE_OUT_MENU) {
                        game.setScreen(new MenuScreen(game));
                    } else {
                        try {
                            // Tenta carregar a próxima fase (Level 2)
                            game.setScreen(new GameScreen(game, nextLevel));
                        } catch (Exception e) {
                            // SE FALHAR (por falta de recurso, como textura/mapa), REGISTRA O ERRO E VOLTA PARA O MENU
                            Gdx.app.error("JOGO_CRASH", "ERRO FATAL AO CARREGAR A FASE " + nextLevel + ": " + e.getMessage());
                            e.printStackTrace();
                            game.setScreen(new MenuScreen(game));
                        }
                    }
                    dispose();
                }
                break;

            case TRANSITIONING:
                break;
        }
    }

    private void updateButtonHover() {
        if (state != ScreenState.WAITING_INPUT) return;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        // Converte coordenadas da tela para coordenadas virtuais do jogo
        Vector3 worldCoords = viewport.unproject(new Vector3(mouseX, mouseY, 0));

        yesHovered = yesButton.contains(worldCoords.x, worldCoords.y);
        noHovered = noButton.contains(worldCoords.x, worldCoords.y);
    }

    private void drawContent() {
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Overlay semi-transparente
        game.batch.setColor(0, 0, 0, 0.7f);
        float contentWidth = V_WIDTH * 0.8f;
        float contentHeight = V_HEIGHT * 0.5f;
        float contentX = (V_WIDTH - contentWidth) / 2f;
        float contentY = (V_HEIGHT - contentHeight) / 2f;

        game.batch.draw(createPlaceholderTexture(1, 1, Color.BLACK),
            contentX,
            contentY,
            contentWidth,
            contentHeight);
        game.batch.setColor(Color.WHITE);

        // Título principal
        float pulse = (float) Math.sin(pulseTime * 3) * 0.1f + 0.9f;
        titleFont.setColor(1, 0.9f, 0.3f * pulse, 1);

        String title = "FASE " + currentLevel + " COMPLETA!";
        layout.setText(titleFont, title);
        titleFont.draw(game.batch, title,
            (V_WIDTH - layout.width) / 2,
            V_HEIGHT - 200);

        // Pontuação
        promptFont.setColor(1, 1, 1, 1);
        String scoreText = "Pontuação: " + pontos;
        layout.setText(promptFont, scoreText);
        promptFont.draw(game.batch, scoreText,
            (V_WIDTH - layout.width) / 2,
            V_HEIGHT - 300);

        // Mensagem
        String message = "Deseja continuar para a próxima fase?";
        layout.setText(promptFont, message);
        promptFont.draw(game.batch, message,
            (V_WIDTH - layout.width) / 2,
            V_HEIGHT - 380);

        // Desenhar botões
        drawButton(yesButton, "SIM, VAMOS LÁ!", yesHovered);
        drawButton(noButton, "NÃO, SAIR", noHovered);

        // Instrução
        if (state == ScreenState.WAITING_INPUT) {
            String instruction = "Selecione uma opção (Mouse ou Teclado)";
            layout.setText(promptFont, instruction);
            promptFont.setColor(1, 1, 1, (float) (Math.sin(pulseTime * 4) * 0.3f + 0.7f));
            promptFont.draw(game.batch, instruction,
                (V_WIDTH - layout.width) / 2,
                120);
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
        if (hovered) {
            optionFont.setColor(1, 0.9f, 0.3f, 1);
        } else {
            optionFont.setColor(0.9f, 0.9f, 0.9f, 1);
        }

        layout.setText(optionFont, text);
        optionFont.draw(game.batch, text,
            button.x + (button.width - layout.width) / 2,
            button.y + (button.height + layout.height) / 2);
    }

    private void handleInput() {
        if (state != ScreenState.WAITING_INPUT) return;

        boolean inputReceived = false;
        boolean toNextLevel = false;

        // 1. Input de Mouse
        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.input.getY();
            Vector3 worldCoords = viewport.unproject(new Vector3(mouseX, mouseY, 0));

            if (yesButton.contains(worldCoords.x, worldCoords.y)) {
                inputReceived = true;
                toNextLevel = true;
            } else if (noButton.contains(worldCoords.x, worldCoords.y)) {
                inputReceived = true;
                toNextLevel = false;
            }
        }

        // 2. Input de Teclado
        if (!inputReceived) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.Y) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
                inputReceived = true;
                toNextLevel = true;
            } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.N) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
                inputReceived = true;
                toNextLevel = false;
            }
        }

        // 3. Mudar o estado para iniciar a transição
        if (inputReceived) {
            state = toNextLevel ? ScreenState.FADE_OUT_NEXT : ScreenState.FADE_OUT_MENU;
        }
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
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (optionFont != null) optionFont.dispose();
        if (promptFont != null) promptFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (backgroundTexture != null && backgroundTexture != game.placeholderTexture) {
            backgroundTexture.dispose();
        }
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
