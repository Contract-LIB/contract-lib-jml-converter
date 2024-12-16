package org.contractlib.jml;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.jml.body.JmlFieldDeclaration;
import com.github.javaparser.ast.jml.clauses.*;
import com.github.javaparser.ast.stmt.Behavior;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.contractlib.antlr4parser.ContractLIBLexer;
import org.contractlib.antlr4parser.ContractLIBParser;
import org.contractlib.ast.*;
import org.contractlib.factory.Mode;
import org.contractlib.parser.ContractLibANTLRParser;
import org.contractlib.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides static methods to translate from Contract-LIB to JML. At the moment, the
 * commands declare-abstractions and define-contract are supported.
 *
 * @author Wolfram Pfeifer
 */
public class JMLTranslator {
    /**
     * "End-to-end" translation from one String containing Contract-LIB to another one containing
     * JML. The result may contain multiple classes and compilation units.
     * @param contractLibInput the input String in Contract-LIB format
     * @param innerView If true, the inner view (implementation) skeleton is produced. Otherwise,
     *                  the interface is produced.
     * @return output String containing Java + JML
     */
    public static String translate(String contractLibInput, boolean innerView) {

        CharStream charStream = CharStreams.fromString(contractLibInput);
        ContractLIBLexer lexer = new ContractLIBLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ContractLIBParser parser = new ContractLIBParser(tokens);

        ContractLIBParser.ScriptContext ctx = parser.script();
        Factory factory = new Factory();
        ContractLibANTLRParser<Term, Type, Abstraction, Datatype, FunDec, Command> converter = new ContractLibANTLRParser<>(factory);
        converter.visit(ctx);

        if (innerView) {
            return InnerViewTranslator.translate(converter);
        } else {
            return OuterViewTranslator.translate(converter);
        }
    }
}
