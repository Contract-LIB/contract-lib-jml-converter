package jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.contractlib.jml.JMLTranslator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JMLTranslatorTests {

    @Test
    void testTranslateAbstractions() {
        String a = """
            (declare-abstractions
              ((Cache 0))
              (((Cache (Cache.entries (Map Key Entry))
                       (Cache.uniques (Set Int))))))
            """;

        String abs = """
            (declare-abstractions
              ((LinkedList 0))
              (((LinkedList (LinkedList.content (Seq Int))))))

            (define-contract LinkedList.add
              ((this (inout LinkedList))
               (v (in Int)))
              ((true
               (= (LinkedList.content this)
                  (seq.++
                    (old (LinkedList.content this))
                    (seq.unit v)
                  )
               )
              ))
            )
            """;
        String expected = """
            public interface LinkedList {
            
                 /*@ ghost Sort content;*/
            
                 /*@ normal_behavior
                      requires true;
                      ensures (LinkedList.content(this) == \\seq_concat(\\old(LinkedList.content(this)), \\seq_singleton(v)));
                   @*/
                     void add(Sort[name=Int, arguments=[]] v) {
                 }
             }
            """;
        String outerView = JMLTranslator.translate(abs, false);
        Assertions.assertEquals(expected, outerView);
    }

    @Test
    void testJMLParser() {
        String cls = """
            public class C {
                //@ private ghost \\seq absValue;
            }
            """;

        ParserConfiguration config = new ParserConfiguration();
        config.setKeepJmlDocs(false);
        config.setProcessJml(true);
        config.setStoreTokens(true);
        JavaParser parser = new JavaParser(config);
        ParseResult<CompilationUnit> ast = parser.parse(cls);
        CompilationUnit cu = ast.getResult().get();
        System.out.println(cu);
    }
}
