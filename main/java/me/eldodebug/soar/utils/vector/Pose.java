package me.eldodebug.soar.utils.vector;

public final class Pose {
	
    public final Matrix4f pose;
    public final Matrix3f normal;

    public Pose(Matrix4f matrix4f, Matrix3f matrix3f) {
        if (matrix4f == null || matrix3f == null) {
            throw new IllegalArgumentException("Matrices cannot be null");
        }
        this.pose = matrix4f;
        this.normal = matrix3f;
    }

    public Matrix4f getPose() {
        return this.pose;
    }

    public Matrix3f getNormal() {
        return this.normal;
    }
}