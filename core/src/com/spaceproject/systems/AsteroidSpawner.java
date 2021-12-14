package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CircumstellarDiscComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class AsteroidSpawner extends EntitySystem implements EntityListener {
    //todo:
    // - asteroid source A: belt / circumstellar disc
    //   a ring of random destructible asteroids around a planet
    //      belt radius: range from source body
    //      belt width: how wide bodies spawn from concentration of bodies at belt
    //      belt density: how many asteroids to populate belt with
    //      direction: which way they orbit around body
    // - asteroid source B: "field" / cluster
    //   random group out in the depths of space
    //      pocket size: how many to spawn in group
    //      direction: which way headed
    // - asteroid source c: rogue rock "odd ball"
    //      just a single rock of random size maybe larger than usual, going in a random direction
    // - if any asteroid is too far from player, unload
    
    private ImmutableArray<Entity> asteroids;
    private ImmutableArray<Entity> spawnDisk;
    SimpleTimer lastSpawnedTimer = new SimpleTimer(1000);
    int maxSpawn = 200;
    
    ShortArray triangles;
    DelaunayTriangulator delaunay = new DelaunayTriangulator();
    EarClippingTriangulator earClip = new EarClippingTriangulator();
    
    @Override
    public void addedToEngine(Engine engine) {
        asteroids = engine.getEntitiesFor(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        spawnDisk = engine.getEntitiesFor(Family.all(CircumstellarDiscComponent.class).get());
        lastSpawnedTimer.setCanDoEvent();
    }
    
    @Override
    public void update(float deltaTime) {
        //super.update(deltaTime);
        
        /*
        if (asteroids.size() <= maxSpawn) {
            if (lastSpawnedTimer.tryEvent()) {
                spawnAsteroid();
            }
        } else {
            lastSpawnedTimer.reset();
        }*/
    
        
        if (asteroids.size() <= maxSpawn) {
            if (lastSpawnedTimer.tryEvent()) {
                spawnAsteroidField(-1000, -1000);
                lastSpawnedTimer.reset();
            }
        }
        
        spawnAsteroidDisk();
        
        /*todo: orbit asteroids around parent body, currently just flings everything out into universe.. ;p
        for (Entity entity : asteroids) {
            AsteroidComponent asteroid = Mappers.asteroid.get(entity);
            if (asteroid.type == AsteroidComponent.Type.orbitLocked) {
                PhysicsComponent physics = Mappers.physics.get(entity);
                //physics.body.setLinearVelocity(); o
            }
        }*/
    }
    
    private void spawnAsteroidDisk() {
        for (Entity parentEntity : spawnDisk) {
            CircumstellarDiscComponent disk = Mappers.circumstellar.get(parentEntity);
            if (asteroids.size() <= maxSpawn) {
                //if (lastSpawnedTimer.tryEvent()) {
                Vector2 pos = Mappers.transform.get(parentEntity).pos.cpy();
                float bandwidthOffset = MathUtils.random(-disk.width/2, disk.width/2);//todo, should bias towards middle and taper off edges
                //alternatively could be a 1D noise from inner to outer with different concentrations?
                float d = MathUtils.random(MathUtils.PI2);
                pos.add(MyMath.vector(d, disk.radius + bandwidthOffset));
                //todo: apply gravity to keep them rotating around star
                // problem: we want asteroids to spawn in rings and stay in rings generally, but allow player to shoot them out of belt.
                // I can't let the bodies float around freely with a simple n body sim as they are not stable.
                // the system will just tear itself apart. even if a "stable" orbit is found it will become unstable as the player starts interacting with it
                // currently this is why planets are locked in an elliptical orbit to sort of fake gravity.
                // do I lock the asteroids in orbit similar to the planets? maybe interpolate to where they "should" be, pulling them back into the ring
                // maybe once hit or disturbed, loses orbit component and floats freely?
                // basically it comes down to how "arcadey" vs "simulator" does it need to be fun?
                // currently there is no friction and we have conservation of momentum
                
                // plan of attack: i think i will start off with a lie, lock them in orbit when spawn.
                // once disturbed or interacted with by an outside force, unlock the orbit and let gravity take over
                // allow belt to "chain react" collapse / disperse and see what that plays like
                Vector2 velocity = MyMath.vector((float) (d + Math.PI/2), 20);
                spawnAsteroid(pos.x, pos.y, velocity.x, velocity.y);
                //}
            } else {
                lastSpawnedTimer.reset();
            }
        }
    }
    
    private void spawnAsteroidField(float x, float y) {
        float d = MathUtils.random(MathUtils.PI2);
        Vector2 vel = MyMath.vector(d, MathUtils.random(1, 50));
        float range = 500.0f;
        int clusterSize = 20;//MathUtils.random(1, 20);
        for (int i = 0; i < clusterSize; i++) {
            float newX = MathUtils.random(x-range, x+range);
            float newY = MathUtils.random(y-range, y+range);
            spawnAsteroid(newX, newY, vel.x, vel.x);
        }
        Gdx.app.log(this.getClass().getSimpleName(), "spawn field: " + clusterSize);
    }
    
    private void spawnAsteroid(float x, float y, float velX, float velY) {
        int size = MathUtils.random(14, 120);
        long seed = MyMath.getSeed(x, y);
        Entity asteroid = EntityFactory.createAsteroid(seed, x, y, velX, velY, size);
        getEngine().addEntity(asteroid);
        //Gdx.app.debug(this.getClass().getSimpleName(), "spawned: " + x + ", " + y);
    }
    
    @Override
    public void entityAdded(Entity entity) {
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        //todo: consider adding a removal flag? what happens if we try to remove all asteroids
        // eg: shatter vs cleanup. to prevent intentional removal from continually spawning children
        
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        if (asteroid != null) {
            spawnChildren(entity, asteroid);
        }
    }
    
    public void spawnChildren(Entity parentAsteroid, AsteroidComponent asteroid) {
        //todo: how should shatter mechanics handle? what if pass down extra damage to children.
        // eg: asteroid has 100 hp, breaks into 2 = 50hp children
        //   damage 110 = -10 hp, breaks into 2 minus 5 each = 45hp
        //   damage 200 = -100 hp, breaks into 2 minus 50 = 0hp = don't spawn children -> instant destruction
        //  could play with different rules and ratios, find what feels good
        //
        float minAsteroidSize = 14; //anything smaller than this will not create more
        if (asteroid.size >= minAsteroidSize) {
            //todo: size = previousSize / numChildren?
            
            TransformComponent parentTransform = Mappers.transform.get(parentAsteroid);
            float x = parentTransform.pos.x;
            float y = parentTransform.pos.y;
            float size = asteroid.size * 0.4f;
            //long seed = MyMath.getSeed(x, y);
            Vector2 parentVelocity = parentAsteroid.getComponent(PhysicsComponent.class).body.getLinearVelocity();
    
            float[] vertices = asteroid.polygon.getVertices();
            triangles = earClip.computeTriangles(vertices);
            //triangles = delaunay.computeTriangles(vertices, true);
        
            //create cells for each triangle
            for (int index = 0; index < triangles.size; index += 3) {
                int p1 = triangles.get(index) * 2;
                int p2 = triangles.get(index + 1) * 2;
                int p3 = triangles.get(index + 2) * 2;
                float[] hull = new float[] {
                        vertices[p1], vertices[p1 + 1],
                        vertices[p2], vertices[p2 + 1],
                        vertices[p3], vertices[p3 + 1]
                };
    
                Gdx.app.debug(this.getClass().getSimpleName(),
                        MyMath.round(hull[0],1) + ", " + MyMath.round(hull[1],1)
                                + " | " + MyMath.round(hull[2],1) + ", " + MyMath.round(hull[3],1)
                                + " | " + MyMath.round(hull[4],1) + ", " + MyMath.round(hull[5],1));
                if (hull[0] == hull[2] || hull[0] == hull[4] || hull[2] == hull[4]) {
                    if (hull[1] == hull[3] || hull[1] == hull[5] || hull[3] == hull[5]) {
                        Gdx.app.error(this.getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                        continue;
                    }
                }
                Entity childAsteroid = EntityFactory.createAsteroid((long) (Math.random() * Long.MAX_VALUE), x, y, parentVelocity.x, parentVelocity.y, size, hull);
                getEngine().addEntity(childAsteroid);
                //java: ../Box2D/Collision/Shapes/b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
            }
        }
    }
    
}