class WebSocketsTagLib {
    def includeWebSockets = { attrs, body ->
        out.println """
        ${javascript(plugin:"groovy-plus-plus", library:"swfobject")}
        ${javascript(plugin:"groovy-plus-plus", library:"FABridge")}
        ${javascript(plugin:"groovy-plus-plus", library:"web_socket")}
        <script type="text/javascript">
            WEB_SOCKET_SWF_LOCATION = \"${resource(plugin:'groovy-plus-plus',dir:'js',file:'WebSocketMain.swf')}\"
            WEB_SOCKET_DEBUG = ${attrs.debug ? true : false}
        </script>
"""
    }
}