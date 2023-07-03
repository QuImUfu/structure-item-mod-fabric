package quimufu.structure_item;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.Arrays;
import java.util.Map;

public class StructureOffsetSettings {

    Expression[] xyz = new Expression[3];
    String[] xyzS = new String[3];
    boolean[] rel = new boolean[3];
    static Function DIR_SELECT = new Function("dirSelect", 7) {
        @Override
        public double apply(double... args) {
            return args[(int) (Math.round(args[0]) + 1L)];
        }
    };
    private BlockRotation rotation;

    public static StructureOffsetSettings ofTag(NbtCompound offsetTag) {
        StructureOffsetSettings settings = new StructureOffsetSettings();
        String x1 = offsetTag.getString("x");
        unwrap(x1, settings, 0);
        String y1 = offsetTag.getString("y");
        unwrap(y1, settings, 1);
        String z1 = offsetTag.getString("z");
        unwrap(z1, settings, 2);
        return settings;
    }

    private static void unwrap(String expr, StructureOffsetSettings settings, int x) {
        if (expr.startsWith("~")) {
            settings.rel[x] = true;
            expr = expr.substring(1);
        }
        if (expr.isBlank()) {
            expr = "0";
        }
        settings.xyzS[x] = expr;
        settings.xyz[x] = new ExpressionBuilder(expr)
                .variables("sizeX", "sizeY", "sizeZ", "dir")
                .function(DIR_SELECT)
                .build();
    }

    public static StructureOffsetSettings dynamic() {
        StructureOffsetSettings settings = new StructureOffsetSettings();
        for (int i = 0; i < 3; i++) {
            settings.xyz[i] = new ExpressionBuilder("0").build();
            settings.xyzS[i] = "0";
        }
        Arrays.fill(settings.rel, true);
        return settings;
    }

    public Vec3i getEffective(Direction direction, Vec3i size) {
        int[] xyzI = new int[3];
        for (int i = 0; i < 3; i++) {
            xyz[i].setVariables(Map.of("sizeX", (double) size.getX(),
                    "sizeY", (double) size.getY(),
                    "sizeZ", (double) size.getZ(),
                    "dir", (double) direction.getId()
            ));
            xyzI[i] = MathHelper.floor(xyz[i].evaluate());
        }
        Vec3i dynamicOffset = getDirectionalOffset(direction, size);

        int[] xyzDynamicI = {dynamicOffset.getX(), dynamicOffset.getY(), dynamicOffset.getZ()};
        for (int i = 0; i < 3; i++) {
            if (rel[i]) {
                xyzI[i] += xyzDynamicI[i];
            }
        }
        return new Vec3i(xyzI[0], xyzI[1], xyzI[2]);
    }

    private Vec3i getDirectionalOffset(Direction direction, Vec3i size) {
        BlockPos loc = new BlockPos(0, 0, 0);
        switch (rotation) {
            case NONE:
                switch (direction) {
                    case WEST:
                        loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                        return loc.offset(Direction.WEST, size.getX() - 1);
                    case EAST://positive x
                        return loc.offset(Direction.NORTH, size.getZ() / 2);
                    case NORTH:
                        loc = loc.offset(Direction.NORTH, size.getZ() - 1);
                        return loc.offset(Direction.WEST, size.getX() / 2);
                    case SOUTH://positive z
                        return loc.offset(Direction.WEST, size.getX() / 2);
                    case UP://positive y
                        loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                        loc = loc.offset(Direction.WEST, size.getX() / 2);
                        return loc.offset(Direction.UP);
                    case DOWN:
                        loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                        loc = loc.offset(Direction.WEST, size.getX() / 2);
                        return loc.offset(Direction.DOWN, size.getY());
                }
            case CLOCKWISE_90:
                switch (direction) {
                    case WEST:
                        return loc.offset(Direction.NORTH, size.getX() / 2);
                    case EAST://positive x
                        loc = loc.offset(Direction.NORTH, size.getX() / 2);
                        return loc.offset(Direction.EAST, size.getZ() - 1);
                    case NORTH:
                        loc = loc.offset(Direction.NORTH, size.getX() - 1);
                        return loc.offset(Direction.EAST, size.getZ() / 2);
                    case SOUTH://positive z
                        return loc.offset(Direction.EAST, size.getZ() / 2);
                    case UP://positive y
                        loc = loc.offset(Direction.NORTH, size.getX() / 2);
                        loc = loc.offset(Direction.EAST, size.getZ() / 2);
                        return loc.offset(Direction.UP);
                    case DOWN:
                        loc = loc.offset(Direction.NORTH, size.getX() / 2);
                        loc = loc.offset(Direction.EAST, size.getZ() / 2);
                        return loc.offset(Direction.DOWN, size.getY());
                }
            case CLOCKWISE_180:
                switch (direction) {
                    case WEST:
                        return loc.offset(Direction.SOUTH, size.getX() / 2);
                    case EAST://positive x
                        loc = loc.offset(Direction.SOUTH, size.getZ() / 2);
                        return loc.offset(Direction.EAST, size.getX() - 1);
                    case NORTH:
                        return loc.offset(Direction.EAST, size.getX() / 2);
                    case SOUTH://positive z
                        loc = loc.offset(Direction.EAST, size.getX() / 2);
                        return loc.offset(Direction.SOUTH, size.getZ() - 1);
                    case UP://positive y
                        loc = loc.offset(Direction.SOUTH, size.getZ() / 2);
                        loc = loc.offset(Direction.EAST, size.getX() / 2);
                        return loc.offset(Direction.UP);
                    case DOWN:
                        loc = loc.offset(Direction.SOUTH, size.getZ() / 2);
                        loc = loc.offset(Direction.EAST, size.getX() / 2);
                        return loc.offset(Direction.DOWN, size.getY());
                }
            case COUNTERCLOCKWISE_90:
                switch (direction) {
                    case WEST:
                        loc = loc.offset(Direction.SOUTH, size.getX() / 2);
                        return loc.offset(Direction.WEST, size.getZ() - 1);
                    case EAST://positive x
                        return loc.offset(Direction.SOUTH, size.getX() / 2);
                    case NORTH:
                        return loc.offset(Direction.WEST, size.getZ() / 2);
                    case SOUTH://positive z
                        loc = loc.offset(Direction.SOUTH, size.getX() - 1);
                        return loc.offset(Direction.WEST, size.getZ() / 2);
                    case UP://positive y
                        loc = loc.offset(Direction.SOUTH, size.getX() / 2);
                        loc = loc.offset(Direction.WEST, size.getZ() / 2);
                        return loc.offset(Direction.UP);
                    case DOWN:
                        loc = loc.offset(Direction.SOUTH, size.getX() / 2);
                        loc = loc.offset(Direction.WEST, size.getZ() / 2);
                        return loc.offset(Direction.DOWN, size.getY());
                }
        }
        return loc;
    }

    public void setRotation(BlockRotation rotation) {
        this.rotation = rotation;
    }
}
