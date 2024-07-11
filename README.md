# Structure Item Mod (Fabric)

**Moved to https://merl.dnshome.de/git/QuImUfu/structure-item-mod-fabric**

Started as a Fabric port of [structure-item-mod](https://github.com/QuImUfu/structure-item-mod).  
Currently, the only supported version of the mod

Usage:
-------
This is an item capable of placing structures on right click:
```
structure_item:item{offset:{X:0,Y:5,Z:0},structure:"structure:mine",allowedOn:"minecraft:stone",blacklist:["minecraft:bedrock"],replaceEntities:0,placeEntities:0}
```  
If the clicked block is Stone, no Bedrock and no entity is in the way, it will place the structure "structure:mine" excluding (potentially) saved Entities moved up by 5 blocks. (The low coordinate corner will start 5 blocks above the block you would have placed if this item were a block).

If you leave out "structure", it won't work.  
If you leave out "offset", it will place the structure at the block you clicked at, expanding in your view direction, up and to both sides. if you look up or down, it'll place the structure centred above or below the block you clicked at.  
If you leave out "blacklist", it will replace anything.  
If you leave out "allowedOn", it will allow "placement" on any block.  
If you leave out "replaceEntities" or set it to 1, it will allow placement even if non player entities are in the way of blocks (non structure voids). Those will get deleted.  
If you leave out "placeEntities" or set it to 1, entities saved in the structure will be placed.

Error handling:
---------------
Errors made by the items' creator will be displayed in Chat, any errors made by the user will produce a fat massage in the middle of the screen.

Model & Localization:
---------------------
If you use this mod, you may want to change the model of the item (currently = stick) and edit the language file.
