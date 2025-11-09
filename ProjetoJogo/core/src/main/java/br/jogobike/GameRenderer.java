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
    // --- MUDANÇA (Request 2 e 4): Novas texturas de UI ---
    private Texture pauseButtonTexture;
    private Texture pontosTexture;


    // UI de Pause
    private Rectangle pauseButtonRect;
    // --- MUDANÇA: Botão de pausa 40x40 ---
    private final float PAUSE_BUTTON_W = 40f;
    private final float PAUSE_BUTTON_H = 40f;
    private Color pauseOverlayColor = new Color(0f, 0f, 0f, 0.7f);
    private GlyphLayout glyphLayout;

    // Alpha para o fade de pausa
    private float pauseFadeAlpha = 0f;

    public GameRenderer(MainGame game, GameWorld world) {
        this.game = game;
        this.world = world;

        camera = new OrthographicCamera();
        viewport = new FitViewport(MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT, camera);
        camera.setToOrtho(false, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);

        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        // Carregar texturas
        loadTextures();

        // Botão de pause
        pauseButtonRect = new Rectangle(
            MainGame.VIRTUAL_WIDTH - PAUSE_BUTTON_W - 10f,
            MainGame.VIRTUAL_HEIGHT - PAUSE_BUTTON_H - 10f,
            PAUSE_BUTTON_W,
            PAUSE_BUTTON_H
        );
    }

    private void loadTextures() {
        try {
            background = new Texture(Gdx.files.internal("background.png"));
            playerTexture = new Texture(Gdx.files.internal("player_bike.png"));
            heartTexture = new Texture(Gdx.files.internal("heart.png"));
            velocimetroTexture = new Texture(Gdx.files.internal("velocimetro.png"));

            // --- MUDANÇA (Request 2 e 4): Carregar novas texturas ---
            pauseButtonTexture = new Texture(Gdx.files.internal("pause.png"));
            pontosTexture = new Texture(Gdx.files.internal("pontos.png"));

            Gdx.app.log("GameRenderer", "Texturas principais e de UI carregadas");

        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "Erro ao carregar texturas: " + e.getMessage());
            background = createPlaceholderTexture((int)MainGame.VIRTUAL_WIDTH, (int)MainGame.VIRTUAL_HEIGHT, Color.GRAY);
            playerTexture = createPlaceholderTexture((int)world.getPlayerWidth(), (int)world.getPlayerHeight(), Color.BLUE);
            heartTexture = createPlaceholderTexture(30, 30, Color.RED);
            velocimetroTexture = createPlaceholderTexture(80, 40, Color.DARK_GRAY);

            // Placeholders para novas texturas
            pauseButtonTexture = createPlaceholderTexture((int)PAUSE_BUTTON_W, (int)PAUSE_BUTTON_H, Color.LIGHT_GRAY);
            pontosTexture = createPlaceholderTexture(70, 25, Color.YELLOW);
        }
    }

    /**
     * O loop principal de desenho.
     */
    public void render(float delta) {
        updatePauseFade(delta);

        viewport.apply();
        camera.update();

        // 1. Desenhar Background (Batch)
        game.batch.setProjectionMatrix(camera.combined);
        drawBackground();

        // 2. Desenhar Efeitos (ShapeRenderer)
        shapeRenderer.setProjectionMatrix(camera.combined);
        drawAvalanche(); // <-- AVALANCHE MELHORADA
        drawWindAndDashEffects();

        // 3. Desenhar Entidades (Batch)
        game.batch.begin();
        drawPlayer();
        drawBot();
        game.batch.end();

        // 4. Desenhar UI (Batch e ShapeRenderer)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawUI();
        drawPauseButton(); // <-- AGORA USA IMAGEM

        if (pauseFadeAlpha > 0) {
            drawPauseOverlay();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updatePauseFade(float delta) {
        if (world.isPaused()) {
            pauseFadeAlpha = Math.min(1.0f, pauseFadeAlpha + delta * 2.5f); // Fade in
        } else {
            pauseFadeAlpha = Math.max(0.0f, pauseFadeAlpha - delta * 2.5f); // Fade out
        }
    }

    // --- Métodos de Desenho ---

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

    // --- MUDANÇA (Request 1): Avalanche totalmente refeita ---
    private void drawAvalanche() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);

        // Partículas de neve (SnowEffect) - Desenhadas primeiro, por trás
        for (GameWorld.SnowEffect effect : world.getSnowEffects()) {
            shapeRenderer.setColor(effect.color);
            shapeRenderer.circle(effect.x, effect.y, effect.size);
        }

        float time = world.getAvalancheTimer();

        // Camada 3 (Fundo, mais lenta, mais larga, mais escura/transparente)
        shapeRenderer.setColor(0.8f, 0.8f, 0.9f, 0.4f);
        for (float y = 0; y < GameWorld.ALTURA_AVALANCHE; y += 12) {
            float wave = (float) Math.sin(y * 0.05f + time * 0.8f) * 10f;
            float wave2 = (float) Math.cos(y * 0.02f + time * 1.0f) * 8f;
            float frontX = GameWorld.LARGURA_AVALANCHE * 0.8f + wave + wave2; // Mais recuada
            shapeRenderer.rect(0, y, frontX, 12);
        }

        // Camada 2 (Meio, cor sólida média)
        shapeRenderer.setColor(0.9f, 0.9f, 1f, 0.7f);
        for (float y = 0; y < GameWorld.ALTURA_AVALANCHE; y += 10) {
            float wave = (float) Math.sin(y * 0.08f + time * 1.5f) * 12f;
            float wave2 = (float) Math.cos(y * 0.03f + time * 2.0f) * 6f;
            float frontX = GameWorld.LARGURA_AVALANCHE * 0.9f + wave + wave2; // Recuada
            shapeRenderer.rect(0, y, frontX, 10);
        }

        // Camada 1 (Frente, mais rápida, mais "branca", define a borda)
        shapeRenderer.setColor(1, 1, 1, 0.9f);
        for (float y = 0; y < GameWorld.ALTURA_AVALANCHE; y += 8) {
            float wave = (float) Math.sin(y * 0.12f + time * 2.2f) * 15f;
            float wave2 = (float) Math.cos(y * 0.05f + time * 3.0f) * 8f;
            float frontX = GameWorld.LARGURA_AVALANCHE + wave + wave2; // Esta define a "hitbox" visual
            shapeRenderer.rect(0, y, frontX, 8);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    // --- FIM DA MUDANÇA DA AVALANCHE ---

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

        // --- MUDANÇA (Request 5): Mover UI para a esquerda (canto da tela) ---
        float uiX = 10f;
        float uiY = MainGame.VIRTUAL_HEIGHT - 20;

        // --- MUDANÇA (Request 4): Substituir texto "Pontos" por ícone ---
        float pontosIconW = 40f;
        float pontosIconH = 40f;
        game.batch.draw(pontosTexture, uiX, uiY - pontosIconH, pontosIconW, pontosIconH);

        String pontosText = "" + world.getPontos();
        glyphLayout.setText(game.font, pontosText);
        game.font.draw(game.batch, pontosText,
            uiX + pontosIconW + 8, // Ao lado do ícone
            uiY - pontosIconH / 2 + glyphLayout.height / 2); // Centralizado no ícone

        uiY -= (pontosIconH + 15); // Espaçamento
        // --- FIM DA MUDANÇA DE PONTOS ---

        // Vidas (Corações)
        int vidas = world.getVidas();
        float heartSize = 30f;
        for (int i = 0; i < vidas; i++) {
            game.batch.draw(heartTexture, uiX + (i * (heartSize + 5)), uiY - heartSize, heartSize, heartSize);
        }
        uiY -= (heartSize + 5); // Espaçamento

        // Velocímetro e Velocidade
        float velocimetroW = 40f;
        float velocimetroH = 40f;
        game.batch.draw(velocimetroTexture, uiX, uiY - velocimetroH, velocimetroW, velocimetroH);

        float currentSpeed = world.getPlayerSpeedX();
        float displaySpeed = 50 + (currentSpeed / world.getBaseMaxSpeed()) * 100f;
        displaySpeed = Math.max(50, displaySpeed); // Garante mínimo de 50

        String speedText = String.format("%.0f km/h", displaySpeed);
        glyphLayout.setText(game.font, speedText);
        game.font.draw(game.batch, speedText,
            uiX + velocimetroW + 8,
            uiY - velocimetroH / 2 + glyphLayout.height / 2);

        uiY -= (velocimetroH + 10);

        // Invencível
        if (!world.podeLevarDano()) {
            game.font.draw(game.batch, "INVENCIVEL!", uiX, uiY);
        }

        game.batch.end();
    }

    private void drawPauseButton() {
        shapeRenderer.end();

        // --- MUDANÇA (Request 2): Desenhar ícone em vez de texto ---
        game.batch.begin();
        game.batch.draw(pauseButtonTexture,
            pauseButtonRect.x, pauseButtonRect.y,
            pauseButtonRect.width, pauseButtonRect.height);
        game.batch.end();
    }

    private void drawPauseOverlay() {
        // Overlay escuro
        shapeRenderer.begin(ShapeType.Filled);
        pauseOverlayColor.a = 0.7f * pauseFadeAlpha;
        shapeRenderer.setColor(pauseOverlayColor);
        shapeRenderer.rect(0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // Textos de pausa
        game.batch.begin();

        game.font.setColor(1, 1, 1, pauseFadeAlpha);

        String pauseText = "PAUSADO";
        glyphLayout.setText(game.font, pauseText);
        float textX = MainGame.VIRTUAL_WIDTH / 2f - glyphLayout.width / 2f;
        float textY = MainGame.VIRTUAL_HEIGHT / 2f + glyphLayout.height / 2f;
        game.font.draw(game.batch, glyphLayout, textX, textY);

        String infoText = "Pressione ESC para continuar";
        glyphLayout.setText(game.font, infoText);
        float infoX = MainGame.VIRTUAL_WIDTH / 2f - glyphLayout.width / 2f;
        float infoY = textY - 40f;
        game.font.draw(game.batch, glyphLayout, infoX, infoY);

        game.font.setColor(Color.WHITE);
        game.batch.end();
    }

    // --- Outros Métodos ---

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        if (background != null) background.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (heartTexture != null && heartTexture != game.placeholderTexture) heartTexture.dispose();
        if (velocimetroTexture != null && velocimetroTexture != game.placeholderTexture) velocimetroTexture.dispose();

        // --- MUDANÇA (Request 2 e 4): Dispose das novas texturas ---
        if (pauseButtonTexture != null && pauseButtonTexture != game.placeholderTexture) pauseButtonTexture.dispose();
        if (pontosTexture != null && pontosTexture != game.placeholderTexture) pontosTexture.dispose();
    }

    private Texture createPlaceholderTexture(int width, int height, Color color) {
        if (game.placeholderTexture != null) {
            return game.placeholderTexture;
        }

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        game.placeholderTexture = texture;
        return texture;
    }

    // Getters para a GameScreen
    public FitViewport getViewport() { return viewport; }
    public Rectangle getPauseButtonRect() { return pauseButtonRect; }
}
