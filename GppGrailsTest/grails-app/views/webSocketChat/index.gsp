<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head>
      <title>Web Socket Chat</title>
      <g:includeWebSockets debug='false'/>
      <g:javascript >
        var ws;
        var connected;

        var userName  = '${userName}';
        
        function reconnect () {
            if(ws)
              ws.close();
            ws = null;
            connected = false;
            setTimeout(function() { connect () }, 5000)
        }

        function connect(){
          output("DBG: trying to connect to the chat");

          var host = document.location.hostname;
          if(host == 'localhost' )
            host = '${InetAddress.localHost.hostAddress}';

          ws = new WebSocket("ws://" + host + ":9090/websockets/chat");

          ws.onopen = function() {
            output("DBG: connected to the chat");
            connected = true;
            ws.send(userName);
          };

          ws.onmessage = function(e) { output(e.data); };

          ws.onerror = ws.onclose = function() {
            output(connected ? "DBG: disconnected from the chat" : "DBG: failed to connect to the chat");
            reconnect();
          };
        }

        function onSubmit() {
          var input = document.getElementById("input");
          ws.send(input.value);
          input.value = "";
          input.focus();
        }

        function output(str) {
          var log = document.getElementById("log");
          var escaped = str.replace(/&/, "&amp;").replace(/</, "&lt;").
            replace(/>/, "&gt;").replace(/"/, "&quot;"); // "
          log.innerHTML = escaped + "<br>" + log.innerHTML;
        }
      </g:javascript>
  </head>
   <body onload="connect()">
      <form onsubmit="onSubmit(); return false;">
        <input type="text" id="input">
        <input type="submit" value="Send">
        <button onclick="ws.close(); connect(); return false;">Reconnect</button>
      </form>
    <div id="log"></div>
  </body>
</html>