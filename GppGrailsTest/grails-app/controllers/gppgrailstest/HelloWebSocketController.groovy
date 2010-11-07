import javax.servlet.http.HttpServletRequest

def bookService

defaultAction: world

allowedMethods: [world: 'GET']

world: {
    Integer cnt = request.session.counter ?: 239
    request.session.counter = cnt + 1
    render """
<html>
<head>
<title>SomeTitle</title>
<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.1.0/prototype.js"></script>
<script type="text/javascript" src="/js/pseudows.js"></script>
<script type="text/javascript">
    var conn, count = 0
    function init () {
        conn = new WebSocket("ws://127.0.0.1:9090/websockets/ws");
        conn.onmessage = function(msg) {
            \$('message').innerHTML = msg.data
            conn.send("Msg " + count)
            count = count + 1
        }
        conn.onerror = function(msg) {
            \$('message').innerHTML = msg
        }
        conn.send('Hello, World')
    }
</script>
</head>
<body onload="init()">
<div id='message'>Hello, World!</div>
${request.session["counter"]}
</body>
</html>
"""
}

world2: {}