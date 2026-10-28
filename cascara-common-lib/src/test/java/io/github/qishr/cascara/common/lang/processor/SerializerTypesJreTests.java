package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.Enum.EnumDesc;
import java.net.HttpCookie;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.Dictionary;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;

/// Testing of Serialier with JRE types
public class SerializerTypesJreTests extends SerializerTestBase {
    @Test
    void test_serializeStackTraceElement() {
        StackTraceElement original = new StackTraceElement("className1", "methodName1", "fileName1", 1);

        AstNode ast = serializer.toAst(original);
        StackTraceElement ste = serializer.fromAst(ast, StackTraceElement.class);

        assertEquals("className1", ste.getClassName());
    }

    public static class JavaLangTypeDescriptorTest {
        public Character p_character;
        public Character.Subset p_characterSubset;
        public Character.UnicodeBlock p_unicodeBlock;
        public Character.UnicodeScript p_unicodeScript;
        public CharSequence p_charSequence;
        public EnumDesc<?> p_enumDesc;
        public Module p_module;
        public ModuleLayer p_moduleLayer;
        public Package p_package;
        public ProcessHandle.Info p_processHandleInfo;
        public Runtime.Version p_runtimeVersion;
        public StringBuffer p_stringBuffer;
        public StringBuilder p_stringBuilder;
        public Void p_void;
    }

    @Test
    void test_lang_typeDescriptors() {
        JavaLangTypeDescriptorTest types = new JavaLangTypeDescriptorTest();
        types.p_character = 'a';
        types.p_characterSubset = Character.UnicodeBlock.BASIC_LATIN; // Subset implementation
        types.p_unicodeBlock = Character.UnicodeBlock.BASIC_LATIN;
        types.p_unicodeScript = Character.UnicodeScript.LATIN;
        types.p_charSequence = "hello cascara";
        // Note: EnumDesc, Module, ModuleLayer, Package, ProcessHandle.Info, Runtime.Version
        // can be populated with live runtime references to test their default serialization behavior.
        types.p_module = JavaLangTypeDescriptorTest.class.getModule();
        types.p_moduleLayer = ModuleLayer.boot();
        types.p_package = JavaLangTypeDescriptorTest.class.getPackage();
        types.p_processHandleInfo = ProcessHandle.current().info();
        types.p_runtimeVersion = Runtime.version();
        types.p_stringBuffer = new StringBuffer("buffer");
        types.p_stringBuilder = new StringBuilder("builder");
        types.p_void = null; // Void instances are inherently null

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        // MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        // Run through and check what falls through or handles correctly
        JavaLangTypeDescriptorTest r = serializer.fromAst(root, JavaLangTypeDescriptorTest.class);

        assertEquals(types.p_character, r.p_character);
        assertEquals(types.p_charSequence.toString(), r.p_charSequence.toString());
        assertEquals(types.p_stringBuffer.toString(), r.p_stringBuffer.toString());
        assertEquals(types.p_stringBuilder.toString(), r.p_stringBuilder.toString());
        assertEquals(types.p_runtimeVersion, r.p_runtimeVersion);
    }

    public static class JavaNetTypeDescriptorTest {
        public URI p_uri;
        public URL p_url;
        public InetAddress p_inetAddress;
        public Inet4Address p_inet4Address;
        public HttpCookie p_httpCookie;
    }

    @SuppressWarnings("deprecation")
    @Test
    void test_net_typeDescriptors() throws Exception {
        JavaNetTypeDescriptorTest types = new JavaNetTypeDescriptorTest();
        types.p_uri = URI.create("http://example.com");
        types.p_url = new URL("http://example.com");
        types.p_inetAddress = InetAddress.getByName("127.0.0.1");
        types.p_inet4Address = (Inet4Address) InetAddress.getByName("192.168.1.1");
        types.p_httpCookie = new HttpCookie("sessionid", "abc123xyz");

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaNetTypeDescriptorTest r = serializer.fromAst(root, JavaNetTypeDescriptorTest.class);
        assertEquals(types.p_uri, r.p_uri);
        assertEquals(types.p_url, r.p_url);
        assertEquals(types.p_inetAddress, r.p_inetAddress);
        assertEquals(types.p_inet4Address, r.p_inet4Address);
        assertEquals(types.p_httpCookie, r.p_httpCookie);
    }

    public static class JavaNioTypeDescriptorTest {
        public Path p_path;
    }

    @Test
    void test_nio_typeDescriptors() {
        JavaNioTypeDescriptorTest types = new JavaNioTypeDescriptorTest();
        types.p_path = Path.of("/tmp");

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaNioTypeDescriptorTest r = serializer.fromAst(root, JavaNioTypeDescriptorTest.class);
        assertEquals(types.p_path, r.p_path);
    }

    public static class JavaTimeTypeDescriptorTest {
        public Instant p_instant;
        public LocalDateTime p_localDateTime;
        public ZonedDateTime p_zonedDateTime;
        public Date p_date;
        public TimeZone p_timeZone;
        public Duration p_duration;
        public Period p_period;
        public LocalTime p_localTime;
        public OffsetDateTime p_offsetDateTime;
        public OffsetTime p_offsetTime;
        public ZoneId p_zoneId;
        public ZoneOffset p_zoneOffset;
    }

    @Test
    void test_time_typeDescriptors() {
        JavaTimeTypeDescriptorTest types = new JavaTimeTypeDescriptorTest();
        types.p_instant = Instant.now();
        types.p_localDateTime = LocalDateTime.now();
        types.p_zonedDateTime = ZonedDateTime.now();
        types.p_date = new Date();
        types.p_timeZone = TimeZone.getTimeZone("UTC");
        types.p_duration = Duration.ofHours(2);
        types.p_period = Period.ofDays(5);
        types.p_localTime = LocalTime.of(13, 45);
        types.p_offsetDateTime = OffsetDateTime.now();
        types.p_offsetTime = OffsetTime.now();
        types.p_zoneId = ZoneId.of("Europe/London");
        types.p_zoneOffset = ZoneOffset.ofHours(1);

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaTimeTypeDescriptorTest r = serializer.fromAst(root, JavaTimeTypeDescriptorTest.class);
        assertEquals(types.p_instant, r.p_instant);
        assertEquals(types.p_localDateTime, r.p_localDateTime);
        assertEquals(types.p_zonedDateTime, r.p_zonedDateTime);
        assertEquals(types.p_date, r.p_date);
        assertEquals(types.p_timeZone, r.p_timeZone);
        assertEquals(types.p_duration, r.p_duration);
        assertEquals(types.p_period, r.p_period);
        assertEquals(types.p_localTime, r.p_localTime);
        assertEquals(types.p_offsetDateTime, r.p_offsetDateTime);
        assertEquals(types.p_offsetTime, r.p_offsetTime);
        assertEquals(types.p_zoneId, r.p_zoneId);
        assertEquals(types.p_zoneOffset, r.p_zoneOffset);
    }

    public static class JavaUtilTypeDescriptorTest {
        public AbstractQueue<String> p_abstractQueue;
        public Deque<String> p_deque;
        public BitSet p_bitSet;
        public Calendar p_calendar;
        public Currency p_currency;
        public Date p_date;
        public Dictionary<String, String> p_dictionary;
        public DoubleSummaryStatistics p_doubleSummaryStatistics;
        public Enumeration<String> p_enumeration;
        public EnumSet<DayOfWeek> p_enumSet;
        public EnumMap<DayOfWeek, String> p_enumMap;
        public EventObject p_eventObject;
        public HashSet<String> p_hashSet;
        public NavigableMap<String, String> p_navigableMap;
        public PriorityQueue<String> p_priorityQueue;
        public Properties p_properties;
        public SimpleTimeZone p_simpleTimeZone;
        public SortedMap<String, String> p_sortedMap;
        public SortedSet<String> p_sortedSet;
        public Stack<String> p_stack;
        public TimeZone p_timeZone;
        public TreeMap<String, String> p_treeMap;
        public TreeSet<String> p_treeSet;
        public UUID p_uuid;
        public Vector<String> p_vector;
        public WeakHashMap<String, String> p_weakHashMap;
    }

    @Test
    void test_util_typeDescriptors() {
        JavaUtilTypeDescriptorTest types = new JavaUtilTypeDescriptorTest();
        types.p_abstractQueue = new PriorityQueue<>();
        types.p_abstractQueue.add("item");
        types.p_deque = new ArrayDeque<>();
        types.p_deque.add("deque-item");
        types.p_bitSet = BitSet.valueOf(new long[]{1L});
        types.p_calendar = Calendar.getInstance();
        types.p_currency = Currency.getInstance("USD");
        types.p_date = new Date();

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("key", "value");
        types.p_dictionary = hashtable;

        types.p_doubleSummaryStatistics = new DoubleSummaryStatistics();
        types.p_doubleSummaryStatistics.accept(10.0);
        types.p_doubleSummaryStatistics.accept(20.0);

        types.p_enumeration = Collections.enumeration(List.of("a", "b"));
        types.p_enumSet = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

        types.p_enumMap = new EnumMap<>(DayOfWeek.class);
        types.p_enumMap.put(DayOfWeek.MONDAY, "Work");

        types.p_eventObject = new EventObject("source-object");
        types.p_hashSet = new HashSet<>(List.of("one", "two"));

        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("k", "v");
        types.p_navigableMap = treeMap;
        types.p_sortedMap = treeMap;
        types.p_treeMap = treeMap;

        types.p_priorityQueue = new PriorityQueue<>(List.of("x", "y"));

        types.p_properties = new Properties();
        types.p_properties.setProperty("propKey", "propVal");

        types.p_simpleTimeZone = new SimpleTimeZone(0, "UTC");

        TreeSet<String> treeSet = new TreeSet<>(List.of("alpha", "beta"));
        types.p_sortedSet = treeSet;
        types.p_treeSet = treeSet;

        types.p_stack = new Stack<>();
        types.p_stack.push("stacked");

        types.p_timeZone = TimeZone.getTimeZone("UTC");
        types.p_uuid = UUID.randomUUID();
        types.p_vector = new Vector<>(List.of("vec1", "vec2"));
        types.p_weakHashMap = new WeakHashMap<>();
        types.p_weakHashMap.put("weakKey", "weakVal");

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);

        JavaUtilTypeDescriptorTest r = serializer.fromAst(root, JavaUtilTypeDescriptorTest.class);
        assertNotNull(r);
        assertEquals(types.p_currency, r.p_currency);
        assertEquals(types.p_properties, r.p_properties);
    }
}
