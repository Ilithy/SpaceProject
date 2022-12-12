package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

public class ParallaxRenderSystem extends EntitySystem implements Disposable {
    
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final Vector3 screenCoords = new Vector3();
    private final Vector3 camWorldPos = new Vector3();
    private final Rectangle boundingBox = new Rectangle();
    private ImmutableArray<Entity> players;
    
    //mouse debug
    Entity camMarker, mouseMarker;
    Vector3 mouseProj = new Vector3();
    float animate = 0;
    
    public ParallaxRenderSystem() {
        shape = new ShapeRenderer();
        projectionMatrix = new Matrix4();
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
    }
    
    
    @Override
    public void update(float deltaTime) {
        //warning: coupling
        if (!getEngine().getSystem(HUDSystem.class).isDraw()) {
            return;
        }
        
        //update matrix and convert screen coords to world cords.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        
        screenCoords.set(0,0,0);
        GameScreen.viewport.project(screenCoords);
        camWorldPos.set(GameScreen.cam.position);
        GameScreen.viewport.project(camWorldPos);
        boundingBox.set(1, 1, Gdx.graphics.getWidth()-2, Gdx.graphics.getHeight()-2);
    
        //debug override background
        //debugClearScreen();
        //debugDrawMousePath();
    
    
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        //todo: apply shader to grid
        int gridSize = 400;
        Color gridColor = Color.BLACK.cpy();
        float brightness = (float) Math.abs(Math.sin(animate * 0.5f) * 0.1f);
        //DebugSystem.addDebugText(brightness + " :" + animate, 100, 100);
    
        
        
        //step function
        // size / 2
        //
        //gridColor.a =  brightness;
        gridColor.a = 0.15f;
        drawGrid(gridColor, boundingBox, gridSize, 0.5f);
        
        //drawGrid(Color.WHITE, boundingBox, 50, 3);
        
        //Color red = Color.RED.cpy();
        //red.a = 0.5f;
        //drawGrid(red, boundingBox, 50, 1);
        
        drawOrigin(Color.WHITE);
        //drawDebugCameraPos(Color.RED);
        //update debug cam position
        
        //debugDrawCameraPath(Color.YELLOW);
        //debugDrawMousePath();
        
    /*
        Entity player = players.first();
        Body body = Mappers.physics.get(player).body;
        //float alpha = MathUtils.clamp((cam.zoom / uiCFG.lodShowOrbitPath / uiCFG.orbitFadeFactor), 0, 1);
        Vector2 velocity = body.getLinearVelocity();
        Vector2 facing = MyMath.vector(body.getAngle(), 1);*/
        
        animate += deltaTime;
        int edgePad = 200;
        Rectangle rectangle = new Rectangle(edgePad, edgePad,
                Gdx.graphics.getWidth() - edgePad * 2,
                Gdx.graphics.getHeight() - edgePad * 2);
        //drawEye((float) (10.0f * Math.sin(animate)), rectangle);
        //drawEye((float) (10.0f + (Math.sin(animate) * 5.0f)), boundingBox);
        //((float) (10.0f + (Math.sin(animate) * 10.0f)), new Rectangle(100F, 200F, 100F, (float) (100 + (Math.sin(animate) * 100.0f))));
        //drawEye((float) (10.0f + ((Math.sin(animate * 10.0f) + MathUtils.PI) * 10.0f)), new Rectangle(100F, 100F, (float) (100 + (Math.sin(animate) * 100.0f)), 100));
        shape.end();
    
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
   
    
    private void drawDebugCameraPos(Color color) {
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
        
        //todo: draw ring from center to target
        //Vector2 average = getEngine().getSystem(CameraSystem.class).average;
        //Vector2 offsetFromTarget = getEngine().getSystem(CameraSystem.class).offsetFromTarget;
        //shape.circle(camWorldPos.x, camWorldPos.y, offsetFromTarget.len());
        //shape.circle(average.x, average.y, 10);
        //GameScreen.viewport.getWorldHeight();
        /*
        shape.setColor(Color.GREEN);
        shape.circle(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(), 8);
        shape.line(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(),
                GameScreen.viewport.getScreenX() + GameScreen.viewport.getWorldWidth(), GameScreen.viewport.getScreenY() + GameScreen.viewport.getWorldHeight());
         */
    }
    
    private void debugDrawCameraPath(Color color) {
        if (camMarker == null) {
            //add debug camera marker
            SplineComponent spline = new SplineComponent();
            spline.color = color;
            camMarker = new Entity().add(spline).add(new TransformComponent());
            getEngine().addEntity(camMarker);
            Gdx.app.log(this.getClass().getSimpleName(), "debug cam marker activated");
        }
        Mappers.transform.get(camMarker).pos.set(GameScreen.cam.position.x, GameScreen.cam.position.y);
    }
    
    private void drawOrigin(Color color) {
        shape.setColor(color);
        shape.circle(screenCoords.x, screenCoords.y, 10);
        shape.line(screenCoords.x, 0, screenCoords.x, Gdx.graphics.getHeight());
        shape.line(0, screenCoords.y, Gdx.graphics.getWidth(), screenCoords.y);
    
        //shape.rect(screenCoords.x, rect.y, width, rect.height);
        //shape.rect(0, screenCoords.y, rect.width, width);
    }
    
    private void debugDrawMousePath(){
        if (mouseMarker == null) {
            mouseMarker = new Entity().add(new SplineComponent()).add(new TransformComponent());
            mouseMarker.getComponent(SplineComponent.class).color = Color.YELLOW;
            getEngine().addEntity(mouseMarker);
        }
        mouseProj.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        GameScreen.viewport.unproject(mouseProj);
        Mappers.transform.get(mouseMarker).pos.set(mouseProj.x, mouseProj.y);
    }
    
    private void drawEye(float segments, Rectangle rectangle) {
        
        if (segments > 0) {
            shape.setColor(Color.RED);
            float height = rectangle.getHeight() / segments;
            float width = rectangle.getWidth() / segments;
            for (int i = 0; i * height <= rectangle.getHeight(); i++) {
                //bottom right
                shape.line(rectangle.x + i * width,  rectangle.y, rectangle.x + rectangle.getWidth(), rectangle.y + i * height);
                
                //top left
                shape.line(rectangle.x,  rectangle.y + i * height,  rectangle.x + i * width, rectangle.y + rectangle.getHeight());
                
                //bottom left
                //shape.line(rectangle.x, rectangle.y + i * height, rectangle.x + i * width, rectangle.y);
        
                //diagonal
                //shape.line(rectangle.x, rectangle.y  + i * height, rectangle.x + i * width, rectangle.y);
            }
        }
        
        shape.setColor(Color.GREEN);
        shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.getHeight());
    }
    
    private void drawGrid(Color color, Rectangle rect, int gridSize, float width) {
        shape.setColor(color);
        
        float halfWidth = rect.width * 0.5f;
        float halfHeight = rect.height * 0.5f;
        float centerX = rect.x + halfWidth;
        float centerY = rect.y + halfHeight;
        float scale = GameScreen.cam.zoom;
        float relativeGridWidth = gridSize / scale;
        
        //dynamic size
        boolean adaptiveGrid = false;
        if (adaptiveGrid) {
            if (relativeGridWidth < gridSize) {
                gridSize *= 2.0f;
            }
            relativeGridWidth = gridSize / scale;
            /*
            if (relativeGridWidth < gridSize * 2f) {
                gridSize *= 0.5f;
                relativeGridWidth = gridSize / scale;
            }*/
        }
        int countX = 0, countY = 0;
        
        //draw X: horizontal lines
        float posX = MyScreenAdapter.cam.position.x;
        int startX = (int) (posX + (-halfWidth * scale)) / gridSize;
        int endX = (int) (posX + (halfWidth * scale)) / gridSize;
        for (int i = startX; i < endX + 1; i++) {
            float finalX = (((i * gridSize) - posX) / scale) + centerX;
            if (width > 1) {
                finalX -= width * 0.5f;
            }
            countX++;
            shape.rect(finalX, rect.y, width, rect.height);
        }
        
        //gridSize *= 1.0f / (16.0f / 9.0f);//test 16x9 asymmetrical grid
        
        //draw Y: vertical lines
        float posY = MyScreenAdapter.cam.position.y;
        int startY = (int) (posY + (-halfHeight * scale)) / gridSize;
        int endY = (int) (posY + (halfHeight * scale)) / gridSize;
        for (int i = startY; i < endY + 1; i++) {
            float finalY = (((i * gridSize) - posY) /  scale) + centerY;
            if (width > 1) {
                finalY -= width * 0.5f;
            }
            countY++;
            shape.rect(rect.x, finalY, rect.width, width);
        }
        
        //todo: highlight tile
        //camera (center tile)
        //mouse
        //draw grid co'ods
        //
        
        int tilesX = countX + 1;
        int tilesY = countY + 1;
        
        boolean showDebug = true;
        if (showDebug) {
            DebugSystem.addDebugText(countX + ", " + countY
                    + " | " + tilesX + ", " + tilesY
                    + " | " + relativeGridWidth, rect.x + rect.width/2, rect.y + 20);
            
            //border
            shape.setColor(new Color(0.1f, 0.63f, 0.88f, 1f));
            shape.rect(rect.x, rect.y, rect.width, rect.height);
    
            //center
            shape.setColor(Color.PURPLE);
            shape.circle(centerX, centerY, 10);
            shape.line(centerX, rect.x, centerX, rect.y + rect.height);
            shape.line(rect.x, centerY, rect.x + rect.width, centerY);
        }
    }
    
    private void debugClearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
