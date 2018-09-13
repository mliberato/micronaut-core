package io.micronaut.http.server.netty.websocket

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.websocket.RxWebSocketClient
import io.micronaut.websocket.RxWebSocketSession
import io.micronaut.websocket.WebSocketClient
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class SimpleTextWebSocketSpec extends Specification {

    void "test simple text websocket exchange"() {
        given:
        EmbeddedServer embeddedServer = ApplicationContext.build('micronaut.server.netty.log-level':'TRACE').run(EmbeddedServer)
        PollingConditions conditions = new PollingConditions(timeout: 15    , delay: 0.5)

        when: "a websocket connection is established"
        RxWebSocketClient wsClient = embeddedServer.applicationContext.createBean(RxWebSocketClient, embeddedServer.getURI())
        ChatClientWebSocket fred = wsClient.connect(ChatClientWebSocket, "/chat/stuff/fred").blockingFirst()
        ChatClientWebSocket bob = wsClient.connect(ChatClientWebSocket, [topic:"stuff",username:"bob"]).blockingFirst()

        then:"The connection is valid"
        fred.session != null
        fred.session.id != null



        then:"A session is established"
        fred.session != null
        fred.session.id != null
        fred.session.id != bob.session.id
        fred.topic == 'stuff'
        fred.username == 'fred'
        bob.username == 'bob'



        when:"A message is sent"
        fred.send("Hello bob!")

        then:
        conditions.eventually {
            bob.replies.contains("[fred] Hello bob!")
            bob.replies.size() == 1
            fred.replies.contains("[bob] Joined!")
            fred.replies.size() == 1
        }

        when:
        bob.send("Hi fred. How are things?")

        then:
        conditions.eventually {

            fred.replies.contains("[bob] Hi fred. How are things?")
            fred.replies.size() == 2
            bob.replies.contains("[fred] Hello bob!")
            bob.replies.size() == 1
        }
        fred.sendAsync("foo").get() == 'foo'
        fred.sendRx("bar").blockingGet() == 'bar'

        when:
        bob.close()
        fred.close()

        then:
        conditions.eventually {
            !bob.session.isOpen()
            !fred.session.isOpen()
        }

        cleanup:
        wsClient.close()
        embeddedServer.close()
    }
}
