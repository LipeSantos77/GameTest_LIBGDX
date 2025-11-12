package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameWorld {
    private MainGame game;
    private Random random;

    // ESTADO DA FASE
    private int currentLevel; // Guarda o nível atual (1 ou 2)
    public static final int PONTUACAO_LIMITE_FASE1 = 100; // Meta de pontos
    private boolean isLevelComplete = false; // Sinaliza que a transição deve ocorrer
    // FIM: ESTADO DA FASE

    // Estado do Jogo
    private int pontos;
    private int vidas = 3;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // Estado do Jogador
    private float playerX, playerY;
    private float playerSpeedX = 0;
    private float playerSpeedY = 0;
    private Rectangle playerRect;

    // Estado dos Obstáculos
    private BotLog bot;

    // Invencibilidade
    private boolean podeLevarDano = true;
    private float tempoInvencivel = 0f;
    private final float TEMPO_INVENCIBILIDADE = 1.5f;

    // Dano Avalanche
    private float tempoUltimoDanoAvalanche = 0f;
    private final float INTERVALO_DANO_AVALANCHE = 0.3f;

    // Constantes do Jogador
    private final float PLAYER_WIDTH = 70f;
    private final float PLAYER_HEIGHT = 90f;
    private final float PLAYER_COLLISION_WIDTH = 40f;
    private final float PLAYER_COLLISION_HEIGHT = 40f;
    private final float PLAYER_COLLISION_OFFSET_X = (PLAYER_WIDTH - PLAYER_COLLISION_WIDTH) / 2f;
    private final float PLAYER_COLLISION_OFFSET_Y = (PLAYER_HEIGHT - PLAYER_COLLISION_HEIGHT) / 2f;

    // Constantes de Física Base
    private final float BASE_MAX_SPEED = 400f;
    private final float BASE_ACCELERATION = 280f;
    private final float BASE_FRICTION = 180f;
    private final float BASE_DRIFT_SPEED = 190f;

    // Variáveis de Física (que mudam com a dificuldade)
    private float maxSpeed;
    private float acceleration;
    private float friction;
    private float driftSpeed;
    private float currentDifficulty;

    // Constantes da Pista e Avalanche
    // Fase 1
    private final float PISTA_BAIXO_F1 = 275f;
    private final float PISTA_CIMA_F1 = 500f;
    private final float ALTURA_AVALANCHE_F1 = 520f; // Altura visual da avalanche na Fase 1

    // Fase 2 (Gelo Estreita)
    private final float PISTA_BAIXO_F2 = 183f;
    private final float PISTA_CIMA_F2 = 346f;
    private final float ALTURA_AVALANCHE_F2 = 346f; // Altura visual da avalanche na Fase 2

    public static final float LARGURA_AVALANCHE = 120f;

    public static final float ALTURA_AVALANCHE = 720f; // Altura Total da Tela

    // Efeitos Visuais
    private List<SnowEffect> snowEffects;
    private List<WindLine> windLines;
    private List<PlayerDashLine> dashLines;
    private float avalancheTimer = 0f;
    private float avalancheIntensity = 0f;
    private float windTimer = 0f;
    private float backgroundOffsetX = 0f;

    //  Lista de Pedras
    private List<Rock> rocks;
    private float rockSpawnTimer = 0f;
    private float nextRockSpawnInterval = 1.5f; // segundos
    private final float ROCK_MIN_SPAWN = 1.0f;
    private final float ROCK_MAX_SPAWN = 2.5f;
    private final float ROCK_BASE_SPEED = 220f; // velocidade base da pedra
    private final float ROCK_WIDTH = 48f;
    private final float ROCK_HEIGHT = 48f;

    // CORREÇÃO APLICADA: Constantes de Colisão da Pedra
    private final float ROCK_COLLISION_WIDTH = 32f;
    private final float ROCK_COLLISION_HEIGHT = 32f;
    private final float ROCK_COLLISION_OFFSET_X = (ROCK_WIDTH - ROCK_COLLISION_WIDTH) / 2f;
    private final float ROCK_COLLISION_OFFSET_Y = (ROCK_HEIGHT - ROCK_COLLISION_HEIGHT) / 2f;
    // FIM CORREÇÃO


    public GameWorld(MainGame game, int level) {
        this.game = game;
        this.currentLevel = level;
        this.random = new Random();
        this.pontos = 0;

        // Posições iniciais
        playerX = MainGame.VIRTUAL_WIDTH / 2f - PLAYER_WIDTH / 2f;
        float initialTrackY = (getTrackBottom() + getTrackTop()) / 2f;
        playerY = initialTrackY - PLAYER_HEIGHT / 2f;

        // Inicializar PlayerRect
        playerRect = new Rectangle(
            playerX + PLAYER_COLLISION_OFFSET_X,
            playerY + PLAYER_COLLISION_OFFSET_Y,
            PLAYER_COLLISION_WIDTH,
            PLAYER_COLLISION_HEIGHT
        );

        // Inicializar Obstáculos
        float initialY = getTrackBottom() + 10f;
        try {
            Texture troncoTexture = new Texture(Gdx.files.internal("tronco.png"));
            bot = new BotLog(troncoTexture, MainGame.VIRTUAL_WIDTH + 50, initialY);
        } catch (Exception e) {
            Gdx.app.error("GameWorld", "Erro ao carregar tronco.png, usando fallback: " + e.getMessage());
            bot = new BotLog("tronco.png", MainGame.VIRTUAL_WIDTH + 50, initialY);
        }

        // Efeitos
        snowEffects = new ArrayList<>();
        windLines = new ArrayList<>();
        dashLines = new ArrayList<>();
        rocks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            windLines.add(new WindLine());
        }

        // Inicializa variáveis de física
        updateDifficulty();

        // inicializa intervalo de spawn
        nextRockSpawnInterval = ROCK_MIN_SPAWN + random.nextFloat() * (ROCK_MAX_SPAWN - ROCK_MIN_SPAWN);

        Gdx.app.log("GameWorld", "Fase " + currentLevel + " iniciada. Pista Y: " + getTrackBottom() + " a " + getTrackTop());
    }

    public void update(float delta) {
        if (isGameOver || isLevelComplete) return;

        updateDifficulty();

        if (!podeLevarDano) {
            tempoInvencivel -= delta;
            if (tempoInvencivel <= 0) {
                podeLevarDano = true;
            }
        }
        tempoUltimoDanoAvalanche += delta;

        // Limites dinâmicos da pista e avalanche
        float trackBottom = getTrackBottom();
        float trackTop = getTrackTop();

        // Atualiza lógica dos efeitos visuais
        updateAvalanche(delta);
        updateWindEffect(delta);
        updateParallax(delta);
        updateDashLines(delta);

        // Atualiza pedras (APENAS na fase 2)
        if (currentLevel == 2) {
            updateRocks(delta, trackBottom, trackTop);
        }

        // Física e Movimento
        playerX += playerSpeedX * delta;
        playerY += playerSpeedY * delta;

        // Limites da tela (Clamp do player nas bordas da pista DINÂMICA)
        playerX = MathUtils.clamp(playerX, LARGURA_AVALANCHE, MainGame.VIRTUAL_WIDTH - PLAYER_WIDTH);
        playerY = MathUtils.clamp(playerY, trackBottom, trackTop - PLAYER_HEIGHT);

        // Atualizar retângulo de colisão
        updatePlayerRect();

        // Checar dano da avalanche
        if (playerX <= LARGURA_AVALANCHE + 5 && podeLevarDano && tempoUltimoDanoAvalanche >= INTERVALO_DANO_AVALANCHE) {
            aplicarDanoAvalanche();
            tempoUltimoDanoAvalanche = 0f;
        }

        // LÓGICA DO TRONCO
        if (bot != null) {
            boolean foiDesviado = bot.update(delta, playerSpeedX, LARGURA_AVALANCHE, MainGame.VIRTUAL_WIDTH, currentDifficulty);
            if (foiDesviado) {
                pontos += 10;
                bot.respawn(MainGame.VIRTUAL_WIDTH, 50, 600, trackBottom, trackTop);

                // GATILHO DA FASE 2: Checagem de nível.
                if (currentLevel == 1 && pontos >= PONTUACAO_LIMITE_FASE1 && !isLevelComplete) {
                    isLevelComplete = true; // SINALIZA A TRANSIÇÃO
                    Gdx.app.log("GameWorld", "NÍVEL 1 COMPLETO! Sinalizando MainGame para Fase 2.");
                }
            }
        }
        // Colisão com tronco
        if (bot != null && playerRect.overlaps(bot.rect) && podeLevarDano) {
            aplicarDano();
            bot.respawn(MainGame.VIRTUAL_WIDTH, 50, 600, trackBottom, trackTop);
        }

        // Checar Game Over
        if (vidas <= 0) {
            isGameOver = true;
        }
    }

    // Atualiza pedras, spawn e colisões
    private void updateRocks(float delta, float trackBottom, float trackTop) {
        // spawn
        rockSpawnTimer += delta;
        if (rockSpawnTimer >= nextRockSpawnInterval) {
            spawnRock(trackBottom, trackTop);
            rockSpawnTimer = 0f;
            nextRockSpawnInterval = ROCK_MIN_SPAWN + random.nextFloat() * (ROCK_MAX_SPAWN - ROCK_MIN_SPAWN);
        }

        // atualizar posição e checar colisões
        float speedMultiplier = currentDifficulty;
        for (int i = rocks.size() - 1; i >= 0; i--) {
            Rock r = rocks.get(i);
            r.x -= r.speed * delta * speedMultiplier;

            // CORREÇÃO APLICADA: Atualiza o retângulo de colisão usando o offset
            r.rect.setPosition(
                r.x + ROCK_COLLISION_OFFSET_X,
                r.y + ROCK_COLLISION_OFFSET_Y
            );

            // Fora da tela? remover
            if (r.x + r.width < -50) {
                rocks.remove(i);
                continue;
            }

            // Colisão com o jogador
            if (playerRect.overlaps(r.rect) && podeLevarDano) {
                aplicarDano();
                rocks.remove(i);
            }
        }
    }

    private void spawnRock(float trackBottom, float trackTop) {
        // Spawn só na faixa de pista (trackBottom .. trackTop-ROCK_HEIGHT)
        float y = trackBottom + 5f + random.nextFloat() * Math.max(0f, (trackTop - trackBottom - ROCK_HEIGHT - 10f));
        float x = MainGame.VIRTUAL_WIDTH + 30f + random.nextFloat() * 80f; // surge da direita
        float speed = ROCK_BASE_SPEED + random.nextFloat() * 80f; // varia um pouco
        Rock r = new Rock(x, y, ROCK_WIDTH, ROCK_HEIGHT, speed);
        rocks.add(r);
    }

    private void updateDifficulty() {
        float scalar = 1.0f + (pontos / 100f) * 0.05f;
        currentDifficulty = Math.min(2.5f, scalar);

        // A dificuldade aumenta a velocidade dos obstáculos e o jogador fica mais rápido.
        maxSpeed = BASE_MAX_SPEED * currentDifficulty;
        acceleration = BASE_ACCELERATION * currentDifficulty;
        friction = BASE_FRICTION * currentDifficulty;
        driftSpeed = BASE_DRIFT_SPEED * currentDifficulty;
    }


    public void handlePlayerInput(boolean isMovingUp, boolean isMovingDown, boolean isAccelerating, boolean isBraking) {
        // MOVIMENTO HORIZONTAL
        if (isAccelerating) {
            playerSpeedX += acceleration * Gdx.graphics.getDeltaTime();
            playerSpeedX = Math.min(maxSpeed, playerSpeedX);

            if (playerSpeedX > maxSpeed * 0.5f && random.nextFloat() > 0.3f) {
                dashLines.add(new PlayerDashLine(
                    playerX - 10f,
                    playerY + PLAYER_HEIGHT / 2f
                ));
            }

        } else if (isBraking) {
            playerSpeedX -= acceleration * 1.5f * Gdx.graphics.getDeltaTime();
            playerSpeedX = Math.max(-maxSpeed * 0.5f, playerSpeedX);
        } else {
            playerSpeedX -= driftSpeed * Gdx.graphics.getDeltaTime();
        }

        if (playerSpeedX > 0) {
            playerSpeedX = Math.max(0, playerSpeedX - friction * Gdx.graphics.getDeltaTime() * 0.5f);
        } else if (playerSpeedX < 0) {
            playerSpeedX = Math.min(0, playerSpeedX + friction * Gdx.graphics.getDeltaTime() * 0.5f);
        }

        // MOVIMENTO VERTICAL
        if (isMovingUp) {
            playerSpeedY = maxSpeed * 0.7f;
        } else if (isMovingDown) {
            playerSpeedY = -maxSpeed * 0.7f;
        } else {
            if (playerSpeedY > 0) {
                playerSpeedY = Math.max(0, playerSpeedY - friction * Gdx.graphics.getDeltaTime());
            } else if (playerSpeedY < 0) {
                playerSpeedY = Math.min(0, playerSpeedY + friction * Gdx.graphics.getDeltaTime());
            }
        }
    }

    private void updatePlayerRect() {
        playerRect.setPosition(
            playerX + PLAYER_COLLISION_OFFSET_X,
            playerY + PLAYER_COLLISION_OFFSET_Y
        );
    }

    // --- Lógica de Dano ---

    private void aplicarDano() {
        if (podeLevarDano) {
            vidas--;
            pontos = Math.max(0, pontos - 20);
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = Math.min(MainGame.VIRTUAL_WIDTH - PLAYER_WIDTH - 100, playerX + 80);
            playerSpeedX = 100;
            Gdx.app.log("GameWorld", "Dano! Vidas restantes: " + vidas);
        }
    }

    private void aplicarDanoAvalanche() {
        if (podeLevarDano) {
            vidas--;
            pontos = Math.max(0, pontos - 15);
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = LARGURA_AVALANCHE + 30;
            playerSpeedX = 200;
            Gdx.app.log("GameWorld", "Avalanche! Vidas restantes: " + vidas);
        }
    }

    // --- Lógica de Efeitos ---
    private void updateAvalanche(float delta) {
        avalancheTimer += delta;
        avalancheIntensity = Math.min(1.0f, avalancheTimer * 0.1f);

        // Usa a altura dinâmica para spawnar neve apenas na área de renderização da avalanche
        float currentAvalancheHeight = getAvalancheHeight();

        if (random.nextFloat() < 0.9f + avalancheIntensity * 0.1f) {
            snowEffects.add(new SnowEffect(currentAvalancheHeight));
        }
        for (int i = snowEffects.size() - 1; i >= 0; i--) {
            SnowEffect effect = snowEffects.get(i);
            effect.update(delta, currentAvalancheHeight);
            if (effect.isDead()) {
                snowEffects.remove(i);
            }
        }
    }
    private void updateWindEffect(float delta) {
        windTimer += delta;
        for (WindLine line : windLines) {
            line.update(delta, playerSpeedX);
        }
    }
    private void updateDashLines(float delta) {
        for (int i = dashLines.size() - 1; i >= 0; i--) {
            PlayerDashLine line = dashLines.get(i);
            line.update(delta);
            if (line.isDead()) {
                dashLines.remove(i);
            }
        }
    }
    private void updateParallax(float delta) {
        float parallaxSpeedMultiplier = 0.5f + (playerSpeedX / (maxSpeed * 2f));
        float currentParallaxSpeed = 300f * parallaxSpeedMultiplier;

        backgroundOffsetX -= currentParallaxSpeed * delta;
        if (backgroundOffsetX < -MainGame.VIRTUAL_WIDTH) {
            backgroundOffsetX += MainGame.VIRTUAL_WIDTH;
        }
    }

    // --- Controle de Pausa ---
    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            game.pauseBackgroundMusic();
        } else {
            game.playBackgroundMusic();
        }
        Gdx.app.log("GameWorld", "Pausado: " + isPaused);
    }

    public void dispose() {
        if (bot != null) bot.dispose();
        // Não há textura de pedra aqui (renderer desenha a textura), então nada para disposar aqui.
    }

    // --- Getters Dinâmicos (CRUCIAIS PARA AS CORES DA FASE 2) ---
    public float getTrackBottom() {
        return currentLevel == 2 ? PISTA_BAIXO_F2 : PISTA_BAIXO_F1;
    }

    public float getTrackTop() {
        return currentLevel == 2 ? PISTA_CIMA_F2 : PISTA_CIMA_F1;
    }


    public float getAvalancheHeight() {
        return currentLevel == 2 ? ALTURA_AVALANCHE_F2 : ALTURA_AVALANCHE_F1;
    }

    // --- Getters (para o GameRenderer e MainGame) ---
    public int getPontos() { return pontos; }
    public int getVidas() { return vidas; }
    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }
    public float getPlayerSpeedX() { return playerSpeedX; }
    public BotLog getBot() { return bot; }
    public boolean isPaused() { return isPaused; }
    public boolean isGameOver() { return isGameOver; }
    public boolean podeLevarDano() { return podeLevarDano; }
    public float getTempoInvencivel() { return tempoInvencivel; }
    public float getBackgroundOffsetX() { return backgroundOffsetX; }
    public List<SnowEffect> getSnowEffects() { return snowEffects; }
    public List<WindLine> getWindLines() { return windLines; }
    public List<PlayerDashLine> getDashLines() { return dashLines; }
    public float getAvalancheTimer() { return avalancheTimer; }
    public float getWindTimer() { return windTimer; }
    public float getPlayerWidth() { return PLAYER_WIDTH; }
    public float getPlayerHeight() { return PLAYER_HEIGHT; }
    public void setPaused(boolean paused) {
        isPaused = paused;
        if (isPaused) game.pauseBackgroundMusic();
    }
    public float getDifficulty() { return currentDifficulty; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getBaseMaxSpeed() { return BASE_MAX_SPEED; }
    public boolean isLevelComplete() { return isLevelComplete; }
    public int getCurrentLevel() { return currentLevel; }

    public void setCurrentLevel(int level) {
        this.currentLevel = level;
        this.isLevelComplete = false;
        // limpa pedras na troca de fase para evitar restos
        this.rocks.clear();
        Gdx.app.log("GameWorld", "Transição para Fase " + level + " concluída.");
    }
    public void resetLevelComplete() { this.isLevelComplete = false; }

    // --- Getter para pedras (o renderer vai desenhar usando a textura) ---
    public List<Rock> getRocks() { return rocks; }

    // --- Inner classes para Efeitos e Obstáculos ---
    class PlayerDashLine {
        float x, y, length, alpha;
        float speed = 1000f;
        public PlayerDashLine(float startX, float startY) {
            this.x = startX;
            this.y = startY + MathUtils.random(-PLAYER_HEIGHT / 4f, PLAYER_HEIGHT / 4f);
            this.length = MathUtils.random(20f, 40f);
            this.alpha = 0.7f;
        }
        public void update(float delta) {
            x -= speed * delta;
            alpha -= 3.0f * delta;
        }
        public boolean isDead() { return alpha <= 0; }
    }
    class WindLine {
        float x, y;
        float length;
        float speed;
        float thickness;
        float alpha;
        boolean isActive;
        public WindLine() { reset(); }
        public void reset() {
            this.x = -50;
            this.y = (float) (Math.random() * MainGame.VIRTUAL_HEIGHT);
            this.length = (float) (Math.random() * 100 + 50);
            this.speed = (float) (Math.random() * 200 + 250);
            this.thickness = (float) (Math.random() * 2 + 1);
            this.alpha = (float) (Math.random() * 0.2 + 0.1);
            this.isActive = true;
        }
        public void update(float delta, float playerSpeed) {
            float scaledSpeed = speed * (1 + (currentDifficulty - 1) * 0.8f);
            x += scaledSpeed * delta;
            alpha -= 0.5f * delta;
            if (x > MainGame.VIRTUAL_WIDTH + 100 || alpha <= 0) {
                reset();
            }
        }
    }
    class SnowEffect {
        float x, y;
        float size;
        float speed;
        float life;
        float maxLife;
        float rotation;
        float rotationSpeed;
        boolean isSnowball;
        Color color;

        public SnowEffect(float avalancheHeight) {
            this.isSnowball = random.nextBoolean();
            this.x = (float) (Math.random() * (LARGURA_AVALANCHE + 20f));

            if (isSnowball) {
                this.size = (float) (Math.random() * 12 + 8);
                this.y = (float) (Math.random() * avalancheHeight);
                this.speed = (float) (Math.random() * 80 + 40) + avalancheIntensity * 80;
                this.maxLife = (float) (Math.random() * 4 + 3);
                this.color = new Color(1, 1, 1, 0.9f);
            } else {
                this.size = (float) (Math.random() * 40 + 20);
                this.y = (float) (Math.random() * avalancheHeight);
                this.speed = (float) (Math.random() * 60 + 30) + avalancheIntensity * 60;
                this.maxLife = (float) (Math.random() * 1.5f + 1.0f);
                this.color = new Color(1, 1, 1, 0.6f);
            }
            this.life = maxLife;
            this.rotation = (float) (Math.random() * 360);
            this.rotationSpeed = (float) (Math.random() * 100 - 50);
        }

        public void update(float delta, float avalancheHeight) {
            float scaledSpeed = speed * currentDifficulty;
            x += scaledSpeed * delta;
            rotation += rotationSpeed * delta;
            life -= delta;
            if (isSnowball) {
                size += 8f * delta;
                color.a = life / maxLife * 0.9f;
            } else {
                size += 40f * delta;
                color.a = (life / maxLife) * 0.4f;
                y += (float) (Math.sin(avalancheTimer * 3 + x * 0.01f) * 20 * delta);
            }
        }
        public boolean isDead() {
            return life <= 0 || x > LARGURA_AVALANCHE + 100 || (isSnowball && size > 50) || (!isSnowball && size > 80);
        }
    }

    // Classe lógica de pedra
    public class Rock {
        public float x, y;
        public float width, height;
        public float speed;
        public Rectangle rect;

        public Rock(float x, float y, float width, float height, float speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
            // CORREÇÃO APLICADA: Cria o retângulo de colisão com o tamanho e offset corretos
            this.rect = new Rectangle(
                x + ROCK_COLLISION_OFFSET_X,
                y + ROCK_COLLISION_OFFSET_Y,
                ROCK_COLLISION_WIDTH,
                ROCK_COLLISION_HEIGHT
            );
        }
    }
}
