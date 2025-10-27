package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {
    private MainGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Texture background;
    private Texture playerTexture;
    private Texture botTexture; // tronco

    // SISTEMA DE PARALLAX - MODIFICADO (movimento automático)
    private float backgroundOffsetX = 0f;
    private float parallaxSpeed = 300f; // Velocidade fixa do parallax

    // Constantes para a Viewport
    private static final float VIRTUAL_WIDTH = 800;
    private static final float VIRTUAL_HEIGHT = 800;

    private float playerX, playerY;
    private float botX, botY;
    private float botSpeedOffset = 0f; // variação de velocidade do tronco (spawn)

    // VARIÁVEIS PARA DINÂMICA DE MOVIMENTO
    private float playerSpeedX = 0;
    private float playerSpeedY = 0;
    private float maxSpeed = 200f;          // ALTERADO: limite máximo agora 200
    private float acceleration = 300f;
    private float friction = 200f;
    private float driftSpeed = 150f;

    // SISTEMA DE VIDAS E PONTOS
    private int pontos = 0;                 // ALTERADO: começa em 0
    private int vidas = 3;
    private boolean podeLevarDano = true;
    private float tempoInvencivel = 0f;
    private final float TEMPO_INVENCIBILIDADE = 1.5f;

    // Sistema de dano na avalanche
    private float tempoUltimoDanoAvalanche = 0f;
    private final float INTERVALO_DANO_AVALANCHE = 0.3f;

    // Tamanhos dos sprites VISUAIS
    private final float PLAYER_WIDTH = 70f;
    private final float PLAYER_HEIGHT = 90f;
    private final float BOT_WIDTH = 70f;
    private final float BOT_HEIGHT = 75f;

    // Tamanhos das áreas de COLISÃO
    private final float PLAYER_COLLISION_WIDTH = 40f;
    private final float PLAYER_COLLISION_HEIGHT = 40f;
    private final float BOT_COLLISION_WIDTH = 40f;
    private final float BOT_COLLISION_HEIGHT = 40f;

    // Offset para centralizar a área de colisão
    private final float PLAYER_COLLISION_OFFSET_X = (PLAYER_WIDTH - PLAYER_COLLISION_WIDTH) / 2f;
    private final float PLAYER_COLLISION_OFFSET_Y = (PLAYER_HEIGHT - PLAYER_COLLISION_HEIGHT) / 2f;
    private final float BOT_COLLISION_OFFSET_X = (BOT_WIDTH - BOT_COLLISION_WIDTH) / 2f;
    private final float BOT_COLLISION_OFFSET_Y = (BOT_HEIGHT - BOT_COLLISION_HEIGHT) / 2f;

    // LIMITES DA PISTA
    private final float PISTA_BAIXO = 275f;
    private final float PISTA_CIMA = 500f;

    // AVALANCHE - sistema realista
    private final float LARGURA_AVALANCHE = 120f;
    private final float ALTURA_AVALANCHE = 520f;
    private ShapeRenderer shapeRenderer;
    private Random random;
    private List<SnowEffect> snowEffects;
    private float avalancheTimer = 0f;
    private float avalancheIntensity = 0f;

    // SISTEMA DE VENTO
    private List<WindLine> windLines;
    private float windTimer = 0f;

    // Pause
    private boolean isPaused = false;
    private Rectangle pauseButtonRect;
    private final float PAUSE_BUTTON_W = 80f;
    private final float PAUSE_BUTTON_H = 30f;
    private Color pauseOverlayColor = new Color(0f, 0f, 0f, 0.5f);

    // Colisões
    private Rectangle playerRect;
    private Rectangle botRect;

    // Inner classes
    private class WindLine {
        float x, y;
        float length;
        float speed;
        float thickness;
        float alpha;
        boolean isActive;

        public WindLine() {
            reset();
        }

        public void reset() {
            this.x = -50;
            this.y = (float) (Math.random() * VIRTUAL_HEIGHT);
            this.length = (float) (Math.random() * 60 + 30);
            this.speed = (float) (Math.random() * 100 + 150);
            this.thickness = (float) (Math.random() * 2 + 1);
            this.alpha = (float) (Math.random() * 0.3 + 0.2);
            this.isActive = true;
        }

        public void update(float delta, float playerSpeed) {
            x += speed * delta;
            alpha -= 0.5f * delta;
            if (x > VIRTUAL_WIDTH + 100 || alpha <= 0) {
                reset();
            }
        }
    }

    private class SnowEffect {
        float x, y;
        float size;
        float speed;
        float life;
        float maxLife;
        float rotation;
        float rotationSpeed;
        boolean isSnowball;
        Color color;

        public SnowEffect() {
            this.isSnowball = random.nextBoolean();
            this.x = (float) (Math.random() * LARGURA_AVALANCHE);

            if (isSnowball) {
                this.size = (float) (Math.random() * 12 + 8);
                this.y = (float) (Math.random() * ALTURA_AVALANCHE);
                this.speed = (float) (Math.random() * 80 + 40) + avalancheIntensity * 80;
                this.maxLife = (float) (Math.random() * 4 + 3);
                this.color = new Color(1, 1, 1, 0.9f);
            } else {
                this.size = (float) (Math.random() * 25 + 15);
                this.y = (float) (Math.random() * ALTURA_AVALANCHE);
                this.speed = (float) (Math.random() * 60 + 30) + avalancheIntensity * 60;
                this.maxLife = (float) (Math.random() * 2 + 1.5f);
                this.color = new Color(1, 1, 1, 0.6f);
            }

            this.life = maxLife;
            this.rotation = (float) (Math.random() * 360);
            this.rotationSpeed = (float) (Math.random() * 100 - 50);
        }

        public void update(float delta) {
            x += speed * delta;
            rotation += rotationSpeed * delta;
            life -= delta;

            if (isSnowball) {
                size += 8f * delta;
                color.a = life / maxLife * 0.9f;
            } else {
                size += 25f * delta;
                color.a = (life / maxLife) * 0.6f;
                y += (float) (Math.sin(avalancheTimer * 3 + x * 0.01f) * 20 * delta);
            }
        }

        public boolean isDead() {
            return life <= 0 || x > LARGURA_AVALANCHE + 100 || (isSnowball && size > 50) || (!isSnowball && size > 80);
        }
    }

    public GameScreen(MainGame game, int level) {
        this.game = game;
        this.random = new Random();

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        shapeRenderer = new ShapeRenderer();
        snowEffects = new ArrayList<>();
        windLines = new ArrayList<>();

        // Inicializar linhas de vento
        for (int i = 0; i < 15; i++) {
            windLines.add(new WindLine());
        }

        // Carregar texturas
        try {
            background = new Texture(Gdx.files.internal("background.png"));
            playerTexture = new Texture(Gdx.files.internal("player_bike.png"));
            botTexture = new Texture(Gdx.files.internal("tronco.png")); // arquivo novo
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Erro ao carregar texturas: " + e.getMessage());
            background = createPlaceholderTexture((int)VIRTUAL_WIDTH, (int)VIRTUAL_HEIGHT, Color.GRAY);
            playerTexture = createPlaceholderTexture((int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, Color.BLUE);
            botTexture = createPlaceholderTexture((int)BOT_WIDTH, (int)BOT_HEIGHT, Color.BROWN);
        }

        // Posições iniciais (Jogador no centro)
        playerX = VIRTUAL_WIDTH / 2f - PLAYER_WIDTH / 2f;
        playerY = VIRTUAL_HEIGHT / 2f - PLAYER_HEIGHT / 2f;

        // Bot inicial fora da tela à direita (spawn melhor: aleatório)
        botX = VIRTUAL_WIDTH + MathUtils.random(50, 400);
        botY = getRandomPositionInTrack();
        botSpeedOffset = MathUtils.random(-80f, 120f);

        playerRect = new Rectangle(
            playerX + PLAYER_COLLISION_OFFSET_X,
            playerY + PLAYER_COLLISION_OFFSET_Y,
            PLAYER_COLLISION_WIDTH,
            PLAYER_COLLISION_HEIGHT
        );

        botRect = new Rectangle(
            botX + BOT_COLLISION_OFFSET_X,
            botY + BOT_COLLISION_OFFSET_Y,
            BOT_COLLISION_WIDTH,
            BOT_COLLISION_HEIGHT
        );

        // Pause button (canto superior direito)
        pauseButtonRect = new Rectangle(
            VIRTUAL_WIDTH - PAUSE_BUTTON_W - 10f,
            VIRTUAL_HEIGHT - PAUSE_BUTTON_H - 10f,
            PAUSE_BUTTON_W,
            PAUSE_BUTTON_H
        );
    }

    // Posição vertical aleatória dentro da pista
    private float getRandomPositionInTrack() {
        return PISTA_BAIXO + (float) (random.nextFloat() * (PISTA_CIMA - PISTA_BAIXO - BOT_HEIGHT));
    }

    private Texture createPlaceholderTexture(int width, int height, Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // Atualizar efeito da avalanche realista
    private void updateAvalanche(float delta) {
        avalancheTimer += delta;
        avalancheIntensity = Math.min(1.0f, avalancheTimer * 0.1f);

        if (random.nextFloat() < 0.6f + avalancheIntensity * 0.4f) {
            snowEffects.add(new SnowEffect());
        }

        for (int i = snowEffects.size() - 1; i >= 0; i--) {
            SnowEffect effect = snowEffects.get(i);
            effect.update(delta);
            if (effect.isDead()) {
                snowEffects.remove(i);
            }
        }
    }

    // Atualizar efeito de vento
    private void updateWindEffect(float delta) {
        windTimer += delta;
        for (WindLine line : windLines) {
            line.update(delta, playerSpeedX);
        }
    }

    // Atualizar efeito de parallax
    private void updateParallax(float delta) {
        backgroundOffsetX -= parallaxSpeed * delta;
        if (backgroundOffsetX < -VIRTUAL_WIDTH) {
            backgroundOffsetX += VIRTUAL_WIDTH;
        }
    }

    // Desenho do vento
    private void drawWindEffect() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);
        for (WindLine line : windLines) {
            if (line.isActive) {
                shapeRenderer.setColor(1, 1, 1, line.alpha);
                float angle = (float) (Math.sin(windTimer * 1.5f + line.y * 0.02f) * 0.05f);
                float endX = line.x + line.length;
                float endY = line.y + angle * line.length;
                shapeRenderer.rectLine(line.x, line.y, endX, endY, line.thickness);

                shapeRenderer.setColor(1, 1, 1, line.alpha * 0.3f);
                float startLength = line.length * 0.2f;
                float startX = line.x - startLength;
                float startY = line.y - angle * startLength;
                shapeRenderer.rectLine(line.x, line.y, startX, startY, line.thickness * 0.5f);
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Filled);
        for (WindLine line : windLines) {
            if (line.isActive && line.alpha > 0.1f) {
                shapeRenderer.setColor(1, 1, 1, line.alpha * 0.6f);
                for (int i = 0; i < 2; i++) {
                    float progress = (float) (Math.random() * 0.8 + 0.2);
                    float particleX = line.x + line.length * progress;
                    float particleY = line.y + (float) (Math.sin(windTimer * 2 + particleX * 0.03f) * 4f);
                    float particleSize = (float) (Math.random() * 1.5 + 0.5);
                    shapeRenderer.circle(particleX, particleY, particleSize);
                }
            }
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // Desenhar background com parallax
    private void drawBackground() {
        game.batch.begin();

        float x1 = backgroundOffsetX;
        float x2 = backgroundOffsetX + VIRTUAL_WIDTH;
        float x3 = backgroundOffsetX - VIRTUAL_WIDTH;

        game.batch.draw(background, x1, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        game.batch.draw(background, x2, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        game.batch.draw(background, x3, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        game.batch.end();
    }

    // Desenhar avalanche com efeitos
    private void drawAvalanche() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 1, 0.95f);
        shapeRenderer.rect(0, 0, LARGURA_AVALANCHE, ALTURA_AVALANCHE);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Filled);
        for (SnowEffect effect : snowEffects) {
            shapeRenderer.setColor(effect.color);
            if (effect.isSnowball) {
                shapeRenderer.circle(effect.x, effect.y, effect.size);
                shapeRenderer.setColor(1, 1, 1, effect.color.a * 0.3f);
                shapeRenderer.circle(effect.x - effect.size * 0.2f, effect.y + effect.size * 0.2f, effect.size * 0.6f);
                shapeRenderer.setColor(effect.color);
            } else {
                shapeRenderer.circle(effect.x, effect.y, effect.size);
                shapeRenderer.circle(effect.x + effect.size * 0.3f, effect.y - effect.size * 0.2f, effect.size * 0.7f);
                shapeRenderer.circle(effect.x - effect.size * 0.2f, effect.y + effect.size * 0.3f, effect.size * 0.5f);
                shapeRenderer.setColor(1, 1, 1, effect.color.a * 0.8f);
                shapeRenderer.circle(effect.x, effect.y, effect.size * 0.4f);
                shapeRenderer.setColor(effect.color);
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(1, 1, 1, 1);
        float time = avalancheTimer * 2f;
        for (float y = 0; y < ALTURA_AVALANCHE; y += 10) {
            float wave = (float) Math.sin(y * 0.1f + time) * 8f;
            float wave2 = (float) Math.cos(y * 0.05f + time * 1.5f) * 5f;
            float irregularity = (float) Math.sin(y * 0.02f) * 3f;
            float frontX = LARGURA_AVALANCHE + wave + wave2 + irregularity;
            shapeRenderer.rect(LARGURA_AVALANCHE, y, frontX - LARGURA_AVALANCHE, 10);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void aplicarDano() {
        if (podeLevarDano) {
            vidas--;
            pontos -= 10; // -10 por tronco
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = Math.min(VIRTUAL_WIDTH - PLAYER_WIDTH - 100, playerX + 80);
            playerSpeedX = 100;
            Gdx.app.log("GameScreen", "Vidas restantes: " + vidas + " | Pontos: " + pontos);
        }
    }

    private void aplicarDanoAvalanche() {
        if (podeLevarDano) {
            vidas--;
            pontos -= 20; 
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = LARGURA_AVALANCHE + 30;
            playerSpeedX = 200;
            Gdx.app.log("GameScreen", "Avalanche! Vidas restantes: " + vidas + " | Pontos: " + pontos);
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (game.backgroundMusic != null) {
            if (isPaused) {
                if (game.backgroundMusic.isPlaying()) game.backgroundMusic.pause();
            } else {
                if (!game.backgroundMusic.isPlaying()) game.backgroundMusic.play();
            }
        }
        Gdx.app.log("GameScreen", "Pausado: " + isPaused);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touch);
            if (pauseButtonRect.contains(touch.x, touch.y)) {
                togglePause();
            }
        }
        
        if (!isPaused) {
            if (!podeLevarDano) {
                tempoInvencivel -= delta;
                if (tempoInvencivel <= 0) {
                    podeLevarDano = true;
                }
            }

            tempoUltimoDanoAvalanche += delta;
            updateAvalanche(delta);
            updateWindEffect(delta);
            updateParallax(delta);

            // --- LÓGICA DE MOVIMENTO ---
            boolean isMovingUp = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP);
            boolean isMovingDown = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN);
            boolean isAccelerating = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT);
            boolean isBraking = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT);

            // MOVIMENTO HORIZONTAL
            if (isAccelerating) {
                playerSpeedX += acceleration * delta;
                playerSpeedX = Math.min(maxSpeed, playerSpeedX);
            } else if (isBraking) {
                playerSpeedX -= acceleration * 1.5f * delta;
                playerSpeedX = Math.max(-maxSpeed * 0.5f, playerSpeedX);
            } else {
                playerSpeedX -= driftSpeed * delta;
            }

            if (playerSpeedX > 0) {
                playerSpeedX = Math.max(0, playerSpeedX - friction * delta * 0.5f);
            } else if (playerSpeedX < 0) {
                playerSpeedX = Math.min(0, playerSpeedX + friction * delta * 0.5f);
            }

            // MOVIMENTO VERTICAL
            if (isMovingUp) {
                playerSpeedY = maxSpeed * 0.7f;
            } else if (isMovingDown) {
                playerSpeedY = -maxSpeed * 0.7f;
            } else {
                if (playerSpeedY > 0) {
                    playerSpeedY = Math.max(0, playerSpeedY - friction * delta);
                } else if (playerSpeedY < 0) {
                    playerSpeedY = Math.min(0, playerSpeedY + friction * delta);
                }
            }

            // APLICAR MOVIMENTO
            playerX += playerSpeedX * delta;
            playerY += playerSpeedY * delta;

            // LIMITES DA TELA
            playerX = MathUtils.clamp(playerX, LARGURA_AVALANCHE, VIRTUAL_WIDTH - PLAYER_WIDTH);
            playerY = MathUtils.clamp(playerY, PISTA_BAIXO, PISTA_CIMA - PLAYER_HEIGHT);

            // ÁREA DE DANO DA AVALANCHE
            if (playerX <= LARGURA_AVALANCHE + 5 && podeLevarDano && tempoUltimoDanoAvalanche >= INTERVALO_DANO_AVALANCHE) {
                aplicarDanoAvalanche();
                tempoUltimoDanoAvalanche = 0f;
            }

            // Movimento do bot (tronco)
            float botSpeed = 200f + botSpeedOffset + (Math.abs(playerSpeedX) * 0.5f);
            botX -= botSpeed * delta;

            if (botX + BOT_WIDTH < LARGURA_AVALANCHE) {
                botX = VIRTUAL_WIDTH + MathUtils.random(50, 600);
                botY = getRandomPositionInTrack();
                botSpeedOffset = MathUtils.random(-80f, 120f);
            }

            // Colisões com bots
            playerRect.setPosition(playerX + PLAYER_COLLISION_OFFSET_X, playerY + PLAYER_COLLISION_OFFSET_Y);
            botRect.setPosition(botX + BOT_COLLISION_OFFSET_X, botY + BOT_COLLISION_OFFSET_Y);

            if (playerRect.overlaps(botRect) && podeLevarDano) {
                aplicarDano();
                botX = VIRTUAL_WIDTH + MathUtils.random(200, 600);
                botY = getRandomPositionInTrack();
                botSpeedOffset = MathUtils.random(-80f, 120f);
            }

            camera.update();
        } // fim do bloco !isPaused

        // --- RENDERIZAÇÃO ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);

        drawBackground();

        drawAvalanche();
        drawWindEffect();

        game.batch.begin();

        if (podeLevarDano || (int)(tempoInvencivel * 10) % 2 == 0) {
            game.batch.draw(playerTexture, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        // Desenha o tronco
        game.batch.draw(botTexture, botX, botY, BOT_WIDTH, BOT_HEIGHT);

        //
        game.font.draw(game.batch, "Pontos: " + pontos, LARGURA_AVALANCHE + 10, VIRTUAL_HEIGHT - 20);
        game.font.draw(game.batch, "Vidas: " + vidas, LARGURA_AVALANCHE + 10, VIRTUAL_HEIGHT - 50);

        float shownSpeed = MathUtils.clamp(playerSpeedX, 0, maxSpeed);
        String speedText = String.format("Velocidade: %.0f", shownSpeed);
        game.font.draw(game.batch, speedText, LARGURA_AVALANCHE + 10, VIRTUAL_HEIGHT - 80);

        if (!podeLevarDano) {
            game.font.draw(game.batch, "INVENCIVEL!", LARGURA_AVALANCHE + 10, VIRTUAL_HEIGHT - 110);
        }

        game.batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(pauseButtonRect.x, pauseButtonRect.y, pauseButtonRect.width, pauseButtonRect.height);
        shapeRenderer.end();

        // Texto no botão
        game.batch.begin();
        game.font.draw(game.batch, "PAUSE", pauseButtonRect.x + 12, pauseButtonRect.y + pauseButtonRect.height - 8);
        game.batch.end();

        if (isPaused) {
            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(pauseOverlayColor);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.end();

            //"PAUSADO"
            GlyphLayout layout = new GlyphLayout();
            String pauseText = "PAUSADO";
            layout.setText(game.font, pauseText);
            float textX = VIRTUAL_WIDTH / 2f - layout.width / 2f;
            float textY = VIRTUAL_HEIGHT / 2f + layout.height / 2f;
            game.batch.begin();
            game.font.draw(game.batch, layout, textX, textY);

            GlyphLayout infoLayout = new GlyphLayout();
            String infoText = "Pressione ESC para continuar";
            infoLayout.setText(game.font, infoText);
            float infoX = VIRTUAL_WIDTH / 2f - infoLayout.width / 2f;
            float infoY = textY - 40f;
            game.font.draw(game.batch, infoLayout, infoX, infoY);
            game.batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Game Over
        if (vidas <= 0) {
            Gdx.app.log("GameScreen", "Game Over! Pontos finais: " + pontos);
            game.setScreen(new MenuScreen(game));
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (background != null) background.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (botTexture != null) botTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        if (game.backgroundMusic != null && !game.backgroundMusic.isPlaying()) {
            game.backgroundMusic.play();
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
