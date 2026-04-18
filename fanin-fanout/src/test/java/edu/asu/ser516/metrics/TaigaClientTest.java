package edu.asu.ser516.metrics;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaigaClientTest {

    private HttpServer mockServer;
    private TaigaClient client;
    private TaigaLoginObject loginObj;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();
        client = new TaigaClient("http://localhost:" + port);
        loginObj = new TaigaLoginObject("testuser", "testpass");
        mockServer.start();
    }
}