package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.MissileComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.IDGen;
import com.spaceproject.utility.MyMath;

public class EntityFactory {
	
	public static float scale = 4.0f;

	public static Entity[] createPlanetarySystem(float x, float y) {
		long seed = MyMath.getSeed(x, y);
		MathUtils.random.setSeed(MyMath.getSeed(x, y));

		//number of planets in a system
		int numPlanets = MathUtils.random(SpaceProject.celestcfg.minPlanets, SpaceProject.celestcfg.maxPlanets);
		
		//distance between planets
		float distance = SpaceProject.celestcfg.minPlanetDist/3; //add some initial distance between star and first planet
		
		//rotation of system (orbits and spins)
		boolean rotDir = MathUtils.randomBoolean();
		
		//collection of planets/stars
		Entity[] entities = new Entity[numPlanets + 1];
	
		//add star to center of planetary system
		Entity star = createStar(seed, x, y, rotDir);
		entities[0] = star;
		
		//create planets around star
		for (int i = 1; i < entities.length; ++i) {
			//add some distance from previous entity
			distance += MathUtils.random(SpaceProject.celestcfg.minPlanetDist, SpaceProject.celestcfg.maxPlanetDist); 
			float angle = MathUtils.random(3.14f * 2); //angle from star
			float orbitX = x + (distance * MathUtils.cos(angle));
			float orbitY = y + (distance * MathUtils.sin(angle));
			entities[i] = createPlanet(MyMath.getSeed(x, y + distance), star, orbitX, orbitY, angle, distance, rotDir);
		}
		
		System.out.println("Planetary System: (" + x + ", " + y + ") Objects: " + (numPlanets));
		
		return entities;
		
	}
	
	public static Entity createStar(long seed, float x, float y, boolean rotationDir) {
		MathUtils.random.setSeed(seed);
		Entity entity = new Entity();

		// create star texture
		TextureComponent texture = new TextureComponent();
		int radius = MathUtils.random(SpaceProject.celestcfg.minStarSize, SpaceProject.celestcfg.maxStarSize);	
		texture.texture = TextureFactory.generateStar(radius);
		texture.scale = scale;
		
		// set position
		TransformComponent transform = new TransformComponent();
		transform.pos.set(x, y, 0); 
		
		//orbit for rotation of self (kinda hacky; not really orbiting, just rotating)
		OrbitComponent orbit = new OrbitComponent();
		orbit.parent = null;//set to null to negate orbit, but keep rotation
		orbit.rotateClockwise = rotationDir;
		orbit.rotSpeed = MathUtils.random(SpaceProject.celestcfg.minStarRot, 
				SpaceProject.celestcfg.maxStarRot); //rotation speed of star
		
		//map
		MapComponent map = new MapComponent();
		map.color = new Color(0.9f, 0.9f, 0.15f, 0.9f);
		map.distance = 80000;
		
		//add components to entity
		entity.add(orbit);
		entity.add(transform);
		entity.add(texture);
		entity.add(map);
		entity.add(new StarComponent());

		return entity;
	}
	
	public static Entity createPlanet(long seed, Entity parent, float orbitX, float orbitY, float angle, float distance, boolean rotationDir) {
		MathUtils.random.setSeed(seed);
		
		Entity entity = new Entity();	
		
		//create texture plain white texture. real texture will be generated by a thread
		TextureComponent texture = new TextureComponent();
		int size = (int) Math.pow(2, MathUtils.random(7, 10));
		texture.texture = TextureFactory.generatePlanet(size);
		texture.scale = 16;
		
		//orbit 
		OrbitComponent orbit = new OrbitComponent();
		orbit.parent = parent; //object to orbit around
		orbit.rotSpeed = MathUtils.random(SpaceProject.celestcfg.minPlanetRot, SpaceProject.celestcfg.maxPlanetRot);
		orbit.orbitSpeed = MathUtils.random(SpaceProject.celestcfg.minPlanetOrbit, SpaceProject.celestcfg.minPlanetOrbit);
		orbit.angle = angle; //angle from star
		orbit.distance = distance;
		orbit.rotateClockwise = rotationDir;
		
		//transform
		TransformComponent transform = new TransformComponent();
		transform.pos.x = orbitX;
		transform.pos.y = orbitY;
		
		//map
		MapComponent map = new MapComponent();
		map.color = new Color(0.15f, 0.5f, 0.9f, 0.9f);
		map.distance = 10000;
		
		//planet
		PlanetComponent planet = new PlanetComponent();
		planet.seed = seed;
		planet.mapSize = size;
		//TODO: randomize features/load from feature profile
		planet.scale = 100;
		planet.octaves = 4;
		planet.persistence = 0.68f;
		planet.lacunarity = 2.6f;
		
		//add components to entity
		entity.add(transform);
		entity.add(texture);
		entity.add(orbit);
		entity.add(map);
		entity.add(planet);
		
		return entity;
	}
	
	public static Entity createMissile(TransformComponent source, Vector2 velocity, CannonComponent cannon, long ID) {
		Entity entity = new Entity();
				
		//create texture
		TextureComponent texture = new TextureComponent();
		texture.texture = TextureFactory.generateProjectile(cannon.size);
		texture.scale = scale;
		
		//bounding box
		BoundsComponent bounds = new BoundsComponent();
		float width = texture.texture.getWidth() * scale;
		float height = texture.texture.getHeight() * scale;
		bounds.poly = new Polygon(new float[]{0, 0, width, 0, width, height, 0, height});
	    bounds.poly.setOrigin(width/2, height/2);
		
		//set position, orientation, velocity and acceleration
		TransformComponent transform = new TransformComponent();
		transform.pos.set(source.pos);
		transform.rotation = source.rotation;		
		transform.velocity.add(velocity);
		transform.accel.add(velocity.cpy().setLength(cannon.acceleration));//speed up over time
		
		//set expire time
		ExpireComponent expire = new ExpireComponent();
		expire.time = 5;//in seconds ~approx
		
		//missile damage
		MissileComponent missile = new MissileComponent();
		missile.damage = cannon.damage;
		missile.ownerID = ID;
		
		
		entity.add(missile);
		entity.add(expire);
		entity.add(texture);
		entity.add(bounds);
		entity.add(transform);
		
		return entity;
	}
	
	public static Entity createCharacter(float x, float y) {
		Entity entity = new Entity();
		
		TransformComponent transform = new TransformComponent();
		transform.pos.set(x, y, 0);
		
		TextureComponent texture = new TextureComponent();
		texture.texture = TextureFactory.generateCharacter();
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		float width = texture.texture.getWidth() * scale;
		float height = texture.texture.getHeight() * scale;
		bounds.poly = new Polygon(new float[]{0, 0, width, 0, width, height, 0, height});
	    bounds.poly.setOrigin(width/2, height/2);

	    CharacterComponent character = new CharacterComponent();
	    character.walkSpeed = 300f;//70f;
	    
	    //entity.add(new ControllableComponent());
		entity.add(bounds);
		entity.add(transform);
		entity.add(texture);
		entity.add(character);
			
		return entity;
	}
	
	public static Entity createShip3(float x, float y) {
		return createShip3(x, y, null);
	}
	
	public static Entity createShip3(float x, float y, Entity driver) {
		return createShip3(x, y, MyMath.getSeed(x, y), driver);
	}
	
	public static Entity createShip3(float x, float y, long seed, Entity driver) {
		Entity entity = new Entity();

		MathUtils.random.setSeed(seed);
		
		//transform
		TransformComponent transform = new TransformComponent();
		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 10;
		int maxSize = 36;
		
		do {
			//generate even size
			size = MathUtils.random(minSize, maxSize);
		} while (size % 2 == 1);
		TextureComponent texture = new TextureComponent();
		Texture pixmapTex = TextureFactory.generateShip(seed, size);
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		//collision detection
		BoundsComponent bounds = new BoundsComponent(); 
		float width = texture.texture.getWidth() * scale;
		float height = texture.texture.getHeight() * scale;
		bounds.poly = new Polygon(new float[]{0, 0,0, height,  width, height, width, 0});
	    bounds.poly.setOrigin(width/2, height/2);
	    
		//weapon
		CannonComponent cannon = new CannonComponent();
		cannon.damage = 15;
		cannon.maxAmmo = 5;
		cannon.curAmmo = cannon.maxAmmo;
		cannon.fireRate = 20; //lower is faster
		cannon.size = 1; //higher is bigger
		cannon.velocity = 680; //higher is faster
		cannon.acceleration = 200;
		cannon.rechargeRate = 100; //lower is faster
		
		//engine data and marks entity as drive-able
		VehicleComponent vehicle = new VehicleComponent();
		vehicle.driver = driver;
		vehicle.thrust = 320;//higher is faster
		vehicle.maxSpeed = -1;//-1 = no max speed/infinite
		vehicle.id = IDGen.get();
		vehicle.seed = seed;
		
		//health
		HealthComponent health = new HealthComponent();
		health.health = 100;
		health.maxHealth = health.health;
		
		//map
		MapComponent map = new MapComponent();
		map.color = new Color(1, 1, 1, 0.9f);
		map.distance = 3000;
		
		//add components to entity
		entity.add(health);
		entity.add(cannon);
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(vehicle);
		entity.add(map);
		
		return entity;
	}
	
	@Deprecated
	public static Entity createShip2(int x, int y) {
		MathUtils.random.setSeed((x + y) * SpaceProject.SEED);
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		//generate random even size 
		int size;
		int minSize = 8;
		int maxSize = 36;
		do {		
			size = MathUtils.random(minSize, maxSize);
		} while (size % 2 == 1);
		
		// generate pixmap texture
		//int size = 24;
		Pixmap pixmap = new Pixmap(size, size/2, Format.RGBA8888);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillRectangle(0, 0, size, size);
		
		pixmap.setColor(0.7f,0.7f,0.7f,1);
		pixmap.drawRectangle(0, 0, size-1, size-1/2);
		
		Texture pixmapTex = new Texture(pixmap);
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		float width = texture.texture.getWidth() * scale;
		float height = texture.texture.getHeight() * scale;
		bounds.poly = new Polygon(new float[]{0, 0, width, 0, width, height, 0, height});
	    bounds.poly.setOrigin(width/2, height/2);
		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new VehicleComponent());
		
		return entity;
	}

	@Deprecated
	public static Entity createShip(int x, int y) {
		Entity entity = new Entity();

		TransformComponent transform = new TransformComponent();
		TextureComponent texture = new TextureComponent();

		transform.pos.set(x, y, -10);
		transform.rotation = (float) Math.PI/2; //face upwards
		
		// generate pixmap texture
		int size = 16;
		Pixmap pixmap = new Pixmap(size, size, Format.RGB565);
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillTriangle(0, 0, 0, size-1, size-1, size/2);
		
		pixmap.setColor(0, 1, 1, 1);
		pixmap.drawLine(size, size/2, size/2, size/2);
		
		
		Texture pixmapTex = new Texture(pixmap);
		pixmap.dispose(); // clean up
		texture.texture = pixmapTex;// give texture component the generated pixmapTexture
		texture.scale = scale;
		
		BoundsComponent bounds = new BoundsComponent();
		float width = texture.texture.getWidth() * scale;
		float height = texture.texture.getHeight() * scale;
		bounds.poly = new Polygon(new float[]{0, 0, width, 0, width, height, 0, height});
	    bounds.poly.setOrigin(width/2, height/2);

		
		entity.add(bounds);
		entity.add(texture);
		entity.add(transform);
		entity.add(new VehicleComponent());
		
		return entity;
	}

	/*
	public static Entity createNoiseTile(int x, int y, int tileSize) {
		Entity entity = new Entity();
		
		TextureComponent texture = new TextureComponent();
		texture.texture = TextureFactory.generateNoiseTile(x, y, tileSize);
		
		TransformComponent transform = new TransformComponent();
		transform.pos.x = x;
		transform.pos.y = y;
		texture.scale = 8;
		
		entity.add(transform);
		entity.add(texture);
		
		return entity;
	}*/



}
