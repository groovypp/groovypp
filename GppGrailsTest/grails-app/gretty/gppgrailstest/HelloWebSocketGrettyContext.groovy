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

webContexts : [
    "/websockets" : [
        static: "./webSocketsFiles",

        default: {
            response.redirect("http://${request.getHeader('Host')}/websockets/")
        },

        public: {
            get("/:none") { args ->
                if(!args.none.empty)
                    response.redirect("http://${request.getHeader('Host')}/websockets/")
                else
                    response.responseBody = new File("./webSocketsFiles/ws.html")
            }

            websocket("/ws",[
                onMessage: { msg ->
                    socket.send(msg.toUpperCase())
                },

                onConnect: {
                    socket.send("Welcome!")
                }
            ])
        },
    ]
]
