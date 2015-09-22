package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.spaceproject.utility.MyMath;

public class TouchUISystem extends EntitySystem {
	
	private Engine engine;
	
	//rendering
	private Matrix4 projectionMatrix = new Matrix4();
	private ShapeRenderer shape = new ShapeRenderer();
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;	
	}
	
	@Override
	public void update(float delta) {

		//set projection matrix so things render using correct coordinates
		// TODO: only needs to be called when screen size changes
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		shape.setProjectionMatrix(projectionMatrix);
		
		//joystick
		int joystickRadius = 200;
		int joystickPosX = 20;
		int joystickPosY = 20;
		int stickX = joystickPosX + joystickRadius;
		int stickY = joystickPosY + joystickRadius;
		
		//shoot button
		int shootButtonRadius = 70;
		int shootButtonPosX = Gdx.graphics.getWidth() - 80;
		int shootButtonPosY = 100;
		
		//vehicle button
		int vehicleButtonRaduis = 50;
		int vehicleButtonPosX = Gdx.graphics.getWidth() - 80;
		int vehicleButtonPosY = 300;


		//FIRE BUTTON---------------------------------------------------
		//finger 1
		float distanceToShootButton = MyMath.distance(
				Gdx.input.getX(0),
				Gdx.graphics.getHeight() - Gdx.input.getY(0), 
				shootButtonPosX - shootButtonRadius, 
				shootButtonPosY + shootButtonRadius);

		//finger 2		
		float distanceToShootButton1 = MyMath.distance(
				Gdx.input.getX(1),
				Gdx.graphics.getHeight() - Gdx.input.getY(1), 
				shootButtonPosX - shootButtonRadius, 
				shootButtonPosY + shootButtonRadius);

		//if a finger is touching the touch is on fire button
		if ((Gdx.input.isTouched(0) && distanceToShootButton <= shootButtonRadius) 
				|| (Gdx.input.isTouched(1) && distanceToShootButton1 <= shootButtonRadius)) {
			engine.getSystem(PlayerControlSystem.class).shoot = true;
		} else {
			engine.getSystem(PlayerControlSystem.class).shoot = false;
		}

		//JOYSTICK-------------------------------------------------------------------------------
		
		boolean move = false;
		
		//padding to register touch if finger is a little bit off the joystick
		int joystickPadding = 100;
		
		
		//check finger 1
		float distanceToJoystick = MyMath.distance(Gdx.input.getX(0), Gdx.graphics.getHeight() - Gdx.input.getY(0), stickX, stickY);		
		
		if (Gdx.input.isTouched(0) && distanceToJoystick <= joystickRadius + joystickPadding) {
			stickX = Gdx.input.getX(0);
			stickY = Gdx.graphics.getHeight() - Gdx.input.getY(0);
			float angle = MyMath.angleTo(stickX, stickY, joystickPosX + joystickRadius, joystickPosY + joystickRadius);
			engine.getSystem(PlayerControlSystem.class).angleFacing = angle;
			move = true;
		}
		
		//check finger 2
		float distanceToJoystick1 = MyMath.distance(Gdx.input.getX(1), Gdx.graphics.getHeight() - Gdx.input.getY(1), stickX, stickY);

		if (Gdx.input.isTouched(1) && distanceToJoystick1 <= joystickRadius + joystickPadding) {
			stickX = Gdx.input.getX(1);
			stickY = Gdx.graphics.getHeight() - Gdx.input.getY(1);
			float angle = MyMath.angleTo(stickX, stickY, joystickPosX + joystickRadius, joystickPosY + joystickRadius);
			engine.getSystem(PlayerControlSystem.class).angleFacing = angle;
			move = true;		
		}

		if (move) {
			//analog movement: how much energy to put into movement
			float powerRatio = distanceToJoystick / joystickRadius;
			if (powerRatio > 1) powerRatio = 1;
			engine.getSystem(PlayerControlSystem.class).movementMultiplier = powerRatio;
			engine.getSystem(PlayerControlSystem.class).moveForward = true;
		} else {
			engine.getSystem(PlayerControlSystem.class).moveForward = false;
		}

		
		//ENTER/EXIT vehicle-----------------------------------------------------
		//TODO: fix vehicle button for multitouch
		float distanceToVehicleButton = MyMath.distance(
				Gdx.input.getX(), 
				Gdx.graphics.getHeight() - Gdx.input.getY(), 
				vehicleButtonPosX - vehicleButtonRaduis,
				vehicleButtonPosY + vehicleButtonRaduis);
		
		if (Gdx.input.isTouched() && distanceToVehicleButton <= vehicleButtonRaduis) {
			if (engine.getSystem(PlayerControlSystem.class).isInVehicle()) {
				engine.getSystem(PlayerControlSystem.class).exitVehicle();
			} else {
				engine.getSystem(PlayerControlSystem.class).enterVehicle();
			}

		}		
		
		
		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shape.begin(ShapeType.Line);
		shape.setColor(1f, 1f, 1f, 0.5f);
		
		//draw joystick base
		shape.circle(joystickPosX + joystickRadius, joystickPosY + joystickRadius, joystickRadius, 12);

		shape.end();
		
		shape.begin(ShapeType.Filled);		
		
		//draw stick on joystick
		shape.circle(stickX, stickY, joystickRadius / 5, 6);
		shape.line(stickX, stickY, joystickPosX + joystickRadius, joystickPosY + joystickRadius);
		
		//draw shoot button
		shape.circle(shootButtonPosX - shootButtonRadius, shootButtonPosY + shootButtonRadius, shootButtonRadius, 6);
		
		//draw vehicle button
		//TODO: test if player is in vehicle or can get in a vehicle;
		//if (engine.getSystem(PlayerControlSystem.class).isInVehicle() || engine.getSystem(PlayerControlSystem.class).canGetInVehicle()) {
		shape.circle(vehicleButtonPosX - vehicleButtonRaduis, vehicleButtonPosY + vehicleButtonRaduis, vehicleButtonRaduis, 6);
		
		
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

	}
}
