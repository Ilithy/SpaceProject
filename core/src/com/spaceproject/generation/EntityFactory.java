package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.AttachedToComponent;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.ShaderComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.math.Physics;
import com.spaceproject.math.PolygonUtil;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Sprite3D;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.SimpleTimer;


public class EntityFactory {
    
    private static final EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private static final CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    
    //region characters
    public static Entity createCharacter(float x, float y) {
        Entity entity = new Entity();
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.CHARACTERS.getHierarchy();
        entity.add(transform);
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateCharacter();
        texture.scale = engineCFG.sprite2DScale;
        entity.add(texture);
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createPlayerBody(x, y, entity);
        entity.add(physics);
        
        CharacterComponent character = new CharacterComponent();
        character.walkSpeed = entityCFG.characterWalkSpeed;
        entity.add(character);
        
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.characterHealth;
        health.health = health.maxHealth;
        entity.add(health);
    
        DashComponent dash = new DashComponent();
        dash.impulse = 3f;
        dash.dashTimeout = new SimpleTimer(1000);
        entity.add(dash);
        
        ControllableComponent control = new ControllableComponent();
        control.timerVehicle = new SimpleTimer(entityCFG.controlTimerVehicle);
        entity.add(control);

        return entity;
    }
    
    public static Entity createPlayer(float x, float y) {
        Entity character = createCharacter(x, y);
        character.add(new CameraFocusComponent());
        character.add(new ControlFocusComponent());
        return character;
    }
    
    public static Entity createCharacterAI(float x, float y) {
        Entity character = createCharacter(x, y);
        character.add(new AIComponent());
        return character;
    }
    
    public static Array<Entity> createPlayerShip(int x, int y, boolean inSpace) {
        Entity player = createPlayer(x, y);
        
        PhysicsComponent physicsComponent = player.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;
        
        Array<Entity> playerShipCluster = createBasicShip(x, y, 0, player, inSpace);
        Entity playerShip = playerShipCluster.first();
    
        ECSUtil.transferControl(player, playerShip);
   
        return playerShipCluster;
    }
    
    public static Array<Entity> createAIShip(float x, float y, boolean inSpace) {
        Entity ai = createCharacterAI(x, y);
        
        PhysicsComponent physicsComponent = ai.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;
        
        Array<Entity> aiShipCluster = createBasicShip(x, y, 0, ai, inSpace);
        Entity aiShip = aiShipCluster.first();
        ECSUtil.transferControl(ai, aiShip);
        
        return aiShipCluster;
    }
    //endregion
    
    
    //region Astronomical / Celestial objects and bodies
    public static Entity createStar(long seed, float x, float y, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();
        
        SeedComponent seedComponent = new SeedComponent();
        seedComponent.seed = seed;
        entity.add(seedComponent);
        
        //star properties
        StarComponent star = new StarComponent();
        star.temperature = MathUtils.random(1000, 50000); //typically (2,000K - 40,000K)
        //star.temperature = Physics.Sun.kelvin;//test sun color
        star.peakWavelength = Physics.temperatureToWavelength(star.temperature) * 1000000;
        int[] colorTemp = Physics.wavelengthToRGB(star.peakWavelength);
        //convert from [0 to 255] -> [0 - 1]
        star.colorTemp  = new float[]{
                colorTemp[0] / 255.0f, // red
                colorTemp[1] / 255.0f, // green
                colorTemp[2] / 255.0f  // blue
        };
        entity.add(star);
        
        // create star texture
        TextureComponent texture = new TextureComponent();
        int radius = MathUtils.random(celestCFG.minStarSize, celestCFG.maxStarSize);
        texture.texture = TextureFactory.generateStar(seed, radius, 20);
        texture.scale = 4;
        entity.add(texture);
        
        // shader
        ShaderComponent shader = new ShaderComponent();
        shader.shaderType = ShaderComponent.ShaderType.star;
        entity.add(shader);
        
        // set position
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);
        
        //orbit for rotation of self (kinda hacky; not really orbiting, just rotating)
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = null;//set to null to negate orbit, but keep rotation
        orbit.rotateClockwise = rotationDir;
        orbit.rotSpeed = MathUtils.random(celestCFG.minStarRot, celestCFG.maxStarRot); //rotation speed of star
        entity.add(orbit);
        
        //mapState
        MapComponent map = new MapComponent();
        map.color = new Color(0.9f, 0.9f, 0.15f, 0.9f);
        map.distance = 80000;
        entity.add(map);
        
        return entity;
    }
    
    public static Entity createPlanet(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();
        
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);
        
        //create placeholder texture. real texture will be generated by a thread
        TextureComponent texture = new TextureComponent();
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        int planetSize = (int) Math.pow(2, MathUtils.random(7, 10));
        texture.texture = TextureFactory.generatePlanetPlaceholder(planetSize, chunkSize);
        texture.scale = 16;
        entity.add(texture);
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);
        
        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        entity.add(orbit);
        
        //minimap marker
        MapComponent map = new MapComponent();
        map.color = new Color(0.15f, 0.5f, 0.9f, 0.9f);
        map.distance = 10000;
        entity.add(map);
        
        //planet
        PlanetComponent planet = new PlanetComponent();
        planet.mapSize = planetSize;
        //TODO: randomize features/load from feature profile
        planet.scale = 100;
        planet.octaves = 4;
        planet.persistence = 0.68f;
        planet.lacunarity = 2.6f;
        entity.add(planet);
        
        
        return entity;
    }
    
    public static Entity createMoon(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();
        
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);
        
        //create placeholder texture.
        TextureComponent texture = new TextureComponent();
        int size = (int) Math.pow(2, MathUtils.random(5, 7));
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        texture.texture = TextureFactory.generatePlanetPlaceholder(size, chunkSize);
        texture.scale = 16;
        entity.add(texture);
        
        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);
        
        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        entity.add(orbit);
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(0.5f, 0.6f, 0.6f, 0.9f);
        map.distance = 10000;
        entity.add(map);
        
        return entity;
    }
    
    public static Entity createAsteroid(long seed, float x, float y, float velX, float velY, float angle, float[] vertices) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();
    
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        entity.add(transform);
    
        /*
        NOTE! Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed coordinate
        system with the z-axis pointing out of the plane.
        */
        AsteroidComponent asteroid = new AsteroidComponent();
        Polygon polygon = new Polygon(vertices);
        float area = Math.abs(GeometryUtils.polygonArea(polygon.getVertices(), 0, polygon.getVertices().length));
        asteroid.polygon = polygon;
        asteroid.area = area;
        asteroid.color = new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1);
        entity.add(asteroid);
    
        PhysicsComponent physics = new PhysicsComponent();
        float density = 0.5f;
        physics.body = BodyFactory.createPoly(transform.pos.x, transform.pos.y,
                polygon.getVertices(), angle, density, BodyDef.BodyType.DynamicBody,
                GameScreen.box2dWorld, entity);
        asteroid.centerOfMass = physics.body.getLocalCenter();
        physics.body.setLinearVelocity(velX, velY);
        entity.add(physics);
        
        HealthComponent health = new HealthComponent();
        health.maxHealth = area * 0.1f;
        health.health = health.maxHealth;
        entity.add(health);
        
        return entity;
    }
    
    public static Entity createAsteroid(long seed, float x, float y, float velX, float velY, float size) {
        //create random set of points
        int numPoints = 7;//Box2D poly vert limit is 8: Assertion `3 <= count && count <= 8' failed.
        FloatArray points = new FloatArray();
        float minX = size;
        float minY = size;
        for (int i = 0; i < numPoints * 2; i += 2) {
            float pX = MathUtils.random(size);
            float pY = MathUtils.random(size);
            points.add(pX);
            points.add(pY);
            minX = Math.min(minX, pX);
            minY = Math.min(minY, pY);
        }
        
        //generate hull poly from random points
        ConvexHull convex = new ConvexHull();
        float[] hull = convex.computePolygon(points, false).toArray();
        
        //shift vertices to be centered around 0,0 relatively
        Vector2 center = new Vector2();
        GeometryUtils.polygonCentroid(hull, 0, hull.length, center);
        center.add(minX, minY);
        for (int index = 0; index < hull.length; index += 2) {
            hull[index] -= center.x;
            hull[index + 1] -= center.y;
        }
        
        return createAsteroid(seed, x, y, velX, velY, 0, hull);
    }
    //endregion
    
    
    //region ships
    public static Array<Entity> createBasicShip(float x, float y, boolean inSpace) {
        return createBasicShip(x, y, null, inSpace);
    }
    
    public static Array<Entity> createBasicShip(float x, float y, Entity driver, boolean inSpace) {
        return createBasicShip(x, y, MyMath.getSeed(x, y), driver, inSpace);
    }
    
    public static Array<Entity> createBasicShip(float x, float y, long seed, Entity driver, boolean inSpace) {
        Array<Entity> entityCluster = new Array<>();
        Entity shipEntity = new Entity();
        
        //seed
        MathUtils.random.setSeed(seed);
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        shipEntity.add(seedComp);
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.VEHICLES.getHierarchy();
        transform.rotation = (float) Math.PI / 2; //face upwards
        shipEntity.add(transform);
        
        //generate 3D sprite with random even size
        int shipSize = MathUtils.random(entityCFG.shipSizeMin, entityCFG.shipSizeMax) * 2;
        Texture shipTop = TextureFactory.generateShip(seed, shipSize);
        Texture shipBottom = TextureFactory.generateShipUnderSide(shipTop);
        Sprite3DComponent sprite3DComp = new Sprite3DComponent();
        sprite3DComp.renderable = new Sprite3D(shipTop, shipBottom, engineCFG.sprite3DScale);
        shipEntity.add(sprite3DComp);
        
        //collision detection
        PhysicsComponent physics = new PhysicsComponent();
        float width = shipTop.getWidth() * engineCFG.bodyScale;
        float height = shipTop.getHeight() * engineCFG.bodyScale;
        physics.body = BodyFactory.createShip(x, y, width, height, shipEntity, inSpace);
        shipEntity.add(physics);
        
        //engine data and marks entity as drive-able
        VehicleComponent vehicle = new VehicleComponent();
        vehicle.dimensions = new Rectangle(0, 0, width, height);
        vehicle.driver = driver;
        vehicle.thrust = entityCFG.engineThrust;
        shipEntity.add(vehicle);
        
        //health
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.shipHealth;
        health.health = health.maxHealth;
        shipEntity.add(health);
        
        //weapon
        if (true) {
            CannonComponent cannon = makeCannon(vehicle.dimensions.width);
            shipEntity.add(cannon);
        } else {
            ChargeCannonComponent chargeCannon = makeChargeCannon(vehicle.dimensions.width);
            shipEntity.add(chargeCannon);
        }
        
        //hyper drive
        HyperDriveComponent hyperDrive = new HyperDriveComponent();
        hyperDrive.speed = entityCFG.hyperSpeed;
        hyperDrive.coolDownTimer = new SimpleTimer(2000);
        hyperDrive.chargeTimer = new SimpleTimer(2000);
        hyperDrive.graceTimer = new SimpleTimer(1000);
        shipEntity.add(hyperDrive);
        
        //shield
        ShieldComponent shield = new ShieldComponent();
        shield.animTimer = new SimpleTimer(100, true);
        shield.defence = 100f;
        BoundingBox boundingBox = PolygonUtil.calculateBoundingBox(physics.body);
        float radius = Math.max(boundingBox.getWidth(), boundingBox.getHeight());
        shield.maxRadius = radius;
        shield.color = Color.BLUE;
        shipEntity.add(shield);
        
        //barrel roll
        BarrelRollComponent barrelRoll = new BarrelRollComponent();
        barrelRoll.cooldownTimer = new SimpleTimer(entityCFG.dodgeCooldown);
        barrelRoll.animationTimer = new SimpleTimer(entityCFG.dodgeAnimationTimer, true);
        barrelRoll.revolutions = 1;
        barrelRoll.flipState = BarrelRollComponent.FlipState.off;
        barrelRoll.force = entityCFG.dodgeForce;
        shipEntity.add(barrelRoll);
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(1, 1, 1, 0.9f);
        map.distance = 3000;
        shipEntity.add(map);
        
        //shield particle effect
        ParticleComponent particle = new ParticleComponent();
        particle.type = ParticleComponent.EffectType.shieldCharge;
        particle.offset = new Vector2();
        particle.angle = 0;
        shipEntity.add(particle);
        
        //spline
        SplineComponent spline = new SplineComponent();
        spline.zOrder = 100;//should be on top of others
        spline.style = SplineComponent.Style.state;
        shipEntity.add(spline);
    
        //engine particle effect
        Entity mainEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineMain, new Vector2(0, height + 0.2f), 0);
        Entity leftEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineLeft, new Vector2(width/2 - 0.2f, 0), -90);
        Entity rightEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineRight, new Vector2(-(width/2 - 0.2f), 0), 90);
        
        entityCluster.add(shipEntity);
        entityCluster.add(mainEngine);
        entityCluster.add(leftEngine);
        entityCluster.add(rightEngine);
        
        return entityCluster;
    }
    
    public static ChargeCannonComponent makeChargeCannon(float width) {
        //width the anchor point relative to body
        ChargeCannonComponent chargeCannon = new ChargeCannonComponent();
        chargeCannon.anchorVec = new Vector2(width, 0);
        chargeCannon.aimAngle = 0;
        chargeCannon.velocity = entityCFG.cannonVelocity;
        chargeCannon.maxSize = 0.30f;
        chargeCannon.minSize = 0.1f;
        chargeCannon.growRateTimer = new SimpleTimer(1500);
        chargeCannon.baseDamage = 8f;
        return chargeCannon;
    }
    
    public static CannonComponent makeCannon(float width) {
        //width the anchor point relative to body
        CannonComponent cannon = new CannonComponent();
        cannon.damage = entityCFG.cannonDamage;
        cannon.maxAmmo = entityCFG.cannonAmmo;
        cannon.curAmmo = cannon.maxAmmo;
        cannon.baseRate = 300;
        cannon.minRate = 40;
        cannon.timerFireRate = new SimpleTimer(entityCFG.cannonFireRate);
        cannon.size = entityCFG.cannonSize;
        cannon.velocity = entityCFG.cannonVelocity;
        cannon.acceleration = entityCFG.cannonAcceleration;
        cannon.anchorVec = new Vector2(width, 0);
        cannon.aimAngle = 0;
        cannon.timerRechargeRate = new SimpleTimer(entityCFG.cannonRechargeRate);
        return cannon;
    }
    
    public static Entity createEngine(Entity parent, ParticleComponent.EffectType type, Vector2 offset, float angle) {
        Entity entity = new Entity();
    
        AttachedToComponent attached = new AttachedToComponent();
        attached.parentEntity = parent;
        entity.add(attached);
        
        //EngineComponent->thrust?
        //ShipEngineComponent engine = new ShipEngineComponent();
        //engine.engineState = ShipEngineComponent.State.off;
        //engine.thrust
        //entity.add(engine);
        
        ParticleComponent particle = new ParticleComponent();
        particle.type = type;
        particle.offset = offset;
        particle.angle = angle;
        entity.add(particle);
    
        /*
        todo: offset and angle like attached to?
        todo: alternatively, move offset and angle into AttachedTo and allow chaining
        SplineComponent test = new SplineComponent();
        test.zOrder = 200;
        test.style = SplineComponent.Style.solid;
        test.color = Color.GOLD;
        entity.add(test);
        */
        
        return entity;
    }
    //endregion
    
    
    public static Entity createWall(float x, float y, int width, int height) {
        Entity entity = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateWall(
                width * engineCFG.pixelPerUnit,
                height * engineCFG.pixelPerUnit,
                new Color(0.4f, 0.4f, 0.4f, 1));
        texture.scale = 0.05f;
        entity.add(texture);
    
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createWall(x, y, width, height, entity);
        entity.add(physics);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.WORLD_OBJECTS.getHierarchy();
        entity.add(transform);
        
        return entity;
    }
    
}
