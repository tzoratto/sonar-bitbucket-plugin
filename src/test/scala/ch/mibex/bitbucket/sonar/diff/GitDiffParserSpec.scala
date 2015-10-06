package ch.mibex.bitbucket.sonar.diff

import org.junit.runner.RunWith
import org.specs2.matcher.{ParserMatchers, StringMatchers}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GitDiffParserSpec extends Specification with ParserMatchers with StringMatchers {

  import GitDiffParser._


  "diff headers" should {

    "parse file mode" in {
      fileMode must succeedOn("100644").withResult(100644)
      fileMode must failOn("1006441")
      fileMode must failOn("x100644")
    }

    "parse old mode" in {
      oldMode must succeedOn("old mode 100644\n").withResult(OldMode(100644))
      oldMode must failOn("old mode 100644")
      oldMode must failOn("oldmode100644")
    }

    "parse new mode" in {
      newMode must succeedOn("new mode 100644\n").withResult(NewMode(100644))
      newMode must failOn("new mode 100644")
      newMode must failOn("newmode100644")
    }

    "parse new file mode" in {
      newFileMode must succeedOn("new file mode 100644\n").withResult(NewFileMode(100644))
      newFileMode must failOn("new file mode 100644")
      newFileMode must failOn("new")
    }

    "parse deleted file mode" in {
      deletedFileMode must succeedOn("deleted file mode 100644\n").withResult(DeletedFileMode(100644))
      deletedFileMode must failOn("deleted file mode 100644")
      deletedFileMode must failOn("deletedfilemode100644")
    }

    "parse file path" in {
      filePath must succeedOn("a/b/c.txt").withResult("a/b/c.txt")
      filePath must failOn("")
    }

    "parse copy from" in {
      copyFrom must succeedOn("copy from describe.c\n").withResult(CopyFrom("describe.c"))
      copyFrom must failOn("copy from describe.c")
      copyFrom must failOn("copyfromdescribe.c")
    }

    "parse copy to" in {
      copyTo must succeedOn("copy to describe.c\n").withResult(CopyTo("describe.c"))
      copyTo must failOn("copy to describe.c")
      copyTo must failOn("copytodescribe.c")
    }

    "parse rename from" in {
      renameFrom must succeedOn("rename from describe.c\n").withResult(RenameFrom("describe.c"))
      renameFrom must failOn("rename from describe.c")
      renameFrom must failOn("renamefromdescribe.c")
    }

    "parse rename to" in {
      renameTo must succeedOn("rename to describe.c\n").withResult(RenameTo("describe.c"))
      renameTo must failOn("rename to describe.c")
      renameTo must failOn("renametodescribe.c")
    }

    "parse similarity" in {
      similarity must succeedOn("100").withResult(100)
      similarity must succeedOn("95").withResult(95)
      similarity must succeedOn("1").withResult(1)
      similarity must failOn("")
      similarity must failOn("1000")
    }

    "parse similarity index" in {
      similarityIndex must succeedOn("similarity index 95%\n").withResult(SimilarityIndex(95))
      similarityIndex must failOn("similarity index 95%")
      similarityIndex must failOn("similarity index")
    }

    "parse dissimilarity index" in {
      dissimilarityIndex must succeedOn("dissimilarity index 5%\n").withResult(DissimilarityIndex(5))
      dissimilarityIndex must failOn("dissimilarity index 5%")
      dissimilarityIndex must failOn("dissimilarity index")
    }

    "parse hash" in {
      hash must succeedOn("cc95eb0").withResult("cc95eb0")
      hash must failOn("")
      hash must failOn("1000")
    }

    "parse index" in {
      index must succeedOn("index fabadb8..cc95eb0\n").withResult(
        Index(fromHash = "fabadb8", toHash = "cc95eb0")
      )
      index must succeedOn("index 7750514..f051b99 100644\n").withResult(
        Index(fromHash = "7750514", toHash = "f051b99", mode = Some(100644))
      )
      index must failOn("index fabadb8..cc95eb0")
      index must failOn("index fabadb8..")
    }

    "parse header" in {
      extendedDiffHeader must succeedOn(
        """similarity index 95%
          |rename from builtin-http-fetch.c
          |rename to http-fetch.c
          |index f3e63d7..e8f44ba 100644
          |""".stripMargin).withResult(
        ExtendedDiffHeader(
          headerLines = List(SimilarityIndex(95), RenameFrom("builtin-http-fetch.c"), RenameTo("http-fetch.c")),
          index = Index(fromHash = "f3e63d7", toHash = "e8f44ba", mode = Some(100644))
        )
      )
      extendedDiffHeader must succeedOn("index f3e63d7..e8f44ba 100644\n").withResult(
        ExtendedDiffHeader(headerLines = List(),
          index = Index(fromHash = "f3e63d7", toHash = "e8f44ba", mode = Some(100644)))
      )
      index must failOn("")
    }

  }

  "file change" should {

    "parse correctly" in {
      gitDiffHeader must succeedOn(
        "diff --git a/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java b/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java\n")
        .withResult(
          FileChange(
            oldFile = "multimod/src/db/src/main/java/ch/mycompany/test/db/App.java",
            newFile = "multimod/src/db/src/main/java/ch/mycompany/test/db/App.java"
          )
        )
    }

  }

  "hunk header" should {

    "parse number" in {
      num must succeedOn("100").withResult(100)
      num must failOn("")
    }

    "parse from file" in {
      fromFile must succeedOn("--- a/builtin-http-fetch.c\n").withResult("builtin-http-fetch.c")
      fromFile must failOn("")
    }

    "parse to file" in {
      toFile must succeedOn("+++ b/http-fetch.c\n").withResult("http-fetch.c")
      toFile must failOn("")
    }

    "parse with context" in {
      hunkStart must succeedOn(
        "@@ -1,8 +1,9 @@")
        .withResult(
          HunkHeader(
            fromToRange = FromToRange(fromLineStart = 1, fromNumLines = 8, toLineStart = 1, toNumLines = 9),
            context = None
          )
        )
    }

    "parse only from/to line range" in {
      hunkStart must succeedOn(
        "@@ -18,6 +19,8 @@ int cmd_http_fetch(int argc, const char **argv, const char *prefix)\n")
        .withResult(
          HunkHeader(
            fromToRange = FromToRange(fromLineStart = 18, fromNumLines = 6, toLineStart = 19, toNumLines = 8),
            context = Some(CtxLine(line = "int cmd_http_fetch(int argc, const char **argv, const char *prefix)"))
          )
        )
    }

  }

  "hunks" should {

    "parse context line" in {
      ctxLine must succeedOn(" import junit.framework.Test;\n").withResult(CtxLine("import junit.framework.Test;"))
      ctxLine must failOn("-import junit.framework.Test;\n")
      ctxLine must failOn("+import junit.framework.Test;\n")
      ctxLine must failOn("")
    }

    "parse added line" in {
      addedLine must succeedOn("+         System.exit(-1);\n").withResult(AddedLine("         System.exit(-1);"))
      addedLine must failOn("         System.exit(-1);\n")
      addedLine must failOn("         System.exit(-1);\n")
      addedLine must failOn("")
    }

    "parse removed line" in {
      removedLine must succeedOn("-        double d = Double.longBitsToDouble(i);  // Noncompliant\n").withResult(
        RemovedLine("        double d = Double.longBitsToDouble(i);  // Noncompliant")
      )
      removedLine must failOn("        double d = Double.longBitsToDouble(i);  // Noncompliant\n")
      removedLine must failOn("+       double d = Double.longBitsToDouble(i);  // Noncompliant\n")
      removedLine must failOn("")
    }

  }

  "complete diffs" should {

    "parse a file change with three hunks" in {
      val fileChangeWithThreeHunks =
        """diff --git a/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java b/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java
          |similarity index 95%
          |index 57b70a8..fc8f0d5 100755
          |--- a/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java
          |+++ b/multimod/src/db/src/main/java/ch/mycompany/test/db/App.java
          |@@ -12 +12 @@ public class App
          |-        double d = Double.longBitsToDouble(i);  // Noncompliant
          |+        double g = Double.longBitsToDouble(i);  // Noncompliant
          |@@ -16 +16 @@ public class App
          |-        System.out.println( "Hello World! " + d);
          |+        System.out.println( "Hello World! " + g);
          |@@ -17,0 +18,8 @@ public class App
          |+
          |+	public void foo () {
          |+		int f = 42;
          |+
          |+        try {
          |+        } catch (Throwable t) {
          |+        }
          |+	}
          |""".stripMargin

      gitDiff must succeedOn(fileChangeWithThreeHunks).withResult(
        GitDiff(
          gitDiffHeader = FileChange(
            oldFile = "multimod/src/db/src/main/java/ch/mycompany/test/db/App.java",
            newFile = "multimod/src/db/src/main/java/ch/mycompany/test/db/App.java"
          ),
          header = ExtendedDiffHeader(
            headerLines = List(SimilarityIndex(percentage = 95)),
            index = Index(fromHash = "57b70a8", toHash = "fc8f0d5", mode = Option(100755))),
          hunks = List(Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 12, fromNumLines = 0, toLineStart = 12, toNumLines = 0),
              context = Some(CtxLine("public class App"))
            ),
            changedLines = List(
              RemovedLine("        double d = Double.longBitsToDouble(i);  // Noncompliant"),
              AddedLine("        double g = Double.longBitsToDouble(i);  // Noncompliant")
            )
          ), Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 16, fromNumLines = 0, toLineStart = 16, toNumLines = 0),
              context = Some(CtxLine("public class App"))
            ),
            changedLines = List(
              RemovedLine( """        System.out.println( "Hello World! " + d);"""),
              AddedLine("""        System.out.println( "Hello World! " + g);""")
            )
          ), Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 17, fromNumLines = 0, toLineStart = 18, toNumLines = 8),
              context = Some(CtxLine("public class App"))
            ),
            changedLines = List(
              AddedLine(""),
              AddedLine("	public void foo () {"),
              AddedLine("		int f = 42;"),
              AddedLine(""),
              AddedLine("        try {"),
              AddedLine("        } catch (Throwable t) {"),
              AddedLine("        }"),
              AddedLine("	}")
            )
          ))
        )
      )
    }

    "parse a file deletion" in {
      val deletedFileDiff =
        """diff --git a/multimod/src/db/src/test/java/ch/mycompany/test/db/AppTest.java b/multimod/src/db/src/test/java/ch/mycompany/test/db/AppTest.java
          |deleted file mode 100755
          |index 4545ecc..0000000
          |--- a/multimod/src/db/src/test/java/ch/mycompany/test/db/AppTest.java
          |+++ /dev/null
          |@@ -1,40 +0,0 @@
          |-package ch.mycompany.test.db;
          |-
          |-import junit.framework.Test;
          |-import junit.framework.TestCase;
          |-import junit.framework.TestSuite;
          |-
          |-
          |-/**
          |- * Unit test for simple App.
          |- */
          |-public class AppTest
          |-    extends TestCase
          |-{
          |-    /**
          |-     * Create the test case
          |-     *
          |-     * @param testName name of the test case
          |-     */
          |-    public AppTest( String testName )
          |-    {
          |-        super( testName );
          |-    }
          |-
          |-    /**
          |-     * @return the suite of tests being tested
          |-     */
          |-    public static Test suite()
          |-    {
          |-        return new TestSuite( AppTest.class );
          |-    }
          |-
          |-    /**
          |-     * Rigourous Test :-)
          |-     */
          |-    public void testApp()
          |-    {
          |-        ch.mycompany.test.db.App.main(new String[]{});
          |-        assertTrue( true );
          |-    }
          |-}
          |""".stripMargin
      gitDiff must succeedOn(deletedFileDiff).withResult(
        GitDiff(
          gitDiffHeader = FileChange(
            oldFile = "multimod/src/db/src/test/java/ch/mycompany/test/db/AppTest.java",
            newFile = "multimod/src/db/src/test/java/ch/mycompany/test/db/AppTest.java"
          ),
          header = ExtendedDiffHeader(
            headerLines = List(DeletedFileMode(mode = 100755)),
            index = Index(fromHash = "4545ecc", toHash = "0000000", mode = None)),
          hunks = List(Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 1, fromNumLines = 40, toLineStart = 0, toNumLines = 0),
              context = None
            ),
            changedLines = List(
              RemovedLine("package ch.mycompany.test.db;"),
              RemovedLine(""),
              RemovedLine("import junit.framework.Test;"),
              RemovedLine("import junit.framework.TestCase;"),
              RemovedLine("import junit.framework.TestSuite;"),
              RemovedLine(""),
              RemovedLine(""),
              RemovedLine("/**"),
              RemovedLine(" * Unit test for simple App."),
              RemovedLine(" */"),
              RemovedLine("public class AppTest"),
              RemovedLine("    extends TestCase"),
              RemovedLine("{"),
              RemovedLine("    /**"),
              RemovedLine("     * Create the test case"),
              RemovedLine("     *"),
              RemovedLine("     * @param testName name of the test case"),
              RemovedLine("     */"),
              RemovedLine("    public AppTest( String testName )"),
              RemovedLine("    {"),
              RemovedLine("        super( testName );"),
              RemovedLine("    }"),
              RemovedLine(""),
              RemovedLine("    /**"),
              RemovedLine("     * @return the suite of tests being tested"),
              RemovedLine("     */"),
              RemovedLine("    public static Test suite()"),
              RemovedLine("    {"),
              RemovedLine("        return new TestSuite( AppTest.class );"),
              RemovedLine("    }"),
              RemovedLine(""),
              RemovedLine("    /**"),
              RemovedLine("     * Rigourous Test :-)"),
              RemovedLine("     */"),
              RemovedLine("    public void testApp()"),
              RemovedLine("    {"),
              RemovedLine("        ch.mycompany.test.db.App.main(new String[]{});"),
              RemovedLine("        assertTrue( true );"),
              RemovedLine("    }"),
              RemovedLine("}")
            )
          ))
        )
      )
    }

    "parse a file change with two chunks" in {
      val fileChangeWithTwoHunks =
        """diff --git a/multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java b/multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java
          |index 356f4b7..f5b8743 100755
          |--- a/multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java
          |+++ b/multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java
          |@@ -26,2 +26,2 @@ public class App
          |-        int i = 100023;
          |-        System.exit(-1);
          |+        int i = 100024;
          |+         System.exit(-1);
          |@@ -40,0 +41,7 @@ public class App
          |+
          |+    private void foo2() {
          |+        int baba = 5555;
          |+        //TODO fix this
          |+    }
          |+
          |+
          |""".stripMargin
      gitDiff must succeedOn(fileChangeWithTwoHunks).withResult(
        GitDiff(
          gitDiffHeader = FileChange(
            oldFile = "multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java",
            newFile = "multimod/src/gui/src/main/java/ch/mycompany/test/gui/App.java"
          ),
          header = ExtendedDiffHeader(
            headerLines = List(),
            index = Index(fromHash = "356f4b7", toHash = "f5b8743", mode = Some(100755))),
          hunks = List(Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 26, fromNumLines = 2, toLineStart = 26, toNumLines = 2),
              context = Some(CtxLine("public class App"))
            ),
            changedLines = List(
              RemovedLine("        int i = 100023;"),
              RemovedLine("        System.exit(-1);"),
              AddedLine("        int i = 100024;"),
              AddedLine("         System.exit(-1);")
            )
          ), Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 40, fromNumLines = 0, toLineStart = 41, toNumLines = 7),
              context = Some(CtxLine("public class App"))
            ),
            changedLines = List(
              AddedLine(""),
              AddedLine("    private void foo2() {"),
              AddedLine("        int baba = 5555;"),
              AddedLine("        //TODO fix this"),
              AddedLine("    }"),
              AddedLine(""),
              AddedLine("")
            )
          ))
        )
      )
    }

    "parse a renamed file with similarity" in {
      val renamedFile =
        """diff --git a/builtin-http-fetch.c b/http-fetch.c
          |similarity index 95%
          |rename from builtin-http-fetch.c
          |rename to http-fetch.c
          |index f3e63d7..e8f44ba 100644
          |--- a/builtin-http-fetch.c
          |+++ b/http-fetch.c
          |@@ -1,8 +1,9 @@
          | #include "cache.h"
          | #include "walker.h"
          |-int cmd_http_fetch(int argc, const char **argv, const char *prefix)
          |+int main(int argc, const char **argv)
          | {
          |+       const char *prefix;
          |        struct walker *walker;
          |        int commits_on_stdin = 0;
          |        int commits;
          |@@ -18,6 +19,8 @@ int cmd_http_fetch(int argc, const char **argv, const char *prefix)
          |        int get_verbosely = 0;
          |        int get_recover = 0;
          |+       prefix = setup_git_directory();
          |+
          |        git_config(git_default_config, NULL);
          |        while (arg < argc && argv[arg][0] == '-') {
          |""".stripMargin
      gitDiff must succeedOn(renamedFile).withResult(
        GitDiff(
          gitDiffHeader = FileChange(
            oldFile = "builtin-http-fetch.c",
            newFile = "http-fetch.c"
          ),
          header = ExtendedDiffHeader(
            headerLines = List(
              SimilarityIndex(percentage = 95),
              RenameFrom("builtin-http-fetch.c"),
              RenameTo("http-fetch.c")
            ),
            index = Index(fromHash = "f3e63d7", toHash = "e8f44ba", mode = Some(100644))),
          hunks = List(Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 1, fromNumLines = 8, toLineStart = 1, toNumLines = 9),
              context = None
            ),
            changedLines = List(
              CtxLine("""#include "cache.h""""),
              CtxLine("""#include "walker.h""""),
              RemovedLine("int cmd_http_fetch(int argc, const char **argv, const char *prefix)"),
              AddedLine("int main(int argc, const char **argv)"),
              CtxLine("{"),
              AddedLine("       const char *prefix;"),
              CtxLine("       struct walker *walker;"),
              CtxLine("       int commits_on_stdin = 0;"),
              CtxLine("       int commits;")
            )
          ), Hunk(
            hunkHeader = HunkHeader(
              fromToRange = FromToRange(fromLineStart = 18, fromNumLines = 6, toLineStart = 19, toNumLines = 8),
              context = Some(CtxLine("int cmd_http_fetch(int argc, const char **argv, const char *prefix)"))
            ),
            changedLines = List(
              CtxLine("       int get_verbosely = 0;"),
              CtxLine("       int get_recover = 0;"),
              AddedLine("       prefix = setup_git_directory();"),
              AddedLine(""),
              CtxLine("       git_config(git_default_config, NULL);"),
              CtxLine("       while (arg < argc && argv[arg][0] == '-') {")
            )
          ))
        )
      )
    }

  }

  val parsers = GitDiffParser

}