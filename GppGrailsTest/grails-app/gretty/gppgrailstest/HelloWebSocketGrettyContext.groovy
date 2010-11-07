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

webContexts = [
    "/websockets" : [
        public: {
            websocket("/ws",[
                onMessage: { msg ->
                    socket.send(msg.toUpperCase())
                },

                onConnect: {
                    socket.send("Welcome!")
                }
            ])

            get("/world") {
                response.html = """<html>
<head>
<title>Life Game</title>
<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.1.0/prototype.js"></script>
<script type="text/javascript" src="/js/pseudows.js"></script>
<script type="text/javascript">
    var conn, count = 0
    function init () {
        conn = new WebSocket("ws://" + window.location.host + '/websockets/ws');
        conn.onmessage = function (msg) {
            \$('message').innerHTML = msg.data
            conn.send("Msg " + count)
            count = count + 1
        }
        conn.send('Hello, World')
    }
</script>
</head>
<body onload="init()">
<div id='message'>Hello, World!</div>
</body>
</html>"""
            }
        }
    ]
]
