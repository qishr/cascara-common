package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.TestService;
import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.security.JarManifestVerifier;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.trackable.tracker.ArrayChangeTracker;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.test.common.junit.util.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;

public class JarSigningTests extends SplTestBase {

    @Test
    void testSignSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            // XXX test with incorrect deps
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            byte[] mf = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sf = jar.extractFile("META-INF/MANIFEST.SF");
            JarManifestVerifier jv = new JarManifestVerifier(mf, sf);
            assertTrue(jv.verifySignatureFileAgainstManifest());
            for (JarEntry jarEntry : jar.getJarEntries()) {
                Certificate[] certs = jarEntry.getCertificates();
                try (InputStream is = jar.getInputStream(jarEntry)) {
                    assertTrue(jv.verifyEntryContent(jarEntry.getName(), is));
                }
            }
        }
    }

    @Test
    void testLoadSignedJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        String layerName = "signingTestLayer";
        SPL layer = spl.create(layerName);

        layer.registerJar(testJar.getPath());

        // Verify the provider is registered
        List<ServiceMetadata> providers = spl.findAllProviders(TestService.class);
        assertEquals(1, providers.size());
        ServiceMetadata providerMetadata = providers.getFirst();
        assertEquals("ProviderA", providerMetadata.getProperties().getString("providerName"));

        // Remove the layer
        spl.remove(layerName);
    }
}
