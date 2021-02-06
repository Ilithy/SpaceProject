package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.utility.Mappers;

public class ShieldSystem extends IteratingSystem {
    
    public ShieldSystem() {
        super(Family.all(ShieldComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ShieldComponent shield = Mappers.shield.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        
        //shield.isCharging = control.defend;
        float shieldReactivateThreshold = 0.3f;
        
        switch (shield.state) {
            case off:
                if (control.defend) {
                    //reactivate if still enough charge
                    if (shield.animTimer.ratio() >= shieldReactivateThreshold) {
                        shield.animTimer.flipRatio();
                    } else {
                        shield.animTimer.reset();
                    }
                    
                    shield.state = ShieldComponent.State.charge;
                }
                break;
            case on:
                if (!control.defend) {
                    shield.state = ShieldComponent.State.discharge;
                    
                    //destroy shield fixture
                    Body body = entity.getComponent(PhysicsComponent.class).body;
                    Fixture circleFixture = body.getFixtureList().get(body.getFixtureList().size - 1);
                    body.destroyFixture(circleFixture);
    
                    shield.animTimer.flipRatio();
                }
                break;
            case charge:
                if (!control.defend) {
                    shield.state = ShieldComponent.State.discharge;
                    break;
                }
                
                //charge
                shield.radius = shield.maxRadius * shield.animTimer.ratio();
                
                //or if (shield.animTimer.canDoEvent())
                if (shield.radius == shield.maxRadius) {
                    shield.state = ShieldComponent.State.on;
                    
                    //add shield fixture to body for protection
                    Body body = entity.getComponent(PhysicsComponent.class).body;
                    BodyFactory.addShieldFixtureToBody(body, shield.radius);
                }
                break;
            case discharge:
                if (control.defend) {
                    //reactivate
                    if (shield.animTimer.ratio() >= shieldReactivateThreshold) {
                        shield.animTimer.flipRatio();
                    } else {
                        shield.animTimer.reset();
                    }
                    
                    shield.state = ShieldComponent.State.charge;
                    break;
                }
                
                //discharge
                shield.radius = shield.maxRadius * (1 - shield.animTimer.ratio());
                
                if (shield.radius <= 0) {
                    shield.state = ShieldComponent.State.off;
                }
                break;
        }
    }
    
}