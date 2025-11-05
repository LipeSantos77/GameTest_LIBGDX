package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport; 

public class GameRenderer {

    private MainGame game;
    private GameWorld world;

    // Utilitários de Desenho
    private OrthographicCamera camera;
    private FitViewport viewport;
    private ShapeRenderer shapeRenderer;

    // Texturas
    private Texture background;
    private Texture playerTexture;
    private Texture heartTexture;
    private Texture velocimetroTexture;
    private Texture pauseButtonTexture;
    private Texture pontosTexture;
    private Texture rockTexture;

    // UI de Pause
    private Rectangle pauseButtonRect;
    private final float PAUSE_BUTTON_W = 40f;
    private final float PAUSE_BUTTON_H = 40f;
    private Color pauseOverlayColor = new Color(0f, 0f, 0f, 0.7f);
    private GlyphLayout glyphLayout;

    // Alpha para o fade de pausa
    private float pauseFadeAlpha = 0f;
    private float gameOverFadeAlpha = 0f; 
    private float levelCompleteFadeAlpha = 0f; 

    // Cores Dinâmicas por Fase 
    private Color avalancheColor1, avalancheColor2, avalancheColor3;
    private Color trackBaseColor;
    private Color skyColor;

    public GameRenderer(MainGame game, GameWorld world) {
        this.game = game;
        this.world = world;

        camera = new OrthographicCamera();
        viewport = new FitViewport(MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT, camera);
        camera.setToOrtho(false, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);

        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        defineLevelColors();
        loadTextures();

        // Botão de pause
        pauseButtonRect = new Rectangle(
            MainGame.VIRTUAL_WIDTH - PAUSE_BUTTON_W - 10f,
            MainGame.VIRTUAL_HEIGHT - PAUSE_BUTTON_H - 10f,
            PAUSE_BUTTON_W,
            PAUSE_BUTTON_H
        );
    }


    private void defineLevelColors() {
        if (world.getCurrentLevel() == 2) {
            avalancheColor1 = new Color(0.9f, 0.95f, 1f, 0.9f); 
            avalancheColor2 = new Color(0.8f, 0.85f, 0.9f, 0.7f); 
            avalancheColor3 = new Color(0.7f, 0.75f, 0.8f, 0.4f);  
            trackBaseColor = new Color(0.95f, 0.98f, 1f, 1); 
            skyColor = new Color(0.1f, 0.1f, 0.3f, 1); 
        } else {
            avalancheColor1 = new Color(1, 1, 1, 0.9f);
            avalancheColor2 = new Color(0.9f, 0.9f, 1f, 0.7f);
            avalancheColor3 = new Color(0.8f, 0.8f, 0.9f, 0.4f);
            trackBaseColor = new Color(0.98f, 0.98f, 0.98f, 1); 
            skyColor = new Color(0.6f, 0.7f, 0.8f, 1); 
        }
    }

    private void loadTextures() {
        try {
            String backgroundPath;
            if (world.getCurrentLevel() == 1) {
                backgroundPath = "background.png";
            } else {
                backgroundPath = "MountainBackground.png";
            }
            if (background != null) background.dispose(); 
            background = new Texture(Gdx.files.internal(backgroundPath));

            playerTexture = new Texture(Gdx.files.internal("player_bike.png"));
            heartTexture = new Texture(Gdx.files.internal("heart.png"));
            velocimetroTexture = new Texture(Gdx.files.internal("velocimetro.png"));

            pauseButtonTexture = new Texture(Gdx.files.internal("pause.png"));
            pontosTexture = new Texture(Gdx.files.internal("pontos.png"));

            try {
                if (rockTexture != null) rockTexture.dispose();
                rockTexture = new Texture(Gdx.files.internal("pedra.png"));
            } catch (Exception ex) {
                Gdx.app.error("GameRenderer", "Erro ao carregar pedra.png: " + ex.getMessage());
                rockTexture = null;
            }

            Gdx.app.log("GameRenderer", "Texturas (Nível " + world.getCurrentLevel() + ") carregadas");

        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "Erro ao carregar texturas: " + e.getMessage());
            background = createPlaceholderTexture((int)MainGame.VIRTUAL_WIDTH, (int)MainGame.VIRTUAL_HEIGHT, Color.GRAY);
            playerTexture = createPlaceholderTexture((int)world.getPlayerWidth(), (int)world.getPlayerHeight(), Color.BLUE);
            heartTexture = createPlaceholderTexture(30, 30, Color.RED);
            velocimetroTexture = createPlaceholderTexture(80, 40, Color.DARK_GRAY);
            pauseButtonTexture = createPlaceholderTexture((int)PAUSE_BUTTON_W, (int)PAUSE_BUTTON_H, Color.LIGHT_GRAY);
            pontosTexture = createPlaceholderTexture(70, 25, Color.YELLOW);
            if (rockTexture == null) rockTexture = createPlaceholderTexture(48, 48, Color.DARK_GRAY);
        }
    }


    public void render(float delta) {
        boolean shouldUpdateColors = false;
        if (world.getCurrentLevel() == 2 && skyColor.b > 0.4f) { /
            shouldUpdateColors = true;
        } else if (world.getCurrentLevel() == 1 && skyColor.b < 0.4f) { 
            shouldUpdateColors = true;
        }

        if (shouldUpdateColors) {
            defineLevelColors();
            loadTextures();
        }

        updatePauseFade(delta);
        updateGameOverFade(delta);
        updateLevelCompleteFade(delta);

        // Limpa a tela com a cor do CÉU DINÂMICA
        Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        // 1. Desenhar a Pista de Neve/Gelo
        shapeRenderer.setProjectionMatrix(camera.combined);
        drawTrackBase();

        // 2. Desenhar Background 
        game.batch.setProjectionMatrix(camera.combined);
        drawBackground();

        // 3. Desenhar a Avalanche 
        drawAvalanche();
        drawWindAndDashEffects();

        // 4. Desenhar Entidades 
        game.batch.begin();
        // Desenha as pedras para fase2
        if (world.getCurrentLevel() == 2 && rockTexture != null) {
            for (GameWorld.Rock r : world.getRocks()) {
                game.batch.draw(rockTexture, r.x, r.y, r.width, r.height);
            }
        }

        // Desenha tronco e jogador 
        drawBot();
        drawPlayer();

        game.batch.end();

        // 5. Desenhar UI 
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawUI();
        drawPauseButton();

        if (pauseFadeAlpha > 0) {
            drawPauseOverlay();
        }
        if (gameOverFadeAlpha > 0) {
            drawGameOverOverlay();
        }
        if (world.isLevelComplete() || levelCompleteFadeAlpha > 0) {
            drawLevelCompleteOverlay();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updatePauseFade(float delta) {
        if (world.isPaused()) {
            pauseFadeAlpha = Math.min(1.0f, pauseFadeAlpha + delta * 2.5f); 
        } else {
            pauseFadeAlpha = Math.max(0.0f, pauseFadeAlpha - delta * 2.5f); 
        }
    }
    private void updateGameOverFade(float delta) {
        if (world.isGameOver()) {
            gameOverFadeAlpha = Math.min(1.0f, gameOverFadeAlpha + delta * 1.5f); 
        } else {
            gameOverFadeAlpha = Math.max(0.0f, gameOverFadeAlpha - delta * 1.5f); 
        }
    }
    private void updateLevelCompleteFade(float delta) {
        if (world.isLevelComplete()) {
            levelCompleteFadeAlpha = Math.min(1.0f, levelCompleteFadeAlpha + delta * 1.5f); 
        } else {
            levelCompleteFadeAlpha = Math.max(0.0f, levelCompleteFadeAlpha - delta * 1.5f);
        }
    }

    // Métodos de Desenh

    private void drawBackground() {
        game.batch.begin();
        float x1 = world.getBackgroundOffsetX();
        float x2 = world.getBackgroundOffsetX() + MainGame.VIRTUAL_WIDTH;
        float x3 = world.getBackgroundOffsetX() - MainGame.VIRTUAL_WIDTH;

        game.batch.draw(background, x1, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.draw(background, x2, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.draw(background, x3, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.end();
    }


    private void drawTrackBase() {
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(trackBaseColor); 
        shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, world.getTrackTop());
        shapeRenderer.end();
    }


    private void drawAvalanche() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);

        // Partículas de neve (SnowEffect)
        for (GameWorld.SnowEffect effect : world.getSnowEffects()) {
            shapeRenderer.setColor(effect.color);
            shapeRenderer.circle(effect.x, effect.y, effect.size);
        }

        float time = world.getAvalancheTimer();
        float currentAvalancheHeight = world.getAvalancheHeight();

        // Camada 3 (Fundo da avalanche, mais transparente)
        shapeRenderer.setColor(avalancheColor3); 
        for (float y = 0; y < currentAvalancheHeight; y += 2) {
            float wave = (float) Math.sin(y * 0.05f + time * 0.8f) * 15f;
            float wave2 = (float) Math.cos(y * 0.08f + time * 1.0f) * 10f;
            float frontX = GameWorld.LARGURA_AVALANCHE * 0.8f + wave + wave2;
            shapeRenderer.rect(0, y, frontX, 2);
        }

        // Camada 2 (Meio da avalanche)
        shapeRenderer.setColor(avalancheColor2); 
        for (float y = 0; y < currentAvalancheHeight; y += 2) {
            float wave = (float) Math.sin(y * 0.08f + time * 1.5f) * 18f;
            float wave2 = (float) Math.cos(y * 0.13f + time * 2.0f) * 9f;
            float frontX = GameWorld.LARGURA_AVALANCHE * 0.9f + wave + wave2;
            shapeRenderer.rect(0, y, frontX, 2);
        }

        // Camada 1 (Frente da avalanche, mais opaca)
        shapeRenderer.setColor(avalancheColor1); 
        for (float y = 0; y < currentAvalancheHeight; y += 1) {
            float wave = (float) Math.sin(y * 0.12f + time * 2.2f) * 20f;
            float wave2 = (float) Math.cos(y * 0.2f + time * 3.0f) * 10f;
            float frontX = GameWorld.LARGURA_AVALANCHE + wave + wave2;
            shapeRenderer.rect(0, y, frontX, 1);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawWindAndDashEffects() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);

        // Efeito de Vento
        for (GameWorld.WindLine line : world.getWindLines()) {
            if (line.isActive) {
                shapeRenderer.setColor(1, 1, 1, line.alpha);
                float angle = (float) (Math.sin(world.getWindTimer() * 1.5f + line.y * 0.02f) * 0.05f);
                float endX = line.x + line.length;
                float endY = line.y + angle * line.length;
                shapeRenderer.rectLine(line.x, line.y, endX, endY, line.thickness);
            }
        }

        // Efeito de Rastro (Dash)
        for (GameWorld.PlayerDashLine line : world.getDashLines()) {
            shapeRenderer.setColor(0.8f, 0.9f, 1f, line.alpha);
            shapeRenderer.rectLine(line.x, line.y, line.x + line.length, line.y, 2.5f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawPlayer() {
        // Efeito de piscar quando invencível
        if (world.podeLevarDano() || (int)(world.getTempoInvencivel() * 10) % 2 == 0) {

            float speedPercent = world.getPlayerSpeedX() / world.getMaxSpeed();
            float tilt = MathUtils.clamp(speedPercent * 15f, -8f, 15f);

            game.batch.draw(playerTexture,
                world.getPlayerX(), world.getPlayerY(),
                world.getPlayerWidth() / 2f, world.getPlayerHeight() / 2f,
                world.getPlayerWidth(), world.getPlayerHeight(),
                1f, 1f,
                tilt,
                0, 0,
                playerTexture.getWidth(), playerTexture.getHeight(),
                false, false);
        }
    }

    private void drawBot() {
        BotLog bot = world.getBot();
        if (bot != null && bot.texture != null) {
            game.batch.setColor(1, 1, 1, bot.getAlpha());
            game.batch.draw(bot.texture, bot.getX(), bot.getY(), bot.getWidth(), bot.getHeight());
            game.batch.setColor(Color.WHITE);
        }
    }

    private void drawUI() {
        game.batch.begin();

        float uiX = 10f;
        float uiY = MainGame.VIRTUAL_HEIGHT - 20;

        // Desenho dos Pontos
        float pontosIconW = 40f;
        float pontosIconH = 40f;
        game.batch.draw(pontosTexture, uiX, uiY - pontosIconH, pontosIconW, pontosIconH);

        String pontosText = "" + world.getPontos();
        glyphLayout.setText(game.font, pontosText);
        game.font.draw(game.batch, pontosText,
            uiX + pontosIconW + 8,
            uiY - pontosIconH / 2 + glyphLayout.height / 2);

        uiY -= (pontosIconH + 15);

        // Desenho das Vidas
        int vidas = world.getVidas();
        float heartSize = 30f;
        for (int i = 0; i < vidas; i++) {
            game.batch.draw(heartTexture, uiX + (i * (heartSize + 5)), uiY - heartSize, heartSize, heartSize);
        }
        uiY -= (heartSize + 5);

        // Desenho do Velocímetro
        float velocimetroW = 40f;
        float velocimetroH = 40f;
        game.batch.draw(velocimetroTexture, uiX, uiY - velocimetroH, velocimetroW, velocimetroH);

        float currentSpeed = world.getPlayerSpeedX();
        float displaySpeed = 50 + (currentSpeed / world.getBaseMaxSpeed()) * 100f;
        displaySpeed = Math.max(50, displaySpeed);

        String speedText = String.format("%.0f km/h", displaySpeed);
        glyphLayout.setText(game.font, speedText);
        game.font.draw(game.batch, speedText,
            uiX + velocimetroW + 8,
            uiY - velocimetroH / 2 + glyphLayout.height / 2);

        uiY -= (velocimetroH + 10);

        // Status de Invencibilidade
        if (!world.podeLevarDano()) {
            game.font.draw(game.batch, "INVENCIVEL!", uiX, uiY);
        }

        // Desenha o Nível Atual
        String levelText = "NIVEL " + world.getCurrentLevel();
        glyphLayout.setText(game.font, levelText);
        game.font.draw(game.batch, levelText,
            MainGame.VIRTUAL_WIDTH - glyphLayout.width - 10f,
            MainGame.VIRTUAL_HEIGHT - 10f);

        game.batch.end();
    }

    // Overlays 

    private void drawPauseOverlay() {
        shapeRenderer.begin(ShapeType.Filled);
        pauseOverlayColor.a = pauseFadeAlpha * 0.7f;
        shapeRenderer.setColor(pauseOverlayColor);
        shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        if (pauseFadeAlpha > 0.5f) {
            game.batch.begin();
            game.font.setColor(Color.WHITE);
            String text = "PAUSADO";
            glyphLayout.setText(game.font, text);
            game.font.draw(game.batch, text,
                (MainGame.VIRTUAL_WIDTH - glyphLayout.width) / 2,
                (MainGame.VIRTUAL_HEIGHT + glyphLayout.height) / 2);
            game.batch.end();
        }
    }

    private void drawGameOverOverlay() {
        shapeRenderer.begin(ShapeType.Filled);
        Color color = new Color(0.5f, 0f, 0f, gameOverFadeAlpha * 0.9f);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        if (gameOverFadeAlpha > 0.5f) {
            game.batch.begin();
            game.font.setColor(Color.WHITE);
            String text = "GAME OVER";
            glyphLayout.setText(game.font, text);
            game.font.draw(game.batch, text,
                (MainGame.VIRTUAL_WIDTH - glyphLayout.width) / 2,
                (MainGame.VIRTUAL_HEIGHT + glyphLayout.height) / 2);

            String restartText = "Toque para Reiniciar";
            glyphLayout.setText(game.font, restartText);
            game.font.draw(game.batch, restartText,
                (MainGame.VIRTUAL_WIDTH - glyphLayout.width) / 2,
                (MainGame.VIRTUAL_HEIGHT + glyphLayout.height) / 2 - 50);

            game.batch.end();
        }
    }

    private void drawLevelCompleteOverlay() {
        shapeRenderer.begin(ShapeType.Filled);
        Color color = new Color(0f, 0.5f, 0f, levelCompleteFadeAlpha * 0.9f);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        if (levelCompleteFadeAlpha > 0.5f) {
            game.batch.begin();
            game.font.setColor(Color.WHITE);
            String text = world.getCurrentLevel() == 1 ? "FASE 1 COMPLETA!" : "TODAS AS FASES CONCLUIDAS!";
            glyphLayout.setText(game.font, text);
            game.font.draw(game.batch, text,
                (MainGame.VIRTUAL_WIDTH - glyphLayout.width) / 2,
                (MainGame.VIRTUAL_HEIGHT + glyphLayout.height) / 2);

            String nextText = world.getCurrentLevel() == 1 ? "Iniciando Fase 2..." : "Reiniciando...";
            glyphLayout.setText(game.font, nextText);
            game.font.draw(game.batch, nextText,
                (MainGame.VIRTUAL_WIDTH - glyphLayout.width) / 2,
                (MainGame.VIRTUAL_HEIGHT + glyphLayout.height) / 2 - 50);

            game.batch.end();
        }
    }

    // Utilitários

    public Rectangle getPauseButtonRect() {
        return pauseButtonRect;
    }

    private void drawPauseButton() {
        game.batch.begin();
        game.batch.draw(pauseButtonTexture,
            pauseButtonRect.x,
            pauseButtonRect.y,
            pauseButtonRect.width,
            pauseButtonRect.height);
        game.batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        // Dispor todas as texturas carregadas para evitar vazamento de memória
        if (background != null) background.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (heartTexture != null) heartTexture.dispose();
        if (velocimetroTexture != null) velocimetroTexture.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (pontosTexture != null) pontosTexture.dispose();
        if (rockTexture != null) rockTexture.dispose(); 
    }

    private Texture createPlaceholderTexture(int width, int height, Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public float getLevelCompleteFadeAlpha() {
        return levelCompleteFadeAlpha;
    }


    public FitViewport getViewport() {
        return viewport;
    }


    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
