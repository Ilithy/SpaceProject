package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.CustomShapeRenderer;
import com.spaceproject.utility.Mappers;

public class AsteroidRenderSystem extends IteratingSystem {
    
    CustomShapeRenderer shapeRenderer;
    Color color = new Color();
    boolean debugFloatingBodies = false;
    
    public AsteroidRenderSystem() {
        super(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        shapeRenderer = new CustomShapeRenderer();
    }
    
    @Override
    public void update(float deltaTime) {
        shapeRenderer.setProjectionMatrix(GameScreen.cam.combined);
        
        //render filled inner poly
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        super.update(deltaTime);
        shapeRenderer.end();
    
        //render outer polygon triangle mesh outline
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        super.update(deltaTime);
        shapeRenderer.end();
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        HealthComponent health = Mappers.health.get(entity);
    
        //set polygon translation and rotation
        Polygon polygon = asteroid.polygon;
        polygon.setRotation(transform.rotation * MathUtils.radiansToDegrees);
        polygon.setPosition(transform.pos.x, transform.pos.y);
        
        //set color based on fill type and health
        if (shapeRenderer.getCurrentType() == ShapeRenderer.ShapeType.Filled) {
            float ratio = health.health / health.maxHealth;
            if (asteroid.parentOrbitBody == null) {
                color.set(1-ratio, 0 ,0, 1);//black to red
            } else {
                color.set(1, ratio, ratio, 1);//white to red
            }
        } else {
            //mesh outline
            color = asteroid.color.cpy();
        }
        
        //debug orbit lock
        if (debugFloatingBodies && asteroid.parentOrbitBody == null) {
            color.set(1, 1 , 0, 1);
        }
        
        shapeRenderer.fillPolygon(polygon.getTransformedVertices(), 0, polygon.getVertices().length, color);
    }
    
}
