package trinsdar.advancedsolars.items;

import ic2.api.classic.item.ICropAnalyzer;
import ic2.api.classic.item.IEUReader;
import ic2.api.classic.item.IThermometer;
import ic2.api.item.ElectricItem;
import ic2.api.item.IMetalArmor;
import ic2.core.IC2;
import ic2.core.block.generator.tile.TileEntitySolarPanel;
import ic2.core.entity.IC2Potion;
import ic2.core.inventory.base.IHasInventory;
import ic2.core.inventory.transport.IItemTransporter;
import ic2.core.inventory.transport.TransporterManager;
import ic2.core.item.armor.base.ItemElectricArmorBase;
import ic2.core.item.misc.ItemTinCan;
import ic2.core.platform.lang.storage.Ic2InfoLang;
import ic2.core.platform.registry.Ic2Lang;
import ic2.core.platform.textures.Ic2Icons;
import ic2.core.platform.textures.obj.IColorEffectedTexture;
import ic2.core.util.misc.StackUtil;
import ic2.core.util.obj.ToolTipType;
import ic2.core.util.obj.plugins.IBaublesPlugin;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import trinsdar.advancedsolars.AdvancedSolarsClassic;
import trinsdar.advancedsolars.util.AdvancedSolarLang;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ItemArmorAdvancedSolarHelmet extends ItemElectricArmorBase implements IMetalArmor, IColorEffectedTexture, IEUReader, IThermometer, ICropAnalyzer {
    private final int id;
    private final int production;
    private final int lowerProduction;
    private final int energyPerDamage;
    double damageAbsorpationRatio;
    String texture;

    public ItemArmorAdvancedSolarHelmet(String name, int id, int pro, int lowPro, int maxCharge, int maxTransfer, int tier, int energyPerDamage, double damageAbsorpationRatio, String texture) {
        super(-1, EntityEquipmentSlot.HEAD, maxCharge, maxTransfer, tier);
        this.id = id;
        this.production = pro;
        this.lowerProduction = lowPro;
        this.energyPerDamage = energyPerDamage;
        this.damageAbsorpationRatio = damageAbsorpationRatio;
        this.texture = texture;
        this.setUnlocalizedName(name + "SolarHelmet");
        this.setMaxDamage(0);
    }

    @Override
    public void onSortedItemToolTip(ItemStack stack, EntityPlayer player, boolean debugTooltip, List<String> tooltip, Map<ToolTipType, List<String>> sortedTooltip) {
        super.onSortedItemToolTip(stack, player, debugTooltip, tooltip, sortedTooltip);
        sortedTooltip.get(ToolTipType.Shift).add(AdvancedSolarLang.helmetProduction.getLocalizedFormatted(production));
        sortedTooltip.get(ToolTipType.Shift).add(AdvancedSolarLang.helmetLowerPorduction.getLocalizedFormatted(lowerProduction));
        sortedTooltip.get(ToolTipType.Shift).add(Ic2InfoLang.qArmorFeedsYou.getLocalized());
        sortedTooltip.get(ToolTipType.Shift).add(Ic2InfoLang.qArmorRemovePotions.getLocalized());
        sortedTooltip.get(ToolTipType.Shift).add(Ic2InfoLang.qArmorGivesAir.getLocalized());
        NBTTagCompound nbt = StackUtil.getNbtData(stack);
        if (nbt.hasKey("EUReaderUpgrade")) {
            sortedTooltip.get(ToolTipType.Shift).add(Ic2Lang.upgradeEU.getLocalized());
        }
        if (nbt.hasKey("CropUpgrade")) {
            sortedTooltip.get(ToolTipType.Shift).add(Ic2Lang.upgradeCrop.getLocalized());
        }
        if (nbt.hasKey("ThermometerUpgrade")) {
            sortedTooltip.get(ToolTipType.Shift).add(Ic2Lang.upgradeThermo.getLocalized());
        }
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
        if (!IC2.platform.isRendering() && world.provider.hasSkyLight() && world.canSeeSky(player.getPosition())) {
            if (TileEntitySolarPanel.isSunVisible(world, player.getPosition())) {
                chargeInventory(player, production, tier, itemStack);
            } else {
                chargeInventory(player, lowerProduction, tier, itemStack);
            }
        }

        boolean server = IC2.platform.isSimulating();
        if (server) {
            int air = player.getAir();
            if (air < 100 && this.canUseEnergy(itemStack, 1000.0)) {
                player.setAir(air + 200);
                this.useEnergy(itemStack, 1000.0, player);
            }

            if (player.getFoodStats().needFood() && this.canUseEnergy(itemStack, 1000.0)) {
                IItemTransporter trans = TransporterManager.manager.getTransporter(player, true);
                if (trans != null) {
                    ItemStack result = trans.removeItem(ItemTinCan.foodCanFilter, EnumFacing.DOWN, 1, true);
                    if (!result.isEmpty()) {
                        ItemTinCan can = (ItemTinCan) result.getItem();
                        can.onFoodEaten(result, world, player);
                        this.useEnergy(itemStack, 1000.0, player);
                    }
                }
            }

            PotionEffect poison = player.getActivePotionEffect(MobEffects.POISON);
            if (poison != null && this.canUseEnergy(itemStack, 10000 * poison.getAmplifier())) {
                this.useEnergy(itemStack, 10000 * poison.getAmplifier(), player);
                IC2.platform.removePotion(player, MobEffects.POISON);
            }

            PotionEffect radiation = player.getActivePotionEffect(IC2Potion.radiation);
            if (radiation != null && this.canUseEnergy(itemStack, 20000 * radiation.getAmplifier())) {
                this.useEnergy(itemStack, 20000 * radiation.getAmplifier(), player);
                IC2.platform.removePotion(player, IC2Potion.radiation);
            }

            PotionEffect wither = player.getActivePotionEffect(MobEffects.WITHER);
            if (wither != null && this.canUseEnergy(itemStack, 25000 * wither.getAmplifier())) {
                this.useEnergy(itemStack, 25000 * wither.getAmplifier(), player);
                IC2.platform.removePotion(player, MobEffects.WITHER);
            }
        }
    }

    @Override
    public List<Integer> getValidVariants() {
        return Arrays.asList(0);
    }

    @Override
    public String getTexture() {
        return AdvancedSolarsClassic.MODID + texture;
    }

    @Override
    public ItemStack getRepairItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public double getDamageAbsorptionRatio() {
        return damageAbsorpationRatio;
    }

    @Override
    public int getEnergyPerDamage() {
        return energyPerDamage;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(int meta) {
        return Ic2Icons.getTextures("advancedsolars_items")[id];
    }

    public int chargeInventory(EntityPlayer player, int provided, int tier, ItemStack helmet) {
        int i;
        List<NonNullList<ItemStack>> invList = Arrays.asList(player.inventory.armorInventory, player.inventory.offHandInventory, player.inventory.mainInventory);

        if (ElectricItem.manager.getCharge(helmet) != ElectricItem.manager.getMaxCharge(helmet)) {
            int charged = (int) (ElectricItem.manager.charge(helmet, provided, this.tier, false, false));
            provided -= charged;
        } else {
            for (NonNullList inventory : invList) {
                int inventorySize = inventory.size();
                for (i = 0; i < inventorySize && provided > 0; i++) {
                    ItemStack tStack = (ItemStack) inventory.get(i);
                    if (tStack.isEmpty()) continue;
                    int charged = (int) (ElectricItem.manager.charge(tStack, provided, this.tier, false, false));
                    provided -= charged;
                }
            }

            IBaublesPlugin plugin = IC2.loader.getPlugin("baubles", IBaublesPlugin.class);
            if (plugin != null) {
                IHasInventory inv = plugin.getBaublesInventory(player);

                for (i = 0; i < inv.getSlotCount(); ++i) {
                    if (provided <= 0) {
                        break;
                    }
                    provided = (int) (provided - ElectricItem.manager.charge(inv.getStackInSlot(i), provided, tier, false, false));
                }
            }
        }

        return provided;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        switch (this.id) {
            case 11:
                return EnumRarity.UNCOMMON;
            case 12:
                return EnumRarity.RARE;
            case 13:
                return EnumRarity.EPIC;
            default:
                return super.getRarity(stack);
        }
    }

    public boolean isCropAnalyzer(ItemStack stack) {
        return StackUtil.getNbtData(stack).getBoolean("CropUpgrade");
    }

    public boolean isThermometer(ItemStack stack) {
        return StackUtil.getNbtData(stack).getBoolean("ThermometerUpgrade");
    }

    public boolean isEUReader(ItemStack stack) {
        return StackUtil.getNbtData(stack).getBoolean("EUReaderUpgrade");
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return !StackUtil.getNbtData(stack).getBoolean("HideDurability");
    }

    @Override
    public boolean hasColor(ItemStack stack) {
        NBTTagCompound nbt = stack.getSubCompound("display");
        return nbt != null && nbt.hasKey("color");
    }

    @Override
    public boolean hasOverlay(ItemStack stack) {
        return this.hasColor(stack);
    }

    @Override
    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound("display").setInteger("color", color);
    }

    @Override
    public int getColor(ItemStack stack) {
        return StackUtil.getNbtData(stack).getCompoundTag("display").getInteger("color");
    }

    public boolean isMetalArmor(ItemStack itemstack, EntityPlayer player) {
        return true;
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot) {
        if (source == DamageSource.FALL && this.armorType == EntityEquipmentSlot.FEET) {
            int energyPerDamage = this.getEnergyPerDamage();
            int damageLimit = (int) (energyPerDamage > 0 ? ElectricItem.manager.discharge(armor, 2.147483647E9, Integer.MAX_VALUE, true, false, true) / energyPerDamage : 0.0);
            return new ISpecialArmor.ArmorProperties(10, IC2.config.getFloat("electricSuitAbsorbtionScale"), damageLimit);
        } else {
            return super.getProperties(player, armor, source, damage, slot);
        }
    }
}
