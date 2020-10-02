package carpet.microtick.enums;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;

import java.util.Map;

public enum BlockUpdateType
{
	NEIGHBOR_CHANGED("NeighborChanged", "Block Update", Constants.NC_UPDATE_ORDER),
	NEIGHBOR_CHANGED_EXCEPT("NeighborChanged Except", "Block Update Except", Constants.NC_UPDATE_ORDER),
	POST_PLACEMENT("PostPlacement", "State Update", Constants.PP_UPDATE_ORDER);

	private final String name, aka;
	private final EnumFacing[] updateOrder;
	private final Map<EnumFacing, String> updateOrderListCache = Maps.newHashMap();
	private final String updateOrderListCacheNoSkip;

	BlockUpdateType(String name, String aka, EnumFacing[] updateOrder)
	{
		this.name = name;
		this.aka = aka;
		this.updateOrder = updateOrder;
		for (EnumFacing enumFacing : EnumFacing.values())
		{
			this.updateOrderListCache.put(enumFacing, this._getUpdateOrderList(enumFacing));
		}
		this.updateOrderListCacheNoSkip = this._getUpdateOrderList(null);

	}

	@Override
	public String toString()
	{
		return this.name;
	}

	private String _getUpdateOrderList(EnumFacing skipSide)
	{
		int counter = 0;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(String.format("aka %s\n", this.aka));
		for (EnumFacing enumfacing : this.updateOrder)
		{
			if (skipSide != enumfacing)
			{
				if (counter > 0)
				{
					stringBuilder.append('\n');
				}
				stringBuilder.append(String.format("%d. %s", (++counter), enumfacing));
			}
		}
		if (skipSide != null)
		{
			stringBuilder.append(String.format("Except: %s\n", skipSide));
		}
		return stringBuilder.toString();
	}

	public String getUpdateOrderList(EnumFacing skipSide)
	{
		return skipSide == null ? this.updateOrderListCacheNoSkip : this.updateOrderListCache.get(skipSide);
	}

	static class Constants
	{
		static final EnumFacing[] NC_UPDATE_ORDER = new EnumFacing[]{EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH};
		static final EnumFacing[] PP_UPDATE_ORDER = Block.UPDATE_ORDER;
	}
}
