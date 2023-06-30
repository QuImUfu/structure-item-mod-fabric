package quimufu.structure_item;

import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.SimpleDefaultedRegistry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

import static quimufu.structure_item.StructureItemMod.LOGGER;

public class MyItem extends Item {
    static Item.Settings p = new FabricItemSettings().fireproof().rarity(Rarity.RARE).maxCount(1);

    public MyItem() {
        super(p);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> texts, TooltipContext tooltipFlag) {
        if (!itemStack.hasNbt() || !itemStack.getNbt().contains("structure", 8)) {
            texts.add((Text.translatable("item.structure_item.item.tooltip.tag.invalid")).formatted(Formatting.RED));
        } else {
            NbtCompound tag = itemStack.getNbt();
            if (tooltipFlag.isAdvanced()) {
                texts.add(Text.translatable("item.structure_item.item.tooltip.structure"));
                texts.add(Text.literal("  " + tag.getString("structure")));
                if (tag.contains("allowedOn", 8)) {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.allowed.on"));
                    texts.add(Text.literal("  " + tag.getString("allowedOn")));
                }
                if (tag.contains("offset", 10)) {
                    BlockPos offset = NbtHelper.toBlockPos(tag.getCompound("offset"));
                    texts.add(Text.translatable("item.structure_item.item.tooltip.fixed.offset"));
                    Text c = Text.translatable("item.structure_item.item.tooltip.xyz",
                            Text.literal(String.valueOf(offset.getX())),
                            Text.literal(String.valueOf(offset.getY())),
                            Text.literal(String.valueOf(offset.getZ())));
                    texts.add(c);
                } else {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.dynamic.offset"));
                }
                if (tag.contains("blacklist", 9)) {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.blacklist"));
                    NbtList bl = tag.getList("blacklist", 8);
                    int i = 0;
                    for ( NbtElement entry : bl) {
                        texts.add(Text.literal("  " + entry.asString()));
                        i++;
                        if (i == 4) {
                            texts.add(Text.translatable("item.structure_item.item.tooltip.blacklist.more",
                                    Text.literal(String.valueOf(bl.size() - i))));
                        }
                    }
                }
                if (!tag.contains("replaceEntities", 99) || tag.getBoolean("replaceEntities")) {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.replaceEntities"));
                } else {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.doNotReplaceEntities"));
                }
                if (!tag.contains("placeEntities", 99) || tag.getBoolean("placeEntities")) {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.placeEntities"));
                } else {
                    texts.add(Text.translatable("item.structure_item.item.tooltip.doNotPlaceEntities"));
                }
            }
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext c) {
        if (!c.getWorld().isClient) {
            ServerPlayerEntity player;
            if (c.getPlayer() instanceof ServerPlayerEntity) {
                player = ((ServerPlayerEntity) c.getPlayer());
            } else {
                player = null;
            }
            NbtCompound tag = c.getStack().getNbt();
            if (tag == null) {
                Text message =
                        Text.translatable("items.structure.spawner.no.tag");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            Block allowed = null;
            if (tag.contains("allowedOn", 8)) {
                String allowedOn = tag.getString("allowedOn");
                allowed = getBlock(allowedOn);
                if (allowed == null) {
                    Text message =
                            Text.translatable("items.structure.spawner.invalid.block",
                                    Text.literal(allowedOn));
                    sendPlayerChat(player, message);
                    return ActionResult.FAIL;
                }
            }
            Block current = c.getWorld().getBlockState(c.getBlockPos()).getBlock();

            if (allowed != null && !current.equals(allowed)) {
                Text currentName = Text.translatable(current.getTranslationKey());
                Text allowedName = Text.translatable(allowed.getTranslationKey());
                Text message =
                        Text.translatable("items.structure.spawner.invalid.block.clicked",
                                currentName, allowedName);
                sendPlayer(player, message);
                return ActionResult.FAIL;
            }
            if (!tag.contains("structure", 8)) {
                LOGGER.info("No structure name set");
                Text message =
                        Text.translatable("items.structure.spawner.no.structure");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            String structureName = tag.getString("structure");
            Identifier structureResourceID = Identifier.tryParse(structureName);
            if (structureResourceID == null) {
                Text message =
                        Text.translatable("items.structure.spawner.invalid.structure.name");
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            Optional<StructureTemplate> xOpt = ((ServerWorld) c.getWorld()).getStructureTemplateManager().getTemplate(structureResourceID);
            if (xOpt.isEmpty()) {
                Text message =
                        Text.translatable("items.structure.spawner.structure.nonexistent",
                                Text.literal(structureResourceID.toString()));
                sendPlayerChat(player, message);
                return ActionResult.FAIL;
            }
            StructureTemplate x = xOpt.get();


            BlockPos loc = c.getBlockPos().offset(c.getSide());
            if (tag.contains("offset", 10)) {
                BlockPos offset = NbtHelper.toBlockPos(tag.getCompound("offset"));
                loc = loc.add(offset);
            } else if (c.getPlayer() != null) {
                Direction direction = Direction.getEntityFacingOrder(c.getPlayer())[0];
                Vec3i size = x.getSize();
                loc = loc.add(getDirectionalOffset(direction, size));
            } else {
                LOGGER.info("No player & no offset");
            }

            MyPlacementSettings ps = (new MyPlacementSettings());
            if (tag.contains("replaceEntities", 99)) {
                ps.setReplaceEntities(tag.getBoolean("replaceEntities"));
            }
            if (tag.contains("placeEntities", 99)) {
                ps.setIgnoreEntities(!tag.getBoolean("placeEntities"));
            }
            if (tag.contains("blacklist", 9)) {
                NbtList bl = tag.getList("blacklist", 8);
                List<Block> blacklist = Lists.newArrayList();
                for (NbtElement b : bl) {
                    Block block = getBlock(b.asString());
                    if (block != null) {
                        blacklist.add(block);
                    } else {
                        Text message =
                                Text.translatable("items.structure.spawner.invalid.block",
                                        Text.literal(b.asString()));
                        sendPlayerChat(player, message);
                    }

                }
                ps.forbidOverwrite(blacklist);
            }
            ps.setWorld(c.getWorld())
                    .setSize(x.getSize())
                    .setMirror(BlockMirror.NONE)
                    .setRotation(BlockRotation.NONE);
            boolean success = false;
            try {
                if(x.place((ServerWorld)c.getWorld(), loc, loc, ps, c.getWorld().getRandom(), 2))
                    success = true;
            } catch (NullPointerException ignored) {
            }
            if (success) {
                c.getStack().decrement(1);
                return ActionResult.SUCCESS;
            }
            Text message =
                    Text.translatable("items.structure.spawner.invalid.location");
            sendPlayer(player, message);
            return ActionResult.FAIL;
        }
        return ActionResult.FAIL;
    }

    private static void sendPlayer(ServerPlayerEntity player, Text message) {
        if (player == null)
            return;
        ServerPlayNetworkHandler connection = player.networkHandler;
        SubtitleS2CPacket packet = new SubtitleS2CPacket(message);
        connection.sendPacket(packet);
        TitleS2CPacket titleS2CPacket = new TitleS2CPacket(Text.literal(""));
        connection.sendPacket(titleS2CPacket);
    }

    private static void sendPlayerChat(ServerPlayerEntity player, Text message) {
        if (player != null)
            player.sendMessage(message, false);
        LOGGER.info(message.getContent());
    }

    private BlockPos getDirectionalOffset(Direction direction, Vec3i size) {
        BlockPos loc = new BlockPos(0, 0, 0);
        switch (direction) {
            case WEST -> {
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() - 1);
            }
            case EAST -> //positive x
                    loc = loc.offset(Direction.NORTH, size.getZ() / 2);
            case NORTH -> {
                loc = loc.offset(Direction.NORTH, size.getZ() - 1);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
            }
            case SOUTH -> //positive z
                    loc = loc.offset(Direction.WEST, size.getX() / 2);
            case UP -> {    //positive y
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.UP);
            }
            case DOWN -> {
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.DOWN, size.getY());
            }
        }
        return loc;
    }

    private Block getBlock(String loc) {
        Identifier location = Identifier.tryParse(loc);
        DefaultedRegistry<Block> blocks = Registries.BLOCK;
        if (location == null) {
            return null;
        }
        return blocks.get(location);
    }
}
