/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.server;

import com.google.common.util.concurrent.ListenableFuture;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.impl.AbstractChatEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.common.SpongeImpl;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class ChatHandler implements Runnable {

    private final BlockingQueue<Tuple<Player, MessageChannelEvent.Chat>> eventQueue = new SynchronousQueue<>();

    @Override
    public void run() {
        while (true) {
            Tuple<Player, MessageChannelEvent.Chat> data;
            try {
                data = eventQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            Player sender = data.getFirst();
            MessageChannelEvent.Chat event = data.getSecond();
            if (!SpongeImpl.postEvent(event) && isRelevant(sender, event)) {
                SpongeImpl.getLogger().info("Sending chat message.");

                // do permission checks on main thread
                LinkedList<AbstractChatEvent.PermissionCheck> checks = event.getPermissionChecks();
                ListenableFuture<Object> lastCheck = null;
                for (int i = 0; i < checks.size(); i++) {
                    ListenableFuture<Object> future = checks.get(i).post();
                    if (i == checks.size() - 1) {
                        lastCheck = future;
                    }
                }

                // send message when checks are completed
                if (lastCheck != null) {
                    lastCheck.addListener(() -> complete(sender, event), Runnable::run);
                } else {
                    complete(sender, event);
                }
            } else {
                SpongeImpl.getLogger().info("Chat message cancelled.");
            }
        }
    }

    private void complete(Player sender, MessageChannelEvent.Chat event) {
        if (isRelevant(sender, event)) {
            sender.getWorld().post(() -> event.getChannel().ifPresent(channel ->
                    channel.send(sender, event.getMessage(), ChatTypes.CHAT)));
        }
    }

    private boolean isRelevant(Player sender, MessageChannelEvent.Chat event) {
        return sender.isOnline() && !event.isCancelled() && !event.isMessageCancelled();
    }

    public void postEvent(Player sender, MessageChannelEvent.Chat event) {
        this.eventQueue.add(new Tuple<>(sender, event));
    }

}
