package gppgrailstest

class HelloWebSocketController {
    def world = {
        def cnt = (Integer) request.session.counter ?: 239
        request.session.counter = cnt + 1
        request.session.setMaxInactiveInterval 1
        System.sleep(2000)
        render """${request.session.counter}
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
    <div id="tringmeph"><script type="text/javascript" src="http://login.tringme.com/widget.php?channel=td776t6i9mdy17817ca447lgb8f6nu&name=alex&divid=tringmeph"></script></div>
    <div id='message'>Hello, World!</div>
</body>
</html>
"""
    }
}
