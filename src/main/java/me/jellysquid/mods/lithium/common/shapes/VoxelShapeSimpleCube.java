package me.jellysquid.mods.lithium.common.shapes;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.AxisRotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapePart;
import net.minecraft.util.math.shapes.VoxelShapes;

import java.util.List;

// optimized VoxelShapeCube class
/**
 * An efficient implementation of {@link VoxelShape} for a shape with one simple cuboid. Since there are only ever two
 * vertices in a single cuboid (the start and end points), we can eliminate needing to iterate over voxels and to find
 * vertices through using simple comparison logic to pick between either the start or end point.
 * <p>
 * Additionally, the function responsible for determining shape penetration has been simplified and optimized by taking
 * advantage of the fact that there is only ever one voxel in a simple cuboid shape, greatly speeding up collision
 * handling in most cases as block shapes are often nothing more than a single cuboid.
 */
public class VoxelShapeSimpleCube extends VoxelShape implements VoxelShapeCaster {
    static final double EPSILON = 1.0E-7D;

    final double minX, minY, minZ, maxX, maxY, maxZ;
    public final boolean isTiny;

    public VoxelShapeSimpleCube(VoxelShapePart voxels, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(voxels);

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        this.isTiny =
                this.minX + 3 * EPSILON >= this.maxX ||
                this.minY + 3 * EPSILON >= this.maxY ||
                this.minZ + 3 * EPSILON >= this.maxZ;
    }

    @Override
    public VoxelShape offset(double x, double y, double z) {
        return new VoxelShapeSimpleCube(this.part, this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    // mcp: protected double func_212431_a(AxisRotation p_212431_1_, AxisAlignedBB p_212431_2_, double p_212431_3_)
    // yarm: public double calculateMaxDistance(AxisRotation cycleDirection, AxisAlignedBB box, double maxDist)
    @Override
    public double func_212431_a(AxisRotation cycleDirection, AxisAlignedBB box, double maxDist) {
        if (Math.abs(maxDist) < EPSILON) {
            return 0.0D;
        }

        double penetration = this.calculatePenetration(cycleDirection, box, maxDist);

        if ((penetration != maxDist) && this.intersects(cycleDirection, box)) {
            return penetration;
        }

        return maxDist;
    }

    private double calculatePenetration(AxisRotation dir, AxisAlignedBB box, double maxDist) {
        switch (dir) {
            case NONE:
                return VoxelShapeSimpleCube.calculatePenetration(this.minX, this.maxX, box.minX, box.maxX, maxDist);
            case FORWARD:
                return VoxelShapeSimpleCube.calculatePenetration(this.minZ, this.maxZ, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return VoxelShapeSimpleCube.calculatePenetration(this.minY, this.maxY, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }

    boolean intersects(AxisRotation dir, AxisAlignedBB box) {
        switch (dir) {
            case NONE:
                return lessThan(this.minY, box.maxY) && lessThan(box.minY, this.maxY) && lessThan(this.minZ, box.maxZ) && lessThan(box.minZ, this.maxZ);
            case FORWARD:
                return lessThan(this.minX, box.maxX) && lessThan(box.minX, this.maxX) && lessThan(this.minY, box.maxY) && lessThan(box.minY, this.maxY);
            case BACKWARD:
                return lessThan(this.minZ, box.maxZ) && lessThan(box.minZ, this.maxZ) && lessThan(this.minX, box.maxX) && lessThan(box.minX, this.maxX);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static double calculatePenetration(double a1, double a2, double b1, double b2, double maxDist) {
        double penetration;

        if (maxDist > 0.0D) {
            penetration = a1 - b2;

            if ((penetration < -EPSILON) || (maxDist < penetration)) {
                //already far enough inside this shape to not collide with the surface or
                //outside the shape and still far enough away for no collision at all
                return maxDist;
            }
            //allow moving up to the shape but not into it. This also includes going backwards by at most EPSILON.
        } else {
            //whole code again, just negated for the other direction
            penetration = a2 - b1;

            if ((penetration > EPSILON) || (maxDist > penetration)) {
                return maxDist;
            }
        }

        return penetration;
    }

    @Override
    public List<AxisAlignedBB> toBoundingBoxList() {
        return Lists.newArrayList(this.getBoundingBox());
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public double getStart(EnumFacing.Axis axis) {
        return axis.getCoordinate(this.minX, this.minY, this.minZ);
    }

    @Override
    public double getEnd(EnumFacing.Axis axis) {
        return axis.getCoordinate(this.maxX, this.maxY, this.maxZ);
    }

    @Override
    protected double getValueUnchecked(EnumFacing.Axis axis, int index) {
        if ((index < 0) || (index > 1)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        switch (axis) {
            case X:
                return (index == 0) ? this.minX : this.maxX;
            case Y:
                return (index == 0) ? this.minY : this.maxY;
            case Z:
                return (index == 0) ? this.minZ : this.maxZ;
        }

        throw new IllegalArgumentException();
    }

    @Override
    protected DoubleList getValues(EnumFacing.Axis axis) {
        switch (axis) {
            case X:
                return DoubleArrayList.wrap(new double[]{this.minX, this.maxX});
            case Y:
                return DoubleArrayList.wrap(new double[]{this.minY, this.maxY});
            case Z:
                return DoubleArrayList.wrap(new double[]{this.minZ, this.maxZ});
        }

        throw new IllegalArgumentException();
    }

    @Override
    protected boolean contains(double x, double y, double z) {
        return (x >= this.minX) && (x < this.maxX) && (y >= this.minY) && (y < this.maxY) && (z >= this.minZ) && (z < this.maxZ);
    }

    @Override
    public boolean isEmpty() {
        return (this.minX >= this.maxX) || (this.minY >= this.maxY) || (this.minZ >= this.maxZ);
    }

    @Override
    protected int getClosestIndex(EnumFacing.Axis axis, double coord) {
        if (coord < this.getStart(axis)) {
            return -1;
        }

        if (coord >= this.getEnd(axis)) {
            return 1;
        }

        return 0;
    }

    private static boolean lessThan(double a, double b) {
        return (a + EPSILON) < b;
    }

    @Override
    public boolean intersects(AxisAlignedBB box, double x, double y, double z) {
        return (box.minX + EPSILON < (this.maxX + x)) && (box.maxX - EPSILON > (this.minX + x)) &&
                (box.minY + EPSILON < (this.maxY + y)) && (box.maxY - EPSILON > (this.minY + y)) &&
                (box.minZ + EPSILON < (this.maxZ + z)) && (box.maxZ - EPSILON > (this.minZ + z));
    }


    @Override
    public void forEachBox(VoxelShapes.LineConsumer boxConsumer) {
        boxConsumer.consume(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
