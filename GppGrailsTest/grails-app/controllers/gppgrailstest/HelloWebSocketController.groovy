package gppgrailstest

class HelloWebSocketController {
    def world = {
        render """\
<html>
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
</html>
"""
    }
}
