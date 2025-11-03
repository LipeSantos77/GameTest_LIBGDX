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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MenuScreen implements Screen {
    private final MainGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private BitmapFont instructionFont;
    private final GlyphLayout layout;

    private ShapeRenderer shapeRenderer;

    // Texturas e animações
    private Texture background;
    private Texture bikeTexture;
    private Animation<TextureRegion> bikeAnimation;
    private float stateTime;

    // Efeitos visuais
    private float pulseTime = 0;
    private float bikeY = 0;
    private boolean bikeMovingUp = true;
    // --- MUDANÇA (Request 3): Rotação da bike ---
    private float bikeAngle = 0;

    // Efeito Parallax
    private float backgroundOffsetX = 0f;
    private float parallaxSpeed = 60f;

    // Sistema de vento
    private List<MenuWindLine> windLines;
    private Random random;

    // Inner class para o efeito de vento
    private class MenuWindLine {
        float x, y, speed, length;
        float alpha;

        public MenuWindLine(float y) {
            this.y = y;
            // --- MUDANÇA (Request 3): Mais rápido e dinâmico ---
            this.speed = MathUtils.random(200f, 500f);
            this.length = MathUtils.random(70f, 200f);
            this.x = MathUtils.random(-length, MainGame.VIRTUAL_WIDTH);
            this.alpha = MathUtils.random(0.1f, 0.3f); // Mais sutil
        }

        public void update(float delta) {
            x += speed * delta;
            if (x > MainGame.VIRTUAL_WIDTH) {
                x = -length;
                y = random.nextFloat() * MainGame.VIRTUAL_HEIGHT;
            }
        }
    }


    public MenuScreen(MainGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT, camera);
        layout = new GlyphLayout();

        shapeRenderer = new ShapeRenderer();
        random = new Random();
        windLines = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            windLines.add(new MenuWindLine(random.nextFloat() * MainGame.VIRTUAL_HEIGHT));
        }

        loadResources();
        createFonts();
        game.playBackgroundMusic();
    }

    private void loadResources() {
        try {
            background = new Texture(Gdx.files.internal("background.png"));
            bikeTexture = new Texture(Gdx.files.internal("player_bike.png"));

            TextureRegion[] frames = new TextureRegion[1];
            frames[0] = new TextureRegion(bikeTexture);
            bikeAnimation = new Animation<>(0.1f, frames);

        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "Erro ao carregar texturas: " + e.getMessage());
            background = createPlaceholderTexture((int)MainGame.VIRTUAL_WIDTH, (int)MainGame.VIRTUAL_HEIGHT, new Color(0.2f, 0.6f, 0.8f, 1));
            bikeTexture = createPlaceholderTexture(100, 120, Color.ORANGE);
        }
    }

    private void createFonts() {
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(new Color(1, 0.8f, 0, 1));

        menuFont = new BitmapFont();
        menuFont.getData().setScale(1.8f);
        menuFont.setColor(Color.WHITE);

        instructionFont = new BitmapFont();
        instructionFont.getData().setScale(1.2f);
        instructionFont.setColor(new Color(0.8f, 0.8f, 0.8f, 1));
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

    private void update(float delta) {
        stateTime += delta;
        pulseTime += delta;

        // Animação de flutuação da bike
        updateBikeAnimation(delta);

        // Animação do parallax
        backgroundOffsetX -= parallaxSpeed * delta;
        if (backgroundOffsetX < -MainGame.VIRTUAL_WIDTH) {
            backgroundOffsetX += MainGame.VIRTUAL_WIDTH;
        }

        // Animação das linhas de vento
        for (MenuWindLine line : windLines) {
            line.update(delta);
        }
    }

    @Override
    public void render(float delta) {
        // --- LÓGICA ---
        update(delta);

        // --- DESENHO ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        // Desenhar Parallax (Batch)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        float x1 = backgroundOffsetX;
        float x2 = backgroundOffsetX + MainGame.VIRTUAL_WIDTH;
        float x3 = backgroundOffsetX - MainGame.VIRTUAL_WIDTH;
        game.batch.draw(background, x1, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.draw(background, x2, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.draw(background, x3, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.end();

        // Desenhar Efeitos (ShapeRenderer)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Efeito de vento
        drawWindLines(shapeRenderer);

        // Efeito de glow no título
        float pulse = (float) (Math.sin(pulseTime * 3) * 0.1f + 0.9f);
        float titleY = MainGame.VIRTUAL_HEIGHT - 100 - 30;
        float titlePulseWidth = 350 * pulse;
        shapeRenderer.setColor(1, 0.8f + pulse * 0.2f, 0.2f, 0.2f * pulse);
        shapeRenderer.rect(MainGame.VIRTUAL_WIDTH / 2f - titlePulseWidth / 2f, titleY, titlePulseWidth, 4f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);


        // Desenhar Cena (Batch)
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Overlay escuro para melhor contraste
        game.batch.setColor(0, 0, 0, 0.3f);
        game.batch.draw(createPlaceholderTexture(1, 1, Color.BLACK), 0, 0, MainGame.VIRTUAL_WIDTH, MainGame.VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE);

        // Título principal com efeito de brilho
        titleFont.setColor(1, 0.8f + pulse * 0.2f, 0.2f, 1);
        String title = "EXTREME BIKE RACE";
        layout.setText(titleFont, title);
        titleFont.draw(game.batch, title,
            (MainGame.VIRTUAL_WIDTH - layout.width) / 2,
            MainGame.VIRTUAL_HEIGHT - 100);

        // Bike animada
        TextureRegion currentFrame = bikeAnimation.getKeyFrame(stateTime, true);
        float bikeX = MainGame.VIRTUAL_WIDTH / 2f - 50f;
        float bikeW = 100f;
        float bikeH = 120f;

        // --- MUDANÇA (Request 3): Desenhar com rotação ---
        game.batch.draw(currentFrame,
            bikeX, 250 + bikeY,             // Posição
            bikeW / 2f, bikeH / 2f,         // Origem (centro)
            bikeW, bikeH,                   // Tamanho
            1f, 1f,                         // Escala
            bikeAngle);                     // Rotação

        // Menu de opções
        String startText = "> INICIAR CORRIDA <";
        layout.setText(menuFont, startText);

        // Efeito de pulsação no texto de iniciar
        float textPulse = (float) (Math.sin(pulseTime * 4) * 0.2f + 0.8f);
        menuFont.setColor(1, textPulse, textPulse, 1);

        menuFont.draw(game.batch, startText,
            (MainGame.VIRTUAL_WIDTH - layout.width) / 2,
            180);



        String enterText = "PRESSIONE [ENTER] PARA COMEÇAR!";
        layout.setText(instructionFont, enterText);

        // Efeito piscante para ENTER
        float enterAlpha = (float) (Math.sin(pulseTime * 6) * 0.5f + 0.5f);
        instructionFont.setColor(1, 1, 1, enterAlpha);
        instructionFont.draw(game.batch, enterText,
            (MainGame.VIRTUAL_WIDTH - layout.width) / 2,
            70);

        // Rodapé
        instructionFont.setColor(0.6f, 0.6f, 0.6f, 1);
        String footer = "Desvie dos obstáculos e mantenha sua velocidade!";
        layout.setText(instructionFont, footer);
        instructionFont.draw(game.batch, footer,
            (MainGame.VIRTUAL_WIDTH - layout.width) / 2,
            30);

        game.batch.end();

        // Verificar input
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            GameScreen gameScreen = new GameScreen(game, 1);
            game.setScreen(gameScreen);
            dispose();
        }
    }

    // --- MUDANÇA (Request 3): Calcular rotação ---
    private void updateBikeAnimation(float delta) {
        if (bikeMovingUp) {
            bikeY += 20 * delta;
            if (bikeY > 5) bikeMovingUp = false;
        } else {
            bikeY -= 20 * delta;
            if (bikeY < -5) bikeMovingUp = true;
        }
        // A bikeY vai de -5 a 5. O ângulo irá de -2.5° a 2.5°
        bikeAngle = bikeY * 0.5f;
    }

    private void drawWindLines(ShapeRenderer renderer) {
        for (MenuWindLine line : windLines) {
            renderer.setColor(1, 1, 1, line.alpha);
            renderer.rectLine(line.x, line.y, line.x + line.length, line.y, 2.5f);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
    }

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (menuFont != null) menuFont.dispose();
        if (instructionFont != null) instructionFont.dispose();
        if (background != null) background.dispose();
        if (bikeTexture != null) bikeTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override
    public void show() {
        game.playBackgroundMusic();
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
