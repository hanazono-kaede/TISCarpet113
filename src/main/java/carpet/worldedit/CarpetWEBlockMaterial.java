/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpet.worldedit;

import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.PassthroughBlockMaterial;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;

public class CarpetWEBlockMaterial extends PassthroughBlockMaterial {

    private final Material delegate;
    private final IBlockState block;

    public CarpetWEBlockMaterial(Material delegate, IBlockState block, @Nullable BlockMaterial secondary) {
        super(secondary);
        this.delegate = delegate;
        this.block = block;
    }

    @Override
    public boolean isAir() {
        return delegate == Material.AIR || super.isAir();
    }

    @Override
    public boolean isOpaque() {
        return delegate.isOpaque();
    }

    @Override
    public boolean isLiquid() {
        return delegate.isLiquid();
    }

    @Override
    public boolean isSolid() {
        return delegate.isSolid();
    }

    @Override
    public boolean isFragileWhenPushed() {
        return delegate.getPushReaction() == EnumPushReaction.DESTROY;
    }

    @Override
    public boolean isUnpushable() {
        return delegate.getPushReaction() == EnumPushReaction.BLOCK;
    }

    @Override
    public boolean isMovementBlocker() {
        return delegate.blocksMovement();
    }

    @Override
    public boolean isBurnable() {
        return delegate.isFlammable();
    }

    @Override
    public boolean isToolRequired() {
        return !delegate.isToolNotRequired();
    }

    @Override
    public boolean isReplacedDuringPlacement() {
        return delegate.isReplaceable();
    }

}
