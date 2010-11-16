/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gppgrailstest

import org.mbte.gretty.httpserver.GrettyWebSocket

@Typed class ChatService {
    private volatile int nextId

    private final Map<GrettyWebSocket,String>       connected = [:]
    private final Map<String,List<GrettyWebSocket>> names     = [:]

    String newUserName() {
       "Guest ${nextId.incrementAndGet()}"
    }

    void onMessage(GrettyWebSocket socket, String msg) {
        synchronized(connected) {
            def name = connected[socket]
            if(!name) {
                connected[socket] = (name = msg)
                def sockets = names[name]
                if(!sockets)
                    names[name] = (sockets = [])

                sockets << socket
                if(sockets.size() == 1)
                    for(s in connected.keySet())
                        if(s == socket)
                            s.send("Moderator: welcome to the chat, $name")
                        else
                            s.send("Moderator: $name joined the chat")
            }
            else {
                for(s in connected.keySet())
                    s.send "$name: $msg"
            }
        }
    }

    void onDisconnect(GrettyWebSocket socket) {
        synchronized(connected) {
            def name = connected.remove(socket)
            if(name) {
                def sockets = names[name]
                sockets.remove(socket)

                if(sockets.empty)
                    for(s in connected.keySet())
                        s.send("Moderator: $name left the chat")
            }
        }
    }
}
