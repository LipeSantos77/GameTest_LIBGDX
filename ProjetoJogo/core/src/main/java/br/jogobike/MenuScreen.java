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
import com.badlogic.gdx.utils.viewport.FitViewport; // Mantendo FitViewport

public class MenuScreen implements Screen {
    private final MainGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport; // FitViewport para manter a proporção 1:1 (800x800) e criar as barras pretas
    private BitmapFont titleFont;
    private BitmapFont menuFont;
    private BitmapFont instructionFont;
    private final GlyphLayout layout;

    // Texturas e animações
    private Texture background;
    private Texture bikeTexture;
    private Animation<TextureRegion> bikeAnimation;
    private float stateTime;

    // Efeitos visuais
    private float pulseTime = 0;
    private float bikeY = 0;
    private boolean bikeMovingUp = true;

    // DEFINIÇÕES IGUAIS A GAMESCREEN
    private static final int VIRTUAL_WIDTH = 800;
    private static final int VIRTUAL_HEIGHT = 800;

    public MenuScreen(MainGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        // Usando FitViewport com a mesma resolução de GameScreen
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        layout = new GlyphLayout();

        loadResources();
        createFonts();

        // Garantir que a música está tocando no menu
        if (game.backgroundMusic != null && !game.backgroundMusic.isPlaying()) {
            game.backgroundMusic.play();
        }
    }

    private void loadResources() {
        try {
            // Carregar texturas
            background = new Texture(Gdx.files.internal("background.png"));
            bikeTexture = new Texture(Gdx.files.internal("player_bike.png"));

            // Criar animação simples da bike (pulsação)
            TextureRegion[] frames = new TextureRegion[1];
            frames[0] = new TextureRegion(bikeTexture);
            bikeAnimation = new Animation<>(0.1f, frames);

        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "Erro ao carregar texturas: " + e.getMessage());
            // Criar placeholders se necessário
            background = createPlaceholderTexture(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, new Color(0.2f, 0.6f, 0.8f, 1));
            bikeTexture = createPlaceholderTexture(100, 120, Color.ORANGE);
        }
    }

    private void createFonts() {
        // Usar fontes padrão do LibGDX com estilização
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
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void render(float delta) {
        // Atualizar animações
        stateTime += delta;
        pulseTime += delta;
        updateBikeAnimation(delta);

        // Limpar tela (a cor é o que aparece nas barras pretas)
        Gdx.gl.glClearColor(0.1f, 0.3f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Aplicar viewport e atualizar câmera
        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Desenhar cena
        game.batch.begin();

        // Fundo (desenhado na resolução virtual 800x800)
        game.batch.draw(background, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Overlay escuro para melhor contraste
        game.batch.setColor(0, 0, 0, 0.3f);
        game.batch.draw(createPlaceholderTexture(1, 1, Color.BLACK), 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE);

        // Título principal com efeito de brilho
        float pulse = (float) (Math.sin(pulseTime * 3) * 0.1f + 0.9f);
        titleFont.setColor(1, 0.8f + pulse * 0.2f, 0.2f, 1);

        String title = "EXTREME BIKE RACE";
        layout.setText(titleFont, title);
        titleFont.draw(game.batch, title,
            (VIRTUAL_WIDTH - layout.width) / 2,
            VIRTUAL_HEIGHT - 100);

        // Bike animada
        TextureRegion currentFrame = bikeAnimation.getKeyFrame(stateTime, true);
        float bikeX = VIRTUAL_WIDTH / 2f - 50f;
        game.batch.draw(currentFrame, bikeX, 250 + bikeY, 100, 120);

        // Efeito de movimento atrás da bike
        drawSpeedLines(game.batch, bikeX, 250 + bikeY);

        // Menu de opções
        String startText = "> INICIAR CORRIDA <";
        layout.setText(menuFont, startText);

        // Efeito de pulsação no texto de iniciar
        float textPulse = (float) (Math.sin(pulseTime * 4) * 0.2f + 0.8f);
        menuFont.setColor(1, textPulse, textPulse, 1);

        menuFont.draw(game.batch, startText,
            (VIRTUAL_WIDTH - layout.width) / 2,
            180);

        // Instruções de controle
        instructionFont.setColor(0.8f, 0.8f, 0.8f, 1);

        String controls1 = "CONTROLES:";
        layout.setText(instructionFont, controls1);
        instructionFont.draw(game.batch, controls1,
            (VIRTUAL_WIDTH - layout.width) / 2,
            140);

        String controls2 = "↑ ACELERAR   ← → MOVER   ↓ FREAR";
        layout.setText(instructionFont, controls2);
        instructionFont.draw(game.batch, controls2,
            (VIRTUAL_WIDTH - layout.width) / 2,
            110);

        String enterText = "PRESSIONE [ENTER] PARA COMEÇAR!";
        layout.setText(instructionFont, enterText);

        // Efeito piscante para ENTER
        float enterAlpha = (float) (Math.sin(pulseTime * 6) * 0.5f + 0.5f);
        instructionFont.setColor(1, 1, 1, enterAlpha);
        instructionFont.draw(game.batch, enterText,
            (VIRTUAL_WIDTH - layout.width) / 2,
            70);

        // Rodapé
        instructionFont.setColor(0.6f, 0.6f, 0.6f, 1);
        String footer = "Desvie dos obstáculos e mantenha sua velocidade!";
        layout.setText(instructionFont, footer);
        instructionFont.draw(game.batch, footer,
            (VIRTUAL_WIDTH - layout.width) / 2,
            30);

        game.batch.end();

        // Verificar input
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game, 1));
            dispose();
        }
    }

    private void updateBikeAnimation(float delta) {
        // Animação de flutuação da bike
        if (bikeMovingUp) {
            bikeY += 20 * delta;
            if (bikeY > 5) bikeMovingUp = false;
        } else {
            bikeY -= 20 * delta;
            if (bikeY < -5) bikeMovingUp = true;
        }
    }

    private void drawSpeedLines(SpriteBatch batch, float bikeX, float bikeY) {
        // Desenhar linhas de velocidade atrás da bike
        batch.setColor(1, 1, 1, 0.4f);

        float time = stateTime * 10f;
        for (int i = 0; i < 8; i++) {
            float offset = (time + i * 0.5f) % 2f;
            float x = bikeX - 60 - offset * 40;
            float height = 10 + i * 3;
            batch.draw(createPlaceholderTexture(1, 1, Color.WHITE),
                x, bikeY + 30 + i * 8, 15, height);
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        // Atualiza o viewport para o novo tamanho de tela, mantendo a proporção 1:1
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
    }

    @Override
    public void show() {
        // Quando a tela do menu é mostrada, garantir que a música está tocando
        if (game.backgroundMusic != null && !game.backgroundMusic.isPlaying()) {
            game.backgroundMusic.play();
        }
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
