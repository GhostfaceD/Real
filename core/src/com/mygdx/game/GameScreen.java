package com.mygdx.game;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.awt.Point;

public class GameScreen implements Screen, InputProcessor {

    private OrthographicCamera camera;
    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private SpriteBatch sb;
    private Hero hero;
    private Sprite heroSprite;

    private Bridge[] bridges;
    private Teleporter[] teleporters;
    private PlatformTiled[] platformTiledArray;
    private PlatformSprite platformS;
    private Sprite[] platformSpriteArray;


    private TextureAtlas textureAtlas;
    private Animation animation;
    private float elapsedTime = 0f;

    private boolean[] platformMovementArray;
    private boolean[] isOnPlatformArray;
    private float[] platformLeft;
    private float[] platformRight;


    public GameScreen(Game aGame) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false,w,h);
        camera.update();

        tiledMap = new TmxMapLoader().load("map1.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        Gdx.input.setInputProcessor(this);

        sb = new SpriteBatch();

        hero = new Hero("hero1.png", 0, 0);
        heroSprite = hero.getSprite();

        textureAtlas = new TextureAtlas(Gdx.files.internal("anim1.atlas"));
        animation = new Animation(1f/15f, textureAtlas.getRegions());

        getBridges();
        getTeleporters();
        getPlatforms();

    }

    public int getNumberOfPlatforms(){
        return platformSpriteArray.length;
    }



    @Override
    public void show() {}



    /**
     *  This methods renders the graphics and keep them updated.
     */
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        sb.setProjectionMatrix(camera.combined);
        sb.begin();
        for (int i = 0; i < getNumberOfPlatforms(); i++) {
            platformSpriteArray[i].draw(sb);
        }

        if (heroMovement()){
            elapsedTime += Gdx.graphics.getDeltaTime();
            sb.draw((TextureAtlas.AtlasRegion)animation.getKeyFrame(elapsedTime,true),heroSprite.getX(),heroSprite.getY());
        } else {
            heroSprite.draw(sb);
        }

        sb.end();

        standingOnTeleporter();
        platformBusiness();
    }


    /**
     * This method create a Bridge object for every object in the bridge layer of the map and then
     * stocks them in the 'bridges' array.
     */
    private void getBridges(){
        int bridgeLayerId = 6; /* The id of the BRIDGE layer is 4 in our tilemap */
        MapLayer bridgesObjectLayer = tiledMap.getLayers().get(bridgeLayerId);
        MapObjects objects = bridgesObjectLayer.getObjects();

        int numberOfObject = objects.getCount();

        bridges = new Bridge[numberOfObject];

        int i = 0;

        for (RectangleMapObject rectangleObject : objects.getByType(RectangleMapObject.class)) {

            bridges[i] = new Bridge(rectangleObject, 3, (TiledMapTileLayer) tiledMap.getLayers().get(i + 1));
            i++;
        }
    }

    private void getTeleporters(){
        int objectLayerId = 3; /* The id of the TELEPORTER layer is 3 in our tilemap */
        MapLayer teleportersObjectLayer = tiledMap.getLayers().get(objectLayerId);
        MapObjects objects = teleportersObjectLayer.getObjects();

        int numberOfObject = objects.getCount();

        RectangleMapObject teleporters1[] = new RectangleMapObject[numberOfObject];

        int i = 0;

        for (RectangleMapObject rectangleObject : objects.getByType(RectangleMapObject.class)) {

            teleporters1[i] = rectangleObject;
            i++;
        }
        teleporters = new Teleporter[(i+1)/2];
        i = 0;

        /* Create pair of teleporters (one with the next one so it has to be created
         * accordingly in the TileMap */
        for (int j = 0; j < teleporters1.length; j+=2){
            Teleporter teleporter = new Teleporter(teleporters1[j], teleporters1[j+1]);

            teleporters[i] = teleporter;
            i++;
        }
    }
    /*
     *
     */
    private void getPlatforms() {
        int objectLayerId = 4;
        MapLayer platformsObjectLayer = tiledMap.getLayers().get(objectLayerId);
        MapObjects objects = platformsObjectLayer.getObjects();

        int numberOfObject = objects.getCount();

        RectangleMapObject platforms1[] = new RectangleMapObject[numberOfObject];

        int i = 0;

        for (RectangleMapObject rectangleObject : objects.getByType(RectangleMapObject.class)) {

            platforms1[i] = rectangleObject;
            i++;
        }

        platformTiledArray = new PlatformTiled[(i + 1) / 2];
        platformSpriteArray = new Sprite[(i + 1) / 2];
        platformLeft = new float[(i + 1) / 2];
        platformRight = new float[(i + 1) / 2];
        platformMovementArray = new boolean[(i + 1) / 2];
        isOnPlatformArray = new boolean[(i + 1) / 2];

        i = 0;

        for (int j = 0; j < platforms1.length; j += 2) {
            PlatformTiled platform = new PlatformTiled(platforms1[j], platforms1[j + 1]);
            platformTiledArray[i] = platform;
            platformLeft[i] = platforms1[j].getRectangle().x;
            platformRight[i] = platforms1[j + 1].getRectangle().x;
            platformS = new PlatformSprite("crate.png", (int) platformLeft[i], (int) platforms1[j].getRectangle().y);
            platformSpriteArray[i] = platformS.getPlatformSprite();
            platformMovementArray[i] = true;
            isOnPlatformArray[i] = false;
            i++;
        }


    }

    /**
     * This methods checks if the hero has to move.
     * If so it also checks if the hero will enter in collision with any object
     * in the COLLISION layer.
     * If not, then the hero will move and activate the moving state by returning true.
     */
    private boolean heroMovement(){
        int heroX = hero.getDx();
        int heroY = hero.getDy();

        if (hero.isMoving() && !isCollision(heroX, heroY) && !isCrossingBrokenBridge(heroX, heroY)){
            heroSprite.setPosition(heroSprite.getX()+heroX, heroSprite.getY()+heroY);
            camera.position.x = Math.max(Math.min(heroSprite.getX(), 1024 - camera.viewportWidth/2), camera.viewportWidth/2);
            camera.position.y = Math.max(Math.min(heroSprite.getY(), 1024 - camera.viewportHeight/2), camera.viewportHeight/2);
            return true;
        }
        return false;
    }

    /**
     * This method deals with the bridges and hero interaction:
     *      - if the hero is on a bridge and go out of a bridge in the next move, then the bridge is
     *      weakened, and the hero 'isOnBridge'(iOB) is set to false. Returns false.
     *      - if the hero is on a bridge and stay on the bridge for the next move, then it
     *      returns false.
     *      - if the hero is not on a bridge and goes on a bridge, set iOB to true. Then if the
     *      bridge is 'broken', return true. Otherwise, returns false.
     *      - if the hero is not on a bridge and won't be on a bridge in the next move, then return
     *      false.
     */
    private boolean isCrossingBrokenBridge(int dx, int dy){
        Rectangle heroPos = heroSprite.getBoundingRectangle();
        heroPos.setX(heroPos.getX()+dx);
        heroPos.setY(heroPos.getY()+dy);
        int bridgeNumber = -1;

        int number = bridges.length;

        for(int i = 0; i < number; i++){

            Bridge bridge = bridges[i];
            if (bridgeNumber == -1 && bridge.getRectangleObject().getRectangle().overlaps(heroPos)){
                bridgeNumber = i;
                if(!hero.isOnBridge()){
                    bridges[bridgeNumber].weakenBridge();
                }
                hero.setOnBridge(true);

            }
        }
        if (bridgeNumber == -1){
            // If the bridge he is going to leave is broken, change the image of the bridge to
            // a broken one.
            for (int i = 0; i < number; i++){
                if (bridges[i].getResistance() == 1 && bridges[i].getRectangleObject().getRectangle().overlaps(heroSprite.getBoundingRectangle())){
                    bridges[i].setBrokenVisible();
                }
            }
            hero.setOnBridge(false);
        }

        try {
            if (bridges[bridgeNumber].getResistance() == 0){
                return true;
            }
        } catch (IndexOutOfBoundsException e) {}
        return false;

    }


    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
    }

    /**
     *  This methods checks if the next move implies a collision between the hero and any
     *  'rectangle' object from the 'COLLISION' layer.
     *  Idea from https://stackoverflow.com/questions/20063281/libgdx-collision-detection-with-tiledmap
     * @param dx direction of the hero on the abscissa
     * @param dy direction of the hero on the ordinate
     * @return false, no collision detected; true otherwise
     */
    private boolean isCollision(int dx, int dy){

        int objectLayerId = 5; /* The id of the COLLISION layer is 2 in our tilemap */
        MapLayer collisionObjectLayer = tiledMap.getLayers().get(objectLayerId);
        MapObjects objects = collisionObjectLayer.getObjects();

        Rectangle heroPos = heroSprite.getBoundingRectangle();
        heroPos.setX(heroPos.getX()+dx);
        heroPos.setY(heroPos.getY()+dy);

        /*
        *  We only treat the rectangle form the COLLISION layer, need improvement if other collision
        *  shapes are added.
        */
        for (RectangleMapObject rectangleObject : objects.getByType(RectangleMapObject.class)) {

            Rectangle rectangle = rectangleObject.getRectangle();
            if (Intersector.overlaps(rectangle, heroPos)) {
                /* collision happened */
                return true;
            }
        }

        return  false;
    }

    private void standingOnTeleporter(){
        int number = teleporters.length;

        for (int i = 0; i < number; i++){
            if (heroSprite.getBoundingRectangle().overlaps(teleporters[i].getTeleporter1().getRectangle())){
                hero.setIsOnTeleporter(i);
                teleporters[i].setReadyForActivation(2);
            } else if (heroSprite.getBoundingRectangle().overlaps(teleporters[i].getTeleporter2().getRectangle())) {
                hero.setIsOnTeleporter(i);
                teleporters[i].setReadyForActivation(1);
            } else {
                hero.setIsOnTeleporter(-1);
            }
        }
    }

    /*
     * This method automates the platform sprite(s) movements and if the player is stood on a platform, moves them along with it.
     * Note to Max: I will probably change this a bit to reduce the amount
     * of declarations at the start as well as generally make things neater.
     */
    private void platformBusiness(){
        for (int i = 0; i < getNumberOfPlatforms(); i++) {
            if (platformMovementArray[i] == true) {
                if (platformSpriteArray[i].getX() <= platformRight[i]) {
                    platformSpriteArray[i].setPosition(platformSpriteArray[i].getX() + 1, platformSpriteArray[i].getY());
                } else {
                    platformMovementArray[i] = false;
                }
            }
            else {
                if (platformSpriteArray[i].getX() >= platformLeft[i]) {
                    platformSpriteArray[i].setPosition(platformSpriteArray[i].getX() - 1, platformSpriteArray[i].getY());
                } else {
                    platformMovementArray[i] = true;
                }
            }
            Vector2 heroPosition = new Vector2(heroSprite.getX(), heroSprite.getY());
            for (int j = 0; j < getNumberOfPlatforms(); j++) {
                if (platformSpriteArray[j].getBoundingRectangle().contains(heroPosition)) {
                    isOnPlatformArray[j] = true;
                } else {
                    isOnPlatformArray[j] = false;
                }
            }
            if (isOnPlatformArray[i] == true) {
                if ((platformMovementArray[i] == true && heroSprite.getX() <= (platformRight[i] + platformSpriteArray[i].getWidth()))) {
                    heroSprite.setPosition(heroSprite.getX() + 1, heroSprite.getY());
                }
                else if (platformMovementArray[i] == false && heroSprite.getX() >= platformLeft[i] - platformSpriteArray[i].getWidth()) {
                    heroSprite.setPosition(heroSprite.getX() - 1, heroSprite.getY());
                }
            }
        }
    }



    /**
     * Called when a key is pressed.
     * @param keycode the key pressed
     */
    @Override public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.LEFT) {
            hero.setDx(-2);
        }
        if (keycode == Input.Keys.RIGHT) {
            hero.setDx(2);
        }
        if (keycode == Input.Keys.UP) {
            hero.setDy(2);
        }
        if (keycode == Input.Keys.DOWN) {
            hero.setDy(-2);
        }
        if (keycode == Input.Keys.D && hero.getIsOnTeleporter() != -1){
            teleporters[hero.getIsOnTeleporter()].teleportTo(heroSprite);
        }
        return false;
    }

    /**
     * Called when a key is released.
     * @param keycode the key released
     */
    @Override public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT) {
            hero.setDx(0);
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.DOWN) {
            hero.setDy(0);
        }
        if(keycode == Input.Keys.NUM_1)
            tiledMap.getLayers().get(0).setVisible(!tiledMap.getLayers().get(0).isVisible());
        if(keycode == Input.Keys.NUM_2)
            tiledMap.getLayers().get(1).setVisible(!tiledMap.getLayers().get(1).isVisible());
        return false;
    }

    /**
     * Called when a key is typed.
     * @param character the character pressed
     */
    @Override public boolean keyTyped(char character) {

        return false;
    }

    /**
     * Called when a mouse button is pressed.
     */
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Vector3 clickCoordinates = new Vector3(screenX,screenY,0);
        Vector3 position = camera.unproject(clickCoordinates);
        heroSprite.setPosition(position.x, position.y);
        return true;
    }

    /**
     * Called when a mouse button is released.
     */
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    /**
     * Called when the mouse is dragged.
     */
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    /**
     * Called when the mouse is moved.
     */
    @Override public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    /**
     * Called when the mouse scroller is used.
     */
    @Override public boolean scrolled(int amount) {
        return false;
    }
}