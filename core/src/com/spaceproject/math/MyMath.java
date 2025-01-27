package com.spaceproject.math;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.screens.GameScreen;

import java.util.Random;

public abstract class MyMath {
    
    //region seed
    public static long getSeed(float x, float y) {
        return getSeed((int) x, (int) y);
    }
    
    public static long getSeed(int x, int y) {
        //long is 64 bits. store x in upper bits, y in lower bits
        return ((long) x << 32) + y + GameScreen.getGalaxySeed();
    }
    
    public static int test(int x, int y) {
        int largePrime = 198491317;
        return x + (largePrime * y);
    }
    
    public static void testUnique() {
        int distCheck = 10; //Integer.MAX_VALUE;
        for (int x = 0; x <= distCheck; x++) {
            for (int y = 0; y <= distCheck; x++) {
                System.out.println(x + ", " + y + ": " + getSeed(x, y));
                //System.out.println(x + ", " + y + ": " + test(x, y));
            }
        }
    }
    
    public static long getNewGalaxySeed() {
        long newSeed = new Random().nextLong();
        
        if (GameScreen.isDebugMode) {
            newSeed = 1; //test seed
        }
        Gdx.app.log("MyMath", "galaxy seed: " + newSeed);
        
        return newSeed;
    }
    //endregion
    
    //region vectors and angles
    private static final Vector2 TEMP_VEC = new Vector2();
    public static Vector2 vector(float direction, float magnitude) {
        float dx = MathUtils.cos(direction) * magnitude;
        float dy = MathUtils.sin(direction) * magnitude;
        return TEMP_VEC.set(dx, dy);
    }
    
    public static Vector2 logVec(Vector2 vec, float scale) {
        float length = (float) Math.log(vec.len()) * scale;
        return vector(vec.angleRad(), length);
    }
    
    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }
    
    public static float angleTo(float x1, float y1, float x2, float y2) {
        return (float) -(Math.atan2(x2 - x1, y2 - y1)) - 1.57f;
    }
   
    public static float angleTo(Vector2 a, Vector2 b) {
        return angleTo(a.x, a.y, b.x, b.y);
    }
    
    public static float angle2(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(x2 - x1, y2 - y1);
    }
    
    public static float vectorToAngle(Vector2 vector) {
        return (float)Math.atan2(-vector.x, vector.y);
    }
    
    public static float getAngularImpulse(Body body, float targetAngle, float delta) {
        //https://www.iforce2d.net/b2dtut/rotate-to-angle
        float nextAngle = body.getAngle() + body.getAngularVelocity() * delta;
        float totalRotation = targetAngle - nextAngle;
        while (totalRotation < -180 * MathUtils.degRad) totalRotation += 360 * MathUtils.degRad;
        while (totalRotation >  180 * MathUtils.degRad) totalRotation -= 360 * MathUtils.degRad;
        float desiredAngularVelocity = totalRotation * 60;
        float change = 50 * MathUtils.degRad; //max degrees of rotation per time step
        desiredAngularVelocity = Math.min(change, Math.max(-change, desiredAngularVelocity));
        float impulse = body.getInertia() * desiredAngularVelocity;
        return impulse;
    }
    //endregion
    
    /**
     * Returns the percentage of the range max-min that corresponds to value.
     *
     * @param min
     * @param max
     * @param value
     * @return interpolant value within the range [min, max].
     */
    public static float inverseLerp(float min, float max, float value) {
        return (value - min) / (max - min);
    }
    
    
    //region formatting
    /** Convert bytes to a human readable format.
     * Eg: 26673720 -> 25.44MB
     * Credit: icza, stackoverflow.com/questions/3758606/#24805871
     *
     * @param bytes
     * @return bytes with SI postfix
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.2f%sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
    
    /** Convert milliseconds to hours, minutes, seconds
     * https://stackoverflow.com/a/21701635
     */
    static StringBuilder builder = new StringBuilder();
    public static String formatDuration(final long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        
        builder.setLength(0);//clear
        builder.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : hours);
        builder.append(":");
        builder.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : minutes);
        builder.append(":");
        builder.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : seconds);
        return builder.toString();
    }
    
    public static String formatVector2(Vector2 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal);
    }
    
    public static String formatVector3(Vector3 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal) + ", " + round(vec.z, decimal);
    }
    
    public static String formatVector3as2(Vector3 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal);
    }
    
    /**
     * Round value with specified precision.
     *
     * @param value
     * @param precision number of decimal points
     * @return rounded value
     */
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    //endregion
    
    
    public static long fibonacci(int n) {
        if (n == 0) {
            return 0;
        }
        if (n <= 2) {
            return 1;
        }
        
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
}
