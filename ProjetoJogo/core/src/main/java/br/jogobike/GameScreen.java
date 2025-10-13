package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {
    private MainGame game;
    private OrthographicCamera camera;
    private Texture background;
    private Texture playerTexture;
    private Texture botTexture;

    private float playerX, playerY;
    private float botX, botY;

    // VARIÁVEIS PARA DINÂMICA DE MOVIMENTO
    private float playerSpeedX = 0;
    private float playerSpeedY = 0;
    private float maxSpeed = 200f;
    private float acceleration = 150f;
    private float friction = 250f;
    private float backwardSpeed = -75f;

    // VARIÁVEIS PARA EFEITO DE VENTO
    private ShapeRenderer shapeRenderer;
    private float[] windLines = new float[40];
    private float windSpeed = 0;
    private float maxWindSpeed = 400f;
    private float baseWindSpeed = 50f;

    // SISTEMA DE FUMAÇA DE PERIGO
    private List<SmokeParticle> smokeParticles;
    private float smokeSpawnTimer = 0f;
    private final float SMOKE_SPAWN_RATE = 0.05f;

    // SISTEMA DE VIDAS E PONTOS
    private int pontos = 100;
    private int vidas = 3;
    private boolean podeLevarDano = true;
    private float tempoInvencivel = 0f;
    private final float TEMPO_INVENCIBILIDADE = 1.5f;

    // Tamanhos dos sprites VISUAIS
    private final float PLAYER_WIDTH = 70f;
    private final float PLAYER_HEIGHT = 75f;
    private final float BOT_WIDTH = 70f;
    private final float BOT_HEIGHT = 75f;

    // Tamanhos das áreas de COLISÃO - AUMENTADAS para colisão mais justa
    private final float PLAYER_COLLISION_WIDTH = 55f;  // Aumentado de 50 para 55
    private final float PLAYER_COLLISION_HEIGHT = 65f; // Aumentado de 60 para 65
    private final float BOT_COLLISION_WIDTH = 55f;     // Aumentado de 50 para 55
    private final float BOT_COLLISION_HEIGHT = 65f;    // Aumentado de 60 para 65

    // Offset para centralizar a área de colisão
    private final float PLAYER_COLLISION_OFFSET_X = (PLAYER_WIDTH - PLAYER_COLLISION_WIDTH) / 2f;
    private final float PLAYER_COLLISION_OFFSET_Y = (PLAYER_HEIGHT - PLAYER_COLLISION_HEIGHT) / 2f;
    private final float BOT_COLLISION_OFFSET_X = (BOT_WIDTH - BOT_COLLISION_WIDTH) / 2f;
    private final float BOT_COLLISION_OFFSET_Y = (BOT_HEIGHT - BOT_COLLISION_HEIGHT) / 2f;

    // LIMITES DA PISTA
    private final float PISTA_ESQUERDA = 200f;
    private final float PISTA_DIREITA = 600f;
    private final float LARGURA_PISTA = PISTA_DIREITA - PISTA_ESQUERDA;

    private Rectangle playerRect;
    private Rectangle botRect;

    // Classe interna para partículas de fumaça CINZA
    private class SmokeParticle {
        float x, y;
        float size;
        float alpha;
        float speed;
        Color color;

        public SmokeParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.size = (float) (Math.random() * 20 + 15); // Tamanho entre 15-35
            this.alpha = (float) (Math.random() * 0.7 + 0.3); // Transparência entre 0.3-1.0
            this.speed = (float) (Math.random() * 40 + 30); // Velocidade entre 30-70
            // Cor CINZA para fumaça realista
            float grayValue = (float) (Math.random() * 0.4 + 0.3); // Cinza médio
            this.color = new Color(grayValue, grayValue, grayValue, alpha);
        }

        public void update(float delta) {
            y += speed * delta;
            alpha -= 0.4f * delta; // Desvanece mais devagar
            size += 15f * delta; // Expande mais
        }

        public boolean isDead() {
            return alpha <= 0;
        }
    }

    public GameScreen(MainGame game, int level) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 800);

        shapeRenderer = new ShapeRenderer();
        smokeParticles = new ArrayList<>();

        // Carregar texturas da pasta assets
        try {
            background = new Texture(Gdx.files.internal("background.png"));
            playerTexture = new Texture(Gdx.files.internal("player_bike.png"));
            botTexture = new Texture(Gdx.files.internal("bot_bike.png"));
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Erro ao carregar texturas: " + e.getMessage());
            background = createPlaceholderTexture(800, 800, Color.GRAY);
            playerTexture = createPlaceholderTexture((int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, Color.BLUE);
            botTexture = createPlaceholderTexture((int)BOT_WIDTH, (int)BOT_HEIGHT, Color.RED);
        }

        initializeWindLines();

        // Posições iniciais
        playerX = 800 / 2f - PLAYER_WIDTH / 2f;
        playerY = 150;

        botX = getRandomPositionInTrack();
        botY = 600;

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
    }

    private float getRandomPositionInTrack() {
        return PISTA_ESQUERDA + (float) (Math.random() * (LARGURA_PISTA - BOT_WIDTH));
    }

    private void initializeWindLines() {
        for (int i = 0; i < windLines.length; i += 2) {
            windLines[i] = (float) (Math.random() * 800);
            windLines[i + 1] = (float) (Math.random() * 800);
        }
    }

    private Texture createPlaceholderTexture(int width, int height, Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // Atualizar sistema de fumaça CINZA
    private void updateSmokeEffect(float delta) {
        smokeSpawnTimer += delta;

        // Spawn de novas partículas - mais partículas para efeito mais denso
        while (smokeSpawnTimer >= SMOKE_SPAWN_RATE) {
            smokeSpawnTimer -= SMOKE_SPAWN_RATE;

            // Spawn ao longo da parte inferior da tela
            float spawnX = PISTA_ESQUERDA + (float) (Math.random() * LARGURA_PISTA);
            smokeParticles.add(new SmokeParticle(spawnX, 0));

            // Adiciona uma partícula extra ocasionalmente para densidade
            if (Math.random() < 0.3f) {
                float extraX = PISTA_ESQUERDA + (float) (Math.random() * LARGURA_PISTA);
                smokeParticles.add(new SmokeParticle(extraX, 0));
            }
        }

        // Atualizar partículas existentes
        for (int i = smokeParticles.size() - 1; i >= 0; i--) {
            SmokeParticle particle = smokeParticles.get(i);
            particle.update(delta);

            if (particle.isDead()) {
                smokeParticles.remove(i);
            }
        }
    }

    // Desenhar efeito de fumaça CINZA
    private void drawSmokeEffect() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeType.Filled);

        for (SmokeParticle particle : smokeParticles) {
            shapeRenderer.setColor(particle.color);
            // Desenha círculos em vez de retângulos para fumaça mais orgânica
            shapeRenderer.circle(particle.x, particle.y, particle.size / 2);
        }

        shapeRenderer.end();
    }

    private void aplicarDano() {
        if (podeLevarDano) {
            vidas--;
            pontos = Math.max(0, pontos - 20);
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerSpeedY -= 100;
            playerY = Math.max(0, playerY - 50);
            Gdx.app.log("GameScreen", "Vidas restantes: " + vidas);
        }
    }

    private void updateWindEffect(float delta) {
        windSpeed = baseWindSpeed + Math.abs(playerSpeedY) * 2.0f;

        for (int i = 0; i < windLines.length; i += 2) {
            windLines[i + 1] -= windSpeed * delta;

            if (windLines[i + 1] < -50) {
                windLines[i + 1] = 850;
                windLines[i] = (float) (Math.random() * 800);
            }
        }
    }

    private void drawWindEffect() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeType.Line);

        float alpha = Math.min(1.0f, windSpeed / maxWindSpeed);
        Color windColor = new Color(1, 1, 1, alpha * 0.7f);
        shapeRenderer.setColor(windColor);

        for (int i = 0; i < windLines.length; i += 2) {
            float x = windLines[i];
            float y = windLines[i + 1];

            float lineLength = 30 + (windSpeed / maxWindSpeed) * 60;
            float angleVariation = (float) (Math.sin(x * 0.01f + System.currentTimeMillis() * 0.001f) * 5f);

            shapeRenderer.line(x, y, x + angleVariation, y + lineLength);
        }

        shapeRenderer.end();
    }

    @Override
    public void render(float delta) {
        if (!podeLevarDano) {
            tempoInvencivel -= delta;
            if (tempoInvencivel <= 0) {
                podeLevarDano = true;
            }
        }

        // Atualizar efeitos visuais
        updateSmokeEffect(delta);
        updateWindEffect(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- LÓGICA DE MOVIMENTO ---
        boolean isAcceleratingForward = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP);
        boolean isAcceleratingBackward = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN);

        if (isAcceleratingForward) {
            playerSpeedY += acceleration * delta;
            playerSpeedY = Math.min(maxSpeed, playerSpeedY);
        } else if (isAcceleratingBackward) {
            playerSpeedY -= acceleration * delta;
            playerSpeedY = Math.max(-maxSpeed, playerSpeedY);
        } else {
            playerSpeedY = backwardSpeed;
        }

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            playerSpeedX -= acceleration * delta;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            playerSpeedX += acceleration * delta;
        }

        if (!Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT) &&
            !Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            if (playerSpeedX > 0) {
                playerSpeedX = Math.max(0, playerSpeedX - friction * delta);
            } else if (playerSpeedX < 0) {
                playerSpeedX = Math.min(0, playerSpeedX + friction * delta);
            }
        }

        playerSpeedX = Math.max(-maxSpeed, Math.min(maxSpeed, playerSpeedX));
        playerX += playerSpeedX * delta;
        playerY += playerSpeedY * delta;

        // --- LIMITES DA PISTA E DANOS ---
        if (playerX < PISTA_ESQUERDA) {
            playerX = PISTA_ESQUERDA;
            playerSpeedX = 0;
        } else if (playerX > PISTA_DIREITA - PLAYER_WIDTH) {
            playerX = PISTA_DIREITA - PLAYER_WIDTH;
            playerSpeedX = 0;
        }

        // DANO NA PARTE DE BAIXO
        if (playerY < 0) {
            aplicarDano();
            playerY = 10;
            playerSpeedY = 50;
        } else if (playerY > 800 - PLAYER_HEIGHT) {
            playerY = 800 - PLAYER_HEIGHT;
            playerSpeedY = 0;
        }

        // Movimento do bot
        botY -= 200 * delta;
        if (botY + BOT_HEIGHT < 0) {
            botY = 800;
            botX = getRandomPositionInTrack();
        }

        if (botX < PISTA_ESQUERDA) {
            botX = PISTA_ESQUERDA;
        } else if (botX > PISTA_DIREITA - BOT_WIDTH) {
            botX = PISTA_DIREITA - BOT_WIDTH;
        }

        // Colisões - AGORA COM ÁREAS MAIORES
        playerRect.setPosition(playerX + PLAYER_COLLISION_OFFSET_X, playerY + PLAYER_COLLISION_OFFSET_Y);
        botRect.setPosition(botX + BOT_COLLISION_OFFSET_X, botY + BOT_COLLISION_OFFSET_Y);

        if (playerRect.overlaps(botRect) && podeLevarDano) {
            aplicarDano();
            botY = Math.min(800, botY + 50);
        }

        // Atualiza câmera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // --- DESENHAR TUDO ---

        // 1. Background
        game.batch.begin();
        game.batch.draw(background, 0, 0, 800, 800);
        game.batch.end();

        // 2. Fumaça cinza (sobre o background, sob os sprites)
        drawSmokeEffect();

        // 3. Efeito de vento
        drawWindEffect();

        // 4. Sprites e UI
        game.batch.begin();

        // Player com efeito de piscar
        if (podeLevarDano || (int)(tempoInvencivel * 10) % 2 == 0) {
            game.batch.draw(playerTexture, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        game.batch.draw(botTexture, botX, botY, BOT_WIDTH, BOT_HEIGHT);

        // UI
        game.font.draw(game.batch, "Pontos: " + pontos, 10, 780);
        game.font.draw(game.batch, "Vidas: " + vidas, 10, 750);

        String speedText = String.format("Velocidade: %.0f", Math.abs(playerSpeedY));
        game.font.draw(game.batch, speedText, 10, 720);

        if (!podeLevarDano) {
            game.font.draw(game.batch, "INVENCIVEL!", 10, 690);
        }

        // Aviso de perigo na parte inferior
        if (playerY < 100) {
            game.font.draw(game.batch, "CUIDADO! FUMAÇA TÓXICA!", 250, 50);
        }

        game.batch.end();

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
        camera.setToOrtho(false, 800, 800);
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
