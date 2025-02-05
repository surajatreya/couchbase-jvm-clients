package com.couchbase.client.core.json.stream;

import com.couchbase.client.core.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.core.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.core.deps.io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonStreamParserTest {

  static {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
  }

  @Test
  void exampleUsage() throws Exception {
    final List<String> matches = new ArrayList<>();

    JsonStreamParser parser = JsonStreamParser.builder()
      .doOnValue("/name", v -> matches.add("Hello " + v.readString()))
      .doOnValue("/pets/-/name", v -> matches.add("I see you have a pet named " + v.readString()))
      .doOnValue("/blob", v -> matches.add("Here's a blob of JSON: " + new String(v.readBytes(), UTF_8)))
      .build();

    byte[] json = "{'name':'Jon','pets':[{'name':'Odie'},{'name':'Garfield'}],'blob':{'magicWord':'xyzzy'}}"
      .replace("'", "\"")
      .getBytes(UTF_8);

    int half = json.length / 2;
    ByteBuf firstChunk = Unpooled.copiedBuffer(json, 0, half);
    ByteBuf secondChunk = Unpooled.copiedBuffer(json, half, json.length - half);

    try {
      parser.feed(firstChunk); // parser takes ownership of the buffer and will release it.
      assertEquals(Arrays.asList("Hello Jon", "I see you have a pet named Odie"), matches);
      matches.clear();

      parser.feed(secondChunk);
      assertEquals(Arrays.asList("I see you have a pet named Garfield", "Here's a blob of JSON: {\"magicWord\":\"xyzzy\"}"), matches);
      matches.clear();


    } finally {
      // Must always call close() when done to release the buffers owned by the parser.
      parser.close();
    }
  }

  @Test
  void jsonPointerExamples() throws Exception {
    String json = "{\n" +
      "  'foo': ['bar', 'baz'],\n" +
      "  '': 0,\n" +
      "  'a/b': 1,\n" +
      "  'c%d': 2,\n" +
      "  'e^f': 3,\n" +
      "  'g|h': 4,\n" +
      "  'i\\\\j': 5,\n" +
      "  'k\\'l': 6,\n" +
      "  ' ': 7,\n" +
      "  'm~n': 8\n" +
      "}";

    new ResultChecker(json)
      //""           // the whole document
      .expect("/foo", "['bar', 'baz']")
      //.expect("/foo/0", "'bar'")
      .expect("/", "0")
      .expect("/a~1b", "1")
      .expect("/c%d", "2")
      .expect("/e^f", "3")
      .expect("/g|h", "4")
      .expect("/i\\j", "5")
      .expect("/k\"l", "6")
      .expect("/ ", "7")
      .expect("/m~0n", "8")
      .check();
  }

  @Test
  void surroundingWhitespaceOkay() throws Exception {
    String json = " {} ";

    new ResultChecker(json)
      .expect("", "{}")
      .check();
  }

  @Test
  void process() throws Exception {
    String json = "{" +
      "'ignoreScalar':false," +
      "'ignoreArray':[[1,2,3],[{}]]," +
      "'ignoreObject':{'a':{},'b':[{}]}," +
      "'greeting':'hello'," +
      "'animal' : {'name' :  'fi\\'do' ,'age':5}," +
      "'numbers' : [1, 'two' , 'π']," +
      "'magicWords': ['xyzzy' , 'abracadabra']," +
      "'null':null" +
      "}";

    new ResultChecker(json)
      .expect("/greeting", "'hello'")
      .expect("/animal/name", "'fi\\'do'")
      .expect("/animal/age", "5")
      .expect("/numbers/-", "1", "'two'", "'π'")
      .expect("/magicWords", "['xyzzy' , 'abracadabra']")
      .expect("/null", "null")
      .check();
  }

  @Test
  void matchElementsOfRootArray() throws Exception {
    String json = "[1,{'foo' : true},3]";

    new ResultChecker(json)
      .expect("/-", "1", "{'foo' : true}", "3")
      .check();
  }

  @Test
  void matchRootObject() throws Exception {
    String json = "{'greeting':'hello'}";

    new ResultChecker(json)
      .expect("", "{'greeting':'hello'}")
      .check();
  }

  @Test
  void matchRootArray() throws Exception {
    String json = "[1, 2 ,3]";

    new ResultChecker(json)
      .expect("", "[1, 2 ,3]")
      .check();
  }

  @Test
  void matchRootScalar() throws Exception {
    String json = "true";

    new ResultChecker(json)
      .expect("", "true")
      .check();
  }

  @Test
  void matchEmptyName() throws Exception {
    String json = "{'':'hello'}";

    new ResultChecker(json)
      .expect("/", "'hello'")
      .check();
  }

  @Test
  void containerTypeMismatchIsNotError() throws Exception {
    String json = "{'greeting':'hello','colors':['red','green','blue']}";

    new ResultChecker(json)
      .expect("/greeting/-")
      .expect("/colors/foo")
      .check();
  }

  @Test
  void hyphenIsValidObjectFieldName() throws Exception {
    String json = "{'foo':{'-':'bar'}}";
    new ResultChecker(json)
      .expect("/foo/-", "'bar'")
      .check();
  }

  @Test
  void canReadMultipleDocuments() throws Exception {
    String json = "{'color':'red'}{'color':'green'}";
    new ResultChecker(json)
      .expect("/color", "'red'", "'green'")
      .check();
  }

  @Test
  void canDescendThroughArrayWildcard() throws Exception {
    String json = "{'colors':[{'r':255,'g':0,'b':0},{'r':0,'g':255,'b':0}]}";
    new ResultChecker(json)
      .expect("/colors/-/g", "0", "255") // I can't believe this works :-)
      .check();
  }

  @Test
  void cannotReconfigureAfterBuilding() throws Exception {
    JsonStreamParser.Builder builder = JsonStreamParser.builder()
      .doOnValue("/foo", v -> {
      });


    builder.build().close();

    assertThrows(IllegalStateException.class, () ->
      // bad because the path tree is mutable and shared by all parsers from this builder
      builder.doOnValue("/bar", v -> {
      }));
  }

  private static class ResultChecker {
    private static class ListenerCheck {
      private final String jsonPointer;
      private final List<String> expected;
      private final List<String> actual = new ArrayList<>();

      public ListenerCheck(String jsonPointer, List<String> expected) {
        this.jsonPointer = jsonPointer;
        this.expected = expected;
      }

      void addActual(String value) {
        actual.add(value);
      }

      void checkResult() {
        assertEquals(expected, actual, jsonPointer);
      }
    }

    private final List<ResultChecker.ListenerCheck> checks = new ArrayList<>();
    private final JsonStreamParser.Builder builder = JsonStreamParser.builder();
    private final byte[] json;

    ResultChecker(String json) {
      this.json = normalizeQuotes(json).getBytes(UTF_8);
    }

    ResultChecker expect(String jsonPointer, String... expectedValues) {
      List<String> expected = Arrays.stream(expectedValues)
        .map(ResultChecker::normalizeQuotes)
        .collect(toList());

      ResultChecker.ListenerCheck check = new ResultChecker.ListenerCheck(jsonPointer, expected);
      builder.doOnValue(jsonPointer, value -> check.addActual(new String(value.readBytes(), UTF_8)));
      checks.add(check);
      return this;
    }

    private static String normalizeQuotes(String s) {
      return s == null ? null : s.replace("'", "\"");
    }

    void check() throws IOException {
      checkWithChunkSizeAndStreamWindow(Integer.MAX_VALUE);

      for (int i = 1; i <= min(32, json.length); i++) {
        checkWithChunkSizeAndStreamWindow(i);
      }
    }

    void checkWithChunkSizeAndStreamWindow(final int chunkSize) throws IOException {
      //System.out.println("testing with chunk size " + chunkSize);
      checks.forEach(c -> c.actual.clear()); // reset

      try (JsonStreamParser parser = builder.build()) {
        ByteBuf buf = Unpooled.wrappedBuffer(json);

        parser.feed(Unpooled.buffer()); // make sure empty chunk doesn't break anything

        int offset = 0;
        while (buf.isReadable()) {
          ByteBuf chunk = Unpooled.buffer();
          chunk.writeBytes(buf, min(chunkSize, buf.readableBytes()));
//          System.out.println("feeding (offset " + offset + ") : `" + chunk.toString(UTF_8) + "`");
          offset += chunkSize;
          parser.feed(chunk);
        }
        parser.endOfInput();
      }

      checks.forEach(ResultChecker.ListenerCheck::checkResult);
    }
  }
}
