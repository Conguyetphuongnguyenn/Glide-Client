package me.eldodebug.soar.utils.vector;

public class Vector2 {
	
    public float x, y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(Vector2 other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot copy null Vector2");
        }
        this.x = other.x;
        this.y = other.y;
    }

    public Vector2 copy() {
        return new Vector2(this.x, this.y);
    }

    public void copy(Vector2 vec) {
        if (vec == null) {
            throw new IllegalArgumentException("Cannot copy null Vector2");
        }
        this.x = vec.x;
        this.y = vec.y;
    }

    public Vector2 add(Vector2 vec) {
        this.x += vec.x;
        this.y += vec.y;
        return this;
    }

    public Vector2 subtract(Vector2 vec) {
        this.x -= vec.x;
        this.y -= vec.y;
        return this;
    }

    public Vector2 div(float amount) {
        this.x /= amount;
        this.y /= amount;
        return this;
    }

    public Vector2 mul(float amount) {
        this.x *= amount;
        this.y *= amount;
        return this;
    }

    public Vector2 normalize() {
        float lengthSquared = this.x * this.x + this.y * this.y;
        
        if (lengthSquared < 1.0E-8F) {
            this.x = 0;
            this.y = 0;
            return this;
        }
        
        float invLength = 1.0F / (float) Math.sqrt(lengthSquared);
        this.x *= invLength;
        this.y *= invLength;
        
        return this;
    }

    public float length() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    @Override
    public String toString() {
        return "Vector2 [x=" + x + ", y=" + y + "]";
    }
}