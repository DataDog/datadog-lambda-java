package com.datadoghq.datadog_lambda_java;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class ExtensionTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);

    @Test public void testDetectExtensionSuccess() {
        Path resourceDirectory = Paths.get("src","test","resources");
        String fakeExtensionPath = resourceDirectory.toFile().getAbsolutePath() + "/fakeExtension";
        Assert.assertTrue(Extension.isExtensionRunning(fakeExtensionPath));
    }
    @Test public void testDetectExtensionFailure() {
        String invalidPath = "/fakeExtension";
        Assert.assertFalse(Extension.isExtensionRunning(invalidPath));
    }

    @Test public void testHitHelloRouteIncorrectUrl() {
        Assert.assertFalse(Extension.hitHelloRoute("toto", "titi"));
    }

    @Test public void testHitHelloRouteNotRespondingEndpoint() {
        Assert.assertFalse(Extension.hitHelloRoute("http://localhost:1111", "/invalid"));
    }

    @Test public void testHitHelloRouteValidEndpoint() {
        stubFor(get(urlEqualTo("/valid"))
                .willReturn(aResponse()));
        Assert.assertTrue(Extension.hitHelloRoute("http://localhost:9999", "/valid"));
    }

    @Test public void testHitFlushRouteIncorrectUrl() {
        Assert.assertFalse(Extension.hitFlushRoute("toto", "titi"));
    }

    @Test public void testHitFlushRouteNotRespondingEndpoint() {
        Assert.assertFalse(Extension.hitFlushRoute("http://localhost:1111", "/invalid"));
    }

    @Test public void testHitFlushRouteValidEndpoint() {
        stubFor(post(urlEqualTo("/valid"))
                .willReturn(aResponse()));
        Assert.assertTrue(Extension.hitFlushRoute("http://localhost:9999", "/valid"));
    }

}