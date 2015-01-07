/*
 * License (MIT)
 *
 * Copyright (c) 2014-2015 Granite Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.granitepowered.granite.impl.entity.player;

import static org.granitepowered.granite.utils.MinecraftUtils.graniteToMinecraftChatComponent;
import static org.granitepowered.granite.utils.MinecraftUtils.unwrap;
import static org.granitepowered.granite.utils.MinecraftUtils.wrap;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;
import com.google.common.base.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.granitepowered.granite.Granite;
import org.granitepowered.granite.impl.entity.living.GraniteLivingBase;
import org.granitepowered.granite.impl.text.chat.GraniteChatType;
import org.granitepowered.granite.impl.text.message.GraniteMessage;
import org.granitepowered.granite.impl.text.message.GraniteMessageBuilder;
import org.granitepowered.granite.mappings.Mappings;
import org.granitepowered.granite.mc.MCEntityPlayerMP;
import org.granitepowered.granite.mc.MCFoodStats;
import org.granitepowered.granite.mc.MCGameProfile;
import org.granitepowered.granite.mc.MCItemStack;
import org.granitepowered.granite.mc.MCPacket;
import org.granitepowered.granite.mc.MCPlayerCapabilities;
import org.granitepowered.granite.utils.MinecraftUtils;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.persistence.DataSource;
import org.spongepowered.api.service.persistence.data.DataContainer;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.message.Message;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.text.title.Titles;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class GranitePlayer extends GraniteLivingBase<MCEntityPlayerMP> implements Player {

    private Optional<Message> displayName = Optional.absent();

    public GranitePlayer(MCEntityPlayerMP obj) {
        super(obj);
    }

    @Override
    public Message getDisplayName() {
        return displayName.or(new GraniteMessageBuilder.GraniteTextMessageBuilder("").content(getName()).build());
    }

    @Override
    public boolean getAllowFlight() {
        return getPlayerCapabilities().fieldGet$allowFlying();
    }

    @Override
    public void setAllowFlight(boolean allowFlight) {
        getPlayerCapabilities().fieldSet$allowFlying(allowFlight);
    }

    @Override
    public Locale getLocale() {
        String translator = this.obj.fieldGet$translator().replace("_", "-");
        return Locale.forLanguageTag(translator);
    }

    @Override
    public void sendMessage(ChatType type, String... strings) {
        List<Message> messages = new ArrayList<>();
        for (String string : strings) {
            messages.add(new GraniteMessageBuilder.GraniteTextMessageBuilder("").content(string).build());
        }

        sendMessage(type, messages);
    }

    @Override
    public void sendTitle(Title title) {
        if (title.isReset() || title.isClear()) {
            Enum type = MinecraftUtils.enumValue(Mappings.getClass("S45PacketTitle$Type"), title.isReset() ? 4 : 3);

            MCPacket packet = MinecraftUtils.instantiate(Mappings.getClass("S45PacketTitle"),
                                                         new Class[]{Mappings.getClass("S45PacketTitle$Type"), Mappings.getClass("IChatComponent")},
                                                         type, null);

            sendPacket(packet);
        }

        if (title.getFadeIn().isPresent() || title.getStay().isPresent() || title.getFadeOut().isPresent()) {
            int fadeIn = title.getFadeIn().or(-1);
            int stay = title.getFadeIn().or(-1);
            int fadeOut = title.getFadeIn().or(-1);

            MCPacket packet = MinecraftUtils.instantiate(Mappings.getClass("S45PacketTitle"),
                                                         new Class[]{int.class, int.class, int.class},
                                                         fadeIn, stay, fadeOut);

            sendPacket(packet);
        }

        if (title.getTitle().isPresent()) {
            Enum type = MinecraftUtils.enumValue(Mappings.getClass("S45PacketTitle$Type"), 0);

            MCPacket packet = MinecraftUtils.instantiate(Mappings.getClass("S45PacketTitle"),
                                                         new Class[]{Mappings.getClass("S45PacketTitle$Type"), Mappings.getClass("IChatComponent")},
                                                         type, MinecraftUtils.graniteToMinecraftChatComponent(title.getTitle().get()));

            sendPacket(packet);
        }

        if (title.getTitle().isPresent()) {
            Enum type = MinecraftUtils.enumValue(Mappings.getClass("S45PacketTitle$Type"), 1);

            MCPacket packet = MinecraftUtils.instantiate(Mappings.getClass("S45PacketTitle"),
                                                         new Class[]{Mappings.getClass("S45PacketTitle$Type"), Mappings.getClass("IChatComponent")},
                                                         type, MinecraftUtils.graniteToMinecraftChatComponent(title.getSubtitle().get()));

            sendPacket(packet);
        }
    }

    @Override
    public void resetTitle() {
        sendTitle(Titles.update().reset().build());
    }

    @Override
    public void clearTitle() {
        sendTitle(Titles.update().clear().build());
    }

    @Override
    public Optional<ItemStack> getHelmet() {
        return Optional.fromNullable((ItemStack) wrap(obj.fieldGet$inventory().fieldGet$armorInventory()[3]));
    }

    @Override
    public void setHelmet(@Nullable ItemStack helmet) {
        obj.fieldGet$inventory().fieldGet$armorInventory()[3] = helmet == null ? null : (MCItemStack) unwrap(helmet);
    }

    @Override
    public Optional<ItemStack> getChestplate() {
        return Optional.fromNullable((ItemStack) wrap(obj.fieldGet$inventory().fieldGet$armorInventory()[2]));
    }

    @Override
    public void setChestplate(@Nullable ItemStack chestplate) {
        obj.fieldGet$inventory().fieldGet$armorInventory()[2] = chestplate == null ? null : (MCItemStack) unwrap(chestplate);
    }

    @Override
    public Optional<ItemStack> getLeggings() {
        return Optional.fromNullable((ItemStack) wrap(obj.fieldGet$inventory().fieldGet$armorInventory()[1]));
    }

    @Override
    public void setLeggings(@Nullable ItemStack leggings) {
        obj.fieldGet$inventory().fieldGet$armorInventory()[1] = leggings == null ? null : (MCItemStack) unwrap(leggings);
    }

    @Override
    public Optional<ItemStack> getBoots() {
        return Optional.fromNullable((ItemStack) wrap(obj.fieldGet$inventory().fieldGet$armorInventory()[0]));
    }

    @Override
    public void setBoots(@Nullable ItemStack boots) {
        obj.fieldGet$inventory().fieldGet$armorInventory()[0] = boots == null ? null : (MCItemStack) unwrap(boots);
    }

    @Override
    public Optional<ItemStack> getItemInHand() {
        return Optional.fromNullable((ItemStack) wrap(obj.fieldGet$inventory().getCurrentItem()));
    }

    @Override
    public void setItemInHand(@Nullable ItemStack itemInHand) {
        obj.fieldGet$inventory().fieldGet$armorInventory()[0] = itemInHand == null ? null : (MCItemStack) unwrap(itemInHand);
    }

    @Override
    public String getName() {
        return getGameProfile().fieldGet$name();
    }

    @Override
    public boolean hasJoinedBefore() {
        return true;
    }

    @Override
    public Date getFirstPlayed() {
        // TODO: Not sure if possible
        throw new NotImplementedException("");
    }

    @Override
    public Date getLastPlayed() {
        // TODO: Not sure if possible
        throw new NotImplementedException("");
    }

    @Override
    public boolean isBanned() {
        // TODO: Check SCM.bannedPlayers
        throw new NotImplementedException("");
    }

    @Override
    public boolean isWhitelisted() {
        // TODO: Check SCM.whiteListedPlayers
        throw new NotImplementedException("");
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public Optional<Player> getPlayer() {
        return Optional.of((Player) this);
    }

    @Override
    public void spawnParticles(ParticleEffect particleEffect, Vector3d position) {
        this.getWorld().spawnParticles(particleEffect, position);
    }

    @Override
    public void spawnParticles(ParticleEffect particleEffect, Vector3d position, int radius) {
        this.getWorld().spawnParticles(particleEffect, position, radius);
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume) {
        this.getWorld().playSound(sound, position, volume, 1.0F);
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume, double pitch) {
        this.getWorld().playSound(sound, position, volume, pitch);
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume, double pitch, double minVolume) {
        this.getWorld().playSound(sound, position, volume, pitch, minVolume);
    }

    @Override
    public float getHunger() {
        return getFoodStats().fieldGet$foodLevel();
    }

    @Override
    public void setHunger(float hunger) {
        getFoodStats().fieldSet$foodLevel((int) hunger);
    }

    @Override
    public float getSaturation() {
        return getFoodStats().fieldGet$foodSaturationLevel();
    }

    @Override
    public void setSaturation(float saturation) {
        getFoodStats().fieldSet$foodSaturationLevel(saturation);
    }

    @Override
    public boolean isViewingInventory() {
        return obj.fieldGet$openContainer() != null;
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<T> projectileClass) {
        // TODO: Projectile API
        throw new NotImplementedException("");
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<T> projectileClass, Vector3f velocity) {
        // TODO: Projectile API
        throw new NotImplementedException("");
    }

    @Override
    public void sendMessage(String... messages) {
        sendMessage(ChatTypes.CHAT, messages);
    }

    @Override
    public void sendMessage(Iterable<Message> messages) {
        sendMessage(ChatTypes.CHAT, messages);
    }

    @Override
    public boolean isAiEnabled() {
        return false;
    }

    @Override
    public void setAiEnabled(boolean aiEnabled) {
        throw new UnsupportedOperationException("Player doesn't have AI");
    }

    @Override
    public void sendMessage(Message... messages) {
        sendMessage(ChatTypes.CHAT, messages);
    }

    @Override
    public void sendMessage(ChatType type, Message... message) {
        sendMessage(type, Arrays.asList(message));
    }

    @Override
    public void sendMessage(ChatType type, Iterable<Message> messages) {
        try {
            Message message;
            if (messages instanceof GraniteMessage) {
                message = (Message) messages;
            } else {
                message = new GraniteMessageBuilder.GraniteTextMessageBuilder("").content("").append(messages).build();
            }

            MCPacket
                    packet =
                    (MCPacket) Mappings.getClass("S02PacketChat").getConstructor(Mappings.getClass("IChatComponent"), byte.class).newInstance(
                            graniteToMinecraftChatComponent(message),
                            (byte) ((GraniteChatType) type).getId()
                    );
            sendPacket(packet);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Granite.error(e);
        }
    }

    public MCGameProfile getGameProfile() {
        return obj.fieldGet$gameProfile();
    }

    public MCPlayerCapabilities getPlayerCapabilities() {
        return obj.fieldGet$capabilities();
    }

    public MCFoodStats getFoodStats() {
        return obj.fieldGet$foodStats();
    }

    public void sendPacket(MCPacket packet) {
        obj.fieldGet$playerNetServerHandler().sendPacket(packet);
    }

    @Override
    public boolean isLeashed() {
        return false;
    }

    @Override
    public void setLeashed(boolean leashed) {
        // TODO: Figure out what to do if this is a player
        throw new NotImplementedException("");
    }

    @Override
    public boolean getCanPickupItems() {
        return true;
    }

    @Override
    public void setCanPickupItems(boolean canPickupItems) {
        // TODO: Figure out what to do if this is a player
        throw new NotImplementedException("");
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public void setPersistent(boolean persistent) {
        // TODO: Figure out what to do if this is a player
        throw new NotImplementedException("");
    }

    @Override
    public DataContainer toContainer() {
        // TODO: Persistence API
        throw new NotImplementedException("");
    }

    @Override
    public void serialize(DataSource source) {
        // TODO: Persistence API
        throw new NotImplementedException("");
    }
}