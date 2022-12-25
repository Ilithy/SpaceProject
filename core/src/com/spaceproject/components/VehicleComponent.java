package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;

public class VehicleComponent implements Component {
    
    public Entity driver;
    
    public float thrust; //move to engine component as sub entity for ship?
    
    public Rectangle dimensions;
    
}
