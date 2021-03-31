# Structure Item Mod (Fabric)
Started as a Fabric port of [structure-item-mod](https://github.com/QuImUfu/structure-item-mod).  
Currently, the only supported version of the mod

## Example Usage
```
/give User structure_item:item{offset: [I; 0, 5, 0],structure: "structure:mine",allowedOn: "minecraft:stone",blacklist:["minecraft:bedrock"]} 1
```  
And tada, you have an item capable of placing structures on right click.  
If the clicked block is Stone and neither Diamond blocks nor Bedrock is in the way, it will place the structure "structure:mine" moved up by 5 blocks. (The low coordinate corner will start 5 blocks above the block you would have placed if this item were a block). Blocks (non structure voids) delete intersecting entities.  
If you leave out "structure", it won't work.  
If you leave out "offset", it will place the structure at the block you clicked at, expanding in your view direction, up and to both sides. if you look up or down, it'll place the structure centered above or below the block you clicked at.  
If you leave out "blacklist", it will replace anything.  
If you leave out "allowedOn", it will allow "placement" on any block.

Any errors made by the creators will be displayed in chat, any errors made by the user will produce a fat massage in the middle of the screen.

If you use this mod, you may want to change the model of the item (currently = stick) and fix the language file.
